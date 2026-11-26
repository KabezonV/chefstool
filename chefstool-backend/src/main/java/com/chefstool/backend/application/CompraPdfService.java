package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.dto.CompraDetalle;
import com.chefstool.backend.adapter.in.web.dto.LineaCompraResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

/** Resumen imprimible de una compra confirmada: cabecera, lineas y totales. */
@Service
public class CompraPdfService {

    private static final float MARGEN = 50;
    private static final float ANCHO_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEN;
    private static final float ALTO = PDRectangle.A4.getHeight();
    private static final Locale ES = Locale.forLanguageTag("es-ES");

    private final CompraService compraService;

    public CompraPdfService(CompraService compraService) {
        this.compraService = compraService;
    }

    public byte[] generar(UUID compraId) {
        CompraDetalle compra = compraService.obtener(compraId);

        try (PDDocument documento = new PDDocument();
                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            PDPageContentStream contenido = new PDPageContentStream(documento, pagina);
            float y = ALTO - MARGEN;

            try {
                y = escribir(contenido, negrita, 18, MARGEN, y, "Albaran de compra");
                y -= 18;
                y = escribir(contenido, normal, 10, MARGEN, y, "Proveedor: " + texto(compra.proveedorNombre()));
                y -= 14;
                String numero = compra.numeroDocumento() == null
                        ? "sin numero"
                        : compra.numeroDocumento();
                y = escribir(contenido, normal, 10, MARGEN, y, "Documento: " + numero);
                y -= 14;
                String fecha = compra.fecha() == null
                        ? "-"
                        : compra.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                escribir(contenido, normal, 10, MARGEN, y, "Fecha: " + fecha);

                y -= 26;
                y = cabeceraTabla(contenido, negrita, y);

                for (LineaCompraResponse linea : compra.lineas()) {
                    if (y < MARGEN + 110) {
                        contenido.close();
                        pagina = new PDPage(PDRectangle.A4);
                        documento.addPage(pagina);
                        contenido = new PDPageContentStream(documento, pagina);
                        y = ALTO - MARGEN;
                        y = escribir(contenido, negrita, 12, MARGEN, y, "Albaran de compra (continuacion)");
                        y -= 20;
                        y = cabeceraTabla(contenido, negrita, y);
                    }
                    y = fila(contenido, normal, y, linea);
                }

                contenido.setLineWidth(0.8f);
                contenido.moveTo(MARGEN, y + 6);
                contenido.lineTo(MARGEN + ANCHO_UTIL, y + 6);
                contenido.stroke();

                y -= 10;
                escribir(contenido, normal, 11, MARGEN + 300, y, "Base imponible");
                escribir(contenido, normal, 11, MARGEN + 420, y, euros(compra.importeNeto()));
                y -= 16;
                escribir(contenido, normal, 11, MARGEN + 300, y, "IVA");
                escribir(contenido, normal, 11, MARGEN + 420, y, euros(compra.importeIva()));
                y -= 18;
                escribir(contenido, negrita, 12, MARGEN + 300, y, "Total");
                escribir(contenido, negrita, 12, MARGEN + 420, y, euros(compra.importeTotal()));

                escribir(
                        contenido,
                        normal,
                        8,
                        MARGEN,
                        MARGEN,
                        "Generado con Chefstool el "
                                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                + ". El original del albaran se descarga desde la ficha del documento.");
            } finally {
                contenido.close();
            }

            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la compra", ex);
        }
    }

    private float cabeceraTabla(
            PDPageContentStream contenido, PDType1Font negrita, float y) throws IOException {
        contenido.setLineWidth(0.8f);
        contenido.moveTo(MARGEN, y + 12);
        contenido.lineTo(MARGEN + ANCHO_UTIL, y + 12);
        contenido.stroke();

        escribir(contenido, negrita, 10, MARGEN, y, "Descripcion");
        escribir(contenido, negrita, 10, MARGEN + 250, y, "Cantidad");
        escribir(contenido, negrita, 10, MARGEN + 340, y, "Precio");
        escribir(contenido, negrita, 10, MARGEN + 420, y, "Importe");

        contenido.moveTo(MARGEN, y - 6);
        contenido.lineTo(MARGEN + ANCHO_UTIL, y - 6);
        contenido.stroke();
        return y - 20;
    }

    private float fila(
            PDPageContentStream contenido, PDType1Font normal, float y, LineaCompraResponse linea)
            throws IOException {
        String descripcion = linea.descripcion();
        if (descripcion == null || descripcion.isBlank()) {
            descripcion = linea.productoNombre();
        }
        escribir(contenido, normal, 10, MARGEN, y, recortar(descripcion, 44));
        escribir(contenido, normal, 10, MARGEN + 250, y, cantidad(linea));
        escribir(contenido, normal, 10, MARGEN + 340, y, euros(linea.precioUnitario()));
        escribir(contenido, normal, 10, MARGEN + 420, y, euros(linea.importe()));
        return y - 16;
    }

    private float escribir(
            PDPageContentStream contenido,
            PDType1Font fuente,
            float tamano,
            float x,
            float y,
            String texto) throws IOException {
        String seguro = textoSeguro(texto);
        if (seguro.isBlank()) {
            return y;
        }
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(seguro);
        contenido.endText();
        return y;
    }

    /** Las fuentes basicas del PDF no admiten cualquier caracter: se limpia lo que no cabe. */
    private String textoSeguro(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder salida = new StringBuilder(texto.length());
        for (char caracter : texto.toCharArray()) {
            boolean latino = caracter >= 32 && caracter <= 255;
            salida.append(latino || caracter == '€' ? caracter : ' ');
        }
        return salida.toString().trim();
    }

    private String recortar(String texto, int maximo) {
        String seguro = textoSeguro(texto);
        return seguro.length() <= maximo ? seguro : seguro.substring(0, maximo - 1) + ".";
    }

    private String cantidad(LineaCompraResponse linea) {
        String numero = linea.cantidad() == null
                ? "-"
                : NumberFormat.getNumberInstance(ES).format(linea.cantidad());
        return linea.unidad() == null || linea.unidad().isBlank()
                ? numero
                : numero + " " + textoSeguro(linea.unidad());
    }

    private String euros(BigDecimal importe) {
        return NumberFormat.getCurrencyInstance(ES).format(importe == null ? BigDecimal.ZERO : importe);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }
}
