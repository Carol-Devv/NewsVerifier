package com.newsverifier.repository;

import com.newsverifier.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================
 * UsuarioRepository — Repositorio JPA para usuarios
 * ============================================================
 *
 * Spring Data JPA genera automáticamente la implementación
 * de todos los métodos basándose en sus nombres.
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save(), findById(), findAll(), delete(), count(), etc.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su email.
     * Usado en AuthService para login y validación de duplicados.
     *
     * @param email Email del usuario a buscar
     * @return Optional con el usuario si existe, vacío si no
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Comprueba si ya existe un usuario con ese email.
     * Usado en AuthService para validar unicidad antes del registro.
     *
     * @param email Email a comprobar
     * @return true si el email ya está registrado
     */
    boolean existsByEmail(String email);
}
