package com.chefstool.backend.adapter.out.storage;

import com.chefstool.backend.domain.port.out.AlmacenamientoArchivos;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Guarda los albaranes en un bucket de Supabase Storage (API REST). Se activa con
 * {@code chefstool.almacenamiento.tipo=supabase}; si no, se usa el disco local.
 *
 * <p>La ruta relativa que se guarda en base de datos es la misma en ambos casos:
 * {@code organizacionId/documentoId/nombre}.
 */
@Component
@ConditionalOnProperty(name = "chefstool.almacenamiento.tipo", havingValue = "supabase")
public class AlmacenamientoSupabase implements AlmacenamientoArchivos {

    private final RestClient cliente;
    private final String bucket;

    public AlmacenamientoSupabase(
            RestClient.Builder constructor,
            @Value("${chefstool.almacenamiento.supabase.url}") String url,
            @Value("${chefstool.almacenamiento.supabase.clave}") String clave,
            @Value("${chefstool.almacenamiento.supabase.bucket:documentos}") String bucket) {
        if (url == null || url.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalStateException(
                    "Con chefstool.almacenamiento.tipo=supabase hay que definir SUPABASE_URL"
                            + " y SUPABASE_SERVICE_KEY");
        }
        this.bucket = bucket;
        this.cliente =
                constructor
                        .baseUrl(url.replaceAll("/+$", ""))
                        .defaultHeader("Authorization", "Bearer " + clave)
                        .defaultHeader("apikey", clave)
                        .build();
    }

    @Override
    public String guardar(
            UUID organizacionId, UUID documentoId, String nombreOriginal, byte[] contenido) {
        String ruta =
                organizacionId
                        + "/"
                        + documentoId
                        + "/"
                        + (nombreOriginal == null ? "documento" : limpiar(nombreOriginal));

        cliente.post()
                .uri(uriObjeto(ruta))
                .header("x-upsert", "true")
                .contentType(tipoContenido(ruta))
                .body(contenido)
                .retrieve()
                .toBodilessEntity();

        return ruta;
    }

    @Override
    public Optional<Resource> cargar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] bytes =
                    cliente.get()
                            .uri(uriObjeto(rutaRelativa))
                            .retrieve()
                            .body(byte[].class);
            return bytes == null
                    ? Optional.empty()
                    : Optional.of(new ByteArrayResource(bytes));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Codifica cada tramo de la ruta sin perder las barras separadoras. */
    private URI uriObjeto(String ruta) {
        return UriComponentsBuilder.fromPath("/storage/v1/object")
                .pathSegment(bucket)
                .pathSegment(ruta.split("/"))
                .build()
                .encode()
                .toUri();
    }

    private String limpiar(String nombre) {
        return nombre.replaceAll("^.*[/\\\\]", "").replaceAll("[^\\w.\\-]", "_");
    }

    private MediaType tipoContenido(String ruta) {
        String minusculas = ruta.toLowerCase();
        if (minusculas.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (minusculas.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (minusculas.endsWith(".jpg") || minusculas.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (minusculas.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
