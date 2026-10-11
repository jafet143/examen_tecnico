package gt.com.chn.tarjetas.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    @Bean
    SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http,
                                               ConvertidorRolesKeycloak convertidorRolesKeycloak,
                                               ManejadorErroresSeguridad manejadorErroresSeguridad,
                                               CorsConfigurationSource fuenteConfiguracionCors) throws Exception {
        http
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(fuenteConfiguracionCors))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/gestion-tarjetas/clientes", "/gestion-tarjetas/clientes/*")
                        .hasRole(Permisos.CLIENTES_CONSULTAR)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/clientes").hasRole(Permisos.CLIENTES_GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, "/gestion-tarjetas/clientes/*").hasRole(Permisos.CLIENTES_GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, "/gestion-tarjetas/clientes/*").hasRole(Permisos.CLIENTES_ELIMINAR)
                        .requestMatchers(HttpMethod.GET, "/gestion-tarjetas/clientes/*/solicitudes", "/gestion-tarjetas/solicitudes",
                                "/gestion-tarjetas/solicitudes/*").hasRole(Permisos.SOLICITUDES_CONSULTAR)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/solicitudes").hasRole(Permisos.SOLICITUDES_REGISTRAR)
                        .requestMatchers(HttpMethod.PUT, "/gestion-tarjetas/solicitudes/*/aprobar", "/gestion-tarjetas/solicitudes/*/rechazar")
                        .hasRole(Permisos.SOLICITUDES_DECIDIR)
                        .requestMatchers(HttpMethod.GET, "/gestion-tarjetas/clientes/*/cuentas", "/gestion-tarjetas/cuentas", "/gestion-tarjetas/cuentas/*",
                                "/gestion-tarjetas/cuentas/*/tarjetas", "/gestion-tarjetas/cuentas/*/movimientos", "/gestion-tarjetas/tarjetas/*")
                        .hasRole(Permisos.CUENTAS_CONSULTAR)
                        .requestMatchers(HttpMethod.GET, "/gestion-tarjetas/catalogos/tipos-tarjeta")
                        .hasAnyRole(Permisos.CUENTAS_CONSULTAR, Permisos.SOLICITUDES_CONSULTAR)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/cuentas/*/consumos")
                        .hasRole(Permisos.CONSUMOS_REGISTRAR)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/cuentas/*/pagos").hasRole(Permisos.PAGOS_REGISTRAR)
                        .requestMatchers(HttpMethod.PUT, "/gestion-tarjetas/consumos/*/reversar", "/gestion-tarjetas/pagos/*/reversar")
                        .hasRole(Permisos.MOVIMIENTOS_REVERSAR)
                        .requestMatchers(HttpMethod.PUT, "/gestion-tarjetas/tarjetas/*/bloquear").hasRole(Permisos.TARJETAS_BLOQUEAR)
                        .requestMatchers(HttpMethod.PUT, "/gestion-tarjetas/tarjetas/*/desbloquear")
                        .hasRole(Permisos.TARJETAS_DESBLOQUEAR)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/tarjetas/*/reposiciones")
                        .hasRole(Permisos.TARJETAS_REPONER)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/tarjetas/*/renovaciones")
                        .hasRole(Permisos.TARJETAS_RENOVAR)
                        .requestMatchers(HttpMethod.PUT, "/gestion-tarjetas/cuentas/*/cancelar").hasRole(Permisos.CUENTAS_CANCELAR)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/cuentas/*/cargos").hasRole(Permisos.CARGOS_REGISTRAR)
                        .requestMatchers(HttpMethod.PUT, "/gestion-tarjetas/cargos/*/reversar")
                        .hasRole(Permisos.CARGOS_AJUSTAR)
                        .requestMatchers(HttpMethod.POST, "/gestion-tarjetas/cuentas/*/exoneracion-mora")
                        .hasRole(Permisos.CARGOS_AJUSTAR)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorRolesKeycloak))
                        .authenticationEntryPoint(manejadorErroresSeguridad)
                        .accessDeniedHandler(manejadorErroresSeguridad))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(manejadorErroresSeguridad)
                        .accessDeniedHandler(manejadorErroresSeguridad));
        return http.build();
    }

    @Bean
    JwtDecoder decodificadorJwt(@Value("${seguridad.keycloak.jwk-set-uri}") String jwkSetUri,
                                @Value("${seguridad.keycloak.issuer-esperado}") String issuerEsperado) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerEsperado));
        return decodificador;
    }

    @Bean
    CorsConfigurationSource fuenteConfiguracionCors(
            @Value("${seguridad.cors.origenes-permitidos}") String origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(",")).map(String::trim).toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setExposedHeaders(List.of("Location"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
