package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.CredencialesInvalidasException;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.LoginRequest;
import com.chefstool.backend.adapter.in.web.dto.RegistroRequest;
import com.chefstool.backend.adapter.in.web.dto.SesionResponse;
import com.chefstool.backend.domain.model.Invitacion;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.model.SesionRefresh;
import com.chefstool.backend.domain.model.Organizacion;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.InvitacionRepository;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import com.chefstool.backend.domain.port.out.OrganizacionRepository;
import com.chefstool.backend.domain.port.out.SesionRefreshRepository;
import com.chefstool.backend.domain.port.out.LocalRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
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
    private final SesionRefreshRepository refreshRepository;
    private final LocalRepository localRepository;
    private final long horasToken;
    private final long accessMinutos;
    private final long refreshDias;

    public AuthService(
            UsuarioRepository usuarioRepository,
            OrganizacionRepository organizacionRepository,
            MiembroOrganizacionRepository miembroRepository,
            InvitacionRepository invitacionRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            SesionRefreshRepository refreshRepository,
            LocalRepository localRepository,
            @Value("${chefstool.seguridad.jwt-horas:12}") long horasToken,
            @Value("${chefstool.seguridad.access-minutos:15}") long accessMinutos,
            @Value("${chefstool.seguridad.refresh-dias:30}") long refreshDias) {
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
        this.miembroRepository = miembroRepository;
        this.invitacionRepository = invitacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.refreshRepository = refreshRepository;
        this.localRepository = localRepository;
        this.horasToken = horasToken;
        this.accessMinutos = accessMinutos;
        this.refreshDias = refreshDias;
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
        usuario.setPasswordHash(passwordEncoder.encode(clave(peticion.password())));
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

        if (!passwordEncoder.matches(clave(peticion.password()), usuario.getPasswordHash())) {
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

    /**
     * Los teclados de movil a veces anaden un espacio al final (autocompletar o dictado);
     * se ignora para no rechazar una contrasena que el usuario ha escrito bien.
     */
    private String clave(String password) {
        return password == null ? "" : password.trim();
    }

    @Transactional
    public SesionResponse refrescar(String refreshToken) {
        String hash = hash(refreshToken);
        SesionRefresh sesion = refreshRepository.findByTokenHashAndRevocadoAtIsNull(hash)
                .orElseThrow(CredencialesInvalidasException::new);
        if (sesion.getExpiraAt().isBefore(OffsetDateTime.now())) {
            sesion.setRevocadoAt(OffsetDateTime.now());
            refreshRepository.save(sesion);
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = usuarioRepository.findById(sesion.getUsuarioId())
                .orElseThrow(CredencialesInvalidasException::new);
        Organizacion organizacion = organizacionRepository.findById(sesion.getOrganizacionId())
                .orElseThrow(CredencialesInvalidasException::new);
        MiembroOrganizacion miembro = miembroRepository
                .findByOrganizacionIdAndUsuarioId(organizacion.getId(), usuario.getId())
                .orElseThrow(CredencialesInvalidasException::new);

        sesion.setRevocadoAt(OffsetDateTime.now());
        refreshRepository.save(sesion);
        return sesion(usuario, organizacion, miembro.getRol(), sesion.getLocalId());
    }

    @Transactional
    public void cerrarSesion(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshRepository.findByTokenHashAndRevocadoAtIsNull(hash(refreshToken))
                .ifPresent(sesion -> {
                    sesion.setRevocadoAt(OffsetDateTime.now());
                    refreshRepository.save(sesion);
                });
    }

    @Transactional
    public SesionResponse cambiarLocal(UUID usuarioId, UUID localId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(CredencialesInvalidasException::new);
        MiembroOrganizacion miembro = miembroRepository
                .findFirstByUsuarioIdOrderByCreatedAtAsc(usuarioId)
                .orElseThrow(CredencialesInvalidasException::new);
        Organizacion organizacion = organizacionRepository.findById(miembro.getOrganizacionId())
                .orElseThrow(CredencialesInvalidasException::new);
        com.chefstool.backend.domain.model.Local local = localRepository
                .findByIdAndOrganizacionIdAndDeletedAtIsNull(localId, organizacion.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Local no encontrado"));
        return sesion(usuario, organizacion, miembro.getRol(), local.getId());
    }

    private SesionResponse sesion(Usuario usuario, Organizacion organizacion, String rol) {
        return sesion(usuario, organizacion, rol, localPrincipal(organizacion.getId()));
    }

    private SesionResponse sesion(
            Usuario usuario, Organizacion organizacion, String rol, UUID localId) {
        if (localId == null) {
            localId = localPrincipal(organizacion.getId());
        }

        Instant ahora = Instant.now();
        Instant expiraAccess = ahora.plus(Duration.ofMinutes(accessMinutos));

        var claimsBuilder = JwtClaimsSet.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("nombre", usuario.getNombre())
                .claim("organizacionId", organizacion.getId().toString())
                .claim("organizacion", organizacion.getNombre())
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(expiraAccess);
        if (localId != null) {
            claimsBuilder.claim("localId", localId.toString());
        }
        JwtClaimsSet claims = claimsBuilder.build();

        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token =
                jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();

        String refreshToken = refreshToken();
        SesionRefresh sesionRefresh = new SesionRefresh();
        sesionRefresh.setUsuarioId(usuario.getId());
        sesionRefresh.setOrganizacionId(organizacion.getId());
        sesionRefresh.setLocalId(localId);
        sesionRefresh.setTokenHash(hash(refreshToken));
        sesionRefresh.setExpiraAt(OffsetDateTime.now().plusDays(refreshDias));
        refreshRepository.save(sesionRefresh);

        return new SesionResponse(
                token,
                refreshToken,
                expiraAccess,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                organizacion.getId(),
                organizacion.getNombre(),
                localId,
                rol);
    }

    private UUID localPrincipal(UUID organizacionId) {
        java.util.List<com.chefstool.backend.domain.model.Local> locales =
                localRepository.findByOrganizacionIdAndDeletedAtIsNullAndActivoTrueOrderByNombreAsc(
                        organizacionId);
        if (!locales.isEmpty()) {
            return locales.getFirst().getId();
        }
        com.chefstool.backend.domain.model.Local principal =
                new com.chefstool.backend.domain.model.Local();
        principal.setOrganizacionId(organizacionId);
        principal.setNombre("Principal");
        principal.setActivo(true);
        return localRepository.save(principal).getId();
    }

    private String refreshToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
