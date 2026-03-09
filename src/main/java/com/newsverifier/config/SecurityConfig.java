package com.newsverifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ============================================================
 * SecurityConfig — Configuración de Spring Security
 * ============================================================
 *
 * Desactiva el formulario de login automático de Spring Security
 * porque usamos nuestro propio (login.html con Thymeleaf).
 *
 * El control de acceso se gestiona manualmente en MainController
 * mediante comprobaciones de HttpSession.
 *
 * Nota para el TFG: En un entorno de producción real se debería:
 *   - Activar CSRF
 *   - Usar Spring Security para gestionar sesiones y roles
 *   - Configurar HTTPS obligatorio
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * Decisiones tomadas para simplificar el TFG:
     *   - Todas las rutas permitidas (el acceso lo controla el Controller)
     *   - Login/logout de Spring Security desactivados (usamos el nuestro)
     *   - CSRF desactivado (simplifica los formularios Thymeleaf)
     *   - frameOptions en sameOrigin para que funcione la consola H2
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Permitir todas las rutas — control de acceso en MainController
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // Desactivar el formulario de login automático de Spring
            .formLogin(form -> form.disable())
            // Desactivar el logout automático de Spring
            .logout(logout -> logout.disable())
            // Desactivar CSRF (simplifica el TFG; activar en producción)
            .csrf(csrf -> csrf.disable())
            // Permitir iframes del mismo origen (necesario para la consola H2)
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    /**
     * Bean de BCryptPasswordEncoder para encriptar y verificar contraseñas.
     * Se inyecta en AuthService y DataInitializer.
     *
     * BCrypt añade un salt aleatorio en cada hash, por lo que dos
     * encriptaciones de la misma contraseña producen hashes distintos,
     * pero ambos se verifican correctamente con matches().
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
