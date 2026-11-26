package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class CorreoServiceTest {
    @Test
    void noEnviaSinConfiguracionSmtp() {
        CorreoService servicio = new CorreoService(mock(JavaMailSender.class), true,
                "no-reply@chefstool.es", "");

        assertThat(servicio.disponible()).isFalse();
        assertThat(servicio.enviar("chef@example.com", "Alerta", "Texto")).isFalse();
    }

    @Test
    void enviaUnMensajeSimpleCuandoEstaConfigurado() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        CorreoService servicio = new CorreoService(mailSender, true,
                "no-reply@chefstool.es", "smtp.example.com");

        assertThat(servicio.enviar("chef@example.com", "Alerta", "Texto de prueba")).isTrue();

        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("chef@example.com");
        assertThat(mensaje.getValue().getSubject()).isEqualTo("Alerta");
    }
}
