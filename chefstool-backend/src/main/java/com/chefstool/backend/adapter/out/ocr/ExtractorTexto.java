package com.chefstool.backend.adapter.out.ocr;

import com.chefstool.backend.application.DocumentoIlegibleException;
import com.chefstool.backend.application.ParserAlbaran;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ExtractorDocumento;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Lector local: extrae el texto del documento (PDF o imagen) y lo interpreta con reglas.
 *
 * <ul>
 *   <li>PDF digital: se extrae el texto embebido (sin OCR, maxima precision).
 *   <li>PDF escaneado: se convierten sus paginas en imagenes y se pasan por OCR.
 *   <li>Imagen: OCR con Tesseract si esta instalado.
 *   <li>Sin archivo (origen=demo): se delega en {@link ExtractorDemo}.
 * </ul>
 */
@Service
public class ExtractorTexto implements ExtractorDocumento {

    /** Un albaran tiene una o dos paginas; no merece la pena procesar mas. */
    private static final int MAXIMO_PAGINAS_OCR = 3;

    private static final int DPI_OCR = 300;

    private final LectorPdf lectorPdf;
    private final LectorImagen lectorImagen;
    private final ParserAlbaran parser;
    private final ExtractorDemo demo;

    public ExtractorTexto(
            LectorPdf lectorPdf,
            LectorImagen lectorImagen,
            ParserAlbaran parser,
            ExtractorDemo demo) {
        this.lectorPdf = lectorPdf;
        this.lectorImagen = lectorImagen;
        this.parser = parser;
        this.demo = demo;
    }

    @Override
    public ResultadoExtraccion extraer(
            byte[] contenido, String nombreOriginal, String tipoMime, UUID organizacionId) {

        if (contenido == null || contenido.length == 0) {
            return demo.extraer(contenido, nombreOriginal, tipoMime, organizacionId);
        }

        String texto;
        if (lectorPdf.esPdf(contenido)) {
            texto = lectorPdf.extraerTexto(contenido);
            if (texto == null || texto.isBlank()) {
                texto = ocrDePdfEscaneado(contenido);
            }
        } else {
            if (!lectorImagen.sePuedeDecodificar(contenido)) {
                throw new DocumentoIlegibleException(
                        "El archivo no es una imagen que se pueda leer (los formatos WebP y HEIC no "
                                + "estan soportados). Sube la foto en JPG o PNG, o el PDF del "
                                + "proveedor.");
            }
            texto =
                    lectorImagen
                            .extraerTexto(contenido)
                            .orElseThrow(
                                    () ->
                                            new DocumentoIlegibleException(
                                                    "No hay OCR de imagenes disponible en este "
                                                            + "servidor. Instala Tesseract "
                                                            + "(brew install tesseract "
                                                            + "tesseract-lang) o sube un PDF con "
                                                            + "texto."));
        }

        ResultadoExtraccion resultado = parser.interpretar(texto, organizacionId);

        if (resultado.extraccion().lineas().isEmpty()
                && resultado.extraccion().proveedorNombre() == null) {
            throw new DocumentoIlegibleException(
                    "Se ha leido el documento pero no se han reconocido lineas ni proveedor. "
                            + "Revisalo y completalo a mano.");
        }

        return resultado;
    }

    /** PDF sin texto embebido: se rasterizan las paginas y se les pasa el OCR. */
    private String ocrDePdfEscaneado(byte[] contenido) {
        StringBuilder texto = new StringBuilder();
        for (byte[] pagina : lectorPdf.paginasComoPng(contenido, MAXIMO_PAGINAS_OCR, DPI_OCR)) {
            lectorImagen
                    .extraerTexto(pagina)
                    .filter(leido -> !leido.isBlank())
                    .ifPresent(leido -> texto.append(leido).append('\n'));
        }

        if (texto.isEmpty()) {
            throw new DocumentoIlegibleException(
                    "El PDF parece un escaneo y no se ha podido leer por OCR. "
                            + "Sube una foto del albaran o revisalo a mano.");
        }
        return texto.toString();
    }
}
