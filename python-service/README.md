# Microservicio IA (Python)

Este servicio expone el endpoint `/analizar` que consume el backend Java.

## Contrato

**POST /analizar**

Request JSON:
```
{
  "titulo": "...",
  "texto": "...",
  "url": "..."
}
```

Response JSON:
```
{
  "etiqueta": "REAL|FAKE|INCIERTO",
  "credibilidad": 0,
  "explicacion": "...",
  "indicadores": [
    { "tipo": "positivo|negativo|neutro", "texto": "..." }
  ],
  "fuentes": []
}
```

## Ejecucion local

```bash
cd python-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 5000
```

## Configuracion del modelo

- Variable `MODEL_NAME`: nombre del modelo en Hugging Face.
- Por defecto se usa: `mrm8488/bert-spanish-cased-finetuned-fake-news`.

Ejemplo:
```bash
MODEL_NAME="tu/modelo" uvicorn app:app --host 0.0.0.0 --port 5000
```
