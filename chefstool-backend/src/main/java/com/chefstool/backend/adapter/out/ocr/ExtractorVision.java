package com.chefstool.backend.adapter.out.ocr;

import com.chefstool.backend.application.DocumentoIlegibleException;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ExtractorDocumento;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Lee albaranes con un modelo de vision (Google Gemini u otro compatible con la API
 * "generateContent"). Es opcional: si no hay clave configurada, la aplicacion usa el
 * lector local con Tesseract.
 *
 * <p>No se envia nada a ningun servicio salvo que {@code chefstool.ocr.vision.clave}
 * tenga valor.
 */
@Component
public class ExtractorVision {

    private static final Logger log = LoggerFactory.getLogger(ExtractorVision.class);

    private static final int ANCHO_MAXIMO = 1600;

    /**
     * Las fotos que llegan por mensajeria rondan los 768 px de ancho y el texto de un ticket
     * a ese tamano no se distingue: se amplian antes de mandarlas al modelo.
     */
    private static final int ANCHO_MINIMO = 1400;

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.02");

    private static final String INSTRUCCIONES =
            """
            Eres un asistente que lee albaranes y facturas de proveedores de hosteleria.
            Devuelve SOLO un JSON valido, sin explicaciones ni markdown, con esta forma exacta:
            {"proveedor":"","numeroDocumento":"","fecha":"AAAA-MM-DD","moneda":"EUR",
             "importeNeto":0,"importeIva":0,"importeTotal":0,
             "lineas":[{"descripcion":"","cantidad":0,"unidad":"","precioUnitario":0,"importe":0}]}
            Reglas: usa null si el dato no aparece; importes con punto decimal y sin simbolo;
            una entrada por linea de articulo; incluye tambien como linea cualquier cargo con
            importe (portes, transporte, envases o botes retornables, tasas, recargos, gastos
            varios) y los descuentos con importe negativo; no inventes lineas que no esten en
            el documento y no incluyas como linea la base imponible, el IVA ni el total.
            Antes de responder comprueba que la suma de los importes de las lineas coincide con
            la base imponible: si no coincide, vuelve a leer el documento y corrige las lineas
            que falten o que esten mal leidas.
            """;

    private final String url;
    private final String clave;
    private final String modelo;

    /** Modelo mas capaz que se usa solo cuando la primera lectura no cuadra (opcional). */
    private final String modeloCorreccion;

    private final ObjectMapper json;
    private final RestClient cliente;
    private final LectorPdf lectorPdf;

    public ExtractorVision(
            @Value("${chefstool.ocr.vision.url:https://generativelanguage.googleapis.com/v1beta}")
                    String url,
            @Value("${chefstool.ocr.vision.clave:}") String clave,
            @Value("${chefstool.ocr.vision.modelo:gemini-3.5-flash}") String modelo,
            @Value("${chefstool.ocr.vision.modelo.correccion:}") String modeloCorreccion,
            ObjectMapper json,
            LectorPdf lectorPdf) {
        this.url = url.replaceAll("/+$", "");
        this.clave = clave;
        this.modelo = modelo;
        this.modeloCorreccion = modeloCorreccion == null ? "" : modeloCorreccion.trim();
        this.json = json;
        this.lectorPdf = lectorPdf;
        this.cliente = RestClient.builder().baseUrl(this.url).build();
    }

    public boolean disponible() {
        return clave != null && !clave.isBlank();
    }

