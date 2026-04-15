package com.intesi.usermanagement.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configurazione di sicurezza provvisoria.
 * Sblocca Swagger UI e API docs; permette tutto il resto in attesa di STEP 22.
 * TODO: sostituire con JWT + RBAC.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs",
            "/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().permitAll() // TODO: sostituire con .authenticated()
                );

        return http.build();
    }
}
