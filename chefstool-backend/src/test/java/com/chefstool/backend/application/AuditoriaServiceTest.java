package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.chefstool.backend.domain.model.Auditoria;
import com.chefstool.backend.domain.port.out.AuditoriaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditoriaServiceTest {
    @Test
    void registraAccionConUsuarioYOrganizacion() {
        AuditoriaRepository repositorio = mock(AuditoriaRepository.class);
        OrganizacionService organizacion = mock(OrganizacionService.class);
        UUID organizacionId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        org.mockito.Mockito.when(organizacion.organizacionActual()).thenReturn(organizacionId);
        org.mockito.Mockito.when(organizacion.usuarioActual()).thenReturn(Optional.of(usuarioId));

        AuditoriaService servicio = new AuditoriaService(repositorio, organizacion);
        UUID entidadId = UUID.randomUUID();
        servicio.registrar("CREAR", "PRODUCTO", entidadId, "Tomate");

        org.mockito.ArgumentCaptor<Auditoria> captor =
                org.mockito.ArgumentCaptor.forClass(Auditoria.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getOrganizacionId()).isEqualTo(organizacionId);
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(usuarioId);
        assertThat(captor.getValue().getAccion()).isEqualTo("CREAR");
        assertThat(captor.getValue().getEntidad()).isEqualTo("PRODUCTO");
        assertThat(captor.getValue().getEntidadId()).isEqualTo(entidadId);
    }
}
