package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefstool.backend.adapter.in.web.dto.ProductoRequest;
import com.chefstool.backend.adapter.in.web.dto.ProductoResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportacionProductosServiceTest {

    private ProductoService productos;
    private ImportacionProductosService servicio;

    @BeforeEach
    void preparar() {
        productos = mock(ProductoService.class);
        when(productos.crear(any(ProductoRequest.class)))
                .thenReturn(new ProductoResponse(UUID.randomUUID(), "x", null, "kg"));
        servicio = new ImportacionProductosService(productos);
    }

    @Test
    void importaCsvConCabeceraYPuntoYComa() {
        String csv =
                """
                Nombre;Categoría;Unidad
                Tomate pera;Verdura;kg
                Cebolla;Verdura;kilos
                Aceite de oliva;Aceites;litros
                """;

        var resumen = servicio.importar(csv);

        assertThat(resumen.creados()).isEqualTo(3);
        assertThat(resumen.omitidos()).isZero();
        verify(productos).crear(new ProductoRequest("Tomate pera", "Verdura", "kg"));
        verify(productos).crear(new ProductoRequest("Cebolla", "Verdura", "kg"));
        verify(productos).crear(new ProductoRequest("Aceite de oliva", "Aceites", "l"));
    }

    @Test
    void aceptaComaYComillas() {
        String csv =
                """
                "Pimiento rojo, asado",Verdura,kg
                Sal,,kg
                """;

        var resumen = servicio.importar(csv);

        assertThat(resumen.creados()).isEqualTo(2);
        verify(productos).crear(new ProductoRequest("Pimiento rojo, asado", "Verdura", "kg"));
        verify(productos).crear(new ProductoRequest("Sal", null, "kg"));
    }

    @Test
    void cuentaLosDuplicadosSinCortarLaImportacion() {
        when(productos.crear(any(ProductoRequest.class)))
                .thenReturn(new ProductoResponse(UUID.randomUUID(), "x", null, "kg"))
                .thenThrow(new IllegalArgumentException("Ya existe un producto llamado Cebolla"))
                .thenReturn(new ProductoResponse(UUID.randomUUID(), "y", null, "kg"));

        var resumen = servicio.importar("Tomate\nCebolla\nSal\n");

        assertThat(resumen.creados()).isEqualTo(2);
        assertThat(resumen.omitidos()).isEqualTo(1);
        assertThat(resumen.errores()).anyMatch(error -> error.contains("Cebolla"));
        verify(productos, times(3)).crear(any(ProductoRequest.class));
    }

    @Test
    void ignoraLineasVaciasYElBomDeExcel() {
        var resumen = servicio.importar("\uFEFFTomate;;\n\n   \nCebolla;;\n");

        assertThat(resumen.creados()).isEqualTo(2);
    }

    @Test
    void unArchivoVacioAvisa() {
        assertThatThrownBy(() -> servicio.importar(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
