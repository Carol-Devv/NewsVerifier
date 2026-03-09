package com.newsverifier.repository;

import com.newsverifier.model.Analisis;
import com.newsverifier.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ============================================================
 * AnalisisRepository — Repositorio JPA para análisis
 * ============================================================
 *
 * Spring Data JPA genera automáticamente la implementación
 * de todos los métodos basándose en sus nombres.
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save(), findById(), findAll(), delete(), count(), etc.
 */
@Repository
public interface AnalisisRepository extends JpaRepository<Analisis, Long> {

    /**
     * Devuelve todos los análisis de un usuario ordenados del más
     * reciente al más antiguo.
     *
     * Usado en MainController para el historial rápido del home (top 5)
     * y para la página completa de historial.
     *
     * @param usuario El usuario propietario de los análisis
     * @return Lista de análisis ordenada por fecha descendente
     */
    List<Analisis> findByUsuarioOrderByFechaAnalisisDesc(Usuario usuario);
}
