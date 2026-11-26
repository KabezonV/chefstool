package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.domain.model.Compra;
import com.chefstool.backend.domain.model.DocumentoCompra;
import com.chefstool.backend.domain.port.out.AlmacenamientoArchivos;
import com.chefstool.backend.domain.port.out.CompraRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Descarga en un ZIP los albaranes originales de las compras de un periodo, opcionalmente de
 * un solo proveedor. Es lo que se manda a la gestoria a fin de mes.
 */
@Service
public class CompraPaqueteService {

    private static final Logger log = LoggerFactory.getLogger(CompraPaqueteService.class);

    /** Tope de albaranes por descarga, para no montar un archivo inmanejable. */
    private static final int MAXIMO_DOCUMENTOS = 300;

    private final CompraRepository compraRepository;
    private final AlmacenamientoArchivos almacenamiento;
    private final OrganizacionService organizacionService;

    public CompraPaqueteService(
            CompraRepository compraRepository,
            AlmacenamientoArchivos almacenamiento,
            OrganizacionService organizacionService) {
        this.compraRepository = compraRepository;
        this.almacenamiento = almacenamiento;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public byte[] descargarZip(UUID proveedorId, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Indica el periodo: fecha desde y fecha hasta");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a la desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > 400) {
            throw new IllegalArgumentException(
                    "El periodo es demasiado largo: descarga como maximo un ano por vez");
        }

        List<Compra> compras = compraRepository.buscarEnPeriodo(
                organizacionService.organizacionActual(),
                organizacionService.localActual(),
                proveedorId,
                desde,
                hasta);

        // Un mismo albaran puede haber generado varias lineas: el documento se baja una vez
        Map<UUID, DocumentoCompra> documentos = new LinkedHashMap<>();
        Map<UUID, String> prefijos = new LinkedHashMap<>();
        for (Compra compra : compras) {
            DocumentoCompra documento = compra.getDocumento();
            if (documento == null || documento.getRutaAlmacenamiento() == null) {
                continue;
            }
            if (documentos.putIfAbsent(documento.getId(), documento) == null) {
                prefijos.put(documento.getId(), prefijo(compra));
            }
        }

        if (documentos.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No hay albaranes con archivo en ese periodo");
        }
        if (documentos.size() > MAXIMO_DOCUMENTOS) {
            throw new IllegalArgumentException(
                    "Hay %d albaranes en ese periodo: acota el rango o elige un proveedor"
                            .formatted(documentos.size()));
        }

        List<PaqueteZip.Archivo> archivos = new ArrayList<>();
        documentos.forEach((id, documento) -> {
            byte[] contenido = almacenamiento
                    .cargar(documento.getRutaAlmacenamiento())
                    .map(this::leer)
                    .orElse(null);
            if (contenido != null) {
                archivos.add(new PaqueteZip.Archivo(
                        "%s_%s".formatted(
                                prefijos.get(id),
                                PaqueteZip.nombreSeguro(documento.getNombreOriginal())),
                        contenido));
            }
        });

        if (archivos.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No se pudo leer ningun archivo de ese periodo");
        }
        return PaqueteZip.empaquetar(archivos);
    }

    private String prefijo(Compra compra) {
        String fecha = compra.getFecha() == null ? "sin-fecha" : compra.getFecha().toString();
        String numero = compra.getNumeroDocumento();
        return numero == null || numero.isBlank() ? fecha : fecha + "_" + numero;
    }

    private byte[] leer(org.springframework.core.io.Resource recurso) {
        try (var entrada = recurso.getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException ex) {
            log.warn("No se pudo leer un albaran para el ZIP: {}", ex.getMessage());
            return null;
        }
    }
}
