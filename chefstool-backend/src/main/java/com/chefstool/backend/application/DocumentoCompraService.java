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
import java.io.IOException;
import java.io.UncheckedIOException;
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
    public DocumentoDetalle subir(MultipartFile archivo, String origen) {
        UUID organizacionId = organizacionService.organizacionActual();
        boolean demo = archivo == null || archivo.isEmpty();

        if (demo && !"demo".equalsIgnoreCase(origen)) {
            throw new IllegalArgumentException("Adjunta un albaran o usa el modo demostracion");
        }

        DocumentoCompra documento = new DocumentoCompra();
        documento.setOrganizacionId(organizacionId);
        documento.setNombreOriginal(demo ? "albaran-demo.pdf" : archivo.getOriginalFilename());
        documento.setTipoMime(demo ? "application/pdf" : archivo.getContentType());
        documento.setOrigen(demo ? "demo" : origen);
        documento.setEstado(DocumentoCompra.PENDIENTE_REVISION);

        DocumentoCompra guardado = documentoRepository.save(documento);

        byte[] contenido = demo ? new byte[0] : leer(archivo);
        if (!demo) {
            guardado.setRutaAlmacenamiento(
                    almacenamiento.guardar(
                            organizacionId,
                            guardado.getId(),
                            archivo.getOriginalFilename(),
                            contenido));
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
}
