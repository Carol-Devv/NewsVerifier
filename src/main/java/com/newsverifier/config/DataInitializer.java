package com.newsverifier.config;

import com.newsverifier.model.Usuario;
import com.newsverifier.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * DataInitializer — Carga de datos iniciales al arrancar
 * ============================================================
 *
 * Implementa CommandLineRunner, por lo que Spring Boot lo ejecuta
 * automáticamente una vez que el contexto está completamente cargado.
 *
 * Crea los usuarios de prueba si no existen todavía en la BBDD.
 * Usa el PasswordEncoder de Spring para generar el hash BCrypt,
 * garantizando que las credenciales siempre funcionen correctamente.
 *
 * Usuarios precargados:
 *   - demo@newsverifier.com  / demo1234
 *   - admin@newsverifier.com / admin1234
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    /**
     * Punto de entrada del inicializador.
     * Se ejecuta una sola vez al arrancar la aplicación.
     */
    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("Usuario Demo",  "demo@newsverifier.com",  "demo1234");
        crearUsuarioSiNoExiste("Administrador", "admin@newsverifier.com", "admin1234");
    }

    /**
     * Crea un usuario solo si su email no está ya registrado.
     * Así evitamos duplicados al reiniciar la aplicación con H2 en disco.
     *
     * @param nombre   Nombre visible del usuario
     * @param email    Email único (clave de login)
     * @param password Contraseña en texto plano (se encripta con BCrypt)
     */
    private void crearUsuarioSiNoExiste(String nombre, String email, String password) {
        if (!usuarioRepository.existsByEmail(email)) {
            String hash = passwordEncoder.encode(password);
            usuarioRepository.save(new Usuario(nombre, email, hash));
            log.info("[DataInitializer] Usuario creado: {}", email);
        } else {
            log.info("[DataInitializer] Usuario ya existe, omitido: {}", email);
        }
    }
}
