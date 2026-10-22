package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.CredencialesInvalidasException;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.LoginRequest;
import com.chefstool.backend.adapter.in.web.dto.RegistroRequest;
import com.chefstool.backend.adapter.in.web.dto.SesionResponse;
import com.chefstool.backend.domain.model.Invitacion;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.model.Organizacion;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.InvitacionRepository;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import com.chefstool.backend.domain.port.out.OrganizacionRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final MiembroOrganizacionRepository miembroRepository;
    private final InvitacionRepository invitacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long horasToken;

    public AuthService(
            UsuarioRepository usuarioRepository,
            OrganizacionRepository organizacionRepository,
            MiembroOrganizacionRepository miembroRepository,
            InvitacionRepository invitacionRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${chefstool.seguridad.jwt-horas:12}") long horasToken) {
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
        this.miembroRepository = miembroRepository;
        this.invitacionRepository = invitacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.horasToken = horasToken;
    }

    @Transactional
    public SesionResponse registrar(RegistroRequest peticion) {
        String email = peticion.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre(
                peticion.nombre() == null || peticion.nombre().isBlank()
                        ? email
                        : peticion.nombre().trim());
        usuario.setPasswordHash(passwordEncoder.encode(peticion.password()));
        usuario = usuarioRepository.save(usuario);

        List<Invitacion> invitaciones =
                invitacionRepository.findByEmailIgnoreCaseAndAceptadaAtIsNullOrderByCreatedAtAsc(
                        email);

        if (!invitaciones.isEmpty()) {
            return unirAOrganizacionesInvitadas(usuario, invitaciones);
        }

        Organizacion organizacion = new Organizacion();
        organizacion.setNombre(
                peticion.organizacion() == null || peticion.organizacion().isBlank()
                        ? "Cocina de " + usuario.getNombre()
                        : peticion.organizacion().trim());
        organizacion = organizacionRepository.save(organizacion);

        MiembroOrganizacion miembro = new MiembroOrganizacion();
        miembro.setOrganizacionId(organizacion.getId());
        miembro.setUsuarioId(usuario.getId());
        miembro.setEmail(usuario.getEmail());
        miembro.setNombre(usuario.getNombre());
        miembro.setRol("propietario");
        miembroRepository.save(miembro);

        return sesion(usuario, organizacion, miembro.getRol());
    }

    /** El correo estaba invitado: se une a esas organizaciones en lugar de crear una. */
    private SesionResponse unirAOrganizacionesInvitadas(
            Usuario usuario, List<Invitacion> invitaciones) {
        Organizacion primera = null;
        String rolEnPrimera = null;

        for (Invitacion invitacion : invitaciones) {
            MiembroOrganizacion miembro = new MiembroOrganizacion();
            miembro.setOrganizacionId(invitacion.getOrganizacionId());
            miembro.setUsuarioId(usuario.getId());
            miembro.setEmail(usuario.getEmail());
            miembro.setNombre(usuario.getNombre());
            miembro.setRol(invitacion.getRol());
            miembroRepository.save(miembro);

            invitacion.setAceptadaAt(java.time.OffsetDateTime.now());
            invitacionRepository.save(invitacion);

            if (primera == null) {
                primera =
                        organizacionRepository
                                .findById(invitacion.getOrganizacionId())
                                .orElseThrow(
                                        () ->
                                                new RecursoNoEncontradoException(
                                                        "Organizacion no encontrada"));
                rolEnPrimera = invitacion.getRol();
            }
        }

        return sesion(usuario, primera, rolEnPrimera);
    }

    @Transactional(readOnly = true)
    public SesionResponse login(LoginRequest peticion) {
        Usuario usuario =
                usuarioRepository
                        .findByEmailIgnoreCase(peticion.email().trim())
                        .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(peticion.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        MiembroOrganizacion miembro =
                miembroRepository
                        .findFirstByUsuarioIdOrderByCreatedAtAsc(usuario.getId())
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "El usuario no pertenece a ninguna organizacion"));

        Organizacion organizacion =
                organizacionRepository
                        .findById(miembro.getOrganizacionId())
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException("Organizacion no encontrada"));

        return sesion(usuario, organizacion, miembro.getRol());
    }

    @Transactional(readOnly = true)
    public SesionResponse sesionActual(UUID usuarioId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        MiembroOrganizacion miembro =
                miembroRepository
                        .findFirstByUsuarioIdOrderByCreatedAtAsc(usuarioId)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                "El usuario no pertenece a ninguna organizacion"));

        Organizacion organizacion =
                organizacionRepository
                        .findById(miembro.getOrganizacionId())
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException("Organizacion no encontrada"));

        return sesion(usuario, organizacion, miembro.getRol());
    }

    private SesionResponse sesion(Usuario usuario, Organizacion organizacion, String rol) {
        Instant ahora = Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .subject(usuario.getId().toString())
                        .claim("email", usuario.getEmail())
                        .claim("nombre", usuario.getNombre())
                        .claim("organizacionId", organizacion.getId().toString())
                        .claim("organizacion", organizacion.getNombre())
                        .claim("rol", rol)
                        .issuedAt(ahora)
                        .expiresAt(ahora.plus(Duration.ofHours(horasToken)))
                        .build();

        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token =
                jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();

        return new SesionResponse(
                token,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                organizacion.getId(),
                organizacion.getNombre(),
                rol);
    }
}
