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
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
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

    /** Tope de albaranes en una sola descarga, para no montar archivos enormes. */
    private static final int MAXIMO_DOCUMENTOS_ZIP = 300;

    private final DocumentoCompraRepository documentoRepository;
    private final OrganizacionService organizacionService;
    private final AlmacenamientoArchivos almacenamiento;
    private final ExtractorDocumento extractor;
    private final CompraService compraService;
    private final ObjectMapper objectMapper;
    private final AuditoriaService auditoriaService;

    public DocumentoCompraService(
            DocumentoCompraRepository documentoRepository,
            OrganizacionService organizacionService,
            AlmacenamientoArchivos almacenamiento,
            ExtractorDocumento extractor,
            CompraService compraService,
            ObjectMapper objectMapper,
            AuditoriaService auditoriaService) {
        this.documentoRepository = documentoRepository;
        this.organizacionService = organizacionService;
        this.almacenamiento = almacenamiento;
        this.extractor = extractor;
        this.compraService = compraService;
        this.objectMapper = objectMapper;
        this.auditoriaService = auditoriaService;
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
            if (todosSonPdf(utiles)) {
                contenido = juntarPdf(utiles);
            } else if (todosSonImagen(utiles)) {
                contenido = juntarEnPdf(utiles);
            } else {
                throw new DocumentoIlegibleException(
                        "No mezcles fotos y PDFs en una misma subida. Sube varias fotos o varios PDFs.");
            }
        }

        DocumentoCompra documento = new DocumentoCompra();
        documento.setOrganizacionId(organizacionId);
        documento.setLocalId(organizacionService.localActual());
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

        DocumentoCompra guardadoFinal = documentoRepository.saveAndFlush(guardado);
        if (!demo && auditoriaService != null) {
            auditoriaService.registrar(
                    "SUBIR", "DOCUMENTO", guardadoFinal.getId(), guardadoFinal.getNombreOriginal());
        }
        return detalle(guardadoFinal);
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

        UUID localId = organizacionService.localActual();
        var documentos = localId == null
                ? documentoRepository.findByOrganizacionId(
                        organizacionService.organizacionActual(), pageable)
                : documentoRepository.findByOrganizacionIdAndLocalId(
                        organizacionService.organizacionActual(), localId, pageable);
        return Pagina.desde(documentos)
                .map(DocumentoResumen::from);
    }

    @Transactional(readOnly = true)
    public DocumentoDetalle obtener(UUID id) {
        return detalle(buscar(id));
    }

    /**
     * Añade hojas a un albarán que aún está en revisión: se fotografía la primera página, se
     * revisa y, al ver que faltan líneas, se hace la foto de la segunda sin empezar de cero.
     *
     * <p>Las hojas se unen en un único PDF (junto con lo ya subido) y el documento se vuelve a
     * leer entero, porque los totales solo aparecen en la última página.
     */
    @Transactional
    public DocumentoDetalle anexarHojas(UUID id, List<MultipartFile> archivos) {
        DocumentoCompra documento = buscarEditable(id);

        List<MultipartFile> utiles =
                archivos == null
                        ? List.of()
                        : archivos.stream()
                                .filter(archivo -> archivo != null && !archivo.isEmpty())
                                .toList();
        if (utiles.isEmpty()) {
            throw new IllegalArgumentException("Adjunta la hoja que falta del albaran");
        }
        if (documento.getRutaAlmacenamiento() == null) {
            throw new IllegalArgumentException(
                    "Este documento no tiene archivo: sube el albaran completo en Documentos");
        }

        List<byte[]> hojas = new ArrayList<>();
        hojas.add(leerGuardado(documento));
        for (MultipartFile archivo : utiles) {
            hojas.add(leer(archivo));
        }

        // Se comprueba que todo se puede unir antes de tocar el documento guardado
        byte[] combinado = combinarHojas(hojas);
        String nombre = nombreDeVariasHojas(documento.getNombreOriginal(), hojas.size());

        String rutaAnterior = documento.getRutaAlmacenamiento();
        String ruta = almacenamiento.guardar(
                documento.getOrganizacionId(), documento.getId(), nombre, combinado);
        documento.setNombreOriginal(nombre);
        documento.setTipoMime("application/pdf");
        documento.setRutaAlmacenamiento(ruta);
        if (rutaAnterior != null && !rutaAnterior.equals(ruta)) {
            almacenamiento.borrar(rutaAnterior);
        }

        try {
            ResultadoExtraccion resultado =
                    extractor.extraer(
                            combinado, nombre, "application/pdf", documento.getOrganizacionId());
            documento.setExtraccionJson(serializar(resultado.extraccion()));
            documento.setConfianzaExtraccion(resultado.confianza());
            documento.setErrorExtraccion(null);
        } catch (RuntimeException ex) {
            log.warn("No se pudo releer el documento {} al anexar hojas", documento.getId(), ex);
            documento.setErrorExtraccion("No se pudo leer el documento: " + ex.getMessage());
        }

        DocumentoCompra guardado = documentoRepository.saveAndFlush(documento);
        if (auditoriaService != null) {
            auditoriaService.registrar(
                    "ANEXAR", "DOCUMENTO", guardado.getId(), guardado.getNombreOriginal());
        }
        return detalle(guardado);
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
        if (auditoriaService != null) auditoriaService.registrar(
                "CONFIRMAR", "DOCUMENTO", documento.getId(), documento.getNombreOriginal());

        return compra;
    }

    @Transactional
    public void descartar(UUID id) {
        DocumentoCompra documento = buscarEditable(id);
        documento.setEstado(DocumentoCompra.DESCARTADO);
        documentoRepository.save(documento);
        if (auditoriaService != null) auditoriaService.registrar(
                "DESCARTAR", "DOCUMENTO", documento.getId(), documento.getNombreOriginal());
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

    /** Nombre con el que se guarda el archivo al descargarlo: fecha + nombre reconocible. */
    @Transactional(readOnly = true)
    public ArchivoDescargable descargar(UUID id) {
        DocumentoCompra documento = buscar(id);
        var recurso =
                almacenamiento
                        .cargar(documento.getRutaAlmacenamiento())
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "El documento no tiene archivo adjunto"));
        String nombre = "%s_%s".formatted(
                fechaDe(documento), PaqueteZip.nombreSeguro(documento.getNombreOriginal()));
        return new ArchivoDescargable(recurso, documento.getTipoMime(), nombre);
    }

    /**
     * Empaqueta en un ZIP los albaranes de un periodo (un mes, por ejemplo) para llevarlos a
     * la gestoria o guardarlos. Solo incluye documentos de la organizacion y local activos y
     * con archivo guardado.
     */
    @Transactional(readOnly = true)
    public byte[] descargarZip(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Indica el periodo: fecha desde y fecha hasta");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a la desde");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) > 400) {
            throw new IllegalArgumentException(
                    "El periodo es demasiado largo: descarga como maximo un ano por vez");
        }

        UUID organizacionId = organizacionService.organizacionActual();
        UUID localId = organizacionService.localActual();
        OffsetDateTime inicio = desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<DocumentoCompra> documentos = localId == null
                ? documentoRepository
                        .findByOrganizacionIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                                organizacionId, inicio, fin)
                : documentoRepository
                        .findByOrganizacionIdAndLocalIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                                organizacionId, localId, inicio, fin);

        List<DocumentoCompra> conArchivo =
                documentos.stream().filter(d -> d.getRutaAlmacenamiento() != null).toList();
        if (conArchivo.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No hay albaranes con archivo en ese periodo");
        }
        if (conArchivo.size() > MAXIMO_DOCUMENTOS_ZIP) {
            throw new IllegalArgumentException(
                    "Hay %d albaranes en ese periodo: acota el rango para no bajar un archivo "
                            + "gigante".formatted(conArchivo.size()));
        }

        List<PaqueteZip.Archivo> archivos = new ArrayList<>();
        for (DocumentoCompra documento : conArchivo) {
            byte[] contenido = almacenamiento
                    .cargar(documento.getRutaAlmacenamiento())
                    .map(this::leerRecurso)
                    .orElse(null);
            if (contenido != null) {
                archivos.add(new PaqueteZip.Archivo(
                        "%s_%s".formatted(
                                fechaDe(documento),
                                PaqueteZip.nombreSeguro(documento.getNombreOriginal())),
                        contenido));
            }
        }
        return PaqueteZip.empaquetar(archivos);
    }

    private byte[] leerRecurso(org.springframework.core.io.Resource recurso) {
        try (var entrada = recurso.getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException ex) {
            log.warn("No se pudo leer un archivo para el ZIP: {}", ex.getMessage());
            return null;
        }
    }

    private String fechaDe(DocumentoCompra documento) {
        return documento.getCreatedAt() == null
                ? "sin-fecha"
                : documento.getCreatedAt().toLocalDate().toString();
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
        // Un documento de otra organización no existe para quien pregunta: 404, no 403,
        // para no revelar que existe.
        return documentoRepository
                .findById(id)
                .filter(documento -> organizacionService.organizacionActual()
                        .equals(documento.getOrganizacionId()))
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

    private boolean todosSonPdf(List<MultipartFile> archivos) {
        return archivos.stream().allMatch(
                archivo -> {
                    byte[] contenido = leer(archivo);
                    return esPdf(archivo.getContentType(), contenido);
                });
    }

    private boolean esPdf(String tipoMime, byte[] contenido) {
        return (tipoMime != null && tipoMime.toLowerCase().startsWith("application/pdf"))
                || (contenido.length >= 4
                        && contenido[0] == '%'
                        && contenido[1] == 'P'
                        && contenido[2] == 'D'
                        && contenido[3] == 'F');
    }

    private boolean esImagen(byte[] contenido) {
        try {
            return ImageIO.read(new ByteArrayInputStream(contenido)) != null;
        } catch (IOException ex) {
            return false;
        }
    }

    private byte[] leerGuardado(DocumentoCompra documento) {
        Resource recurso =
                almacenamiento
                        .cargar(documento.getRutaAlmacenamiento())
                        .orElseThrow(
                                () ->
                                        new DocumentoIlegibleException(
                                                "El archivo del documento ya no esta disponible"));
        try (var entrada = recurso.getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo guardado", ex);
        }
    }

    /** Une las hojas: PDFs entre si, fotos como paginas de un PDF nuevo. */
    private byte[] combinarHojas(List<byte[]> hojas) {
        boolean todasPdf = hojas.stream().allMatch(hoja -> esPdf(null, hoja));
        if (todasPdf) {
            return juntarPdfBytes(hojas);
        }
        boolean todasImagen = hojas.stream().allMatch(this::esImagen);
        if (todasImagen) {
            return juntarImagenesEnPdf(hojas);
        }
        throw new DocumentoIlegibleException(
                "No mezcles fotos y PDFs en un mismo albaran. Anade la hoja en el mismo formato "
                        + "que las anteriores (foto con foto, PDF con PDF).");
    }

    private boolean todosSonImagen(List<MultipartFile> archivos) {
        return archivos.stream().allMatch(
                archivo -> {
                    String tipo = archivo.getContentType();
                    if (tipo != null && tipo.toLowerCase().startsWith("image/")) {
                        return true;
                    }
                    try {
                        return ImageIO.read(new ByteArrayInputStream(leer(archivo))) != null;
                    } catch (IOException ex) {
                        return false;
                    }
                });
    }

    private byte[] juntarPdf(List<MultipartFile> archivos) {
        return juntarPdfBytes(archivos.stream().map(this::leer).toList());
    }

    private byte[] juntarPdfBytes(List<byte[]> documentos) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDFMergerUtility mezclador = new PDFMergerUtility();
            for (byte[] documento : documentos) {
                mezclador.addSource(new RandomAccessReadBuffer(documento));
            }
            mezclador.setDestinationStream(salida);
            mezclador.mergeDocuments(null);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new DocumentoIlegibleException(
                    "No se pudieron juntar los PDFs del albaran: " + ex.getMessage());
        }
    }

    /** Convierte varias fotos en un PDF de varias paginas, una por hoja. */
    private byte[] juntarEnPdf(List<MultipartFile> archivos) {
        return juntarImagenesEnPdf(archivos.stream().map(this::leer).toList());
    }

    private byte[] juntarImagenesEnPdf(List<byte[]> imagenes) {
        try (PDDocument pdf = new PDDocument()) {
            for (byte[] contenido : imagenes) {
                BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
                if (imagen == null) {
                    throw new DocumentoIlegibleException(
                            "Una de las hojas no es una imagen que se pueda leer. Usa JPG o PNG.");
                }
                float ancho = ANCHO_PAGINA_PDF;
                float alto = ancho * imagen.getHeight() / imagen.getWidth();
                PDPage pagina = new PDPage(new PDRectangle(ancho, alto));
                pdf.addPage(pagina);
                PDImageXObject imagenPdf = LosslessFactory.createFromImage(pdf, imagen);
                try (PDPageContentStream contenidoPagina = new PDPageContentStream(pdf, pagina)) {
                    contenidoPagina.drawImage(imagenPdf, 0, 0, ancho, alto);
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
