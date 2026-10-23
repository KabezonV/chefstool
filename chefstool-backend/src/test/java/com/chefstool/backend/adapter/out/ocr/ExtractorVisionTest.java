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
}
