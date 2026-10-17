package com.bancolombia.finpulse

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * FinPulse Mobile - Monitoreo de KPIs Transaccionales y Alertas de Anomalías en Tiempo Real
 * Autor: Juan José Raigoza Calderón
 * Curso: Diseño de Aplicaciones Móviles - Taller ABP Entrega 1 (20%)
 */
class MainActivity : ComponentActivity() {

    companion object {
        const val CHANNEL_ID = "finpulse_high_priority_alerts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        setContent {
            MaterialTheme {
                FinPulseDashboardScreen(
                    onSendPushNotification = { title, message ->
                        sendNativePushNotification(this, title, message)
                    }
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas Críticas Transaccionales",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal push en tiempo real para anomalías en PSE, QR y Tarjetas"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNativePushNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(Random.nextInt(1000, 9999), notification)
    }
}

data class ChannelMetric(
    val name: String,
    val rate: Double,
    val volume: Int,
    val latencyMs: Int
)

@Composable
fun FinPulseDashboardScreen(onSendPushNotification: (String, String) -> Unit) {
    val context = LocalContext.current

    // Permiso de notificaciones en Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Estado reactivo del flujo en la nube
    var anomalyMode by remember { mutableStateOf("NONE") } // NONE, PSE_DROP, QR_SPIKE, LATENCY
    var globalApproval by remember { mutableStateOf(97.6) }
    var tps by remember { mutableStateOf(4.4) }
    var avgLatency by remember { mutableStateOf(165) }
    var edgeLatency by remember { mutableStateOf(28) }
    var activeAlertBanner by remember { mutableStateOf<String?>(null) }
    var historyRates by remember {
        mutableStateOf(listOf(97.2f, 97.5f, 96.9f, 97.8f, 97.4f, 98.0f, 97.3f, 97.6f, 97.9f, 97.6f))
    }

    var channels by remember {
        mutableStateOf(
            listOf(
                ChannelMetric("💳 PSE (Pagos en Línea)", 98.1, 78, 180),
                ChannelMetric("📱 QR Bancolombia", 97.5, 92, 135),
                ChannelMetric("🏧 Tarjeta Débito Visa/MC", 96.9, 64, 205),
                ChannelMetric("🔄 Transferencias Cuentas", 99.1, 51, 110)
            )
        )
    }

    // Actualización periódica en tiempo real (cada 2.5 segundos)
    LaunchedEffect(anomalyMode) {
        while (true) {
            delay(2500)
            edgeLatency = Random.nextInt(19, 39)

            val pseRate = if (anomalyMode == "PSE_DROP") Random.nextDouble(69.5, 76.8) else Random.nextDouble(96.5, 98.8)
            val qrRate = if (anomalyMode == "QR_SPIKE") Random.nextDouble(72.0, 79.5) else Random.nextDouble(96.8, 98.9)
            val cardRate = Random.nextDouble(96.0, 98.4)
            val transRate = Random.nextDouble(98.2, 99.6)

            val pseLat = if (anomalyMode == "PSE_DROP" || anomalyMode == "LATENCY") Random.nextInt(2350, 3100) else Random.nextInt(150, 220)
            val qrLat = if (anomalyMode == "QR_SPIKE" || anomalyMode == "LATENCY") Random.nextInt(1950, 2800) else Random.nextInt(120, 175)

            channels = listOf(
                ChannelMetric("💳 PSE (Pagos en Línea)", pseRate, Random.nextInt(65, 95), pseLat),
                ChannelMetric("📱 QR Bancolombia", qrRate, Random.nextInt(75, 110), qrLat),
                ChannelMetric("🏧 Tarjeta Débito Visa/MC", cardRate, Random.nextInt(50, 80), Random.nextInt(180, 240)),
                ChannelMetric("🔄 Transferencias Cuentas", transRate, Random.nextInt(40, 70), Random.nextInt(95, 140))
            )

            globalApproval = ((pseRate + qrRate + cardRate + transRate) / 4.0 * 10.0).roundToInt() / 10.0
            avgLatency = (pseLat + qrLat + 200 + 115) / 4
            tps = ((channels.sumOf { it.volume } / 60.0) * 10.0).roundToInt() / 10.0

            historyRates = (historyRates.drop(1) + globalApproval.toFloat())
        }
    }

    val isAnomaly = anomalyMode != "NONE" || globalApproval < 90.0
    val bgMain = Color(0xFF070D1E)
    val bgCard = Color(0xFF101B38)
    val accentYellow = Color(0xFFFDDA24)
    val accentCyan = Color(0xFF00E5FF)
    val statusGreen = Color(0xFF00E676)
    val statusRed = Color(0xFFFF3B30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgMain)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("B", color = bgMain, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("FinPulse Mobile", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("Juan José Raigoza • Analítica", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isAnomaly) statusRed.copy(alpha = 0.2f) else statusGreen.copy(alpha = 0.15f))
                    .border(1.dp, if (isAnomaly) statusRed else statusGreen, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isAnomaly) "🔴 Anomalía Activa" else "🟢 En Línea",
                    color = if (isAnomaly) statusRed else statusGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Barra de Telemetría Cloud
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0B132B))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Cloud: Firebase / FastAPI", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text("Latencia Edge: ${edgeLatency} ms", color = accentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("TLS 1.3 + JWT", color = Color(0xFF94A3B8), fontSize = 11.sp)
        }

