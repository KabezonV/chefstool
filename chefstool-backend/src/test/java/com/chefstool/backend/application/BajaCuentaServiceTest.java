package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefstool.backend.adapter.in.web.ConflictoException;
import com.chefstool.backend.adapter.in.web.CredencialesInvalidasException;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.InvitacionRepository;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import com.chefstool.backend.domain.port.out.OrganizacionRepository;
import com.chefstool.backend.domain.port.out.RecuperacionPasswordRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BajaCuentaServiceTest {

    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MiembroOrganizacionRepository miembroRepository;
    @Mock private OrganizacionRepository organizacionRepository;
    @Mock private InvitacionRepository invitacionRepository;
    @Mock private RecuperacionPasswordRepository recuperacionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private BajaCuentaService servicio;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(USUARIO);
        usuario.setEmail("chef@demo.es");
        usuario.setNombre("Chef Demo");
        usuario.setPasswordHash("hash");
    }

    private MiembroOrganizacion pertenencia(String rol) {
        MiembroOrganizacion miembro = new MiembroOrganizacion();
        miembro.setId(UUID.randomUUID());
        miembro.setOrganizacionId(ORG);
        miembro.setUsuarioId(USUARIO);
        miembro.setEmail("chef@demo.es");
        miembro.setRol(rol);
        return miembro;
    }

    @Test
    void noBorraNadaSiLaContrasenaNoCoincide() {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra", "hash")).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(USUARIO, "otra"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void borraLaOrganizacionCuandoEraElUnicoMiembro() {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("chefstool2026", "hash")).thenReturn(true);
        when(miembroRepository.findByUsuarioId(USUARIO)).thenReturn(List.of(pertenencia("propietario")));
        when(miembroRepository.findByOrganizacionId(ORG)).thenReturn(List.of(pertenencia("propietario")));
        when(invitacionRepository.findByInvitadoPor(USUARIO)).thenReturn(List.of());
        when(recuperacionRepository.findByUsuarioIdAndUsadoAtIsNull(USUARIO)).thenReturn(List.of());

        servicio.eliminar(USUARIO, "chefstool2026");

        verify(organizacionRepository).deleteById(ORG);
        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void noBorraLaOrganizacionSiQuedaMasGente() {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("chefstool2026", "hash")).thenReturn(true);
        when(miembroRepository.findByUsuarioId(USUARIO))
                .thenReturn(List.of(pertenencia("miembro")))
                .thenReturn(List.of(pertenencia("miembro")));
        when(miembroRepository.findByOrganizacionId(ORG))
                .thenReturn(List.of(pertenencia("miembro"), pertenencia("propietario")));
        when(invitacionRepository.findByInvitadoPor(USUARIO)).thenReturn(List.of());
        when(recuperacionRepository.findByUsuarioIdAndUsadoAtIsNull(USUARIO)).thenReturn(List.of());

        servicio.eliminar(USUARIO, "chefstool2026");

        verify(organizacionRepository, never()).deleteById(any());
        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void bloqueaLaBajaSiEsElUnicoPropietarioConEquipo() {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("chefstool2026", "hash")).thenReturn(true);
        when(miembroRepository.findByUsuarioId(USUARIO)).thenReturn(List.of(pertenencia("propietario")));
        when(miembroRepository.findByOrganizacionId(ORG))
                .thenReturn(List.of(pertenencia("propietario"), pertenencia("miembro")));
        when(miembroRepository.countByOrganizacionIdAndRol(ORG, "propietario")).thenReturn(1L);

        assertThatThrownBy(() -> servicio.eliminar(USUARIO, "chefstool2026"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("propietario");

        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void exportaDatosPersonalesConSusOrganizaciones() {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(miembroRepository.findByUsuarioId(USUARIO)).thenReturn(List.of(pertenencia("admin")));

        BajaCuentaService.DatosPersonales datos = servicio.datosPersonales(USUARIO);

        assertThat(datos.email()).isEqualTo("chef@demo.es");
        assertThat(datos.organizaciones()).hasSize(1);
        assertThat(datos.organizaciones().get(0).rol()).isEqualTo("admin");
    }
}