    public ResultadoExtraccion extraer(
            byte[] contenido, String nombreOriginal, String tipoMime, UUID organizacionId) {
        if (!disponible()) {
            throw new DocumentoIlegibleException("La lectura con IA no esta configurada");
        }
        if (contenido == null || contenido.length == 0) {
            throw new DocumentoIlegibleException("No hay documento que leer");
        }

        List<String> imagenes = imagenesDelDocumento(contenido);
        if (imagenes.isEmpty()) {
            throw new DocumentoIlegibleException("No se pudo preparar el documento para la IA");
        }

        ResultadoExtraccion primera = interpretar(llamarAlModelo(imagenes, modelo), organizacionId);
        if (!hayQueReintentar(primera.extraccion())) {
            return primera;
        }
        if (modeloCorreccion.isBlank() || modeloCorreccion.equals(modelo)) {
            log.debug("La lectura no cuadra y no hay modelo de correccion configurado");
            return primera;
        }

        log.info(
                "La lectura no cuadra ({} lineas); se reintenta con {}",
                primera.extraccion().lineas().size(),
                modeloCorreccion);
        ResultadoExtraccion segunda =
                interpretar(llamarAlModelo(imagenes, modeloCorreccion), organizacionId);
        return elegir(primera, segunda);
    }

    /** Se queda con la lectura que menos se aleja de la base imponible del documento. */
    ResultadoExtraccion elegir(ResultadoExtraccion primera, ResultadoExtraccion segunda) {
        if (segunda.extraccion().lineas().isEmpty()) {
            return primera;
        }
        return desvio(segunda.extraccion()) <= desvio(primera.extraccion()) ? segunda : primera;
    }

    /** Solo se paga una segunda lectura cuando la primera no cuadra o no trae lineas. */
    boolean hayQueReintentar(ExtraccionDocumento extraccion) {
        if (extraccion == null || extraccion.lineas().isEmpty()) {
            return true;
        }
        BigDecimal neto = extraccion.importeNeto();
        if (neto == null || neto.signum() <= 0) {
            return true;
        }
        return sumaDeLineas(extraccion).subtract(neto).abs().compareTo(TOLERANCIA) > 0;
    }

    /** Cuanto se aleja la suma de las lineas de la base imponible: cuanto menos, mejor. */
    double desvio(ExtraccionDocumento extraccion) {
        BigDecimal neto = extraccion.importeNeto();
        if (neto != null && neto.signum() > 0) {
            return sumaDeLineas(extraccion).subtract(neto).abs().doubleValue();
        }
        return 1_000_000 - extraccion.lineas().size();
    }

    private BigDecimal sumaDeLineas(ExtraccionDocumento extraccion) {
        return extraccion.lineas().stream()
                .map(LineaExtraccion::importe)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Devuelve las imagenes en base64: la foto tal cual o las paginas del PDF. */
    private List<String> imagenesDelDocumento(byte[] contenido) {
        if (lectorPdf.esPdf(contenido)) {
            List<String> imagenes = new ArrayList<>();
            for (byte[] pagina : lectorPdf.paginasComoPng(contenido, 2, 200)) {
                imagenes.add(Base64.getEncoder().encodeToString(imagenesDelPng(pagina)));
            }
            return imagenes;
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(contenido));
            if (original == null) {
                throw new DocumentoIlegibleException("La imagen no se puede abrir");
            }
            return List.of(Base64.getEncoder().encodeToString(imagenesDelPng(original)));
        } catch (IOException ex) {
            throw new DocumentoIlegibleException("La imagen no se puede abrir: " + ex.getMessage());
        }
    }

    private byte[] imagenesDelPng(byte[] png) {
        try {
            return imagenesDelPng(ImageIO.read(new ByteArrayInputStream(png)));
        } catch (IOException ex) {
            throw new DocumentoIlegibleException("La imagen no se puede abrir");
        }
    }

    private byte[] imagenesDelPng(BufferedImage original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        int nuevoAncho = ancho;
        if (ancho > ANCHO_MAXIMO) {
            nuevoAncho = ANCHO_MAXIMO;
        } else if (ancho < ANCHO_MINIMO) {
            nuevoAncho = (int) Math.round(ancho * Math.min(ANCHO_MAXIMO / (double) ancho, 3.0));
        }

        BufferedImage destino = original;
        if (nuevoAncho != ancho) {
            int nuevoAlto = Math.max((int) Math.round((double) alto * nuevoAncho / ancho), 1);
            destino = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
            var g = destino.createGraphics();
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
            g.dispose();
        }

        return aJpeg(destino);
    }

