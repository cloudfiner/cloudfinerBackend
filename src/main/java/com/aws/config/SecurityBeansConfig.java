package com.aws.config;

import com.aws.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityBeansConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
public CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOrigins(List.of(
        "http://localhost:5173",
        "https://cloudfiner-frontend.vercel.app"
    ));

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    config.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return source;
}

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setContentType("application/json");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
            )

            .authorizeHttpRequests(auth -> auth

                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Auth सार्वजनिक endpoints
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/**"
                ).permitAll()

                // WebSocket
                .requestMatchers("/ws/**").permitAll()

                // Telegram webhook
                .requestMatchers("/api/telegram/webhook").permitAll()

                // Demo APIs (सबसे जरूरी)
                .requestMatchers(
                        "/api/cost/**",          // demo cost
                        "/api/alerts/all",       // demo alerts
                        "/api/notifications/**", // demo notifications
                        "/api/budgets"
                		
                		).permitAll()

                // Admin
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // बाकी सब secured
                .requestMatchers("/api/**").authenticated()
                
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}