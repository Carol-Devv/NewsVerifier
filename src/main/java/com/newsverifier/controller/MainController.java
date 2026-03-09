package com.newsverifier.controller;

import com.newsverifier.model.Analisis;
import com.newsverifier.model.Resultado;
import com.newsverifier.model.Usuario;
import com.newsverifier.repository.AnalisisRepository;
import com.newsverifier.service.AuthService;
import com.newsverifier.service.VerificadorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * MainController — Controlador principal de NewsVerifier
 * ============================================================
 *
 * Gestiona todas las rutas HTTP de la aplicación y actúa como
 * intermediario entre la capa de servicios y las vistas Thymeleaf.
 *
 * Rutas disponibles:
 *   GET  /            → Redirige a /home o /login según sesión
 *   GET  /login       → Página de acceso (pestaña login)
 *   GET  /registro    → Página de acceso (pestaña registro)
 *   POST /login       → Procesa el formulario de login
 *   POST /registro    → Procesa el formulario de registro
 *   GET  /logout      → Cierra la sesión y redirige a /login
 *   GET  /home        → Página principal con el verificador
 *   POST /verificar   → Analiza una noticia y muestra el resultado
 *   GET  /historial   → Historial completo de análisis del usuario
 */
@Controller
public class MainController {

    // ── Dependencias ─────────────────────────────────────────

    private final AuthService        authService;        // Login y registro
    private final VerificadorService verificadorService; // Análisis con IA
    private final AnalisisRepository analisisRepository; // Acceso a BBDD

    public MainController(AuthService authService,
                          VerificadorService verificadorService,
                          AnalisisRepository analisisRepository) {
        this.authService        = authService;
        this.verificadorService = verificadorService;
        this.analisisRepository = analisisRepository;
    }

    // ── Utilidad: obtener usuario de sesión ───────────────────

    /**
     * Recupera el usuario autenticado de la sesión HTTP.
     * Devuelve null si no hay sesión activa.
     */
    private Usuario usuarioSesion(HttpSession session) {
        return (Usuario) session.getAttribute("usuario");
    }

    // =========================================================
    // RUTAS PÚBLICAS (no requieren autenticación)
    // =========================================================

    /** Redirige a /home si hay sesión activa, a /login si no */
    @GetMapping("/")
    public String root(HttpSession session) {
        return usuarioSesion(session) != null ? "redirect:/home" : "redirect:/login";
    }

