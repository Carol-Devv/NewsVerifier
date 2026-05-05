import os
from typing import List, Optional

import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from transformers import pipeline

# Comentario: servicio FastAPI que expone /analizar para el backend Java.
app = FastAPI(title="NewsVerifier IA", version="1.0.0")
logger = logging.getLogger("newsverifier")

# Comentario: el modelo se configura por variable de entorno para facilitar despliegue.
MODEL_NAME = os.getenv(
    "MODEL_NAME",
    "Narrativaai/fake-news-detection-spanish",
)

# Comentario: pipeline de clasificacion de texto en castellano.
classifier = pipeline(
    task="text-classification",
    model=MODEL_NAME,
    truncation=True,
)


class Indicador(BaseModel):
    # Comentario: indicador interpretable para mostrar en la UI.
    tipo: str = Field(..., description="positivo|negativo|neutro")
    texto: str = Field(..., description="descripcion corta")


class Fuente(BaseModel):
    # Comentario: se deja disponible para futuras integraciones con fuentes reales.
    nombre: str
    url: str
    dominio: str
    relevancia: str


class AnalisisRequest(BaseModel):
    # Comentario: contrato esperado por el backend Java.
    titulo: Optional[str] = ""
    texto: Optional[str] = ""
    url: Optional[str] = ""


class AnalisisResponse(BaseModel):
    # Comentario: contrato de respuesta consumido por VerificadorService.
    etiqueta: str
    credibilidad: int
    explicacion: str
    indicadores: List[Indicador]
    fuentes: List[Fuente]


def _construir_texto(titulo: str, texto: str, url: str) -> str:
    # Comentario: concatenamos para dar mas contexto al modelo.
    partes = []
    if titulo:
        partes.append(titulo.strip())
    if texto:
        partes.append(texto.strip())
    if url:
        partes.append(url.strip())
    return "\n\n".join([p for p in partes if p])


def _mapear_etiqueta(model_label: str) -> str:
    # Comentario: normalizamos etiquetas comunes a REAL/FAKE/INCIERTO.
    l = model_label.lower()
    if "fake" in l:
        return "FAKE"
    if "real" in l or "true" in l:
        return "REAL"
    return "INCIERTO"


@app.get("/health")
def healthcheck() -> dict:
    # Comentario: endpoint simple para verificar que el servicio esta vivo.
    return {"status": "ok", "model": MODEL_NAME}


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    # Comentario: registramos detalles del 422 para depurar el contrato.
    body = await request.body()
    logger.warning("Validacion fallida en /analizar. Body=%s Errors=%s", body, exc.errors())
    return JSONResponse(status_code=422, content={"detail": exc.errors()})


@app.post("/analizar", response_model=AnalisisResponse)
def analizar(payload: AnalisisRequest) -> AnalisisResponse:
    # Comentario: validamos que haya alguna entrada antes de invocar el modelo.
    if not (payload.titulo or payload.texto or payload.url):
        raise HTTPException(status_code=400, detail="Falta titulo, texto o URL")

    texto_modelo = _construir_texto(payload.titulo, payload.texto, payload.url)

    # Comentario: inferencia directa del modelo; devuelve label y score.
    salida = classifier(texto_modelo)[0]
    etiqueta = _mapear_etiqueta(salida.get("label", ""))
    score = float(salida.get("score", 0.0))
    credibilidad = int(round(score * 100))

    # Comentario: si la confianza es baja, marcamos como INCIERTO.
    if credibilidad < 60:
        etiqueta = "INCIERTO"

    # Comentario: explicacion corta basada en el resultado del modelo.
    if etiqueta == "REAL":
        explicacion = (
            "El modelo detecta patrones compatibles con una noticia verificada. "
            "La confianza es alta y no aparecen senales claras de desinformacion."
        )
        indicadores = [
            Indicador(tipo="positivo", texto="Patrones de redaccion informativa"),
            Indicador(tipo="positivo", texto="Confianza alta del modelo"),
        ]
    elif etiqueta == "FAKE":
        explicacion = (
            "El modelo detecta patrones compatibles con desinformacion. "
            "Se recomienda contrastar con fuentes fiables."
        )
        indicadores = [
            Indicador(tipo="negativo", texto="Patrones asociados a desinformacion"),
            Indicador(tipo="negativo", texto="Confianza alta del modelo"),
        ]
    else:
        explicacion = (
            "El modelo no alcanza un nivel de confianza suficiente. "
            "Aporta mas texto o una URL completa para un analisis mejor."
        )
        indicadores = [
            Indicador(tipo="neutro", texto="Confianza insuficiente para decidir"),
        ]

    # Comentario: de momento no se devuelven fuentes reales.
    fuentes: List[Fuente] = []

    return AnalisisResponse(
        etiqueta=etiqueta,
        credibilidad=credibilidad,
        explicacion=explicacion,
        indicadores=indicadores,
        fuentes=fuentes,
    )
