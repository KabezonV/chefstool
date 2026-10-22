package com.chefstool.backend.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlmacenamientoSupabaseTest {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOC = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void subeElArchivoAlBucketConLaRutaEsperada() {
        RestClient.Builder constructor = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
        AlmacenamientoSupabase almacen =
                new AlmacenamientoSupabase(
                        constructor, "https://proyecto.supabase.co/", "clave-secreta", "documentos");

        servidor
                .expect(
                        requestTo(
                                "https://proyecto.supabase.co/storage/v1/object/documentos/"
                                        + ORG
                                        + "/"
                                        + DOC
                                        + "/albaran.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer clave-secreta"))
                .andExpect(header("x-upsert", "true"))
                .andRespond(withSuccess());

        String ruta = almacen.guardar(ORG, DOC, "albaran.pdf", "contenido".getBytes());

        assertThat(ruta).isEqualTo(ORG + "/" + DOC + "/albaran.pdf");
        servidor.verify();
    }

    @Test
    void limpiaElNombreDelArchivo() {
        RestClient.Builder constructor = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
        AlmacenamientoSupabase almacen =
                new AlmacenamientoSupabase(constructor, "https://proyecto.supabase.co", "clave", "b");

        servidor
                .expect(
                        requestTo(
                                "https://proyecto.supabase.co/storage/v1/object/b/"
                                        + ORG
                                        + "/"
                                        + DOC
                                        + "/mi_albaran.pdf"))
                .andRespond(withSuccess());

        assertThat(almacen.guardar(ORG, DOC, "../../mi albaran.pdf", new byte[] {1}))
                .endsWith("mi_albaran.pdf");
        servidor.verify();
    }

    @Test
    void descargaElContenido() throws Exception {
        RestClient.Builder constructor = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
        AlmacenamientoSupabase almacen =
                new AlmacenamientoSupabase(constructor, "https://proyecto.supabase.co", "clave", "b");

        servidor
                .expect(
                        requestTo(
                                "https://proyecto.supabase.co/storage/v1/object/b/"
                                        + ORG
                                        + "/"
                                        + DOC
                                        + "/albaran.pdf"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("datos del albaran", MediaType.APPLICATION_PDF));

        var recurso = almacen.cargar(ORG + "/" + DOC + "/albaran.pdf").orElseThrow();

        assertThat(new String(recurso.getContentAsByteArray())).isEqualTo("datos del albaran");
        servidor.verify();
    }

    @Test
    void sinRutaNoHayRecurso() {
        AlmacenamientoSupabase almacen =
                new AlmacenamientoSupabase(
                        RestClient.builder(), "https://proyecto.supabase.co", "clave", "b");

        assertThat(almacen.cargar("")).isEmpty();
        assertThat(almacen.cargar(null)).isEmpty();
    }

    @Test
    void exigeUrlYClave() {
        assertThatThrownBy(
                        () ->
                                new AlmacenamientoSupabase(
                                        RestClient.builder(), "", "clave", "b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPABASE_URL");

        assertThatThrownBy(
                        () ->
                                new AlmacenamientoSupabase(
                                        RestClient.builder(), "https://x.supabase.co", " ", "b"))
                .isInstanceOf(IllegalStateException.class);
    }
}
