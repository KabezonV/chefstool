package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.ProductoRequest;
import com.chefstool.backend.adapter.in.web.dto.ProductoResponse;
import java.util.Optional;
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

    @Test
    void actualizaProductoExistente() {
        Producto existente = producto("Tomate");
        when(productoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(productoRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                        ORG, "Tomate pera"))
                .thenReturn(false);
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponse actualizado =
                productoService.actualizar(
                        existente.getId(), new ProductoRequest("Tomate pera", "Verdura", "kg"));

        assertThat(actualizado.nombre()).isEqualTo("Tomate pera");
        assertThat(actualizado.categoria()).isEqualTo("Verdura");
    }

    @Test
    void permiteMantenerElMismoNombreAlActualizar() {
        Producto existente = producto("Tomate");
        when(productoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productoService.actualizar(
                existente.getId(), new ProductoRequest("tomate", "Verdura", "kg"));

        verify(productoRepository, never())
                .existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(any(), any());
    }

    @Test
    void eliminarAplicaBorradoLogico() {
        Producto existente = producto("Tomate");
        when(productoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

        productoService.eliminar(existente.getId());

        assertThat(existente.getDeletedAt()).isNotNull();
    }

    @Test
    void actualizarProductoInexistenteDevuelve404() {
        UUID id = UUID.randomUUID();
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> productoService.actualizar(id, new ProductoRequest("X", null, "kg")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private Producto producto(String nombre) {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setOrganizacionId(ORG);
        producto.setNombre(nombre);
        producto.setUnidadBase("kg");
        return producto;
    }
}
