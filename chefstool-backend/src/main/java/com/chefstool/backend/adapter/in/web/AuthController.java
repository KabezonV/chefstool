package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.BajaCuentaRequest;
import com.chefstool.backend.adapter.in.web.dto.LoginRequest;
import com.chefstool.backend.adapter.in.web.dto.RecuperacionResponse;
import com.chefstool.backend.adapter.in.web.dto.RecuperarRequest;
import com.chefstool.backend.adapter.in.web.dto.RegistroRequest;
import com.chefstool.backend.adapter.in.web.dto.RestablecerRequest;
import com.chefstool.backend.adapter.in.web.dto.SesionResponse;
import com.chefstool.backend.application.AuthService;
import com.chefstool.backend.application.BajaCuentaService;
import com.chefstool.backend.application.LimitadorIntentos;
import com.chefstool.backend.application.RecuperacionPasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacionPasswordService recuperacionService;
    private final BajaCuentaService bajaCuentaService;
    private final LimitadorIntentos limitador;

    public AuthController(
            AuthService authService,
            RecuperacionPasswordService recuperacionService,
            BajaCuentaService bajaCuentaService,
            LimitadorIntentos limitador) {
        this.authService = authService;
        this.recuperacionService = recuperacionService;
        this.bajaCuentaService = bajaCuentaService;
        this.limitador = limitador;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registrar(@Valid @RequestBody RegistroRequest peticion) {
        return authService.registrar(peticion);
    }

    @PostMapping("/login")
    public SesionResponse login(
            @Valid @RequestBody LoginRequest peticion, HttpServletRequest peticionHttp) {
        String clave = clave(peticionHttp, peticion.email());
        limitador.comprobar(clave);
        try {
            SesionResponse sesion = authService.login(peticion);
            limitador.exito(clave);
            return sesion;
        } catch (RuntimeException ex) {
            limitador.fallo(clave);
            throw ex;
        }
    }

    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RecuperacionResponse recuperar(
            @Valid @RequestBody RecuperarRequest peticion, HttpServletRequest peticionHttp) {
        String clave = "recuperar|" + clave(peticionHttp, peticion.email());
        limitador.comprobar(clave);
        limitador.fallo(clave);

        // La respuesta es la misma exista o no la cuenta, para no revelar correos
        String token = recuperacionService.solicitar(peticion.email()).orElse(null);
        return new RecuperacionResponse(
                "Si el correo esta registrado, recibiras un enlace para cambiar la contrasena",
                token);
    }

    @PostMapping("/restablecer")
    public Map<String, String> restablecer(@Valid @RequestBody RestablecerRequest peticion) {
        recuperacionService.restablecer(peticion.token(), peticion.password());
        return Map.of("mensaje", "Contrasena actualizada. Ya puedes entrar.");
    }

    private String clave(HttpServletRequest peticion, String email) {
        String ip = peticion.getRemoteAddr();
        return (email == null ? "" : email.trim().toLowerCase()) + "|" + ip;
    }

    /** Derecho de acceso y portabilidad (RGPD): datos personales en JSON. */
    @GetMapping("/datos")
    public BajaCuentaService.DatosPersonales datos(@AuthenticationPrincipal Jwt jwt) {
        return bajaCuentaService.datosPersonales(UUID.fromString(jwt.getSubject()));
    }

    /** Derecho de supresion (RGPD): elimina la cuenta y, si procede, su organizacion. */
    @DeleteMapping("/cuenta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void baja(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BajaCuentaRequest peticion) {
        bajaCuentaService.eliminar(UUID.fromString(jwt.getSubject()), peticion.password());
    }

    @GetMapping("/yo")
    public SesionResponse yo(@AuthenticationPrincipal Jwt jwt) {
        return authService.sesionActual(UUID.fromString(jwt.getSubject()));
    }
}
