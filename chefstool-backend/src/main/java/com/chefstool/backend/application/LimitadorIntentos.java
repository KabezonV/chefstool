package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.DemasiadosIntentosException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limite de intentos en memoria (ventana deslizante por clave). Suficiente para una
 * instancia; con varias instancias habria que apoyarse en Redis o en la base de datos.
 */
@Component
public class LimitadorIntentos {

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maximo;
    private final Duration ventana;

    public LimitadorIntentos(
            @Value("${chefstool.seguridad.intentos-maximos:5}") int maximo,
            @Value("${chefstool.seguridad.intentos-ventana-minutos:15}") long minutos) {
        this.maximo = maximo;
        this.ventana = Duration.ofMinutes(minutos);
    }

    public void comprobar(String clave) {
        Registro registro = registros.get(clave);
        if (registro == null) {
            return;
        }
        if (registro.caducado(ventana)) {
            registros.remove(clave);
            return;
        }
        if (registro.fallos >= maximo) {
            long minutosRestantes =
                    Math.max(1, ventana.minus(Duration.between(registro.ultimo, Instant.now())).toMinutes());
            throw new DemasiadosIntentosException(
                    "Demasiados intentos fallidos. Prueba de nuevo en "
                            + minutosRestantes
                            + " minutos");
        }
    }

    public void fallo(String clave) {
        registros.compute(
                clave,
                (k, registro) ->
                        registro == null || registro.caducado(ventana)
                                ? new Registro(1, Instant.now())
                                : new Registro(registro.fallos + 1, Instant.now()));
    }

    public void exito(String clave) {
        registros.remove(clave);
    }

    /** Limpia las claves caducadas; se llama desde cada comprobacion de forma barata. */
    public int limpiarCaducados() {
        registros.entrySet().removeIf(entrada -> entrada.getValue().caducado(ventana));
        return registros.size();
    }

    private record Registro(int fallos, Instant ultimo) {
        boolean caducado(Duration ventana) {
            return ultimo.plus(ventana).isBefore(Instant.now());
        }
    }
}
