package com.newsverifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsverifier.model.Resultado;
import com.newsverifier.model.Resultado.Fuente;
import com.newsverifier.model.Resultado.Indicador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * VerificadorService — Servicio de análisis de fake news
 * ============================================================
 *
 * Se comunica con la API Python de IA para analizar noticias.
 * Si la API no está disponible, activa automáticamente un modo
 * MOCK que simula resultados para desarrollo y pruebas.
 *
 * ── Contrato esperado de la API Python ──────────────────────
 *
 * Petición:
 *   POST /analizar
 *   Content-Type: application/json
 *   { "texto": "...", "url": "..." }
 *
 * Respuesta 200 OK:
 *   {
 *     "etiqueta":      "REAL" | "FAKE" | "INCIERTO",
 *     "credibilidad":  0-100,
 *     "explicacion":   "Texto explicativo...",
 *     "indicadores": [
 *       { "tipo": "positivo" | "negativo" | "neutro", "texto": "..." }
 *     ],
 *     "fuentes": [
 *       {
 *         "nombre":     "Reuters",
 *         "url":        "https://reuters.com/...",
 *         "dominio":    "reuters.com",
 *         "relevancia": "alta" | "media" | "baja"
 *       }
 *     ]
 *   }
 * ────────────────────────────────────────────────────────────
 */
@Service
public class VerificadorService {

    private static final Logger log = LoggerFactory.getLogger(VerificadorService.class);

    // ── Configuración inyectada desde application.properties ─

    /** URL del microservicio Python. Por defecto: http://localhost:5000/analizar */
    @Value("${ia.api.url:http://localhost:5000/analizar}")
    private String apiUrl;

    /** Si es true, usa el mock sin intentar llamar a la API Python */
    @Value("${ia.mock.enabled:false}")
    private boolean mockEnabled;

    // ── Clientes HTTP y JSON ──────────────────────────────────

    private final HttpClient   httpClient;
    private final ObjectMapper objectMapper;

    public VerificadorService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    /**
     * Analiza una noticia y devuelve un resultado de credibilidad.
     *
     * Prioridad:
     *   1. Si mockEnabled=true  → devuelve mock directamente
     *   2. Si mockEnabled=false → llama a la API Python
     *   3. Si la API falla      → cae al mock de emergencia (fallback)
     *
     * @param texto Texto de la noticia (puede ser null si se aporta url)
     * @param url   URL del artículo (puede ser null si se aporta texto)
     * @return Resultado con etiqueta, credibilidad, explicación, indicadores y fuentes
     */
    public Resultado analizar(String texto, String url) {
        if (mockEnabled) {
            log.info("[VerificadorService] Modo mock activo — devolviendo resultado simulado.");
            return buildMock(texto);
        }
        try {
            return llamarApi(texto, url);
        } catch (Exception e) {
            // Fallback automático al mock si la API no responde
            log.warn("[VerificadorService] API no disponible ({}). Activando fallback mock.", e.getMessage());
            return buildMock(texto);
        }
    }

    // =========================================================
    // LLAMADA REAL A LA API PYTHON
    // =========================================================

    /**
     * Realiza la llamada HTTP POST a la API Python y parsea la respuesta.
     *
     * @throws Exception si la API no responde o devuelve un error HTTP
     */
    private Resultado llamarApi(String texto, String url) throws Exception {
        // Construir el cuerpo JSON de la petición
        String body = objectMapper.writeValueAsString(
                new ApiRequest(
                        texto != null ? texto : "",
                        url   != null ? url   : ""
                )
        );

        // Configurar la petición HTTP con timeout de 30 segundos
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        // Ejecutar la petición y verificar el código de respuesta
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("La API devolvió HTTP " + response.statusCode());
        }

