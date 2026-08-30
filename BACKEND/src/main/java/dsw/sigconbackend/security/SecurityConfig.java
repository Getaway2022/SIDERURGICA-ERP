package dsw.sigconbackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtFilter jwtFilter,
            @Value("${cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── Públicos ──────────────────────────────────────────────
                .requestMatchers("/login", "/auth/refresh").permitAll()

                // ── Solo ADMIN ────────────────────────────────────────────
                .requestMatchers("/usuarios/**").hasRole("ADMIN")

                // ── Comercial: VENTAS + ADMIN ─────────────────────────────
                .requestMatchers("/cotizaciones/**").hasAnyRole("VENTAS", "ADMIN")
                .requestMatchers("/pedidos/**").hasAnyRole("VENTAS", "ADMIN")
                .requestMatchers("/ventas/**").hasAnyRole("VENTAS", "ADMIN")

                // ── Almacén: ALMACEN + ADMIN ──────────────────────────────
                // DEF-02: agregados /orden-compra/**, /compra/**, /presupuesto/**
                .requestMatchers("/orden-compra/**").hasAnyRole("ALMACEN", "ADMIN")
                .requestMatchers("/compra/**").hasAnyRole("ALMACEN", "ADMIN")
                .requestMatchers("/presupuesto/**").hasAnyRole("ALMACEN", "ADMIN")
                // DEF-03: agregado /proveedor/**
                .requestMatchers("/proveedor/**").hasAnyRole("ALMACEN", "ADMIN")

                // ── Almacén + VENTAS (despacho e inventario son compartidos) ──
                .requestMatchers("/inventario/**").hasAnyRole("ALMACEN", "ADMIN", "VENTAS")
                .requestMatchers("/despachos/**").hasAnyRole("ALMACEN", "ADMIN", "VENTAS")

                // ── RRHH: RRHH + ADMIN ────────────────────────────────────
                .requestMatchers("/empleados/**").hasAnyRole("RRHH", "ADMIN")
                .requestMatchers("/asistencia/**").hasAnyRole("RRHH", "ADMIN")
                .requestMatchers("/planillas/**").hasAnyRole("RRHH", "ADMIN")
                .requestMatchers("/incidencias-personal/**").hasAnyRole("RRHH", "ADMIN")

                // ── Cualquier otro endpoint requiere autenticación ─────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
