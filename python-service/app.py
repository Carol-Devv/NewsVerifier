import os
from typing import List, Optional

import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
import requests
from bs4 import BeautifulSoup
from transformers import pipeline

# servicio FastAPI que expone /analizar para el backend Java.
app = FastAPI(title="NewsVerifier IA", version="1.0.0")
logger = logging.getLogger("newsverifier")

# el modelo se configura por variable de entorno para facilitar despliegue.
MODEL_NAME = os.getenv(
    "MODEL_NAME",
    "Narrativaai/fake-news-detection-spanish",
)

# pipeline de clasificacion de texto en castellano.
classifier = pipeline(
    task="text-classification",
    model=MODEL_NAME,
    truncation=True,
)


class Indicador(BaseModel):
    # indicador interpretable para mostrar en la UI.
    tipo: str = Field(..., description="positivo|negativo|neutro")
    texto: str = Field(..., description="descripcion corta")


class Fuente(BaseModel):
    # se deja disponible para futuras integraciones con fuentes reales.
    nombre: str
    url: str
    dominio: str
    relevancia: str


class AnalisisRequest(BaseModel):
    # contrato esperado por el backend Java.
    titulo: Optional[str] = ""
    texto: Optional[str] = ""
    url: Optional[str] = ""


class AnalisisResponse(BaseModel):
    # contrato de respuesta consumido por VerificadorService.
    etiqueta: str
    credibilidad: float
    explicacion: str
    indicadores: List[Indicador]
    fuentes: List[Fuente]


def _extraer_texto_url(url: str) -> str:
    # extrae texto visible de una URL para alimentar el modelo.
    headers = {
        "User-Agent": "NewsVerifierBot/1.0 (+https://example.com)",
    }
    try:
        respuesta = requests.get(url, headers=headers, timeout=10)
        respuesta.raise_for_status()
    except requests.RequestException as exc:
        logger.warning("No se pudo descargar la URL=%s Error=%s", url, exc)
        return ""

    soup = BeautifulSoup(respuesta.text, "html.parser")
    for tag in soup(["script", "style", "noscript"]):
        tag.decompose()

    texto = " ".join(soup.stripped_strings)
    return texto


def _construir_texto(titulo: str, texto: str) -> str:
    # concatenamos para dar mas contexto al modelo.
    partes = []
    if titulo:
        partes.append(titulo.strip())
    if texto:
        partes.append(texto.strip())
    return "\n\n".join([p for p in partes if p])


def _mapear_etiqueta(model_label: str) -> str:
    # normalizamos etiquetas comunes a REAL/FAKE/INCIERTO.
    l = model_label.lower()
    if "fake" in l:
        return "FAKE"
    if "real" in l or "true" in l:
        return "REAL"
    return "INCIERTO"


@app.get("/health")
def healthcheck() -> dict:
    # endpoint simple para verificar que el servicio esta vivo.
    return {"status": "ok", "model": MODEL_NAME}


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    # registramos detalles del 422 para depurar el contrato.
    body = await request.body()
    logger.warning("Validacion fallida en /analizar. Body=%s Errors=%s", body, exc.errors())
    return JSONResponse(status_code=422, content={"detail": exc.errors()})


@app.post("/analizar", response_model=AnalisisResponse)
def analizar(payload: AnalisisRequest) -> AnalisisResponse:
    # validamos que haya alguna entrada antes de invocar el modelo.
    if not (payload.titulo or payload.texto or payload.url):
        raise HTTPException(status_code=400, detail="Falta titulo, texto o URL")

    texto = payload.texto or ""
    if payload.url and not texto:
        # Comentario: si solo llega URL, extraemos el contenido para el modelo.
        texto = _extraer_texto_url(payload.url)
        if not texto:
            raise HTTPException(status_code=400, detail="No se pudo extraer texto de la URL")

    texto_modelo = _construir_texto(payload.titulo, texto)

    # inferencia directa del modelo; devuelve label y score.
    salida = classifier(texto_modelo)[0]
    etiqueta = _mapear_etiqueta(salida.get("label", ""))
    # Comentario: devolvemos el score crudo del modelo, sin convertir a porcentaje.
    credibilidad = float(salida.get("score", 0.0))

    # si la confianza es baja, marcamos como INCIERTO.
    # Comentario: umbral en score crudo 0-1 (0.6 equivale a 60%).
    if credibilidad < 0.6:
        etiqueta = "INCIERTO"

    # explicacion corta basada en el resultado del modelo.
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

    # de momento no se devuelven fuentes reales.
    fuentes: List[Fuente] = []

    return AnalisisResponse(
        etiqueta=etiqueta,
        credibilidad=credibilidad,
        explicacion=explicacion,
        indicadores=indicadores,
        fuentes=fuentes,
    )
