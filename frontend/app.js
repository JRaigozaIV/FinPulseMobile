/**
 * FinPulse Mobile - Lógica de Cliente Móvil y Notificaciones Push
 */

let chartInstance = null;
let historicalPoints = [];
const MAX_CHART_POINTS = 15;
let lastAlertId = null;

// Inicialización de Service Worker PWA
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/static/sw.js')
      .then(reg => console.log('FinPulse ServiceWorker registrado:', reg.scope))
      .catch(err => console.log('Fallo en registro de SW:', err));
  });
}

// Inicialización del Gráfico de Tiempo Real
function initChart() {
  const ctx = document.getElementById('approvalChart').getContext('2d');
  
  // Rellenar inicialmente con 10 puntos saludables
  for (let i = 10; i >= 1; i--) {
    historicalPoints.push({
      time: `-${i * 15}s`,
      val: (97.0 + Math.random() * 1.5).toFixed(1)
    });
  }

  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: historicalPoints.map(p => p.time),
      datasets: [
        {
          label: 'Aprobación Global (%)',
          data: historicalPoints.map(p => p.val),
          borderColor: '#00E676',
          backgroundColor: 'rgba(0, 230, 118, 0.12)',
          fill: true,
          tension: 0.35,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 4
        },
        {
          label: 'SLA Mínimo (95%)',
          data: historicalPoints.map(() => 95.0),
          borderColor: 'rgba(255, 59, 48, 0.5)',
          borderDash: [5, 5],
          fill: false,
          pointRadius: 0,
          borderWidth: 1.5
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        x: {
          grid: { color: 'rgba(255, 255, 255, 0.05)' },
          ticks: { color: '#64748B', font: { size: 9 } }
        },
        y: {
          min: 60,
          max: 100,
          grid: { color: 'rgba(255, 255, 255, 0.05)' },
          ticks: { color: '#64748B', font: { size: 9 }, stepSize: 10 }
        }
      }
    }
  });
}

// Actualizar métricas periódicamente
async function fetchLiveMetrics() {
  const startTime = performance.now();
  try {
    const res = await fetch('/api/v1/metrics/live');
    const elapsed = Math.round(performance.now() - startTime);
    const data = await res.json();

    document.getElementById('edgeLatency').innerText = `${elapsed} ms`;
    renderUI(data);
  } catch (err) {
    console.warn('Modo sin conexión o backend iniciando:', err);
    document.getElementById('edgeLatency').innerText = 'Offline Cache';
  }
}

// Renderizado reactivo en el DOM
function renderUI(data) {
  const approvalRate = data.approval_rate;
  const isAnomaly = data.status === 'ANOMALY_ACTIVE' || approvalRate < 90.0;

  // Actualizar Badge de Estado
  const badge = document.getElementById('statusBadge');
  const statusText = document.getElementById('statusText');
  if (isAnomaly) {
    badge.className = 'status-badge anomaly';
    statusText.innerText = 'Anomalía Activa';
  } else {
    badge.className = 'status-badge';
    statusText.innerText = 'En Línea';
  }

  // Hero Card
  const heroVal = document.getElementById('globalApprovalVal');
  const heroDelta = document.getElementById('globalApprovalDelta');
  heroVal.innerText = `${approvalRate.toFixed(1)}%`;

  if (approvalRate < 90.0) {
    heroVal.className = 'hero-value critical';
    heroDelta.className = 'hero-delta negative';
    heroDelta.innerText = `${(approvalRate - 95.0).toFixed(1)}% vs SLA`;
  } else {
    heroVal.className = 'hero-value';
    heroDelta.className = 'hero-delta';
    heroDelta.innerText = `+${(approvalRate - 95.0).toFixed(1)}% vs SLA`;
  }

  document.getElementById('tpsVal').innerText = `${data.tps} TPS`;
  document.getElementById('avgLatencyVal').innerText = `${data.avg_latency_ms.toFixed(0)} ms`;
  document.getElementById('volTotalVal').innerText = data.total_volume_minute;
  document.getElementById('volRatioVal').innerText = `${data.approved_volume_minute} / ${data.total_volume_minute - data.approved_volume_minute}`;

  // Canales
  const channels = data.channels || {};
  updateChannelRow('pse', channels.pse);
  updateChannelRow('qr', channels.qr_bancolombia);
  updateChannelRow('card', channels.tarjeta_debito);
  updateChannelRow('trans', channels.transferencia);

  // Gráfico en tiempo real
  if (chartInstance) {
    const nowTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    historicalPoints.push({ time: nowTime, val: approvalRate });
    if (historicalPoints.length > MAX_CHART_POINTS) {
      historicalPoints.shift();
    }

    chartInstance.data.labels = historicalPoints.map(p => p.time);
    chartInstance.data.datasets[0].data = historicalPoints.map(p => p.val);
    chartInstance.data.datasets[0].borderColor = isAnomaly ? '#FF3B30' : '#00E676';
    chartInstance.data.datasets[0].backgroundColor = isAnomaly ? 'rgba(255, 59, 48, 0.15)' : 'rgba(0, 230, 118, 0.12)';
    chartInstance.update('none');
  }

  // Notificación Push
  if (data.push_alert && data.push_alert.id !== lastAlertId) {
    lastAlertId = data.push_alert.id;
    triggerPushNotification(data.push_alert);
  }
}

