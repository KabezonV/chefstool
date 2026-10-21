package com.chefstool.backend.adapter.out.ocr;

import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ExtractorDocumento;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Extractor de demostracion: no lee el archivo, pero devuelve una extraccion realista
 * construida con el catalogo de la organizacion para poder recorrer el flujo completo.
 */
@Service
public class ExtractorDemo implements ExtractorDocumento {

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    public ExtractorDemo(
            ProductoRepository productoRepository, ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public ResultadoExtraccion extraer(
            byte[] contenido, String nombreOriginal, String tipoMime, UUID organizacionId) {
        Random random = new Random(semilla(contenido, nombreOriginal));

        List<Proveedor> proveedores =
                proveedorRepository.findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(
                        organizacionId);
        Proveedor proveedor = proveedores.isEmpty() ? null : proveedores.get(random.nextInt(proveedores.size()));

        List<Producto> productos =
                productoRepository.findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(
                        organizacionId);

        List<LineaExtraccion> lineas = new ArrayList<>();
        if (productos.isEmpty()) {
            lineas.add(linea("Caja de tomate", "kg", 8, "2.40", random, null));
            lineas.add(linea("Saco de cebolla", "kg", 12, "1.15", random, null));
        } else {
            int cuantas = Math.min(productos.size(), 3);
            for (int i = 0; i < cuantas; i++) {
                Producto producto = productos.get(i);
                int cantidad = 1 + random.nextInt(12);
                BigDecimal base =
                        BigDecimal.valueOf(1 + random.nextInt(1800), 2);
                lineas.add(
                        linea(
                                producto.getNombre(),
                                producto.getUnidadBase(),
                                cantidad,
                                base.toPlainString(),
                                random,
                                producto.getId()));
            }
        }

        BigDecimal neto =
                lineas.stream()
                        .map(LineaExtraccion::importe)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva = neto.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = neto.add(iva);

        ExtraccionDocumento extraccion = new ExtraccionDocumento(
                proveedor != null ? proveedor.getNombre() : "Proveedor de demostracion",
                proveedor != null ? proveedor.getId() : null,
                "ALB-" + (1000 + random.nextInt(9000)),
                LocalDate.now(),
                "EUR",
                neto.setScale(2, RoundingMode.HALF_UP),
                iva,
                total,
                lineas);

        return new ResultadoExtraccion(extraccion, new BigDecimal("0.94"));
    }

    private LineaExtraccion linea(
            String descripcion,
            String unidad,
            int cantidad,
            String precio,
            Random random,
            UUID productoId) {
        BigDecimal precioUnitario = new BigDecimal(precio);
        BigDecimal importe =
                precioUnitario
                        .multiply(BigDecimal.valueOf(cantidad))
                        .setScale(2, RoundingMode.HALF_UP);
        return new LineaExtraccion(
                descripcion,
                BigDecimal.valueOf(cantidad),
                unidad,
                precioUnitario,
                importe,
                productoId,
                null,
                null);
    }

    private long semilla(byte[] contenido, String nombreOriginal) {
        long semilla = nombreOriginal == null ? 0 : nombreOriginal.hashCode();
        return semilla + (contenido == null ? 0 : contenido.length);
    }
}
