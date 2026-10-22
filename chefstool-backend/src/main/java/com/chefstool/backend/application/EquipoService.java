package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.InvitacionRequest;
import com.chefstool.backend.adapter.in.web.dto.InvitacionResponse;
import com.chefstool.backend.adapter.in.web.dto.MiembroResponse;
import com.chefstool.backend.domain.model.Invitacion;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.InvitacionRepository;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipoService {

    private static final Set<String> ROLES = Set.of("propietario", "admin", "miembro");
    private static final String ROL_POR_DEFECTO = "miembro";

    private final MiembroOrganizacionRepository miembroRepository;
    private final UsuarioRepository usuarioRepository;
    private final InvitacionRepository invitacionRepository;
    private final OrganizacionService organizacionService;

    public EquipoService(
            MiembroOrganizacionRepository miembroRepository,
            UsuarioRepository usuarioRepository,
            InvitacionRepository invitacionRepository,
            OrganizacionService organizacionService) {
        this.miembroRepository = miembroRepository;
        this.usuarioRepository = usuarioRepository;
        this.invitacionRepository = invitacionRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public List<MiembroResponse> listar() {
        return miembroRepository.findByOrganizacionId(organizacionService.organizacionActual())
                .stream()
                .map(MiembroResponse::from)
                .toList();
    }

    /**
     * Invita a un correo. Si la persona ya tiene cuenta se anade al momento; si no,
     * queda una invitacion pendiente que se resuelve cuando se registre con ese correo.
     */
    @Transactional
    public InvitacionResponse invitar(InvitacionRequest peticion) {
        UUID organizacionId = organizacionService.organizacionActual();
        String email = peticion.email().trim().toLowerCase();
        String rol = rolValido(peticion.rol());

        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(email);

        if (usuario.isPresent()) {
            boolean yaEsMiembro =
                    miembroRepository
                            .findByOrganizacionIdAndUsuarioId(organizacionId, usuario.get().getId())
                            .isPresent();
            if (yaEsMiembro) {
                throw new IllegalArgumentException("Ese correo ya pertenece a la organizacion");
            }

            MiembroOrganizacion miembro = new MiembroOrganizacion();
            miembro.setOrganizacionId(organizacionId);
            miembro.setUsuarioId(usuario.get().getId());
            miembro.setEmail(usuario.get().getEmail());
            miembro.setNombre(usuario.get().getNombre());
            miembro.setRol(rol);

            return InvitacionResponse.anadido(
                    MiembroResponse.from(miembroRepository.save(miembro)));
        }

        if (invitacionRepository.existsByOrganizacionIdAndEmailIgnoreCaseAndAceptadaAtIsNull(
                organizacionId, email)) {
            throw new IllegalArgumentException("Ya hay una invitacion pendiente para ese correo");
        }

        Invitacion invitacion = new Invitacion();
        invitacion.setOrganizacionId(organizacionId);
        invitacion.setEmail(email);
        invitacion.setRol(rol);
        invitacion.setInvitadoPor(organizacionService.usuarioActual().orElse(null));

        return InvitacionResponse.pendiente(invitacionRepository.saveAndFlush(invitacion));
    }

    @Transactional(readOnly = true)
    public List<InvitacionResponse> invitacionesPendientes() {
        return invitacionRepository
                .findByOrganizacionIdAndAceptadaAtIsNullOrderByCreatedAtAsc(
                        organizacionService.organizacionActual())
                .stream()
                .map(InvitacionResponse::pendiente)
                .toList();
    }

    @Transactional
    public void cancelarInvitacion(UUID id) {
        UUID organizacionId = organizacionService.organizacionActual();
        Invitacion invitacion =
                invitacionRepository
                        .findById(id)
                        .filter(i -> i.getOrganizacionId().equals(organizacionId))
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException("Invitacion no encontrada"));

        invitacionRepository.delete(invitacion);
    }

    @Transactional
    public MiembroResponse cambiarRol(UUID miembroId, String rol) {
        String nuevoRol = rolValido(rol);
        UUID organizacionId = organizacionService.organizacionActual();

        MiembroOrganizacion miembro =
                miembroRepository
                        .findById(miembroId)
                        .filter(m -> m.getOrganizacionId().equals(organizacionId))
                        .orElseThrow(() -> new RecursoNoEncontradoException("Miembro no encontrado"));

        boolean eraPropietario = "propietario".equals(miembro.getRol());
        if (eraPropietario
                && !"propietario".equals(nuevoRol)
                && miembroRepository.countByOrganizacionIdAndRol(organizacionId, "propietario") <= 1) {
            throw new IllegalArgumentException(
                    "La organizacion necesita al menos un propietario");
        }

        miembro.setRol(nuevoRol);
        return MiembroResponse.from(miembroRepository.save(miembro));
    }

    private String rolValido(String rol) {
        String normalizado = rol == null || rol.isBlank() ? ROL_POR_DEFECTO : rol.trim().toLowerCase();
        if (!ROLES.contains(normalizado)) {
            throw new IllegalArgumentException("Rol no valido: usa propietario, admin o miembro");
        }
        return normalizado;
    }
}
