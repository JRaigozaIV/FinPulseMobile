"""
Módulo de Detección de Anomalías Analíticas en Tiempo Real
Proyecto: FinPulse Mobile - Monitoreo Analítico en la Nube
"""
from typing import List, Dict, Any, Optional
from datetime import datetime
import numpy as np


class AnomalyDetector:
    """
    Detector estadístico y basado en umbrales dinámicos para KPIs transaccionales bancarios.
    Implementa:
    1. Análisis de Z-Score sobre ventanas móviles de 20 periodos.
    2. Reglas heurísticas de umbrales críticos de aprobación por canal (PSE, QR, Tarjetas).
    3. Clasificación de severidad y generación de payloads para Notificaciones Push (FCM).
    """

    def __init__(self, z_threshold: float = 2.5, min_approval_threshold: float = 90.0):
        self.z_threshold = z_threshold
        self.min_approval_threshold = min_approval_threshold
        self.history: List[Dict[str, Any]] = []
        self.max_history = 100

    def evaluate_metrics(self, current_metrics: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        Evalúa las métricas actuales y retorna un objeto de alerta push si detecta anomalía.
        """
        self.history.append(current_metrics)
        if len(self.history) > self.max_history:
            self.history.pop(0)

        approval_rate = current_metrics.get("approval_rate", 100.0)
        tps = current_metrics.get("tps", 0.0)
        avg_latency_ms = current_metrics.get("avg_latency_ms", 120.0)
        channel_breakdown = current_metrics.get("channels", {})

        alerts = []

        # 1. Regla Crítica: Tasa de Aprobación Global bajo el umbral mínimo
        if approval_rate < self.min_approval_threshold:
            alerts.append({
                "type": "GLOBAL_APPROVAL_DROP",
                "severity": "CRITICAL" if approval_rate < 85.0 else "WARNING",
                "message": f"Caída crítica en Tasa de Aprobación Global: {approval_rate:.1f}% (Umbral base: {self.min_approval_threshold}%)",
                "metric": "approval_rate",
                "value": approval_rate
            })

        # 2. Regla Específica por Canales (PSE / QR)
        for channel, data in channel_breakdown.items():
            ch_approval = data.get("approval_rate", 100.0)
            ch_vol = data.get("volume", 0)
            if ch_approval < 88.0 and ch_vol > 5:
                alerts.append({
                    "type": f"CHANNEL_DROP_{channel.upper()}",
                    "severity": "CRITICAL" if ch_approval < 80.0 else "WARNING",
                    "message": f"Anomalía en canal {channel.upper()}: Aprobación cayó a {ch_approval:.1f}%",
                    "metric": f"{channel}_approval",
                    "value": ch_approval
                })

        # 3. Regla de Latencia Transaccional Alta
        if avg_latency_ms > 2000.0:
            alerts.append({
                "type": "LATENCY_SPIKE",
                "severity": "WARNING",
                "message": f"Pico de latencia transaccional: {avg_latency_ms:.0f} ms (esperado < 600 ms)",
                "metric": "latency",
                "value": avg_latency_ms
            })

        # 4. Análisis Estadístico Z-Score sobre historial (cuando hay al menos 10 puntos)
        if len(self.history) >= 10:
            rates = [h["approval_rate"] for h in self.history[:-1]]
            mean_rate = float(np.mean(rates))
            std_rate = float(np.std(rates))

            if std_rate > 0.05:
                z_score = (approval_rate - mean_rate) / std_rate
                if z_score < -self.z_threshold:
                    alerts.append({
                        "type": "STATISTICAL_ZSCORE_ANOMALY",
                        "severity": "CRITICAL",
                        "message": f"Desviación estadística severa: Z-Score = {z_score:.2f} (< -{self.z_threshold})",
                        "metric": "z_score",
                        "value": round(z_score, 2)
                    })

        if alerts:
            # Retornar la alerta de mayor severidad
            priority_order = {"CRITICAL": 0, "WARNING": 1, "INFO": 2}
            alerts.sort(key=lambda a: priority_order.get(a["severity"], 3))
            primary_alert = alerts[0]

            # Construir payload compatible con Firebase Cloud Messaging (FCM)
            push_payload = {
                "id": f"alert-{int(datetime.now().timestamp()*1000)}",
                "timestamp": datetime.now().isoformat(),
                "title": f"🚨 Alerta {primary_alert['severity']}: FinPulse Bancolombia",
                "body": primary_alert["message"],
                "severity": primary_alert["severity"],
                "details": alerts,
                "data": {
                    "click_action": "FLUTTER_NOTIFICATION_CLICK",
                    "screen": "live_metrics",
                    "approval_rate": str(approval_rate),
                    "channel_affected": primary_alert["type"]
                }
            }
            return push_payload

        return None
