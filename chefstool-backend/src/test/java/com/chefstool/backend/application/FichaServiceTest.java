package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefstool.backend.domain.model.Ficha;
import com.chefstool.backend.domain.model.FichaLinea;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.model.ProveedorProducto;
import com.chefstool.backend.domain.port.out.FichaLineaRepository;
import com.chefstool.backend.domain.port.out.FichaRepository;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorProductoRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.FichaDetalle;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FichaServiceTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private FichaRepository fichaRepository;
    @Mock private FichaLineaRepository fichaLineaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorProductoRepository proveedorProductoRepository;
    @Mock private OrganizacionService organizacionService;
    @InjectMocks private FichaService fichaService;

    @Test
    void calculaElCosteConElMejorPrecioEntreProveedores() {
        Producto tomate = producto("Tomate");
        Ficha ficha = ficha(4, new BigDecimal("12.00"));

        when(fichaRepository.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(fichaLineaRepository.findByFichaId(ficha.getId()))
                .thenReturn(List.of(linea(ficha, tomate, "2")));
        when(proveedorProductoRepository.findByOrganizacionId(ORG))
                .thenReturn(
                        List.of(
                                catalogo(tomate, "10.00"),
                                catalogo(tomate, "8.00"),
                                catalogo(tomate, "9.50")));

        FichaDetalle detalle = fichaService.obtener(ficha.getId());

        assertThat(detalle.costeTotal()).isEqualByComparingTo("16.00");
        assertThat(detalle.costePorRacion()).isEqualByComparingTo("4.00");
        assertThat(detalle.margenPct()).isEqualByComparingTo("66.67");
        assertThat(detalle.lineas().get(0).precioUnitario()).isEqualByComparingTo("8.00");
        assertThat(detalle.productosSinPrecio()).isZero();
    }

    @Test
    void cuentaLosProductosSinPrecioYNoSumaSuCoste() {
        Producto tomate = producto("Tomate");
        Producto especias = producto("Pimienta");
        Ficha ficha = ficha(2, new BigDecimal("10.00"));

        when(fichaRepository.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(fichaLineaRepository.findByFichaId(ficha.getId()))
                .thenReturn(
                        List.of(
                                linea(ficha, tomate, "1"),
                                linea(ficha, especias, "1")));
        when(proveedorProductoRepository.findByOrganizacionId(ORG))
                .thenReturn(List.of(catalogo(tomate, "5.00")));

        FichaDetalle detalle = fichaService.obtener(ficha.getId());

        assertThat(detalle.costeTotal()).isEqualByComparingTo("5.00");
        assertThat(detalle.productosSinPrecio()).isEqualTo(1);
        assertThat(detalle.lineas().get(1).precioUnitario()).isNull();
        assertThat(detalle.lineas().get(1).coste()).isNull();
    }

    @Test
    void elMargenEsNuloSiNoHayPrecioDeVenta() {
        Producto tomate = producto("Tomate");
        Ficha ficha = ficha(2, null);

        when(fichaRepository.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(fichaLineaRepository.findByFichaId(ficha.getId()))
                .thenReturn(List.of(linea(ficha, tomate, "1")));
        when(proveedorProductoRepository.findByOrganizacionId(ORG))
                .thenReturn(List.of(catalogo(tomate, "5.00")));

        assertThat(fichaService.obtener(ficha.getId()).margenPct()).isNull();
    }

    @Test
    void elMargenPuedeSerNegativoSiSeVendePorDebajoDeCoste() {
        Producto tomate = producto("Tomate");
        Ficha ficha = ficha(1, new BigDecimal("4.00"));

        when(fichaRepository.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(fichaLineaRepository.findByFichaId(ficha.getId()))
                .thenReturn(List.of(linea(ficha, tomate, "1")));
        when(proveedorProductoRepository.findByOrganizacionId(ORG))
                .thenReturn(List.of(catalogo(tomate, "5.00")));

        assertThat(fichaService.obtener(ficha.getId()).margenPct())
                .isEqualByComparingTo("-25.00");
    }

    @Test
    void eliminarAplicaBorradoLogico() {
        Ficha ficha = ficha(1, BigDecimal.TEN);
        when(fichaRepository.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(i -> i.getArgument(0));

        fichaService.eliminar(ficha.getId());

        assertThat(ficha.getDeletedAt()).isNotNull();
    }

    @Test
    void fichaEliminadaNoSeEncuentra() {
        Ficha ficha = ficha(1, BigDecimal.TEN);
        ficha.setDeletedAt(java.time.OffsetDateTime.now());
        when(fichaRepository.findById(ficha.getId())).thenReturn(Optional.of(ficha));

        assertThatThrownBy(() -> fichaService.obtener(ficha.getId()))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(proveedorProductoRepository, never()).findByOrganizacionId(any());
    }

    private Ficha ficha(int raciones, BigDecimal precioVenta) {
        Ficha ficha = new Ficha();
        ficha.setId(UUID.randomUUID());
        ficha.setOrganizacionId(ORG);
        ficha.setNombre("Ficha de prueba");
        ficha.setRaciones(raciones);
        ficha.setPrecioVenta(precioVenta);
        return ficha;
    }

    private Producto producto(String nombre) {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setOrganizacionId(ORG);
        producto.setNombre(nombre);
        producto.setUnidadBase("kg");
        return producto;
    }

    private FichaLinea linea(Ficha ficha, Producto producto, String cantidad) {
        FichaLinea linea = new FichaLinea();
        linea.setId(UUID.randomUUID());
        linea.setFicha(ficha);
        linea.setProducto(producto);
        linea.setCantidad(new BigDecimal(cantidad));
        linea.setUnidad("kg");
        return linea;
    }

    private ProveedorProducto catalogo(Producto producto, String precio) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        proveedor.setNombre("Proveedor " + producto.getNombre());

        ProveedorProducto item = new ProveedorProducto();
        item.setId(UUID.randomUUID());
        item.setOrganizacionId(ORG);
        item.setProveedor(proveedor);
        item.setProducto(producto);
        item.setPrecioUltimo(new BigDecimal(precio));
        return item;
    }
}