        // Banner de Notificación Push Activa
        activeAlertBanner?.let { bannerText ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D1515))
                    .border(1.dp, statusRed, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("🚨 NOTIFICACIÓN PUSH EN TIEMPO REAL (FCM)", color = Color(0xFFFF7B72), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(bannerText, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Tarjeta Principal de KPI Global
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bgCard)
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "TASA DE APROBACIÓN GLOBAL (ÚLTIMO MINUTO)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${globalApproval}%",
                        color = if (isAnomaly) statusRed else statusGreen,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isAnomaly) "⚠️ Bajo SLA (95%)" else "✅ Óptimo vs SLA",
                        color = if (isAnomaly) statusRed else statusGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("FLUJO TRANSACCIONAL", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text("$tps TPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("LATENCIA PROMEDIO", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text("$avgLatency ms", color = if (avgLatency > 1000) statusRed else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("Z-SCORE VENTANA", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(if (isAnomaly) "-3.14 σ (Crítico)" else "+0.42 σ (Normal)", color = if (isAnomaly) statusRed else accentCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Gráfico de Línea en Tiempo Real (Canvas Nativo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bgCard)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tendencia en Tiempo Real (%)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Meta SLA: 95.0%", color = accentCyan, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val minVal = 65f
                    val maxVal = 100f

                    // Línea punteada de SLA 95%
                    val slaY = height - ((95f - minVal) / (maxVal - minVal)) * height
                    drawLine(
                        color = statusRed.copy(alpha = 0.5f),
                        start = Offset(0f, slaY),
                        end = Offset(width, slaY),
                        strokeWidth = 3f
                    )

                    // Trazado de la curva en vivo
                    val path = Path()
                    val stepX = width / (historyRates.size - 1).coerceAtLeast(1)
                    historyRates.forEachIndexed { index, rate ->
                        val x = index * stepX
                        val clamped = rate.coerceIn(minVal, maxVal)
                        val y = height - ((clamped - minVal) / (maxVal - minVal)) * height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = if (isAnomaly) statusRed else statusGreen,
                        style = Stroke(width = 6f)
                    )
                }
            }
        }

        // Desglose por Canales de Pago
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bgCard)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Desglose por Canal Bancario", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                channels.forEach { ch ->
                    val chCritical = ch.rate < 88.0
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ch.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${(ch.rate * 10.0).roundToInt() / 10.0}%",
                                color = if (chCritical) statusRed else statusGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (ch.rate / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (chCritical) statusRed else statusGreen,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Text(
                            "Volumen: ${ch.volume} tx/min • Latencia: ${ch.latencyMs} ms",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Panel de Simulación de Anomalías y Pruebas Push
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0E162D))
                .border(1.dp, accentYellow.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "⚡ PANEL DE SIMULACIÓN DE ANOMALÍAS Y PUSH",
                    color = accentYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            anomalyMode = "PSE_DROP"
                            val msg = "Caída crítica en canal PSE: Aprobación cayó a 73.2% (Z-Score: -3.14)"
                            activeAlertBanner = msg
                            onSendPushNotification("🚨 Alerta CRITICAL: FinPulse", msg)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = statusRed)
                    ) {
                        Text("🚨 Caída PSE", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            anomalyMode = "QR_SPIKE"
                            val msg = "Pico de rechazos en QR Bancolombia: Aprobación en 75.4%"
                            activeAlertBanner = msg
                            onSendPushNotification("⚠️ Alerta QR Bancolombia", msg)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                    ) {
                        Text("⚠️ Fallo QR", fontSize = 11.sp, color = Color.White)
                    }
                }

                Button(
                    onClick = {
                        anomalyMode = "NONE"
                        activeAlertBanner = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("✅ Restablecer Flujo Normal", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}