package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.dto.FichaDetalle;
import com.chefstool.backend.adapter.in.web.dto.FichaLineaResponse;
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

/** Genera la ficha tecnica en PDF para imprimirla en cocina. */
@Service
public class FichaPdfService {

    private static final float MARGEN = 50;
    private static final float ANCHO_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEN;
    private static final Locale ES = Locale.forLanguageTag("es-ES");

    private final FichaService fichaService;

    public FichaPdfService(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    public byte[] generar(UUID fichaId, String organizacion) {
        FichaDetalle ficha = fichaService.obtener(fichaId);

        try (PDDocument documento = new PDDocument();
                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);

            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                float y = PDRectangle.A4.getHeight() - MARGEN;

                y = escribir(contenido, negrita, 18, MARGEN, y, "Ficha tecnica");
                y = escribir(contenido, normal, 10, MARGEN, y - 16, fecha());
                if (organizacion != null && !organizacion.isBlank()) {
                    y = escribir(contenido, negrita, 10, MARGEN, y, organizacion);
                }

                y -= 26;
                y = escribir(contenido, negrita, 16, MARGEN, y, ficha.nombre());
                if (ficha.descripcion() != null && !ficha.descripcion().isBlank()) {
                    y = escribir(contenido, normal, 10, MARGEN, y - 14, ficha.descripcion());
                }

                // Datos economicos
                y -= 24;
                float mitad = MARGEN + ANCHO_UTIL / 2;
                escribir(contenido, normal, 10, MARGEN, y, "Raciones: " + valor(ficha.raciones()));
                escribir(contenido, normal, 10, mitad, y, "Coste total: " + euros(ficha.costeTotal()));
                y -= 16;
                escribir(contenido, normal, 10, MARGEN, y, "Precio de venta: " + euros(ficha.precioVenta()));
                escribir(contenido, normal, 10, mitad, y, "Coste por racion: " + euros(ficha.costePorRacion()));
                y -= 16;
                escribir(contenido, negrita, 11, MARGEN, y, "Margen: " + porcentaje(ficha.margenPct()));
                if (ficha.productosSinPrecio() > 0) {
                    escribir(
                            contenido,
                            normal,
                            9,
                            mitad,
                            y,
                            "(!) " + ficha.productosSinPrecio() + " ingrediente(s) sin precio");
                }

                // Cabecera de la tabla
                y -= 30;
                contenido.setLineWidth(0.8f);
                contenido.moveTo(MARGEN, y + 12);
                contenido.lineTo(MARGEN + ANCHO_UTIL, y + 12);
                contenido.stroke();

                escribir(contenido, negrita, 10, MARGEN, y, "Ingrediente");
                escribir(contenido, negrita, 10, MARGEN + 250, y, "Cantidad");
                escribir(contenido, negrita, 10, MARGEN + 330, y, "Precio");
                escribir(contenido, negrita, 10, MARGEN + 410, y, "Coste");

                y -= 14;
                contenido.moveTo(MARGEN, y + 10);
                contenido.lineTo(MARGEN + ANCHO_UTIL, y + 10);
                contenido.stroke();
                y -= 6;

                for (FichaLineaResponse linea : ficha.lineas()) {
                    if (y < MARGEN + 60) {
                        break;
                    }
                    String nombre = recortar(linea.descripcion() != null
                            ? linea.descripcion()
                            : linea.productoNombre(), 42);
                    escribir(contenido, normal, 10, MARGEN, y, nombre);
                    escribir(contenido, normal, 10, MARGEN + 250, y, cantidad(linea));
                    escribir(contenido, normal, 10, MARGEN + 330, y, euros(linea.precioUnitario()));
                    escribir(contenido, normal, 10, MARGEN + 410, y, euros(linea.coste()));
                    y -= 18;
                }

                // Pie
                escribir(
                        contenido,
                        normal,
                        8,
                        MARGEN,
                        MARGEN,
                        "Generado con Chefstool. Los costes usan el mejor precio de compra disponible.");
            }

            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ex.getMessage(), ex);
        }
    }

    private float escribir(
            PDPageContentStream contenido,
            PDType1Font fuente,
            float tamano,
            float x,
            float y,
            String texto) {
        if (texto == null || texto.isBlank()) {
            return y;
        }
        try {
            contenido.beginText();
            contenido.setFont(fuente, tamano);
            contenido.newLineAtOffset(x, y);
            contenido.showText(texto);
            contenido.endText();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo escribir en el PDF", ex);
        }
        return y;
    }

    private String fecha() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String valor(Integer numero) {
        return numero == null ? "-" : String.valueOf(numero);
    }

    private String euros(BigDecimal importe) {
        if (importe == null) {
            return "-";
        }
        NumberFormat formato = NumberFormat.getCurrencyInstance(ES);
        return formato.format(importe);
    }

    private String porcentaje(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        return String.format(ES, "%.1f %%", valor);
    }

    private String cantidad(FichaLineaResponse linea) {
        if (linea.cantidad() == null) {
            return "-";
        }
        String unidad = linea.unidad() == null ? "" : " " + linea.unidad();
        return linea.cantidad().stripTrailingZeros().toPlainString() + unidad;
    }

    private String recortar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
