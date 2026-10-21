package com.chefstool.backend.adapter.out.storage;

import com.chefstool.backend.domain.port.out.AlmacenamientoArchivos;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Guarda los albaranes en disco. Para produccion se sustituye por object storage. */
@Service
public class AlmacenamientoLocal implements AlmacenamientoArchivos {

    private final Path base;

    public AlmacenamientoLocal(
            @Value("${chefstool.almacenamiento.directorio:./uploads}") String directorio) {
        this.base = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(
            UUID organizacionId, UUID documentoId, String nombreOriginal, byte[] contenido) {
        String nombreSeguro = nombreOriginal == null
                ? "documento"
                : Path.of(nombreOriginal).getFileName().toString().replaceAll("[^\\w.\\-]", "_");
        Path destino = base.resolve(organizacionId.toString()).resolve(documentoId.toString());
        try {
            Files.createDirectories(destino);
            Path archivo = destino.resolve(nombreSeguro);
            Files.write(archivo, contenido);
            return base.relativize(archivo).toString();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el archivo", ex);
        }
    }

    @Override
    public Optional<Resource> cargar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return Optional.empty();
        }
        Path archivo = base.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(base) || !Files.isReadable(archivo)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(archivo));
    }
}
