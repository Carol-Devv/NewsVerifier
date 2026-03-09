# NewsVerifier — Detector de Fake News (Alpha)
> TFG · Frontend Thymeleaf + Spring Boot

---

## Requisitos

| Herramienta | Versión mínima |
|-------------|---------------|
| Java (JDK)  | 21            |
| Maven       | 3.9+          |
| (Opcional) API Python de IA | cualquiera |

---

## Arrancar la aplicación

```bash
# 1. Clonar / descomprimir el proyecto
cd newsverifier

# 2. Compilar y lanzar
mvn spring-boot:run
```

La aplicación estará disponible en: **http://localhost:8080**

---

## Usuario de prueba (precargado)

| Campo      | Valor            |
|------------|------------------|
| Email      | demo@newsverifier.com  |
| Contraseña | demo1234         |

---

## Modo Mock vs API real

En `src/main/resources/application.properties`:

```properties
# true  → usa datos simulados (no necesita la API Python)
# false → llama a la API Python; si falla, cae al mock
ia.mock.enabled=true

# URL de la API Python cuando esté disponible
ia.api.url=http://localhost:5000/analizar
```

Con `ia.mock.enabled=true` la aplicación funciona completamente
sin necesitar la API Python. Ideal para desarrollo del front.

---

## Contrato esperado de la API Python

```
POST /analizar
Content-Type: application/json

{
  "texto": "Texto de la noticia...",
  "url":   "https://..."
}
```

Respuesta esperada:

```json
{
  "etiqueta":     "REAL",
  "credibilidad": 87,
  "explicacion":  "El texto presenta...",
  "indicadores": [
    { "tipo": "positivo", "texto": "Fuentes verificables citadas" },
    { "tipo": "negativo", "texto": "Lenguaje alarmista detectado" }
  ],
  "fuentes": [
    {
      "nombre":     "Reuters",
      "url":        "https://reuters.com/...",
      "dominio":    "reuters.com",
      "relevancia": "alta"
    }
  ]
}
```

Valores posibles:
- `etiqueta`: `REAL` | `FAKE` | `INCIERTO`
- `relevancia`: `alta` | `media` | `baja`
- `tipo` (indicador): `positivo` | `negativo` | `neutro`

---

## Consola H2 (base de datos)

Accede a la BBDD en memoria en: **http://localhost:8080/h2-console**

| Campo    | Valor                                      |
|----------|--------------------------------------------|
| JDBC URL | `jdbc:h2:mem:newsverifierdb`                    |
| Usuario  | `sa`                                       |
| Password | *(vacío)*                                  |

---

## Estructura del proyecto

```
src/main/
├── java/com/newsverifier/
│   ├── NewsVerifierApplication.java      ← Punto de entrada Spring Boot
│   ├── config/
│   │   └── SecurityConfig.java       ← Config Spring Security + BCrypt
│   ├── controller/
│   │   └── MainController.java       ← Rutas y lógica de navegación
│   ├── model/
│   │   ├── Usuario.java              ← Entidad JPA de usuario
│   │   ├── Analisis.java             ← Entidad JPA de cada análisis
│   │   └── Resultado.java            ← DTO de respuesta de la IA
│   ├── repository/
│   │   ├── UsuarioRepository.java    ← Acceso a BBDD usuarios
│   │   └── AnalisisRepository.java   ← Acceso a BBDD análisis
│   └── service/
│       ├── AuthService.java          ← Login y registro con BCrypt
│       └── VerificadorService.java   ← Llamada a API Python + mock
└── resources/
    ├── application.properties        ← Configuración general
    ├── data.sql                      ← Usuario demo precargado
    └── templates/
        ├── layout.html               ← Plantilla base (topbar + footer)
        ├── login.html                ← Login + Registro
        └── home.html                 ← Verificador + Historial
```
