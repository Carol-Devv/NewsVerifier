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

## Arrancar la aplicación (modo actual con IA)

```bash
# 1. Microservicio Python (IA)
cd python-service
# Si no existe el entorno virtual:
# python -m venv .venv
source ../.venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 5000

# 2. Backend + Web (Spring Boot)
cd ..
mvn spring-boot:run
```

La web estará disponible en: **http://localhost:8080**

---

## Usuario de prueba (precargado)

| Campo      | Valor            |
|------------|------------------|
| Email      | demo@newsverifier.com  |
| Contraseña | demo1234         |

---

## Contrato esperado de la API Python

```
POST /analizar
Content-Type: application/json

{
  "titulo": "Titular de la noticia...",
  "texto":  "Texto de la noticia...",
  "fuente": "El Pais"
}
```

Respuesta esperada:

```json
{
  "etiqueta":     "REAL",
  "credibilidad": 0.87,
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
- `credibilidad`: score crudo del modelo (0-1)

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
