package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefstool.backend.adapter.in.web.dto.InvitacionRequest;
import com.chefstool.backend.adapter.in.web.dto.InvitacionResponse;
import com.chefstool.backend.adapter.in.web.dto.MiembroResponse;
import com.chefstool.backend.domain.model.Invitacion;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.InvitacionRepository;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private MiembroOrganizacionRepository miembroRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InvitacionRepository invitacionRepository;
    @Mock private OrganizacionService organizacionService;
    @InjectMocks private EquipoService equipoService;

    @BeforeEach
    void setUp() {
        when(organizacionService.organizacionActual()).thenReturn(ORG);
    }

    @Test
    void invitarAUnUsuarioExistenteLoAnadeAlMomento() {
        Usuario usuario = usuario("chef@cocina.es");
        when(usuarioRepository.findByEmailIgnoreCase("chef@cocina.es"))
                .thenReturn(Optional.of(usuario));
        when(miembroRepository.findByOrganizacionIdAndUsuarioId(ORG, usuario.getId()))
                .thenReturn(Optional.empty());
        when(miembroRepository.save(any(MiembroOrganizacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        InvitacionResponse respuesta =
                equipoService.invitar(new InvitacionRequest("chef@cocina.es", "admin"));

        assertThat(respuesta.estado()).isEqualTo("anadido");
        assertThat(respuesta.rol()).isEqualTo("admin");
        verify(invitacionRepository, never()).saveAndFlush(any(Invitacion.class));
    }

    @Test
    void invitarAUnCorreoNuevoDejaLaInvitacionPendiente() {
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@cocina.es")).thenReturn(Optional.empty());
        when(invitacionRepository.existsByOrganizacionIdAndEmailIgnoreCaseAndAceptadaAtIsNull(
                        ORG, "nuevo@cocina.es"))
                .thenReturn(false);
        when(invitacionRepository.saveAndFlush(any(Invitacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        InvitacionResponse respuesta =
                equipoService.invitar(new InvitacionRequest("Nuevo@Cocina.es", null));

        assertThat(respuesta.estado()).isEqualTo("pendiente");
        assertThat(respuesta.email()).isEqualTo("nuevo@cocina.es");
        assertThat(respuesta.rol()).isEqualTo("miembro");
    }

    @Test
    void noInvitaAQuienYaEsMiembro() {
        Usuario usuario = usuario("chef@cocina.es");
        when(usuarioRepository.findByEmailIgnoreCase("chef@cocina.es"))
                .thenReturn(Optional.of(usuario));
        when(miembroRepository.findByOrganizacionIdAndUsuarioId(ORG, usuario.getId()))
                .thenReturn(Optional.of(new MiembroOrganizacion()));

        assertThatThrownBy(
                        () -> equipoService.invitar(new InvitacionRequest("chef@cocina.es", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya pertenece");
    }

    @Test
    void noDuplicaInvitacionesPendientes() {
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@cocina.es")).thenReturn(Optional.empty());
        when(invitacionRepository.existsByOrganizacionIdAndEmailIgnoreCaseAndAceptadaAtIsNull(
                        ORG, "nuevo@cocina.es"))
                .thenReturn(true);

        assertThatThrownBy(
                        () -> equipoService.invitar(new InvitacionRequest("nuevo@cocina.es", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invitacion pendiente");
    }

    @Test
    void rechazaRolesInventados() {
        assertThatThrownBy(
                        () -> equipoService.invitar(new InvitacionRequest("x@cocina.es", "dios")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rol no valido");
    }

    @Test
    void noDejaALaOrganizacionSinPropietario() {
        MiembroOrganizacion propietario = miembro("propietario");
        when(miembroRepository.findById(propietario.getId()))
                .thenReturn(Optional.of(propietario));
        when(miembroRepository.countByOrganizacionIdAndRol(ORG, "propietario")).thenReturn(1L);

        assertThatThrownBy(() -> equipoService.cambiarRol(propietario.getId(), "miembro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un propietario");
    }

    @Test
    void permiteCambiarDeRolSiHayOtroPropietario() {
        MiembroOrganizacion propietario = miembro("propietario");
        when(miembroRepository.findById(propietario.getId()))
                .thenReturn(Optional.of(propietario));
        when(miembroRepository.countByOrganizacionIdAndRol(ORG, "propietario")).thenReturn(2L);
        when(miembroRepository.save(any(MiembroOrganizacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        MiembroResponse respuesta = equipoService.cambiarRol(propietario.getId(), "admin");

        assertThat(respuesta.rol()).isEqualTo("admin");
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail(email);
        usuario.setNombre("Chef");
        usuario.setPasswordHash("hash");
        return usuario;
    }

    private MiembroOrganizacion miembro(String rol) {
        MiembroOrganizacion miembro = new MiembroOrganizacion();
        miembro.setId(UUID.randomUUID());
        miembro.setOrganizacionId(ORG);
        miembro.setUsuarioId(UUID.randomUUID());
        miembro.setEmail("chef@cocina.es");
        miembro.setRol(rol);
        return miembro;
    }
}
