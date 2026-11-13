package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.DocumentoCompra;
import com.chefstool.backend.domain.port.out.DocumentoCompraRepository;
import com.chefstool.backend.domain.port.out.AlmacenamientoArchivos;
import com.chefstool.backend.domain.port.out.ExtractorDocumento;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.adapter.in.web.ConflictoException;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.ArchivoDescargable;
import com.chefstool.backend.application.dto.Pagina;
import com.chefstool.backend.adapter.in.web.dto.CompraResponse;
import com.chefstool.backend.adapter.in.web.dto.DocumentoDetalle;
import com.chefstool.backend.adapter.in.web.dto.DocumentoResumen;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
public class DocumentoCompraService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoCompraService.class);

    /** Ancho de pagina del PDF que se genera al juntar varias hojas (A4 en puntos). */
    private static final float ANCHO_PAGINA_PDF = 595f;

    private final DocumentoCompraRepository documentoRepository;
    private final OrganizacionService organizacionService;
    private final AlmacenamientoArchivos almacenamiento;
    private final ExtractorDocumento extractor;
    private final CompraService compraService;
    private final ObjectMapper objectMapper;

    public DocumentoCompraService(
            DocumentoCompraRepository documentoRepository,
            OrganizacionService organizacionService,
            AlmacenamientoArchivos almacenamiento,
            ExtractorDocumento extractor,
            CompraService compraService,
            ObjectMapper objectMapper) {
        this.documentoRepository = documentoRepository;
        this.organizacionService = organizacionService;
        this.almacenamiento = almacenamiento;
        this.extractor = extractor;
        this.compraService = compraService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DocumentoDetalle subir(List<MultipartFile> archivos, String origen) {
        UUID organizacionId = organizacionService.organizacionActual();
        List<MultipartFile> utiles =
                archivos == null
                        ? List.of()
                        : archivos.stream()
                                .filter(archivo -> archivo != null && !archivo.isEmpty())
                                .toList();
        boolean demo = utiles.isEmpty();

        if (demo && !"demo".equalsIgnoreCase(origen)) {
            throw new IllegalArgumentException("Adjunta un albaran o usa el modo demostracion");
        }

        String nombre;
        String tipoMime;
        byte[] contenido;
        if (demo) {
            nombre = "albaran-demo.pdf";
            tipoMime = "application/pdf";
            contenido = new byte[0];
        } else if (utiles.size() == 1) {
            MultipartFile archivo = utiles.getFirst();
            nombre = archivo.getOriginalFilename();
            tipoMime = archivo.getContentType();
            contenido = leer(archivo);
        } else {
            // Varias hojas del mismo albaran: se juntan en un unico PDF para que la revision
            // sea una sola compra y no una por foto.
            nombre = nombreDeVariasHojas(utiles.getFirst().getOriginalFilename(), utiles.size());
            tipoMime = "application/pdf";
            contenido = juntarEnPdf(utiles);
        }

        DocumentoCompra documento = new DocumentoCompra();
        documento.setOrganizacionId(organizacionId);
        documento.setNombreOriginal(nombre);
        documento.setTipoMime(tipoMime);
        documento.setOrigen(demo ? "demo" : origen);
        documento.setEstado(DocumentoCompra.PENDIENTE_REVISION);

        DocumentoCompra guardado = documentoRepository.save(documento);

        if (!demo) {
            guardado.setRutaAlmacenamiento(
                    almacenamiento.guardar(
                            organizacionId, guardado.getId(), nombre, contenido));
        }

        try {
            ResultadoExtraccion resultado =
                    extractor.extraer(
                            contenido,
                            guardado.getNombreOriginal(),
                            guardado.getTipoMime(),
                            organizacionId);
            guardado.setExtraccionJson(serializar(resultado.extraccion()));
            guardado.setConfianzaExtraccion(resultado.confianza());
            guardado.setErrorExtraccion(null);
        } catch (RuntimeException ex) {
            log.warn("No se pudo extraer el documento {}", guardado.getId(), ex);
            guardado.setErrorExtraccion("No se pudo leer el documento: " + ex.getMessage());
        }

        return detalle(documentoRepository.saveAndFlush(guardado));
    }

    @Transactional(readOnly = true)
    public Pagina<DocumentoResumen> listar(
            int pagina, int tamano, String orden, String direccion) {
        Pageable pageable =
                Pagina.peticion(
                        pagina,
                        tamano,
                        Pagina.orden(
                                orden,
                                direccion,
                                Sort.by("createdAt").descending(),
                                "createdAt", "nombreOriginal", "estado", "confianzaExtraccion"));

        return Pagina.desde(
                        documentoRepository.findByOrganizacionId(
                                organizacionService.organizacionActual(), pageable))
                .map(DocumentoResumen::from);
    }

    @Transactional(readOnly = true)
    public DocumentoDetalle obtener(UUID id) {
        return detalle(buscar(id));
    }

    @Transactional
    public DocumentoDetalle guardarBorrador(UUID id, ExtraccionDocumento extraccion) {
        DocumentoCompra documento = buscarEditable(id);
        documento.setExtraccionJson(serializar(extraccion));
        return detalle(documentoRepository.save(documento));
    }

    @Transactional
    public CompraResponse confirmar(UUID id, ExtraccionDocumento extraccion) {
        DocumentoCompra documento = buscarEditable(id);

        CompraResponse compra = compraService.crearDesdeDocumento(documento, extraccion);

        documento.setExtraccionJson(serializar(extraccion));
        documento.setEstado(DocumentoCompra.CONFIRMADO);
        documentoRepository.save(documento);

        return compra;
    }

    @Transactional
    public void descartar(UUID id) {
        DocumentoCompra documento = buscarEditable(id);
        documento.setEstado(DocumentoCompra.DESCARTADO);
        documentoRepository.save(documento);
    }

    @Transactional(readOnly = true)
    public ArchivoDescargable archivo(UUID id) {
        DocumentoCompra documento = buscar(id);
        var recurso =
                almacenamiento
                        .cargar(documento.getRutaAlmacenamiento())
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "El documento no tiene archivo adjunto"));
        return new ArchivoDescargable(
                recurso, documento.getTipoMime(), documento.getNombreOriginal());
    }

    private DocumentoDetalle detalle(DocumentoCompra documento) {
        return new DocumentoDetalle(
                documento.getId(),
                documento.getNombreOriginal(),
                documento.getEstado(),
                documento.getOrigen(),
                documento.getConfianzaExtraccion(),
                documento.getCreatedAt(),
                documento.getRutaAlmacenamiento() != null,
                documento.getErrorExtraccion(),
                deserializar(documento.getExtraccionJson()));
    }

    private DocumentoCompra buscar(UUID id) {
        return documentoRepository
                .findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));
    }

    private DocumentoCompra buscarEditable(UUID id) {
        DocumentoCompra documento = buscar(id);
        if (DocumentoCompra.CONFIRMADO.equals(documento.getEstado())) {
            throw new ConflictoException("El documento ya esta confirmado");
        }
        if (DocumentoCompra.DESCARTADO.equals(documento.getEstado())) {
            throw new ConflictoException("El documento esta descartado");
        }
        return documento;
    }

    private byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo subido", ex);
        }
    }

    private String serializar(ExtraccionDocumento extraccion) {
        return extraccion == null ? null : objectMapper.writeValueAsString(extraccion);
    }

    private ExtraccionDocumento deserializar(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readValue(json, ExtraccionDocumento.class);
    }

    private String nombreDeVariasHojas(String primero, int hojas) {
        String base = primero == null ? "albaran" : primero.replaceAll("\\.[^.]+$", "");
        return "%s-%d-hojas.pdf".formatted(base, hojas);
    }

    /** Convierte varias fotos en un PDF de varias paginas, una por hoja. */
    private byte[] juntarEnPdf(List<MultipartFile> archivos) {
        try (PDDocument pdf = new PDDocument()) {
            for (MultipartFile archivo : archivos) {
                BufferedImage imagen =
                        ImageIO.read(new ByteArrayInputStream(leer(archivo)));
                if (imagen == null) {
                    throw new DocumentoIlegibleException(
                            "El archivo " + archivo.getOriginalFilename() + " no es una imagen "
                                    + "que se pueda leer. Usa JPG o PNG.");
                }
                float ancho = ANCHO_PAGINA_PDF;
                float alto = ancho * imagen.getHeight() / imagen.getWidth();
                PDPage pagina = new PDPage(new PDRectangle(ancho, alto));
                pdf.addPage(pagina);
                PDImageXObject imagenPdf = LosslessFactory.createFromImage(pdf, imagen);
                try (PDPageContentStream contenido = new PDPageContentStream(pdf, pagina)) {
                    contenido.drawImage(imagenPdf, 0, 0, ancho, alto);
                }
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            pdf.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new DocumentoIlegibleException(
                    "No se pudieron juntar las hojas del albaran: " + ex.getMessage());
        }
    }
}
