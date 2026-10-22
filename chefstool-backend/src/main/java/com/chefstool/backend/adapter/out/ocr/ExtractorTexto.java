package com.chefstool.backend.adapter.out.ocr;

import com.chefstool.backend.application.DocumentoIlegibleException;
import com.chefstool.backend.application.ParserAlbaran;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ExtractorDocumento;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Extractor principal: lee el texto real del documento y lo interpreta.
 *
 * <ul>
 *   <li>PDF digital: se extrae el texto embebido (sin OCR, maxima precision).
 *   <li>Imagen o PDF escaneado: OCR con Tesseract si esta instalado.
 *   <li>Sin archivo (origen=demo): se delega en {@link ExtractorDemo}.
 * </ul>
 */
@Service
@Primary
public class ExtractorTexto implements ExtractorDocumento {

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
                throw new DocumentoIlegibleException(
                        "El PDF no tiene texto seleccionable (parece un escaneo). "
                                + "Sube una foto o instala Tesseract para leerlo por OCR.");
            }
        } else {
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
}
