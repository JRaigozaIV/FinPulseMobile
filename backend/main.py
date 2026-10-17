"""
Servicio API REST y Motor Analítico en la Nube
Proyecto: FinPulse Mobile - Monitoreo Analítico en Tiempo Real
Curso: Diseño de Aplicaciones Móviles
"""
import os
from typing import Dict, Any, List
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel

from anomaly_detector import AnomalyDetector
from transaction_generator import BankingStreamSimulator

app = FastAPI(
    title="FinPulse Cloud API - Real-Time Banking Analytics & Alerts",
    description="Backend en la nube para ingesta analítica, cálculo de KPIs transaccionales y despacho de notificaciones push móviles.",
    version="1.0.0"
)

# Habilitar CORS para consumo desde la aplicación móvil / PWA
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Instancias del simulador y detector de anomalías
simulator = BankingStreamSimulator()
detector = AnomalyDetector(z_threshold=2.2, min_approval_threshold=90.0)

# Buffer de historial y alertas emitidas
metrics_history: List[Dict[str, Any]] = []
recent_alerts: List[Dict[str, Any]] = []


class AnomalyTriggerRequest(BaseModel):
    anomaly_type: str  # "pse_drop", "qr_spike", "system_latency"
    duration_seconds: int = 60


@app.get("/api/v1/health")
def healthcheck():
    return {
        "service": "FinPulse Mobile Backend Cloud Service",
        "status": "ONLINE",
        "cloud_provider": "AWS / Serverless Microservice",
        "architecture_layer": "Edge API Gateway -> Cloud Compute -> In-Memory KPI Cache",
        "supported_mobile_platforms": ["Android", "iOS", "Progressive Web App"]
    }


@app.get("/api/v1/metrics/live")
def get_live_metrics():
    """
    Endpoint de alta velocidad consumido por la app móvil (<50ms).
    Calcula el snapshot actual, evalúa anomalías y genera payload push si corresponde.
    """
    snapshot = simulator.generate_current_window_metrics()
    alert_payload = detector.evaluate_metrics(snapshot)

    snapshot["push_alert"] = alert_payload
    if alert_payload:
        recent_alerts.insert(0, alert_payload)
        if len(recent_alerts) > 20:
            recent_alerts.pop()

    metrics_history.append(snapshot)
    if len(metrics_history) > 30:
        metrics_history.pop(0)

    return snapshot


@app.get("/api/v1/metrics/history")
def get_metrics_history():
    """Retorna los últimos 20 puntos de métricas para graficación en la app móvil."""
    return metrics_history[-20:] if metrics_history else []


@app.get("/api/v1/alerts/recent")
def get_recent_alerts():
    """Retorna las notificaciones push emitidas recientemente."""
    return recent_alerts


@app.post("/api/v1/anomalies/simulate")
def trigger_anomaly(req: AnomalyTriggerRequest):
    """
    Permite simular una anomalía transaccional para verificar
    el disparo inmediato de notificaciones push en el cliente móvil.
    """
    valid_types = ["pse_drop", "qr_spike", "system_latency"]
    if req.anomaly_type not in valid_types:
        raise HTTPException(status_code=400, detail=f"Tipo no válido. Opciones: {valid_types}")

    simulator.set_anomaly(req.anomaly_type, req.duration_seconds)
    # Generar de inmediato para capturar la anomalía
    snapshot = simulator.generate_current_window_metrics()
    alert = detector.evaluate_metrics(snapshot)
    if alert:
        recent_alerts.insert(0, alert)

    return {
        "status": "ANOMALY_INJECTED",
        "type": req.anomaly_type,
        "duration_seconds": req.duration_seconds,
        "immediate_alert": alert
    }


@app.post("/api/v1/anomalies/resolve")
def resolve_anomaly():
    """Restaura el estado saludable del flujo transaccional."""
    simulator.clear_anomaly()
    return {"status": "ANOMALY_CLEARED", "message": "Flujo transaccional restaurado a niveles basales"}


# Servir el frontend móvil si existe el directorio
frontend_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "frontend")
if os.path.exists(frontend_dir):
    app.mount("/static", StaticFiles(directory=frontend_dir), name="static")

    @app.get("/")
    def serve_mobile_app():
        return FileResponse(os.path.join(frontend_dir, "index.html"))
