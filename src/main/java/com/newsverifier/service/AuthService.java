package com.newsverifier.service;

import com.newsverifier.model.Usuario;
import com.newsverifier.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * AuthService — Servicio de autenticación y registro
 * ============================================================
 *
 * Gestiona el ciclo de vida de los usuarios:
 *   - Registro con encriptación BCrypt de contraseña
 *   - Login con verificación segura de credenciales
 *
 * Las contraseñas NUNCA se almacenan en texto plano.
 * BCrypt añade un salt aleatorio en cada encriptación.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;    // BCrypt (configurado en SecurityConfig)

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────

    /**
     * Registra un nuevo usuario en la base de datos.
     *
     * @param nombre   Nombre completo del usuario
     * @param email    Email único (usado como identificador de login)
     * @param password Contraseña en texto plano (se encripta antes de guardar)
     * @return El usuario recién creado y persistido
     * @throws IllegalArgumentException si el email ya está en uso
     */
    public Usuario registrar(String nombre, String email, String password) {
        // Verificar que el email no esté ya registrado
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }

        // Encriptar la contraseña con BCrypt antes de persistir
        String passwordEncriptada = passwordEncoder.encode(password);

        Usuario usuario = new Usuario(nombre, email, passwordEncriptada);
        return usuarioRepository.save(usuario);
    }

    // ─────────────────────────────────────────────────────────

    /**
     * Autentica un usuario verificando email y contraseña.
     *
     * @param email    Email del usuario
     * @param password Contraseña en texto plano a verificar
     * @return El usuario autenticado
     * @throws IllegalArgumentException si las credenciales son incorrectas
     */
    public Usuario login(String email, String password) {
        // Buscar usuario por email
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas."));

        // Verificar contraseña con BCrypt (compara texto plano vs hash almacenado)
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales incorrectas.");
        }

        return usuario;
    }
}