        return parsearRespuesta(response.body());
    }

    // =========================================================
    // PARSEO DE RESPUESTA JSON
    // =========================================================

    /**
     * Convierte el JSON de respuesta de la API en un objeto Resultado.
     * Usa valores por defecto seguros si algún campo está ausente.
     */
    private Resultado parsearRespuesta(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        // Campos principales
        String etiqueta    = root.path("etiqueta").asText("INCIERTO");
        int    credibilidad= root.path("credibilidad").asInt(50);
        String explicacion = root.path("explicacion").asText("Sin explicación disponible.");

        // Lista de indicadores (señales detectadas en el texto)
        List<Indicador> indicadores = new ArrayList<>();
        JsonNode indNode = root.path("indicadores");
        if (indNode.isArray()) {
            for (JsonNode n : indNode) {
                indicadores.add(new Indicador(
                        n.path("tipo").asText("neutro"),
                        n.path("texto").asText("")
                ));
            }
        }

        // Lista de fuentes relacionadas
        List<Fuente> fuentes = new ArrayList<>();
        JsonNode fuentesNode = root.path("fuentes");
        if (fuentesNode.isArray()) {
            for (JsonNode n : fuentesNode) {
                fuentes.add(new Fuente(
                        n.path("nombre").asText(""),
                        n.path("url").asText("#"),
                        n.path("dominio").asText(""),
                        n.path("relevancia").asText("media")
                ));
            }
        }

        return new Resultado(etiqueta, credibilidad, explicacion, indicadores, fuentes);
    }

    // =========================================================
    // MODO MOCK — DATOS SIMULADOS PARA DESARROLLO
    // =========================================================

    /**
     * Genera un resultado simulado plausible para desarrollo y pruebas.
     *
     * Aplica heurísticas simples sobre el texto para decidir el veredicto:
     *   - Palabras alarmistas → FAKE (credibilidad baja)
     *   - Texto muy corto     → INCIERTO (insuficiente para analizar)
     *   - Texto normal        → REAL (credibilidad alta)
     *
     * Los resultados incluyen la etiqueta [MODO DEMO] para que quede
     * claro que no provienen de la IA real.
     */
    private Resultado buildMock(String texto) {
        String t = texto != null ? texto.toLowerCase() : "";

        // Detectar palabras típicas de desinformación
        boolean tieneAlarmismo = t.contains("¡¡")
                || t.contains("increíble") || t.contains("secreto")
                || t.contains("te lo ocultan") || t.contains("urgente")
                || t.contains("viral")        || t.contains("no quieren que sepas");

        String          etiqueta;
        int             credibilidad;
        String          explicacion;
        List<Indicador> indicadores = new ArrayList<>();
        List<Fuente>    fuentes     = new ArrayList<>();

        if (tieneAlarmismo) {
            // ── Veredicto: FAKE ──────────────────────────────
            etiqueta     = "FAKE";
            credibilidad = 18;
            explicacion  = "[MODO DEMO] El texto contiene patrones lingüísticos frecuentes "
                         + "en desinformación: lenguaje alarmista, apelaciones emocionales "
                         + "y ausencia de fuentes verificables. Se recomienda contrastar "
                         + "con medios de referencia antes de compartir.";
            indicadores.add(new Indicador("negativo", "Lenguaje alarmista detectado"));
            indicadores.add(new Indicador("negativo", "Ausencia de fuentes citadas"));
            indicadores.add(new Indicador("neutro",   "Contenido emocional elevado"));
            fuentes.add(new Fuente("Maldita.es", "https://maldita.es",  "maldita.es",  "alta"));
            fuentes.add(new Fuente("Newtral",    "https://newtral.es",  "newtral.es",  "alta"));

        } else if (t.length() < 80) {
            // ── Veredicto: INCIERTO ──────────────────────────
            etiqueta     = "INCIERTO";
            credibilidad = 50;
            explicacion  = "[MODO DEMO] El texto es demasiado breve para realizar un "
                         + "análisis concluyente. Proporciona el artículo completo o "
                         + "la URL para obtener un resultado más preciso.";
            indicadores.add(new Indicador("neutro", "Texto insuficiente para análisis"));
            indicadores.add(new Indicador("neutro", "Se recomienda aportar más contexto"));

        } else {
            // ── Veredicto: REAL ──────────────────────────────
            etiqueta     = "REAL";
            credibilidad = 82;
            explicacion  = "[MODO DEMO] No se han detectado patrones claros de desinformación. "
                         + "El texto presenta una estructura informativa coherente. "
                         + "Este resultado es orientativo; la verificación real requiere "
                         + "la API Python activa.";
            indicadores.add(new Indicador("positivo", "Estructura informativa coherente"));
            indicadores.add(new Indicador("positivo", "Lenguaje neutro y descriptivo"));
            indicadores.add(new Indicador("neutro",   "Sin fuentes explícitas detectadas"));
            fuentes.add(new Fuente("Reuters",      "https://reuters.com",    "reuters.com",    "alta"));
            fuentes.add(new Fuente("EFE",          "https://efe.com",        "efe.com",        "alta"));
            fuentes.add(new Fuente("Europa Press", "https://europapress.es", "europapress.es", "media"));
        }

        return new Resultado(etiqueta, credibilidad, explicacion, indicadores, fuentes);
    }

    // ── DTO interno para serializar la petición a la API ─────

    /** Representa el cuerpo JSON que se envía a la API Python */
    private record ApiRequest(String texto, String url) {}
}
