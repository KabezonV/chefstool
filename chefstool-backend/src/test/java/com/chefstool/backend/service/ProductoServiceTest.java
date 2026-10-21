package com.chefstool.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefstool.backend.domain.Producto;
import com.chefstool.backend.repository.ProductoRepository;
import com.chefstool.backend.web.dto.ProductoRequest;
import com.chefstool.backend.web.dto.ProductoResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private ProductoRepository productoRepository;
    @Mock private OrganizacionService organizacionService;
    @InjectMocks private ProductoService productoService;

    @BeforeEach
    void setUp() {
        lenient().when(organizacionService.organizacionActual()).thenReturn(ORG);
    }

    @Test
    void creaProductoRecortandoEspaciosYConUnidadPorDefecto() {
        when(productoRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                        ORG, "Sal"))
                .thenReturn(false);
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> {
                    Producto producto = invocation.getArgument(0);
                    producto.setId(UUID.randomUUID());
                    return producto;
                });

        ProductoResponse creado = productoService.crear(new ProductoRequest("  Sal  ", null, null));

        assertThat(creado.nombre()).isEqualTo("Sal");
        assertThat(creado.unidadBase()).isEqualTo("ud");
        assertThat(creado.id()).isNotNull();
    }

    @Test
    void respetaLaUnidadIndicada() {
        when(productoRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                        eq(ORG), eq("Harina")))
                .thenReturn(false);
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponse creado =
                productoService.crear(new ProductoRequest("Harina", "Secos", "kg"));

        assertThat(creado.unidadBase()).isEqualTo("kg");
        assertThat(creado.categoria()).isEqualTo("Secos");
    }

    @Test
    void rechazaNombresDuplicados() {
        when(productoRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                        eq(ORG), eq("Tomate")))
                .thenReturn(true);

        assertThatThrownBy(
                        () -> productoService.crear(new ProductoRequest("Tomate", null, "kg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tomate");

        verify(productoRepository, never()).save(any(Producto.class));
    }
}