    /** Muestra la página de login/registro con la pestaña de login activa */
    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (usuarioSesion(session) != null) return "redirect:/home";
        model.addAttribute("activeTab", "login");
        return "login"; // → templates/login.html
    }

    /** Muestra la página de login/registro con la pestaña de registro activa */
    @GetMapping("/registro")
    public String registroPage(HttpSession session, Model model) {
        if (usuarioSesion(session) != null) return "redirect:/home";
        model.addAttribute("activeTab", "registro");
        return "login"; // → templates/login.html (misma vista, distinta pestaña)
    }

    // ── Login (POST) ──────────────────────────────────────────

    /**
     * Procesa el formulario de inicio de sesión.
     * Si las credenciales son correctas, guarda el usuario en sesión
     * y redirige al home. Si no, devuelve la vista con el error.
     */
    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        try {
            Usuario usuario = authService.login(email, password);
            session.setAttribute("usuario", usuario);
            return "redirect:/home";
        } catch (Exception e) {
            // Credenciales incorrectas → volver al formulario con mensaje de error
            model.addAttribute("activeTab",  "login");
            model.addAttribute("loginEmail", email);   // Conservar el email introducido
            model.addAttribute("loginError", "Correo o contraseña incorrectos.");
            return "login";
        }
    }

    // ── Registro (POST) ───────────────────────────────────────

    /**
     * Procesa el formulario de registro de nuevo usuario.
     * Valida que las contraseñas coincidan y tengan longitud mínima.
     * Si el email ya existe, devuelve error específico.
     */
    @PostMapping("/registro")
    public String doRegistro(@RequestParam String nombre,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             HttpSession session,
                             Model model) {
        model.addAttribute("activeTab", "registro");

        // Validación: contraseñas coinciden
        if (!password.equals(confirmPassword)) {
            model.addAttribute("regNombre", nombre);
            model.addAttribute("regEmail",  email);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "login";
        }

        // Validación: longitud mínima de contraseña
        if (password.length() < 8) {
            model.addAttribute("regNombre", nombre);
            model.addAttribute("regEmail",  email);
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "login";
        }

        try {
            Usuario usuario = authService.registrar(nombre, email, password);
            session.setAttribute("usuario", usuario);
            return "redirect:/home";
        } catch (IllegalArgumentException e) {
            // Email ya registrado
            model.addAttribute("regNombre",     nombre);
            model.addAttribute("regEmail",      email);
            model.addAttribute("regEmailError", "Este correo ya está registrado.");
            return "login";
        }
    }

    // ── Logout ────────────────────────────────────────────────

    /** Invalida la sesión y redirige a la página de login */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // =========================================================
    // RUTAS PRIVADAS (requieren sesión activa)
    // =========================================================

    /**
     * Página principal del verificador.
     * Carga los últimos 5 análisis del usuario para el historial rápido
     * que aparece al pie de la página.
     */
    @GetMapping("/home")
    public String homePage(HttpSession session, Model model) {
        if (usuarioSesion(session) == null) return "redirect:/login";

        Usuario usuario = usuarioSesion(session);

        // Historial rápido: solo los 5 más recientes para el widget del home
        List<Analisis> historial = analisisRepository
                .findByUsuarioOrderByFechaAnalisisDesc(usuario);
        model.addAttribute("historial",
                historial.size() > 5 ? historial.subList(0, 5) : historial);

        return "home"; // → templates/home.html
    }

    /**
     * Procesa el formulario de verificación de noticias.
     *
     * Flujo:
     *   1. Valida que haya texto o URL
     *   2. Llama a VerificadorService (API Python o mock)
     *   3. Persiste el resultado en la BBDD
     *   4. Devuelve la vista con el resultado y el historial actualizado
     */
    @PostMapping("/verificar")
    public String verificar(@RequestParam(required = false) String texto,
                            @RequestParam(required = false) String url,
                            HttpSession session,
                            Model model) {

        if (usuarioSesion(session) == null) return "redirect:/login";

        // Validar que se ha introducido al menos texto o URL
        if ((texto == null || texto.isBlank()) && (url == null || url.isBlank())) {
            model.addAttribute("error", "Introduce el texto o la URL de la noticia.");
            return "home";
        }

        try {
            // Llamar al servicio de análisis (API Python o mock si no está disponible)
            Resultado resultado = verificadorService.analizar(texto, url);

            // Pasar resultado y datos del formulario a la vista
            model.addAttribute("resultado",    resultado);
            model.addAttribute("textoEnviado", texto);
            model.addAttribute("urlEnviada",   url);

            // Persistir el análisis en H2 vinculado al usuario actual
            Usuario usuario = usuarioSesion(session);
            Analisis analisis = new Analisis(
                    texto, url,
                    resultado.getEtiqueta(),
                    resultado.getCredibilidad(),
                    resultado.getExplicacion(),
                    usuario
            );
            analisisRepository.save(analisis);

            // Recargar historial rápido tras guardar el nuevo análisis
            List<Analisis> historial = analisisRepository
                    .findByUsuarioOrderByFechaAnalisisDesc(usuario);
            model.addAttribute("historial",
                    historial.size() > 5 ? historial.subList(0, 5) : historial);

        } catch (Exception e) {
            model.addAttribute("error",
                    "Error al conectar con el servicio de análisis. Inténtalo de nuevo.");
        }

        return "home";
    }

    /**
     * Página de historial completo del usuario.
     * Muestra todos los análisis realizados con estadísticas por veredicto
     * y permite filtrar por tipo (REAL / FAKE / INCIERTO) en el cliente.
     */
    @GetMapping("/historial")
    public String historialPage(HttpSession session, Model model) {
        if (usuarioSesion(session) == null) return "redirect:/login";

        Usuario usuario = usuarioSesion(session);

        // Obtener todos los análisis del usuario, ordenados del más reciente
        List<Analisis> historial = analisisRepository
                .findByUsuarioOrderByFechaAnalisisDesc(usuario);

        // Calcular estadísticas para la barra de resumen
        long totalReal     = historial.stream().filter(a -> "REAL".equals(a.getEtiqueta())).count();
        long totalFake     = historial.stream().filter(a -> "FAKE".equals(a.getEtiqueta())).count();
        long totalIncierto = historial.stream().filter(a -> "INCIERTO".equals(a.getEtiqueta())).count();

        model.addAttribute("historial",     historial);
        model.addAttribute("totalAnalisis", historial.size());
        model.addAttribute("totalReal",     totalReal);
        model.addAttribute("totalFake",     totalFake);
        model.addAttribute("totalIncierto", totalIncierto);

        return "historial"; // → templates/historial.html
    }
}
