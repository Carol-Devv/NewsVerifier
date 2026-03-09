package com.newsverifier.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================================
 * Usuario — Entidad JPA que representa a un usuario registrado
 * ============================================================
 *
 * Se persiste en la tabla USUARIOS de la base de datos H2.
 * La contraseña siempre se almacena encriptada con BCrypt;
 * nunca en texto plano.
 *
 * Relación: un Usuario puede tener muchos Analisis (1:N).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    /** Identificador único autoincremental */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre visible del usuario en la interfaz */
    @Column(nullable = false)
    private String nombre;

    /** Email único, usado como identificador de login */
    @Column(nullable = false, unique = true)
    private String email;

    /** Contraseña encriptada con BCrypt. NUNCA en texto plano. */
    @Column(nullable = false)
    private String password;

    /** Fecha y hora de registro en el sistema */
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // ── Constructores ─────────────────────────────────────────

    /** Constructor vacío requerido por JPA */
    public Usuario() {}

    /** Constructor principal para crear nuevos usuarios */
    public Usuario(String nombre, String email, String password) {
        this.nombre        = nombre;
        this.email         = email;
        this.password      = password;
        this.fechaRegistro = LocalDateTime.now();
    }

    // ── Getters y Setters ─────────────────────────────────────

    public Long          getId()                          { return id; }
    public void          setId(Long v)                    { this.id = v; }

    public String        getNombre()                      { return nombre; }
    public void          setNombre(String v)              { this.nombre = v; }

    public String        getEmail()                       { return email; }
    public void          setEmail(String v)               { this.email = v; }

    public String        getPassword()                    { return password; }
    public void          setPassword(String v)            { this.password = v; }

    public LocalDateTime getFechaRegistro()               { return fechaRegistro; }
    public void          setFechaRegistro(LocalDateTime v){ this.fechaRegistro = v; }
}
