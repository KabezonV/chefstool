package com.chefstool.backend.adapter.out.storage;

import com.chefstool.backend.domain.port.out.AlmacenamientoArchivos;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Guarda los albaranes en un bucket de S3 (o en cualquier servicio compatible, como
 * MinIO). Se activa con {@code chefstool.almacenamiento.tipo=s3}.
 *
 * <p>La clave del objeto es {@code organizacionId/documentoId/nombre}, la misma ruta que
 * se guarda en base de datos. El bucket es privado: las descargas las sirve el backend,
 * que valida antes a quién pertenece el documento.
 */
@Component
@ConditionalOnProperty(name = "chefstool.almacenamiento.tipo", havingValue = "s3")
public class AlmacenamientoS3 implements AlmacenamientoArchivos {

    private final S3Client cliente;
    private final String bucket;

    public AlmacenamientoS3(
            @Value("${chefstool.almacenamiento.s3.bucket}") String bucket,
            @Value("${chefstool.almacenamiento.s3.region:eu-south-2}") String region,
            @Value("${chefstool.almacenamiento.s3.endpoint:}") String endpoint,
            @Value("${chefstool.almacenamiento.s3.access-key:}") String accessKey,
            @Value("${chefstool.almacenamiento.s3.secret-key:}") String secretKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException(
                    "Con chefstool.almacenamiento.tipo=s3 hay que definir el bucket"
                            + " (CHEFSTOOL_S3_BUCKET)");
        }
        this.bucket = bucket;
        this.cliente = construir(region, endpoint, accessKey, secretKey);
    }

    /** Constructor para pruebas con un cliente ya montado. */
    AlmacenamientoS3(S3Client cliente, String bucket) {
        this.cliente = cliente;
        this.bucket = bucket;
    }

    private S3Client construir(String region, String endpoint, String accessKey, String secretKey) {
        var constructor = S3Client.builder().region(Region.of(region));

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            constructor =
                    constructor.credentialsProvider(
                            software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                                            accessKey, secretKey)));
        }
        if (endpoint != null && !endpoint.isBlank()) {
            constructor =
                    constructor
                            .endpointOverride(java.net.URI.create(endpoint))
                            .forcePathStyle(true);
        }
        return constructor.build();
    }

    @Override
    public String guardar(
            UUID organizacionId, UUID documentoId, String nombreOriginal, byte[] contenido) {
        String clave =
                organizacionId
                        + "/"
                        + documentoId
                        + "/"
                        + (nombreOriginal == null ? "documento" : limpiar(nombreOriginal));

        cliente.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(clave)
                        .contentType(tipoContenido(clave))
                        .build(),
                RequestBody.fromBytes(contenido));

        return clave;
    }

    @Override
    public Optional<Resource> cargar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] bytes =
                    cliente.getObjectAsBytes(
                                    GetObjectRequest.builder()
                                            .bucket(bucket)
                                            .key(rutaRelativa)
                                            .build())
                            .asByteArray();
            return Optional.of(new ByteArrayResource(bytes));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String limpiar(String nombre) {
        return nombre.replaceAll("^.*[/\\\\]", "").replaceAll("[^\\w.\\-]", "_");
    }

    private String tipoContenido(String clave) {
        String minusculas = clave.toLowerCase();
        if (minusculas.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (minusculas.endsWith(".png")) {
            return "image/png";
        }
        if (minusculas.endsWith(".jpg") || minusculas.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (minusculas.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
