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