function updateChannelRow(prefix, chData) {
  if (!chData) return;
  const rateElem = document.getElementById(`${prefix}Rate`);
  const barElem = document.getElementById(`${prefix}Bar`);
  const statsElem = document.getElementById(`${prefix}Stats`);

  rateElem.innerText = `${chData.approval_rate.toFixed(1)}%`;
  barElem.style.width = `${Math.max(10, chData.approval_rate)}%`;
  statsElem.innerText = `Vol: ${chData.volume} | Lat: ${chData.avg_latency_ms.toFixed(0)} ms`;

  if (chData.approval_rate < 88.0) {
    rateElem.className = 'channel-rate critical';
    barElem.className = 'progress-bar-fill critical';
  } else {
    rateElem.className = 'channel-rate';
    barElem.className = 'progress-bar-fill';
  }
}

// Disparador de Notificación Push
function triggerPushNotification(alertData) {
  const toast = document.getElementById('pushToast');
  document.getElementById('pushTitle').innerText = alertData.title;
  document.getElementById('pushBody').innerText = alertData.body;
  document.getElementById('pushTime').innerText = `Hace unos segundos • ${alertData.severity}`;
  
  toast.classList.add('active');

  // Disparar vibración en móvil si está soportada
  if (navigator.vibrate) {
    navigator.vibrate([250, 100, 250]);
  }

  // Si hay permiso de notificaciones de sistema del navegador (Desktop / Android Chrome)
  if (window.Notification && Notification.permission === 'granted') {
    new Notification(alertData.title, {
      body: alertData.body,
      icon: 'https://cdn-icons-png.flaticon.com/512/2830/2830284.png'
    });
  }

  // Ocultar toast flotante después de 6 segundos
  setTimeout(() => {
    toast.classList.remove('active');
  }, 6500);
}

// Pedir permiso para notificaciones nativas del sistema
async function requestNotificationPerm() {
  if ('Notification' in window) {
    const perm = await Notification.requestPermission();
    if (perm === 'granted') {
      alert('¡Notificaciones Push del sistema activadas correctamente!');
    } else {
      alert('Permiso de notificaciones no concedido.');
    }
  } else {
    alert('Tu navegador simula las alertas push mediante banners integrados.');
  }
}

// Simulación de Anomalías
async function simulateAnomaly(type) {
  try {
    await fetch('/api/v1/anomalies/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ anomaly_type: type, duration_seconds: 45 })
    });
    fetchLiveMetrics();
  } catch (err) {
    console.error('Error al simular:', err);
  }
}

async function resolveAnomaly() {
  try {
    await fetch('/api/v1/anomalies/resolve', { method: 'POST' });
    fetchLiveMetrics();
  } catch (err) {
    console.error('Error al resolver:', err);
  }
}

// Ciclo de vida inicial
window.addEventListener('DOMContentLoaded', () => {
  initChart();
  fetchLiveMetrics();
  setInterval(fetchLiveMetrics, 2500);
});
