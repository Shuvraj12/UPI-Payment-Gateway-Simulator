package com.upisimulator.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Base security setup for the whole 14-phase build.
 * <p>
 * Two things here are intentionally temporary and called out so they aren't
 * mistaken for the final state:
 * <ul>
 *   <li>{@code anyRequest().permitAll()} - there's no {@code User} entity or
 *       login endpoint yet. Phase 2 tightens this to permit only
 *       {@code /api/v1/auth/**} and require authentication everywhere else.</li>
 *   <li>No {@code JwtAuthenticationFilter} is registered yet - {@link JwtUtil}
 *       exists, but a filter that validates tokens needs a
 *       {@code UserDetailsService} to load users against, which also arrives
 *       in Phase 2.</li>
 * </ul>
 * CSRF is disabled and sessions are stateless because this is a token-based
 * API, not a server-rendered app with cookie sessions.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // TODO(Phase 2): .requestMatchers(ApiPaths.BASE + "/auth/**").permitAll()
                        //                .anyRequest().authenticated()
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
