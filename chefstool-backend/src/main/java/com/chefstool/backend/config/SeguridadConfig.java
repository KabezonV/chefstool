package com.chefstool.backend.config;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    private static final String SECRETO_DE_DESARROLLO = "chefstool-dev-secret";

    @Value("${chefstool.seguridad.jwt-secret}")
    private String secretoJwt;

    @Value("${chefstool.seguridad.estricto:false}")
    private boolean estricto;

    /** Falla al arrancar si el secreto no es utilizable, en vez de firmar tokens debiles. */
    @PostConstruct
    void validarSecreto() {
        if (secretoJwt == null || secretoJwt.length() < 32) {
            throw new IllegalStateException(
                    "chefstool.seguridad.jwt-secret debe tener al menos 32 caracteres");
        }
        if (estricto && secretoJwt.contains(SECRETO_DE_DESARROLLO)) {
            throw new IllegalStateException(
                    "chefstool.seguridad.jwt-secret sigue siendo el de desarrollo:"
                            + " define CHEFSTOOL_JWT_SECRET antes de desplegar");
        }
    }

    /** Origenes permitidos para el frontend (lista separada por comas). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value(
                            "${chefstool.cors.origenes:http://localhost:5173,http://localhost:5180,"
                                    + "http://192.168.1.153:5180}")
                    List<String> origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(
            @Value("${chefstool.seguridad.jwt-secret}") String secreto) {
        return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(clave(secreto)));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${chefstool.seguridad.jwt-secret}") String secreto) {
        return NimbusJwtDecoder.withSecretKey(clave(secreto))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** El rol del token se convierte en la autoridad ROLE_<ROL> para @PreAuthorize. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            @Value("${chefstool.seguridad.habilitada:true}") boolean habilitada)
            throws Exception {

        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> {
                    peticiones
                            .requestMatchers(
                                    "/api/auth/login",
                                    "/api/auth/registro",
                                    "/api/auth/recuperar",
                                    "/api/auth/restablecer",
                                    "/error")
                            .permitAll();
                    if (habilitada) {
                        peticiones.anyRequest().authenticated();
                    } else {
                        peticiones.anyRequest().permitAll();
                    }
                })
                .oauth2ResourceServer(servidor -> servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    private SecretKey clave(String secreto) {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
