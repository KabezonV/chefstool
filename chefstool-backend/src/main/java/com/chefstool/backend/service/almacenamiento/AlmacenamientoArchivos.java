package com.chefstool.backend.service.almacenamiento;

import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;

/** Abstraccion del almacen de archivos (local ahora, Supabase Storage u otro despues). */
public interface AlmacenamientoArchivos {

    /**
     * Guarda el contenido y devuelve la ruta relativa con la que recuperarlo.
     */
    String guardar(
            UUID organizacionId, UUID documentoId, String nombreOriginal, byte[] contenido);

    Optional<Resource> cargar(String rutaRelativa);
}
