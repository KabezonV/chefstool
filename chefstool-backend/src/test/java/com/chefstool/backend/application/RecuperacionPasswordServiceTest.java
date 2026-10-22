package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chefstool.backend.domain.model.RecuperacionPassword;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.RecuperacionPasswordRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecuperacionPasswordServiceTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RecuperacionPasswordRepository recuperacionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private RecuperacionPasswordService servicio;
    private String email;

    @BeforeEach
    void preparar() {
        servicio =
                new RecuperacionPasswordService(
                        usuarioRepository, recuperacionRepository, passwordEncoder, true);

        Usuario usuario = new Usuario();
        email = "reset-" + UUID.randomUUID() + "@cocina.es";
        usuario.setEmail(email);
        usuario.setNombre("Chef");
        usuario.setPasswordHash(passwordEncoder.encode("claveVieja123"));
        usuarioRepository.save(usuario);
    }

    @Test
    void elEnlaceCambiaLaContrasenaUnaSolaVez() {
        String token = servicio.solicitar(email).orElseThrow();

        servicio.restablecer(token, "claveNueva123");

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(passwordEncoder.matches("claveNueva123", usuario.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("claveVieja123", usuario.getPasswordHash())).isFalse();

        assertThatThrownBy(() -> servicio.restablecer(token, "otraClave123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no es valido o ha caducado");
    }

    @Test
    void unEnlaceNuevoAnulaElAnterior() {
        String primero = servicio.solicitar(email).orElseThrow();
        String segundo = servicio.solicitar(email).orElseThrow();

        assertThatThrownBy(() -> servicio.restablecer(primero, "claveNueva123"))
                .isInstanceOf(IllegalArgumentException.class);

        servicio.restablecer(segundo, "claveNueva123");
        assertThat(passwordEncoder.matches(
                        "claveNueva123",
                        usuarioRepository.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash()))
                .isTrue();
    }

    @Test
    void unEnlaceCaducadoNoSirve() {
        String token = servicio.solicitar(email).orElseThrow();
        UUID usuarioId = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow().getId();

        RecuperacionPassword recuperacion =
                recuperacionRepository.findByUsuarioIdAndUsadoAtIsNull(usuarioId).getFirst();
        recuperacion.setExpiraAt(OffsetDateTime.now().minusMinutes(1));
        recuperacionRepository.save(recuperacion);

        assertThatThrownBy(() -> servicio.restablecer(token, "claveNueva123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("caducado");
    }

    @Test
    void unCorreoDesconocidoNoRevelaNada() {
        assertThat(servicio.solicitar("no-existe-" + UUID.randomUUID() + "@cocina.es"))
                .isEqualTo(Optional.empty());
    }

    @Test
    void sinModoDemostracionElTokenNoViajaEnLaRespuesta() {
        RecuperacionPasswordService sinEnlace =
                new RecuperacionPasswordService(
                        usuarioRepository, recuperacionRepository, passwordEncoder, false);

        assertThat(sinEnlace.solicitar(email)).isEmpty();
        // pero el enlace si se ha creado
        assertThat(recuperacionRepository.findAll())
                .anyMatch(recuperacion -> recuperacion.getUsadoAt() == null);
    }
}
