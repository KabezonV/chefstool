package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chefstool.backend.IntegracionBase;
import com.chefstool.backend.adapter.in.web.ConflictoException;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.RechazoSolicitudCompraRequest;
import com.chefstool.backend.adapter.in.web.dto.SolicitudCompraRequest;
import com.chefstool.backend.adapter.in.web.dto.SolicitudCompraResponse;
import com.chefstool.backend.domain.model.EstadoSolicitudCompra;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.AuditoriaRepository;
import com.chefstool.backend.domain.port.out.CompraRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@ActiveProfiles("test")
class SolicitudCompraServiceTest extends IntegracionBase {

    private static final UUID ORGANIZACION =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private SolicitudCompraService solicitudService;
    @Autowired private OrganizacionService organizacionService;
    @Autowired private AuditoriaRepository auditoriaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompraRepository compraRepository;

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaSolicitudPendienteSinRegistrarUnaCompra() {
        long comprasAntes = compraRepository.count();
        autenticar(usuario("solicitante"));

        var solicitud =
                solicitudService.crear(
                        new SolicitudCompraRequest(
                                "Horno nuevo",
                                "Sustitución del horno averiado",
                                "Proveedor Cocina",
                                new BigDecimal("1250.00")));

        assertThat(solicitud.estado()).isEqualTo(EstadoSolicitudCompra.PENDIENTE);
        assertThat(solicitud.solicitanteNombre()).isEqualTo("Solicitante");
        assertThat(solicitudService.listar())
                .extracting(SolicitudCompraResponse::id)
                .contains(solicitud.id());
        assertThat(compraRepository.count()).isEqualTo(comprasAntes);
        assertThat(auditoriaRepository.findAll())
                .anySatisfy(
                        evento -> {
                            assertThat(evento.getEntidadId()).isEqualTo(solicitud.id());
                            assertThat(evento.getAccion()).isEqualTo("SOLICITAR");
                        });
    }

    @Test
    void soloUnaPersonaDistintaPuedeResolverYElEstadoEsFinal() {
        UUID solicitanteId = usuario("solicitante");
        autenticar(solicitanteId);
        var solicitud =
                solicitudService.crear(
                        new SolicitudCompraRequest(
                                "Lavavajillas",
                                null,
                                null,
                                new BigDecimal("890.00")));

        autenticar(usuario("responsable"));
        var aprobada = solicitudService.aprobar(solicitud.id());

        assertThat(aprobada.estado()).isEqualTo(EstadoSolicitudCompra.APROBADA);
        assertThat(aprobada.revisadaPorNombre()).isEqualTo("Responsable");
        assertThat(auditoriaRepository.findAll())
                .anySatisfy(
                        evento -> {
                            assertThat(evento.getEntidadId()).isEqualTo(solicitud.id());
                            assertThat(evento.getAccion()).isEqualTo("APROBAR");
                        });
        assertThatThrownBy(() -> solicitudService.rechazar(
                        solicitud.id(), new RechazoSolicitudCompraRequest("Cambio de planes")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya ha sido resuelta");
    }

    @Test
    void solicitanteNoPuedeAprobarSuPropiaSolicitud() {
        UUID solicitanteId = usuario("solicitante");
        autenticar(solicitanteId);
        var solicitud =
                solicitudService.crear(
                        new SolicitudCompraRequest(
                                "Batidora",
                                null,
                                null,
                                new BigDecimal("120.00")));

        assertThatThrownBy(() -> solicitudService.aprobar(solicitud.id()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No puedes resolver tu propia solicitud");
    }

    @Test
    void rechazoGuardaElMotivoYElResponsable() {
        autenticar(usuario("solicitante"));
        var solicitud =
                solicitudService.crear(
                        new SolicitudCompraRequest(
                                "Estantería",
                                null,
                                null,
                                new BigDecimal("300.00")));

        autenticar(usuario("responsable"));
        var rechazada =
                solicitudService.rechazar(
                        solicitud.id(), new RechazoSolicitudCompraRequest("No está presupuestada"));

        assertThat(rechazada.estado()).isEqualTo(EstadoSolicitudCompra.RECHAZADA);
        assertThat(rechazada.motivoRechazo()).isEqualTo("No está presupuestada");
        assertThat(rechazada.revisadaPorNombre()).isEqualTo("Responsable");
        assertThat(auditoriaRepository.findAll())
                .anySatisfy(
                        evento -> {
                            assertThat(evento.getEntidadId()).isEqualTo(solicitud.id());
                            assertThat(evento.getAccion()).isEqualTo("RECHAZAR");
                        });
    }

    @Test
    void noPermiteLeerNiResolverUnaSolicitudDesdeOtroLocal() {
        UUID solicitanteId = usuario("solicitante");
        UUID localId = organizacionService.localActual();
        autenticar(solicitanteId, ORGANIZACION, localId);
        var solicitud =
                solicitudService.crear(
                        new SolicitudCompraRequest(
                                "Nevera",
                                null,
                                null,
                                new BigDecimal("800.00")));

        autenticar(usuario("responsable"), ORGANIZACION, UUID.randomUUID());

        assertThat(solicitudService.listar()).isEmpty();
        assertThatThrownBy(() -> solicitudService.aprobar(solicitud.id()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void noPermiteLeerNiResolverUnaSolicitudDesdeOtraOrganizacion() {
        UUID solicitanteId = usuario("solicitante");
        UUID localId = organizacionService.localActual();
        autenticar(solicitanteId, ORGANIZACION, localId);
        var solicitud =
                solicitudService.crear(
                        new SolicitudCompraRequest(
                                "Congelador",
                                null,
                                null,
                                new BigDecimal("950.00")));

        autenticar(usuario("responsable"), UUID.randomUUID(), localId);

        assertThat(solicitudService.listar()).isEmpty();
        assertThatThrownBy(() -> solicitudService.aprobar(solicitud.id()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private UUID usuario(String tipo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(tipo + "-" + UUID.randomUUID() + "@chefstool.es");
        usuario.setNombre(tipo.equals("responsable") ? "Responsable" : "Solicitante");
        usuario.setPasswordHash("hash-de-pruebas");
        return usuarioRepository.save(usuario).getId();
    }

    private void autenticar(UUID usuarioId) {
        autenticar(usuarioId, ORGANIZACION, organizacionService.localActual());
    }

    private void autenticar(UUID usuarioId, UUID organizacionId, UUID localId) {
        Jwt jwt =
                Jwt.withTokenValue("token-de-pruebas")
                        .header("alg", "none")
                        .subject(usuarioId.toString())
                        .claim("organizacionId", organizacionId.toString())
                        .claim("localId", localId.toString())
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