    /** JPEG con calidad alta: recomprimir otra vez lo que ya venia comprimido se nota. */
    private byte[] aJpeg(BufferedImage imagen) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
            var salidaImagen = ImageIO.createImageOutputStream(salida);
            escritor.setOutput(salidaImagen);
            var parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(0.95f);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
            escritor.dispose();
            salidaImagen.close();
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new DocumentoIlegibleException("No se pudo convertir la imagen");
        }
    }

    private JsonNode llamarAlModelo(List<String> imagenesBase64, String modelo) {
        try {
            String texto =
                    esOpenAi()
                            ? peticionOpenAi(imagenesBase64, modelo)
                            : peticionGemini(imagenesBase64, modelo);
            if (texto == null || texto.isBlank()) {
                throw new DocumentoIlegibleException("La IA no devolvio texto para este documento");
            }
            return json.readTree(limpiarJson(texto));
        } catch (DocumentoIlegibleException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Fallo la lectura con IA: {}", ex.getMessage());
            throw new DocumentoIlegibleException(
                    "No se pudo consultar el servicio de lectura con IA: " + ex.getMessage());
        }
    }

    /** OpenAI usa /chat/completions con las imagenes como data URL. */
    private boolean esOpenAi() {
        return url.toLowerCase().contains("openai");
    }

    private String peticionOpenAi(List<String> imagenesBase64, String modelo) {
        List<Object> contenido = new ArrayList<>();
        contenido.add(java.util.Map.of("type", "text", "text", INSTRUCCIONES));
        for (String imagen : imagenesBase64) {
            contenido.add(
                    java.util.Map.of(
                            "type",
                            "image_url",
                            "image_url",
                            java.util.Map.of("url", "data:image/jpeg;base64," + imagen)));
        }

        var cuerpo =
                java.util.Map.of(
                        "model", modelo,
                        "temperature", 0,
                        "response_format", java.util.Map.of("type", "json_object"),
                        "messages", List.of(java.util.Map.of("role", "user", "content", contenido)));

        String respuesta =
                cliente.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo)
                        .retrieve()
                        .body(String.class);

        return json.readTree(respuesta).path("choices").path(0).path("message").path("content").asText();
    }

    private String peticionGemini(List<String> imagenesBase64, String modelo) {
        List<Object> partes = new ArrayList<>();
        partes.add(java.util.Map.of("text", INSTRUCCIONES));
        for (String imagen : imagenesBase64) {
            partes.add(
                    java.util.Map.of(
                            "inline_data", java.util.Map.of("mime_type", "image/jpeg", "data", imagen)));
        }

        var cuerpo =
                java.util.Map.of(
                        "contents", List.of(java.util.Map.of("parts", partes)),
                        "generationConfig",
                                java.util.Map.of(
                                        "temperature", 0, "responseMimeType", "application/json"));

        String respuesta =
                cliente.post()
                        .uri("/models/{modelo}:generateContent", modelo)
                        .header("x-goog-api-key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo)
                        .retrieve()
                        .body(String.class);

        JsonNode raiz = json.readTree(respuesta);
        JsonNode texto =
                raiz.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        return texto.isMissingNode() ? null : texto.asText();
    }

    /** Los modelos a veces envuelven el JSON en ```json ... ```. */
    String limpiarJson(String texto) {
        String limpio = texto.trim();
        if (limpio.startsWith("```")) {
            int salto = limpio.indexOf('\n');
            limpio = salto >= 0 ? limpio.substring(salto + 1) : limpio;
            int cierre = limpio.lastIndexOf("```");
            if (cierre >= 0) {
                limpio = limpio.substring(0, cierre);
            }
        }
        int inicio = limpio.indexOf('{');
        int fin = limpio.lastIndexOf('}');
        if (inicio >= 0 && fin > inicio) {
            limpio = limpio.substring(inicio, fin + 1);
        }
        return limpio.trim();
    }

    ResultadoExtraccion interpretar(JsonNode datos, UUID organizacionId) {
        if (datos == null || datos.isNull()) {
            throw new DocumentoIlegibleException("La IA no devolvio datos");
        }

        String proveedor = texto(datos, "proveedor");
        String numero = texto(datos, "numeroDocumento");
        LocalDate fecha = fecha(texto(datos, "fecha"));
        String moneda = texto(datos, "moneda") == null ? "EUR" : texto(datos, "moneda");

        List<LineaExtraccion> lineas = new ArrayList<>();
        JsonNode articulos = datos.path("lineas");
        if (articulos.isArray()) {
            for (JsonNode articulo : articulos) {
                String descripcion = texto(articulo, "descripcion");
                if (descripcion == null || descripcion.isBlank()) {
                    continue;
                }
                BigDecimal cantidad = decimal(articulo, "cantidad");
                BigDecimal precio = decimal(articulo, "precioUnitario");
                BigDecimal importe = decimal(articulo, "importe");
                if (importe == null && cantidad != null && precio != null) {
                    importe = cantidad.multiply(precio).setScale(2, RoundingMode.HALF_UP);
                }
                lineas.add(
                        new LineaExtraccion(
                                descripcion.trim(),
                                cantidad,
                                texto(articulo, "unidad"),
                                precio,
                                importe,
                                null,
                                null,
                                null));
            }
        }

        BigDecimal neto = decimal(datos, "importeNeto");
        BigDecimal iva = decimal(datos, "importeIva");
        BigDecimal total = decimal(datos, "importeTotal");

        BigDecimal sumaLíneas =
                lineas.stream()
                        .map(LineaExtraccion::importe)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (neto == null) {
            neto = sumaLíneas;
        }
        if (total == null) {
            total = neto.add(iva == null ? BigDecimal.ZERO : iva);
        }

        if (proveedor == null && lineas.isEmpty() && total.signum() == 0) {
            throw new DocumentoIlegibleException(
                    "La IA no ha reconocido datos en el documento");
        }

        ExtraccionDocumento extraccion =
                new ExtraccionDocumento(
                        proveedor,
                        null,
                        numero,
                        fecha,
                        moneda,
                        neto.setScale(2, RoundingMode.HALF_UP),
                        (iva == null ? BigDecimal.ZERO : iva).setScale(2, RoundingMode.HALF_UP),
                        total.setScale(2, RoundingMode.HALF_UP),
                        lineas);

        return new ResultadoExtraccion(extraccion, confianza(proveedor, fecha, numero, lineas, total));
    }

    private BigDecimal confianza(
            String proveedor,
            LocalDate fecha,
            String numero,
            List<LineaExtraccion> lineas,
            BigDecimal total) {
        double confianza = 0.2;
        if (proveedor != null) {
            confianza += 0.2;
        }
        if (fecha != null) {
            confianza += 0.1;
        }
        if (numero != null) {
            confianza += 0.05;
        }
        if (!lineas.isEmpty()) {
            confianza += 0.3;
        }
        if (total != null && total.signum() > 0) {
            confianza += 0.15;
        }
        return BigDecimal.valueOf(Math.min(0.98, confianza)).setScale(2, RoundingMode.HALF_UP);
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        String texto = valor.asText().trim();
        return texto.isEmpty() || "null".equalsIgnoreCase(texto) ? null : texto;
    }

    private BigDecimal decimal(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        if (valor.isNumber()) {
            return valor.decimalValue();
        }
        String texto = valor.asText().replace("€", "").replace(" ", "").trim();
        if (texto.isEmpty() || "null".equalsIgnoreCase(texto)) {
            return null;
        }
        // "1.234,56" o "1,234.56" o "12,50"
        if (texto.contains(",") && texto.contains(".")) {
            texto =
                    texto.lastIndexOf(',') > texto.lastIndexOf('.')
                            ? texto.replace(".", "").replace(",", ".")
                            : texto.replace(",", "");
        } else if (texto.contains(",")) {
            texto = texto.replace(",", ".");
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate fecha(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
