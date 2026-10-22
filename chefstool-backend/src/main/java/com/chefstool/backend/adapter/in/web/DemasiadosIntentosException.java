package com.chefstool.backend.adapter.in.web;

/** Se lanza cuando se superan los intentos permitidos en una ventana de tiempo. */
public class DemasiadosIntentosException extends RuntimeException {

    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
