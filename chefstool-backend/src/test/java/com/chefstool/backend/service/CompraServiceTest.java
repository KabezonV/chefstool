package com.chefstool.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chefstool.backend.domain.AlertaPrecio;
import com.chefstool.backend.domain.DocumentoCompra;
import com.chefstool.backend.domain.Producto;
import com.chefstool.backend.domain.Proveedor;
import com.chefstool.backend.repository.AlertaPrecioRepository;
import com.chefstool.backend.repository.DocumentoCompraRepository;
import com.chefstool.backend.repository.ProductoRepository;
import com.chefstool.backend.repository.ProveedorRepository;
import com.chefstool.backend.web.dto.CompraResponse;
import com.chefstool.backend.web.dto.ExtraccionDocumento;
import com.chefstool.backend.web.dto.LineaExtraccion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CompraServiceTest {

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
