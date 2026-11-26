package com.chefstool.backend.domain.port.out;

import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;

/** Abstraccion del almacen de archivos: disco local o un bucket S3. */
public interface AlmacenamientoArchivos {

    /**
     * Guarda el contenido y devuelve la ruta relativa con la que recuperarlo.
     */
    String guardar(
            UUID organizacionId, UUID documentoId, String nombreOriginal, byte[] contenido);

    Optional<Resource> cargar(String rutaRelativa);

    /**
     * Borra un archivo ya guardado. Se usa al rehacer un albaran con mas hojas: el PDF nuevo
     * sustituye al anterior y no tiene sentido seguir pagando por el viejo.
     */
    default void borrar(String rutaRelativa) {}
}
