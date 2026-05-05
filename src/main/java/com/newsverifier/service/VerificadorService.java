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
 * No se usan respuestas de prueba: requiere la API activa.
 *
 * ── Contrato esperado de la API Python ──────────────────────
 *
 * Petición:
 *   POST /analizar
 *   Content-Type: application/json
 *   { "titulo": "...", "texto": "...", "url": "..." }
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

    // ── Clientes HTTP y JSON ──────────────────────────────────

    private final HttpClient   httpClient;
    private final ObjectMapper objectMapper;

    public VerificadorService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                // Comentario: forzamos HTTP/1.1 para evitar upgrades h2c no soportados.
                .version(HttpClient.Version.HTTP_1_1)
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
     *   1. Llama a la API Python
     *   2. Si falla, propaga la excepción para informar al usuario
     *
     * Nota: se eliminan los datos simulados para que el TFG use
     * siempre el modelo real (Hugging Face vía microservicio Python).
     *
     * @param titulo Título de la noticia (puede ser null si se aporta texto/url)
     * @param texto Texto de la noticia (puede ser null si se aporta url)
     * @param url   URL del artículo (puede ser null si se aporta texto)
     * @return Resultado con etiqueta, credibilidad, explicación, indicadores y fuentes
     */
    public Resultado analizar(String titulo, String texto, String url) throws Exception {
        // Comentario: enviamos titulo+texto+url para maximizar contexto del modelo.
        Resultado resultado = llamarApi(titulo, texto, url);
        // Comentario: guardamos la entrada original para mostrarla en la vista.
        resultado.setTitulo(titulo);
        resultado.setTexto(texto);
        resultado.setUrl(url);
        return resultado;
    }

    // =========================================================
    // LLAMADA REAL A LA API PYTHON
    // =========================================================

    /**
     * Realiza la llamada HTTP POST a la API Python y parsea la respuesta.
     *
     * @throws Exception si la API no responde o devuelve un error HTTP
     */
        private Resultado llamarApi(String titulo, String texto, String url) throws Exception {
        // Comentario: construimos el JSON y lo registramos para depurar errores 422.
        String body = objectMapper.writeValueAsString(
                new ApiRequest(
                titulo != null ? titulo : "",
                texto  != null ? texto  : "",
                url    != null ? url    : ""
                )
        );
        log.debug("[VerificadorService] Payload enviado a IA: {}", body);

        // Configurar la petición HTTP con timeout de 30 segundos
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
            .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        // Ejecutar la petición y verificar el código de respuesta
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            // Comentario: registramos el cuerpo para ver el motivo del error.
            log.warn("[VerificadorService] Respuesta IA HTTP {}: {}",
                    response.statusCode(), response.body());
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

    // ── DTO interno para serializar la petición a la API ─────

    /** Representa el cuerpo JSON que se envía a la API Python */
    private record ApiRequest(String titulo, String texto, String url) {}
}
