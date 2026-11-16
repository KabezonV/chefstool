package com.chefstool.backend.adapter.in.web;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
