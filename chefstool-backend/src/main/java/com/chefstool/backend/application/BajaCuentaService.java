package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.ConflictoException;
import com.chefstool.backend.adapter.in.web.CredencialesInvalidasException;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.InvitacionRepository;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import com.chefstool.backend.domain.port.out.OrganizacionRepository;
import com.chefstool.backend.domain.port.out.RecuperacionPasswordRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derecho de supresion (RGPD art. 17). Al darse de baja se elimina el usuario y sus
 * pertenencias. Si era el unico miembro de una organizacion, se borra la organizacion
 * completa con todos sus datos. Si la organizacion tiene mas gente, la persona debe
 * transferir antes la propiedad para no dejarla sin responsable.
 */
@Service
public class BajaCuentaService {

    private final UsuarioRepository usuarioRepository;
    private final MiembroOrganizacionRepository miembroRepository;
    private final OrganizacionRepository organizacionRepository;
    private final InvitacionRepository invitacionRepository;
    private final RecuperacionPasswordRepository recuperacionRepository;
    private final PasswordEncoder passwordEncoder;

    public BajaCuentaService(
            UsuarioRepository usuarioRepository,
            MiembroOrganizacionRepository miembroRepository,
            OrganizacionRepository organizacionRepository,
            InvitacionRepository invitacionRepository,
            RecuperacionPasswordRepository recuperacionRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.miembroRepository = miembroRepository;
        this.organizacionRepository = organizacionRepository;
        this.invitacionRepository = invitacionRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void eliminar(UUID usuarioId, String password) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no existe"));

        if (password == null || !passwordEncoder.matches(password.trim(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("La contrasena no es correcta");
        }

        List<MiembroOrganizacion> pertenencias = miembroRepository.findByUsuarioId(usuarioId);
        comprobarPropiedad(pertenencias);

        List<UUID> organizacionesVacias = new ArrayList<>();
        for (MiembroOrganizacion pertenencia : pertenencias) {
            UUID organizacionId = pertenencia.getOrganizacionId();
            if (miembroRepository.findByOrganizacionId(organizacionId).size() == 1) {
                organizacionesVacias.add(organizacionId);
            } else {
                miembroRepository.delete(pertenencia);
            }
        }
        miembroRepository.flush();

        // La pertenencia de las organizaciones vacias la borra el cascade de la base de datos
        organizacionesVacias.forEach(organizacionRepository::deleteById);
        invitacionRepository.deleteAll(invitacionRepository.findByInvitadoPor(usuarioId));
        recuperacionRepository.deleteAll(
                recuperacionRepository.findByUsuarioIdAndUsadoAtIsNull(usuarioId));
        usuarioRepository.delete(usuario);
    }

    /** Exporta los datos personales del usuario (RGPD art. 20, portabilidad). */
    @Transactional(readOnly = true)
    public DatosPersonales datosPersonales(UUID usuarioId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no existe"));

        List<DatosPersonales.Pertenencia> pertenencias =
                miembroRepository.findByUsuarioId(usuarioId).stream()
                        .map(
                                m ->
                                        new DatosPersonales.Pertenencia(
                                                m.getOrganizacionId(), m.getRol(), m.getCreatedAt()))
                        .toList();

        return new DatosPersonales(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getCreatedAt(),
                pertenencias);
    }

    private void comprobarPropiedad(List<MiembroOrganizacion> pertenencias) {
        for (MiembroOrganizacion pertenencia : pertenencias) {
            if (!"propietario".equals(pertenencia.getRol())) {
                continue;
            }
            UUID organizacionId = pertenencia.getOrganizacionId();
            boolean hayOtraGente = miembroRepository.findByOrganizacionId(organizacionId).size() > 1;
            boolean hayOtroPropietario =
                    miembroRepository.countByOrganizacionIdAndRol(organizacionId, "propietario") > 1;
            if (hayOtraGente && !hayOtroPropietario) {
                throw new ConflictoException(
                        "Eres el unico propietario de una organizacion con mas personas. "
                                + "Nombra otro propietario antes de borrar tu cuenta.");
            }
        }
    }

    /** Datos personales exportables en JSON. */
    public record DatosPersonales(
            UUID id,
            String email,
            String nombre,
            java.time.OffsetDateTime alta,
            List<Pertenencia> organizaciones) {

        public record Pertenencia(UUID organizacionId, String rol, java.time.OffsetDateTime desde) {}
    }
}
