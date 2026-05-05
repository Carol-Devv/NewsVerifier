package com.newsverifier.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================================
 * Analisis — Entidad JPA que representa un análisis realizado
 * ============================================================
 *
 * Persiste cada verificación de noticia en la tabla ANALISIS.
 * Sirve como historial del usuario y como fuente de estadísticas.
 *
 * Relación: muchos Analisis pertenecen a un Usuario (N:1).
 *
 * Los campos de texto se truncan al persistir para no sobrecargar
 * la base de datos con artículos completos.
 */
@Entity
@Table(name = "analisis")
public class Analisis {

    /** Identificador único autoincremental */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Texto de la noticia analizada (truncado a 2000 caracteres) */
    @Column(length = 2000)
    private String texto;

    /** Título de la noticia analizada (truncado a 300 caracteres) */
    @Column(length = 300)
    private String titulo;

    /** URL enviada para análisis, si se proporcionó */
    @Column(length = 500)
    private String url;

    /** Veredicto de la IA: "REAL", "FAKE" o "INCIERTO" */
    @Column(nullable = false, length = 20)
    private String etiqueta;

    /** Porcentaje de credibilidad devuelto por la IA (0-100) */
    private int credibilidad;

    /** Explicación resumida del análisis (truncada a 1000 caracteres) */
    @Column(length = 1000)
    private String explicacion;

    /** Fecha y hora en que se realizó el análisis */
    @Column(name = "fecha_analisis")
    private LocalDateTime fechaAnalisis;

    /**
     * Usuario que realizó el análisis.
     * LAZY: el usuario no se carga hasta que se accede explícitamente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // ── Constructores ─────────────────────────────────────────

    /** Constructor vacío requerido por JPA */
    public Analisis() {}

    /** Constructor principal usado desde MainController al persistir un análisis */
    public Analisis(String titulo, String texto, String url, String etiqueta,
                    int credibilidad, String explicacion, Usuario usuario) {
        // Comentario: truncamos campos largos para no sobrecargar la BBDD.
        this.titulo      = titulo != null && titulo.length() > 300
                           ? titulo.substring(0, 300) : titulo;
        this.texto       = texto != null && texto.length() > 2000
                           ? texto.substring(0, 2000) : texto;
        this.url         = url;
        this.etiqueta    = etiqueta;
        this.credibilidad= credibilidad;
        this.explicacion = explicacion != null && explicacion.length() > 1000
                           ? explicacion.substring(0, 1000) : explicacion;
        this.usuario     = usuario;
        this.fechaAnalisis = LocalDateTime.now();
    }

    // ── Getters y Setters ─────────────────────────────────────

    public Long          getId()                            { return id; }

    public String        getTexto()                         { return texto; }
    public void          setTexto(String v)                 { this.texto = v; }

    public String        getTitulo()                        { return titulo; }
    public void          setTitulo(String v)                { this.titulo = v; }

    public String        getUrl()                           { return url; }
    public void          setUrl(String v)                   { this.url = v; }

    public String        getEtiqueta()                      { return etiqueta; }
    public void          setEtiqueta(String v)              { this.etiqueta = v; }

    public int           getCredibilidad()                  { return credibilidad; }
    public void          setCredibilidad(int v)             { this.credibilidad = v; }

    public String        getExplicacion()                   { return explicacion; }
    public void          setExplicacion(String v)           { this.explicacion = v; }

    public LocalDateTime getFechaAnalisis()                 { return fechaAnalisis; }
    public void          setFechaAnalisis(LocalDateTime v)  { this.fechaAnalisis = v; }

    public Usuario       getUsuario()                       { return usuario; }
    public void          setUsuario(Usuario v)              { this.usuario = v; }
}
