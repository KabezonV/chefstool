package com.chefstool.backend.adapter.out.ocr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OCR de imagenes usando el binario de Tesseract si esta instalado.
 *
 * <p>Se invoca el ejecutable con ProcessBuilder en lugar de enlazar librerias nativas:
 * si no esta disponible, la aplicacion sigue funcionando y avisa en el documento.
 */
@Component
public class LectorImagen {

    private static final Logger log = LoggerFactory.getLogger(LectorImagen.class);

    private final String comando;
    private final String idioma;

    public LectorImagen(
            @Value("${chefstool.ocr.tesseract:tesseract}") String comando,
            @Value("${chefstool.ocr.idioma:spa}") String idioma) {
        this.comando = comando;
        this.idioma = idioma;
    }

    public boolean disponible() {
        try {
            Process proceso =
                    new ProcessBuilder(comando, "--version")
                            .redirectErrorStream(true)
                            .start();
            boolean termino = proceso.waitFor(5, TimeUnit.SECONDS);
            return termino && proceso.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public Optional<String> extraerTexto(byte[] imagen) {
        if (!disponible()) {
            return Optional.empty();
        }

        Path temporal = null;
        try {
            temporal = Files.createTempFile("chefstool-ocr-", ".img");
            Files.write(temporal, imagen);

            Process proceso =
                    new ProcessBuilder(comando, temporal.toString(), "stdout", "-l", idioma)
                            .redirectErrorStream(false)
                            .start();

            String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean termino = proceso.waitFor(60, TimeUnit.SECONDS);

            if (!termino || proceso.exitValue() != 0) {
                log.warn("Tesseract termino con codigo {}", termino ? proceso.exitValue() : -1);
                return Optional.empty();
            }
            return Optional.of(salida);
        } catch (IOException ex) {
            log.warn("No se pudo ejecutar Tesseract: {}", ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ex) {
                    log.debug("No se pudo borrar el temporal de OCR", ex);
                }
            }
        }
    }
}
