package com.chefstool.backend.application;

import com.chefstool.backend.IntegracionBase;
import static org.assertj.core.api.Assertions.assertThat;

import com.chefstool.backend.domain.model.AlertaPrecio;
import com.chefstool.backend.domain.model.DocumentoCompra;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.port.out.AlertaPrecioRepository;
import com.chefstool.backend.domain.port.out.DocumentoCompraRepository;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import com.chefstool.backend.adapter.in.web.dto.CompraResponse;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
class CompraServiceTest extends IntegracionBase {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private CompraService compraService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private DocumentoCompraRepository documentoRepository;
    @Autowired private AlertaPrecioRepository alertaPrecioRepository;

    @Test
    void creaCompraConLineas() {
        Producto producto = producto();
        Proveedor proveedor = proveedor();

        CompraResponse compra =
                compraService.crearDesdeDocumento(documento(), extraccion(proveedor, producto, "10.00"));

        assertThat(compra.id()).isNotNull();
        assertThat(compra.numeroLineas()).isEqualTo(1);
        assertThat(compra.proveedorNombre()).isEqualTo(proveedor.getNombre());
        assertThat(compra.importeTotal()).isEqualByComparingTo("33.00");
    }

    @Test
    void generaAlertaCuandoSubeElPrecioMasDeUnCincoPorCiento() {
        Producto producto = producto();
        Proveedor proveedor = proveedor();

        compraService.crearDesdeDocumento(documento(), extraccion(proveedor, producto, "10.00"));
        compraService.crearDesdeDocumento(documento(), extraccion(proveedor, producto, "12.50"));

        List<AlertaPrecio> alertas =
                alertaPrecioRepository.findByOrganizacionIdAndReconocidaFalseOrderByCreatedAtDesc(
                        ORG);

        assertThat(alertas)
                .filteredOn(alerta -> alerta.getProducto().getId().equals(producto.getId()))
                .singleElement()
                .satisfies(
                        alerta -> {
                            assertThat(alerta.getPorcentajeCambio())
                                    .isEqualByComparingTo("25.00");
                            assertThat(alerta.getSeveridad()).isEqualTo("critico");
                            assertThat(alerta.getPrecioAnterior()).isEqualByComparingTo("10.00");
                            assertThat(alerta.getPrecioNuevo()).isEqualByComparingTo("12.50");
                        });
    }

    @Test
    void noGeneraAlertaSiElPrecioSubeMenosDelUmbral() {
        Producto producto = producto();
        Proveedor proveedor = proveedor();

        compraService.crearDesdeDocumento(documento(), extraccion(proveedor, producto, "10.00"));
        compraService.crearDesdeDocumento(documento(), extraccion(proveedor, producto, "10.20"));

        List<AlertaPrecio> alertas =
                alertaPrecioRepository.findByOrganizacionIdAndReconocidaFalseOrderByCreatedAtDesc(
                        ORG);

        assertThat(alertas)
                .noneMatch(alerta -> alerta.getProducto().getId().equals(producto.getId()));
    }

    @Test
    void creaElProveedorCuandoElNombreDelAlbaranNoEstaEnElCatalogo() {
        Producto producto = producto();

        CompraResponse compra =
                compraService.crearDesdeDocumento(
                        documento(),
                        extraccionConNombre("  Distribuciones   Makro SL  ", producto));

        assertThat(compra.proveedorNombre()).isEqualTo("Distribuciones Makro SL");
        assertThat(proveedores())
                .anyMatch(proveedor -> proveedor.getNombre().equals("Distribuciones Makro SL"));
    }

    @Test
    void reutilizaElProveedorExistenteAunqueCambienMayusculasYEspacios() {
        Producto producto = producto();
        Proveedor existente = proveedor();
        long antes = proveedores().size();

        CompraResponse compra =
                compraService.crearDesdeDocumento(
                        documento(),
                        extraccionConNombre("  " + existente.getNombre().toUpperCase() + "  ", producto));

        assertThat(compra.proveedorNombre()).isEqualTo(existente.getNombre());
        assertThat(proveedores()).hasSize((int) antes);
    }

    @Test
    void noCreaProveedorSiElNombreNoPareceReal() {
        Producto producto = producto();
        long antes = proveedores().size();

        CompraResponse compra =
                compraService.crearDesdeDocumento(documento(), extraccionConNombre(" 1 ", producto));

        assertThat(compra.proveedorNombre()).isNull();
        assertThat(proveedores()).hasSize((int) antes);
    }

    @Test
    void noCreaProveedorSiElNombreLlevaLaFechaPegada() {
        Producto producto = producto();
        long antes = proveedores().size();

        CompraResponse compra =
                compraService.crearDesdeDocumento(
                        documento(),
                        extraccionConNombre(
                                "Muebles Salinas Rodriguez, S.L. Fecha08/08/2024", producto));

        assertThat(compra.proveedorNombre()).isNull();
        assertThat(proveedores()).hasSize((int) antes);
    }

    private List<Proveedor> proveedores() {
        return proveedorRepository.findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(ORG);
    }

    private ExtraccionDocumento extraccionConNombre(String nombre, Producto producto) {
        BigDecimal precio = new BigDecimal("10.00");
        BigDecimal importe = precio.multiply(BigDecimal.valueOf(3));
        BigDecimal iva = importe.multiply(new BigDecimal("0.10"));

        return new ExtraccionDocumento(
                nombre,
                null,
                "ALB-" + UUID.randomUUID().toString().substring(0, 6),
                LocalDate.now(),
                "EUR",
                importe,
                iva,
                importe.add(iva),
                List.of(
                        new LineaExtraccion(
                                producto.getNombre(),
                                BigDecimal.valueOf(3),
                                producto.getUnidadBase(),
                                precio,
                                importe,
                                producto.getId(),
                                null,
                                null)));
    }

    private ExtraccionDocumento extraccion(
            Proveedor proveedor, Producto producto, String precioUnitario) {
        BigDecimal precio = new BigDecimal(precioUnitario);
        BigDecimal importe = precio.multiply(BigDecimal.valueOf(3));
        BigDecimal iva = importe.multiply(new BigDecimal("0.10"));

        return new ExtraccionDocumento(
                proveedor.getNombre(),
                proveedor.getId(),
                "ALB-" + UUID.randomUUID().toString().substring(0, 6),
                LocalDate.now(),
                "EUR",
                importe,
                iva,
                importe.add(iva),
                List.of(
                        new LineaExtraccion(
                                producto.getNombre(),
                                BigDecimal.valueOf(3),
                                producto.getUnidadBase(),
                                precio,
                                importe,
                                producto.getId(),
                                null,
                                null)));
    }

    private Producto producto() {
        Producto producto = new Producto();
        producto.setOrganizacionId(ORG);
        producto.setNombre("Producto " + UUID.randomUUID());
        producto.setUnidadBase("kg");
        return productoRepository.save(producto);
    }

    private Proveedor proveedor() {
        Proveedor proveedor = new Proveedor();
        proveedor.setOrganizacionId(ORG);
        proveedor.setNombre("Proveedor " + UUID.randomUUID());
        return proveedorRepository.save(proveedor);
    }

    private DocumentoCompra documento() {
        DocumentoCompra documento = new DocumentoCompra();
        documento.setOrganizacionId(ORG);
        documento.setNombreOriginal("albaran.pdf");
        return documentoRepository.save(documento);
    }
}
