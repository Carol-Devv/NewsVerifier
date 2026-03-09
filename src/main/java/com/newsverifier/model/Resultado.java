package com.newsverifier.model;

import java.util.List;

/**
 * ============================================================
 * Resultado — DTO de respuesta del análisis de IA
 * ============================================================
 *
 * Data Transfer Object que transporta el resultado del análisis
 * desde VerificadorService hasta las vistas Thymeleaf.
 *
 * No es una entidad JPA (no se persiste directamente).
 * Los datos relevantes se extraen y guardan en Analisis.java.
 *
 * Campos principales:
 *   - etiqueta      → veredicto: "REAL", "FAKE" o "INCIERTO"
 *   - credibilidad  → porcentaje 0-100
 *   - explicacion   → texto explicativo generado por la IA
 *   - indicadores   → señales detectadas (positivas/negativas/neutras)
 *   - fuentes       → fuentes relacionadas sugeridas por la IA
 */
public class Resultado {

    private String          etiqueta;      // "REAL" | "FAKE" | "INCIERTO"
    private int             credibilidad;  // 0 – 100
    private String          explicacion;
    private List<Indicador> indicadores;
    private List<Fuente>    fuentes;

    // ── Constructores ─────────────────────────────────────────

    public Resultado() {}

    public Resultado(String etiqueta, int credibilidad, String explicacion,
                     List<Indicador> indicadores, List<Fuente> fuentes) {
        this.etiqueta     = etiqueta;
        this.credibilidad = credibilidad;
        this.explicacion  = explicacion;
        this.indicadores  = indicadores;
        this.fuentes      = fuentes;
    }

    // ── Getters y Setters ─────────────────────────────────────

    public String          getEtiqueta()                   { return etiqueta; }
    public void            setEtiqueta(String v)           { this.etiqueta = v; }

    public int             getCredibilidad()               { return credibilidad; }
    public void            setCredibilidad(int v)          { this.credibilidad = v; }

    public String          getExplicacion()                { return explicacion; }
    public void            setExplicacion(String v)        { this.explicacion = v; }

    public List<Indicador> getIndicadores()                { return indicadores; }
    public void            setIndicadores(List<Indicador> v){ this.indicadores = v; }

    public List<Fuente>    getFuentes()                    { return fuentes; }
    public void            setFuentes(List<Fuente> v)      { this.fuentes = v; }

    // =========================================================
    // CLASES INTERNAS
    // =========================================================

    /**
     * Indicador — Una señal detectada en el texto analizado.
     *
     * tipo  → "positivo" (favorece credibilidad)
     *         "negativo" (reduce credibilidad)
     *         "neutro"   (informativo, sin impacto claro)
     * texto → descripción legible de la señal detectada
     */
    public static class Indicador {
        private String tipo;
        private String texto;

        public Indicador() {}
        public Indicador(String tipo, String texto) {
            this.tipo  = tipo;
            this.texto = texto;
        }

        public String getTipo()          { return tipo; }
        public void   setTipo(String v)  { this.tipo = v; }
        public String getTexto()         { return texto; }
        public void   setTexto(String v) { this.texto = v; }
    }

    /**
     * Fuente — Una fuente relacionada sugerida por la IA.
     *
     * nombre     → nombre legible del medio (ej: "Reuters")
     * url        → URL completa del artículo relacionado
     * dominio    → dominio base para cargar el favicon (ej: "reuters.com")
     * relevancia → "alta" | "media" | "baja"
     */
    public static class Fuente {
        private String nombre;
        private String url;
        private String dominio;
        private String relevancia;

        public Fuente() {}
        public Fuente(String nombre, String url, String dominio, String relevancia) {
            this.nombre     = nombre;
            this.url        = url;
            this.dominio    = dominio;
            this.relevancia = relevancia;
        }

        public String getNombre()            { return nombre; }
        public void   setNombre(String v)    { this.nombre = v; }
        public String getUrl()               { return url; }
        public void   setUrl(String v)       { this.url = v; }
        public String getDominio()           { return dominio; }
        public void   setDominio(String v)   { this.dominio = v; }
        public String getRelevancia()        { return relevancia; }
        public void   setRelevancia(String v){ this.relevancia = v; }
    }
}
