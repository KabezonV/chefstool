package com.chefstool.backend.application;

/** El documento no contiene texto legible (escaneo sin OCR disponible o ilegible). */
public class DocumentoIlegibleException extends RuntimeException {

    public DocumentoIlegibleException(String mensaje) {
        super(mensaje);
    }
}
