"""
Generador y Agregador de Transacciones Bancarias Sintéticas
Proyecto: FinPulse Mobile - Monitoreo Analítico en la Nube
"""
import random
import time
from typing import Dict, Any, List
from datetime import datetime


class BankingStreamSimulator:
    """
    Simula el flujo de transacciones bancarias en tiempo real y calcula agregaciones
    por minuto para alimentar la API en la nube y el frontend móvil.
    """

    def __init__(self):
        self.channels = ["pse", "qr_bancolombia", "tarjeta_debito", "transferencia"]
        self.anomaly_mode = None  # None, "pse_drop", "qr_spike", "system_latency"
        self.anomaly_expires_at = 0

    def set_anomaly(self, mode: str, duration_seconds: int = 45):
        """Activa una anomalía simulada por un tiempo determinado."""
        self.anomaly_mode = mode
        self.anomaly_expires_at = time.time() + duration_seconds

    def clear_anomaly(self):
        """Restaura el comportamiento normal del flujo."""
        self.anomaly_mode = None
        self.anomaly_expires_at = 0

    def generate_current_window_metrics(self) -> Dict[str, Any]:
        """
        Genera un snapshot de métricas agregadas del último minuto
        simulando la respuesta de un microservicio serverless o caché Redis.
        """
        now = time.time()
        if self.anomaly_mode and now > self.anomaly_expires_at:
            self.clear_anomaly()

        channel_metrics = {}
        total_tx = 0
        approved_tx = 0
        latencies = []

        for ch in self.channels:
            vol = random.randint(35, 90)
            base_approval = 0.965 + random.uniform(-0.015, 0.02)
            base_latency = random.uniform(120, 260)

            # Inyección de anomalías controladas
            if self.anomaly_mode == "pse_drop" and ch == "pse":
                base_approval = random.uniform(0.68, 0.78)  # Caída severa en PSE
                base_latency = random.uniform(2200, 3400)
            elif self.anomaly_mode == "qr_spike" and ch == "qr_bancolombia":
                base_approval = random.uniform(0.72, 0.82)
                base_latency = random.uniform(1500, 2800)
            elif self.anomaly_mode == "system_latency":
                base_latency = random.uniform(2400, 4200)
                base_approval -= 0.08

            ch_approved = int(vol * max(0.0, min(1.0, base_approval)))
            total_tx += vol
            approved_tx += ch_approved
            latencies.append(base_latency)

            channel_metrics[ch] = {
                "volume": vol,
                "approved": ch_approved,
                "rejected": vol - ch_approved,
                "approval_rate": round((ch_approved / vol) * 100, 2) if vol > 0 else 100.0,
                "avg_latency_ms": round(base_latency, 1)
            }

        overall_approval_rate = round((approved_tx / total_tx) * 100, 2) if total_tx > 0 else 100.0
        overall_latency = round(sum(latencies) / len(latencies), 1)
        tps = round(total_tx / 60.0, 2)

        return {
            "timestamp": datetime.now().isoformat(),
            "status": "ANOMALY_ACTIVE" if self.anomaly_mode else "HEALTHY",
            "active_anomaly_type": self.anomaly_mode,
            "tps": tps,
            "total_volume_minute": total_tx,
            "approved_volume_minute": approved_tx,
            "approval_rate": overall_approval_rate,
            "avg_latency_ms": overall_latency,
            "channels": channel_metrics,
            "cloud_edge_region": "us-east-1 (AWS Serverless / Lambda)",
            "api_response_time_ms": round(random.uniform(18, 42), 2)
        }
