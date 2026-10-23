package com.chefstool.backend.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class AlmacenamientoS3Test {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOC = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void subeElArchivoConLaClaveEsperada() {
        S3Client cliente = mock(S3Client.class);
        AlmacenamientoS3 almacen = new AlmacenamientoS3(cliente, "chefstool-albaranes");

        String clave = almacen.guardar(ORG, DOC, "../../Albaran Marzo.pdf", "contenido".getBytes());

        assertThat(clave).isEqualTo(ORG + "/" + DOC + "/Albaran_Marzo.pdf");

        ArgumentCaptor<PutObjectRequest> peticion = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(cliente).putObject(peticion.capture(), any(RequestBody.class));
        assertThat(peticion.getValue().bucket()).isEqualTo("chefstool-albaranes");
        assertThat(peticion.getValue().key()).isEqualTo(clave);
        assertThat(peticion.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void descargaElContenido() throws Exception {
        S3Client cliente = mock(S3Client.class);
        AlmacenamientoS3 almacen = new AlmacenamientoS3(cliente, "bucket");
        when(cliente.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(
                        ResponseBytes.fromByteArray(
                                GetObjectResponse.builder().build(), "datos".getBytes()));

        var recurso = almacen.cargar(ORG + "/" + DOC + "/albaran.pdf").orElseThrow();

        assertThat(new String(recurso.getContentAsByteArray())).isEqualTo("datos");
    }

    @Test
    void siElObjetoNoExisteDevuelveVacio() {
        S3Client cliente = mock(S3Client.class);
        AlmacenamientoS3 almacen = new AlmacenamientoS3(cliente, "bucket");
        when(cliente.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("no existe").build());

        assertThat(almacen.cargar(ORG + "/" + DOC + "/no-existe.pdf")).isEmpty();
    }

    @Test
    void sinRutaNoLlamaAlServicio() {
        S3Client cliente = mock(S3Client.class);
        AlmacenamientoS3 almacen = new AlmacenamientoS3(cliente, "bucket");

        assertThat(almacen.cargar("")).isEmpty();
        assertThat(almacen.cargar(null)).isEmpty();
    }

    @Test
    void exigeBucket() {
        assertThatThrownBy(
                        () ->
                                new AlmacenamientoS3(
                                        "  ", "eu-south-2", "", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHEFSTOOL_S3_BUCKET");
    }
}
