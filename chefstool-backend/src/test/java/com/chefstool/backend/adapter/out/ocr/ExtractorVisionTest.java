package com.chefstool.backend.adapter.out.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chefstool.backend.application.DocumentoIlegibleException;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ExtractorVisionTest {

    private final ObjectMapper json = new ObjectMapper();

    private ExtractorVision extractor(String clave) {
        return new ExtractorVision(
                "https://generativelanguage.googleapis.com/v1beta",
                clave,
                "gemini-3.5-flash",
                "",
                json,
                new LectorPdf());
    }

    @Test
    void sinClaveNoEstaDisponible() {
        assertThat(extractor("").disponible()).isFalse();
        assertThat(extractor(null).disponible()).isFalse();
        assertThat(extractor("clave").disponible()).isTrue();
    }

    @Test
    void interpretaLaRespuestaDelModelo() {
        String respuesta =
                """
                {"proveedor":"Frutas Garcia S.L.","numeroDocumento":"A-2026-0451",
                 "fecha":"2026-10-21","moneda":"EUR","importeNeto":29.80,"importeIva":2.98,
                 "importeTotal":32.78,
                 "lineas":[
                   {"descripcion":"Tomate pera","cantidad":10,"unidad":"kg",
                    "precioUnitario":2.10,"importe":21.00},
                   {"descripcion":"Cebolla","cantidad":8,"unidad":"kg",
                    "precioUnitario":1.10,"importe":8.80}]}
                """;

        ResultadoExtraccion resultado =
                extractor("clave").interpretar(json.readTree(respuesta), UUID.randomUUID());

        assertThat(resultado.extraccion().proveedorNombre()).isEqualTo("Frutas Garcia S.L.");
        assertThat(resultado.extraccion().numeroDocumento()).isEqualTo("A-2026-0451");
        assertThat(resultado.extraccion().fecha()).hasToString("2026-10-21");
        assertThat(resultado.extraccion().importeTotal()).isEqualByComparingTo("32.78");
        assertThat(resultado.extraccion().lineas()).hasSize(2);
        assertThat(resultado.extraccion().lineas().getFirst().unidad()).isEqualTo("kg");
        assertThat(resultado.confianza()).isGreaterThan(new java.math.BigDecimal("0.9"));
    }

    @Test
    void calculaImportesQueFaltan() {
        String respuesta =
                """
                {"proveedor":"Proveedor","lineas":[
                  {"descripcion":"Aceite","cantidad":2,"precioUnitario":6.50}]}
                """;

        ResultadoExtraccion resultado =
                extractor("clave").interpretar(json.readTree(respuesta), UUID.randomUUID());

        assertThat(resultado.extraccion().lineas().getFirst().importe())
                .isEqualByComparingTo("13.00");
        assertThat(resultado.extraccion().importeTotal()).isEqualByComparingTo("13.00");
    }

    @Test
    void aceptaNumerosEnFormatoEspanol() {
        String respuesta =
                """
                {"proveedor":"Proveedor","importeTotal":"1.234,56",
                 "lineas":[{"descripcion":"Caja","cantidad":"1","precioUnitario":"1.234,56",
                            "importe":"1.234,56"}]}
                """;

        ResultadoExtraccion resultado =
                extractor("clave").interpretar(json.readTree(respuesta), UUID.randomUUID());

        assertThat(resultado.extraccion().importeTotal()).isEqualByComparingTo("1234.56");
    }

    @Test
    void ignoraLosBloquesDeMarkdown() {
        String texto = "```json\n{\"proveedor\":\"X\"}\n```";

        assertThat(extractor("clave").limpiarJson(texto)).isEqualTo("{\"proveedor\":\"X\"}");
    }

    @Test
    void siNoReconoceNadaAvisa() {
        String respuesta = "{\"proveedor\":null,\"lineas\":[],\"importeTotal\":null}";

        assertThatThrownBy(
                        () ->
                                extractor("clave")
                                        .interpretar(json.readTree(respuesta), UUID.randomUUID()))
                .isInstanceOf(DocumentoIlegibleException.class);
    }

    @Test
    void reintentaCuandoLaSumaNoCuadra() {
        ExtractorVision extractor = extractor("clave");

        assertThat(extractor.hayQueReintentar(extraccion("45.11", "27.63"))).isTrue();
        assertThat(extractor.hayQueReintentar(extraccion("32.78", "32.78"))).isFalse();
        // Las cuentas cuadran y aun asi no hay ni una linea: hay que releer.
        assertThat(extractor.hayQueReintentar(extraccion("32.78"))).isTrue();
        // Sin base imponible no hay con que comparar: se reintenta por si no se leyo.
        assertThat(extractor.hayQueReintentar(extraccion("0", "12.00"))).isTrue();
    }

    @Test
    void seQuedaConLaLecturaQueCuadra() {
        ExtractorVision extractor = extractor("clave");
        ResultadoExtraccion mala = resultado(extraccion("45.11", "27.63"));
        ResultadoExtraccion buena = resultado(extraccion("45.11", "45.11"));

        assertThat(extractor.elegir(mala, buena)).isSameAs(buena);
        assertThat(extractor.elegir(buena, mala)).isSameAs(buena);
        // Si la segunda no trae nada, se queda la primera.
        assertThat(extractor.elegir(mala, resultado(extraccion("45.11")))).isSameAs(mala);
    }

    @Test
    void filtraTotalesEImponeUnaDescripcionRealEnLaLinea() {
        String respuesta = """
            {"proveedor":"Proveedor","importeNeto":13.00,"importeIva":1.30,
             "importeTotal":14.30,"lineas":[
               {"descripcion":"Aceite","cantidad":2,"precioUnitario":6.50,"importe":13.00},
               {"descripcion":"IVA 10%","cantidad":1,"precioUnitario":1.30,"importe":1.30},
               {"descripcion":"TOTAL","cantidad":1,"precioUnitario":14.30,"importe":14.30}]}
            """;

        ResultadoExtraccion resultado =
                extractor("clave").interpretar(json.readTree(respuesta), UUID.randomUUID());

        assertThat(resultado.extraccion().lineas()).hasSize(1);
        assertThat(resultado.extraccion().lineas().getFirst().descripcion()).isEqualTo("Aceite");
    }

    @Test
    void completaPrecioYCantidadCuandoLaIASoloTraeElImporte() {
        String respuesta = """
            {"proveedor":"Proveedor","importeTotal":"14,30","lineas":[
              {"descripcion":"Aceite","importe":"14,30"}]}
            """;

        ResultadoExtraccion resultado =
                extractor("clave").interpretar(json.readTree(respuesta), UUID.randomUUID());
        var linea = resultado.extraccion().lineas().getFirst();

        assertThat(linea.cantidad()).isEqualByComparingTo("1");
        assertThat(linea.precioUnitario()).isEqualByComparingTo("14.30");
        assertThat(linea.importe()).isEqualByComparingTo("14.30");
    }

    @Test
    void aceptaFechasDeAlbaranEnFormatoEspañol() {
        String respuesta = """
            {"proveedor":"Proveedor","fecha":"08/11/2026","importeTotal":10,
             "lineas":[{"descripcion":"Producto","cantidad":1,"precioUnitario":10,"importe":10}]}
            """;

        ResultadoExtraccion resultado =
                extractor("clave").interpretar(json.readTree(respuesta), UUID.randomUUID());

        assertThat(resultado.extraccion().fecha()).isEqualTo(java.time.LocalDate.of(2026, 11, 8));
    }

    private com.chefstool.backend.domain.model.ExtraccionDocumento extraccion(
            String neto, String... importes) {
        java.util.List<com.chefstool.backend.domain.model.LineaExtraccion> lineas =
                java.util.Arrays.stream(importes)
                        .map(
                                importe ->
                                        new com.chefstool.backend.domain.model.LineaExtraccion(
                                                "Articulo",
                                                java.math.BigDecimal.ONE,
                                                "ud",
                                                new java.math.BigDecimal(importe),
                                                new java.math.BigDecimal(importe),
                                                null,
                                                null,
                                                null))
                        .toList();
        return new com.chefstool.backend.domain.model.ExtraccionDocumento(
                null,
                null,
                null,
                null,
                "EUR",
                new java.math.BigDecimal(neto),
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                lineas);
    }

    private ResultadoExtraccion resultado(
            com.chefstool.backend.domain.model.ExtraccionDocumento extraccion) {
        return new ResultadoExtraccion(extraccion, new java.math.BigDecimal("0.9"));
    }
}
