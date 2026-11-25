package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chefstool.backend.adapter.in.web.dto.LocalRequest;
import com.chefstool.backend.domain.model.Local;
import com.chefstool.backend.domain.port.out.LocalRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalServiceTest {
    @Test
    void creaUnLocalEnLaOrganizacionActiva() {
        LocalRepository repositorio = mock(LocalRepository.class);
        OrganizacionService organizacion = mock(OrganizacionService.class);
        UUID organizacionId = UUID.randomUUID();
        when(organizacion.organizacionActual()).thenReturn(organizacionId);
        when(repositorio.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                organizacionId, "Salón") ).thenReturn(false);
        when(repositorio.save(org.mockito.ArgumentMatchers.any(Local.class)))
                .thenAnswer(invocacion -> {
                    Local local = invocacion.getArgument(0);
                    local.setId(UUID.randomUUID());
                    return local;
                });

        LocalService servicio = new LocalService(repositorio, organizacion);
        var creado = servicio.crear(new LocalRequest("  Salón  "));

        assertThat(creado.nombre()).isEqualTo("Salón");
        assertThat(creado.activo()).isTrue();
    }
}
