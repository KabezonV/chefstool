package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.RecuperacionPassword;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.RecuperacionPasswordRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperacion de contrasena con un enlace de un solo uso. El token se guarda hasheado
 * y caduca en una hora. Si no hay proveedor de correo configurado, el token se escribe
 * en el log y (solo si {@code chefstool.auth.enlace-directo} esta activo) se devuelve a
 * la interfaz para poder probar el flujo.
 */
@Service
public class RecuperacionPasswordService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPasswordService.class);
    private static final Duration VIGENCIA = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final RecuperacionPasswordRepository recuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enlaceDirecto;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionPasswordService(
            UsuarioRepository usuarioRepository,
            RecuperacionPasswordRepository recuperacionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${chefstool.auth.enlace-directo:false}") boolean enlaceDirecto) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.enlaceDirecto = enlaceDirecto;
    }

    @PostConstruct
    void avisarModoDemostracion() {
        if (enlaceDirecto) {
            log.warn(
                    "chefstool.auth.enlace-directo activo: los enlaces de recuperacion se"
                            + " devuelven en la respuesta. No usar en produccion.");
        }
    }

    /** Devuelve el token en claro solo cuando el modo demostracion esta activo. */
    @Transactional
    public Optional<String> solicitar(String email) {
        Optional<Usuario> usuario =
                usuarioRepository.findByEmailIgnoreCase(email == null ? "" : email.trim());
        if (usuario.isEmpty()) {
            return Optional.empty();
        }

        // Los enlaces anteriores dejan de servir en cuanto se pide uno nuevo
        for (RecuperacionPassword anterior :
                recuperacionRepository.findByUsuarioIdAndUsadoAtIsNull(usuario.get().getId())) {
            anterior.setUsadoAt(OffsetDateTime.now());
            recuperacionRepository.save(anterior);
        }

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RecuperacionPassword recuperacion = new RecuperacionPassword();
        recuperacion.setUsuarioId(usuario.get().getId());
        recuperacion.setTokenHash(hash(token));
        recuperacion.setExpiraAt(OffsetDateTime.now().plus(VIGENCIA));
        recuperacionRepository.save(recuperacion);

        log.info(
                "Enlace de recuperacion para {}: /auth/restablecer?token={}",
                usuario.get().getEmail(),
                token);

        return enlaceDirecto ? Optional.of(token) : Optional.empty();
    }

    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        RecuperacionPassword recuperacion =
                recuperacionRepository
                        .findByTokenHash(hash(token == null ? "" : token.trim()))
                        .filter(encontrada -> encontrada.vigente(OffsetDateTime.now()))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El enlace no es valido o ha caducado. Pide uno nuevo."));

        Usuario usuario =
                usuarioRepository
                        .findById(recuperacion.getUsuarioId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("La cuenta ya no existe"));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        recuperacion.setUsadoAt(OffsetDateTime.now());
        recuperacionRepository.save(recuperacion);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
