package com.chefstool.backend.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
    private final JavaMailSender mailSender;
    private final boolean habilitado;
    private final String remitente;
    private final String host;

    public CorreoService(
            JavaMailSender mailSender,
            @Value("${chefstool.alertas.correo.habilitado:false}") boolean habilitado,
            @Value("${chefstool.mail.from:no-reply@chefstool.local}") String remitente,
            @Value("${spring.mail.host:}") String host) {
        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.remitente = remitente;
        this.host = host;
    }

    public boolean disponible() {
        return habilitado && host != null && !host.isBlank();
    }

    /** Explica que falta para que el correo funcione (sin ensenar contrasenas). */
    public EstadoCorreo estado() {
        java.util.List<String> falta = new java.util.ArrayList<>();
        if (host == null || host.isBlank()) {
            falta.add("CHEFSTOOL_SMTP_HOST (servidor de correo del proveedor)");
        }
        if (!habilitado) {
            falta.add("CHEFSTOOL_ALERTAS_CORREO=true (avisos por correo)");
        }
        return new EstadoCorreo(
                disponible(), habilitado, host, remitente, java.util.List.copyOf(falta));
    }

    /** Envia un correo de prueba y devuelve el motivo exacto si algo falla. */
    public ResultadoEnvio probar(String destinatario) {
        if (!disponible()) {
            return new ResultadoEnvio(
                    false,
                    destinatario,
                    "El correo no esta configurado: falta "
                            + String.join(", ", estado().falta()));
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject("Chefstool: correo de prueba");
            mensaje.setText(
                    "Este es un correo de prueba enviado desde Chefstool.\n"
                            + "Si lo estas leyendo, las alertas de precio y la recuperacion "
                            + "de contrasena ya pueden salir por correo.");
            mailSender.send(mensaje);
            return new ResultadoEnvio(true, destinatario, "Correo de prueba enviado");
        } catch (RuntimeException ex) {
            return new ResultadoEnvio(
                    false,
                    destinatario,
                    "No se pudo enviar: " + mensajeDeError(ex));
        }
    }

    private String mensajeDeError(RuntimeException ex) {
        String texto = ex.getMessage();
        if (texto == null || texto.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        // Los servidores SMTP incluyen la contestacion; con eso se sabe si es usuario,
        // contrasena, puerto o TLS.
        return texto.length() > 300 ? texto.substring(0, 300) : texto;
    }

    public boolean enviar(String destinatario, String asunto, String texto) {
        if (!disponible() || destinatario == null || destinatario.isBlank()) {
            return false;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        mailSender.send(mensaje);
        return true;
    }
}
