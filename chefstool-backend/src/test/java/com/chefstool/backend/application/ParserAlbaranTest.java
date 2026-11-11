package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParserAlbaranTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @InjectMocks private ParserAlbaran parser;

    private static final String ALBARAN =
            """
            FRUTAS GARCIA S.L.
            CIF B12345678
            ALBARAN: ALB-9911
            Fecha: 05/10/2026

            Descripcion Cantidad Precio Importe
            TOMATE PERA 10 kg 2,50 25,00
            CEBOLLA 12 kg 1,15 13,80

            Base imponible 38,80
            IVA 10% 3,88
            TOTAL 42,68
            """;

    @Test
    void interpretaUnAlbaranCompleto() {
        stubProveedores(proveedor("Frutas Garcia"));
        stubCatalogo(producto("Tomate pera"), producto("Cebolla"));

        ResultadoExtraccion resultado = parser.interpretar(ALBARAN, ORG);
        ExtraccionDocumento extraccion = resultado.extraccion();

        assertThat(extraccion.proveedorNombre()).isEqualTo("Frutas Garcia");
        assertThat(extraccion.proveedorId()).isNotNull();
        assertThat(extraccion.numeroDocumento()).isEqualTo("ALB-9911");
        assertThat(extraccion.fecha()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(extraccion.importeNeto()).isEqualByComparingTo("38.80");
        assertThat(extraccion.importeIva()).isEqualByComparingTo("3.88");
        assertThat(extraccion.importeTotal()).isEqualByComparingTo("42.68");

        assertThat(extraccion.lineas()).hasSize(2);

        LineaExtraccion tomate = extraccion.lineas().get(0);
        assertThat(tomate.descripcion()).isEqualTo("TOMATE PERA");
        assertThat(tomate.cantidad()).isEqualByComparingTo("10");
        assertThat(tomate.unidad()).isEqualTo("kg");
        assertThat(tomate.precioUnitario()).isEqualByComparingTo("2.50");
        assertThat(tomate.importe()).isEqualByComparingTo("25.00");
        assertThat(tomate.productoId()).isNotNull();

        assertThat(resultado.confianza()).isGreaterThan(new BigDecimal("0.7"));
    }

    @Test
    void relacionaCadaLineaConSuProducto() {
        stubCatalogo(producto("Tomate pera"), producto("Cebolla"));

        ExtraccionDocumento extraccion = parser.interpretar(ALBARAN, ORG).extraccion();

        assertThat(extraccion.lineas())
                .extracting(LineaExtraccion::productoId)
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void ignoraLineasCuyoImporteNoCuadra() {
        String roto =
                """
                ALBARAN: ALB-1
                TOMATE PERA 10 kg 2,50 99,00
                TOTAL 99,00
                """;

        ExtraccionDocumento extraccion = parser.interpretar(roto, ORG).extraccion();

        assertThat(extraccion.lineas()).isEmpty();
        assertThat(extraccion.importeTotal()).isEqualByComparingTo("99.00");
    }

    @Test
    void admiteLineasSinUnidadCalculandoLaCantidad() {
        String sinUnidad =
                """
                ALBARAN: ALB-2
                PIMIENTA NEGRA 8,00 16,00
                """;

        ExtraccionDocumento extraccion = parser.interpretar(sinUnidad, ORG).extraccion();

        assertThat(extraccion.lineas()).hasSize(1);
        assertThat(extraccion.lineas().get(0).cantidad()).isEqualByComparingTo("2");
        assertThat(extraccion.lineas().get(0).importe()).isEqualByComparingTo("16.00");
    }

    @Test
    void devuelveExtraccionVaciaSiNoHayTexto() {
        ResultadoExtraccion resultado = parser.interpretar("   ", ORG);

        assertThat(resultado.extraccion().lineas()).isEmpty();
        assertThat(resultado.extraccion().proveedorNombre()).isNull();
        assertThat(resultado.confianza()).isEqualByComparingTo("0");
    }

    @Test
    void interpretaImportesConMilesYComaDecimal() {
        assertThat(ParserAlbaran.numero("1.234,56")).isEqualByComparingTo("1234.56");
        assertThat(ParserAlbaran.numero("2,50")).isEqualByComparingTo("2.50");
        assertThat(ParserAlbaran.numero("12")).isEqualByComparingTo("12");
    }

    @Test
    void noDescartaUnaLineaDeAceiteDeOlivaPorContenerIva() {
        stubProveedores(proveedor("Aceites del Sur"));
        stubCatalogo(producto("Aceite de oliva virgen"));

        String texto =
                """
                ACEITES DEL SUR S.L.
                ALBARAN: ALB-77

                ACEITE DE OLIVA VIRGEN 5L 2,000 ud 38,900 77,80
                TOTAL 77,80
                """;

        ExtraccionDocumento extraccion = parser.interpretar(texto, ORG).extraccion();

        assertThat(extraccion.lineas()).hasSize(1);
        assertThat(extraccion.lineas().get(0).descripcion())
                .isEqualTo("ACEITE DE OLIVA VIRGEN");
        assertThat(extraccion.lineas().get(0).importe()).isEqualByComparingTo("77.80");
    }

    @Test
    void noAsignaUnProveedorDelCatalogoQueSoloComparteUnaPalabraGenerica() {
        stubProveedores(proveedor("Distribuciones Levante"));
        stubCatalogo();

        String texto =
                """
                DISTRIBUCIONES HORTOFRUTICOLAS EJEMPLO S.L.
                ALBARAN: A-2026/0453
                TOMATE 10 2,50 25,00
                """;

        ExtraccionDocumento extraccion = parser.interpretar(texto, ORG).extraccion();

        assertThat(extraccion.proveedorId()).isNull();
        assertThat(extraccion.proveedorNombre())
                .isEqualTo("DISTRIBUCIONES HORTOFRUTICOLAS EJEMPLO S.L.");
    }

    private Proveedor proveedor(String nombre) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        proveedor.setNombre(nombre);
        return proveedor;
    }

    private Producto producto(String nombre) {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre(nombre);
        producto.setUnidadBase("kg");
        return producto;
    }

    private void stubProveedores(Proveedor... proveedores) {
        when(proveedorRepository.findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(ORG))
                .thenReturn(List.of(proveedores));
    }

    private void stubCatalogo(Producto... productos) {
        when(productoRepository.findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(ORG))
                .thenReturn(List.of(productos));
    }

    @Test
    void noConfundeLaPalabraNumeroConElNumeroDelDocumento() {
        String texto =
                """
                FRUTAS GARCIA SL
                Albaran numero A-2026-0451
                Fecha: 21/10/2026
                Tomate pera —10kg 2,10 21,00
                Cebolla 8 kg 1,10 8,80
                TOTAL: 32,78
                """;

        var resultado = parser.interpretar(texto, ORG);

        assertThat(resultado.extraccion().numeroDocumento()).isEqualTo("A-2026-0451");
        assertThat(resultado.extraccion().lineas()).hasSize(2);
        assertThat(resultado.extraccion().lineas().getFirst().descripcion()).isEqualTo("Tomate pera");
        assertThat(resultado.extraccion().lineas().getFirst().cantidad())
                .isEqualByComparingTo("10");
    }

    @Test
    void noDejaSeparadorFinalSiElNumeroLlegaCortado() {
        String texto =
                """
                DISTRIBUCIONES MAKRO SL
                ALBARAN N: A-2026/
                Fecha: 08/11/2026
                Aceite de oliva 5L 2 24,50 49,00
                TOTAL: 49,00
                """;

        var resultado = parser.interpretar(texto, ORG);

        assertThat(resultado.extraccion().numeroDocumento()).isEqualTo("A-2026");
    }

    @Test
    void recortaLaFechaQueElOcrPegaAlNombreDelProveedor() {
        String texto =
                """
                Muebles Salinas Rodriguez Alhendin, S.L. Fecha08/08/2024
                CIF B12345678
                Albaran numero A-77
                Fecha: 08/08/2024
                """;

        var resultado = parser.interpretar(texto, ORG);

        assertThat(resultado.extraccion().proveedorNombre())
                .isEqualTo("Muebles Salinas Rodriguez Alhendin, S.L.");
    }
}
