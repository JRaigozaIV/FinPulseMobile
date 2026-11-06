# FinPulse Mobile: Monitoreo de KPIs Transaccionales y Alertas de Anomalías en Tiempo Real

[![Curso](https://img.shields.io/badge/Curso-Dise%C3%B1o%20de%20Aplicaciones%20M%C3%B3viles-0084F4?style=for-the-badge&logo=android)](https://github.com)
[![IDE](https://img.shields.io/badge/IDE-Android%20Studio%20%2F%20Kotlin%20Compose-3DDC84?style=for-the-badge&logo=android-studio)](https://github.com)
[![Cloud](https://img.shields.io/badge/Cloud-Firebase%20%2F%20FastAPI%20REST-FF9900?style=for-the-badge&logo=google-cloud)](https://github.com)
[![Autor](https://img.shields.io/badge/Autor-Juan%20Jos%C3%A9%20Raigoza%20Calder%C3%B3n-00E676?style=for-the-badge)](https://github.com)

Solución tecnológica móvil (Android Studio / Kotlin Jetpack Compose y PWA) conectada a la nube para el monitoreo en tiempo real de KPIs transaccionales bancarios (PSE, QR Bancolombia, Tarjetas Débito y Transferencias), detección estadística de anomalías (Rolling Z-Score) y despacho inmediato de notificaciones push móviles.

---

## 📋 1. Identificación y Metodología del Proyecto

- **Título Formal del Proyecto Móvil**: FinPulse Mobile: Monitoreo de KPIs transaccionales y alertas de anomalías en tiempo real
- **Integrante**: Juan José Raigoza Calderón (Ingeniería Informática / Analítica de Datos en Banca)
- **Metodología de Desarrollo Móvil**: **Mobile Agile / Scrum (Sprints + Prototipado UX/UI)**, estructurada en iteraciones cortas para diseño centrado en el analista bancario, desarrollo modular cliente-servidor en Android Studio y validación de rendimiento en red móvil.

---

## 📌 2. Planteamiento del Problema y Pregunta Problema

### Contexto y Diagnóstico del Problema Móvil
En la operación analítica y transaccional bancaria, los analistas de datos, incident managers y líderes de negocio experimentan altas tasas de abandono al intentar acceder a los tableros de monitoreo desde navegadores móviles, debido a interfaces web no optimizadas, falta de notificaciones push en tiempo real y tiempos de carga superiores a **8 segundos** en redes 4G/5G. Como consecuencia, las caídas súbitas en la tasa de aprobación o los picos de rechazos en canales críticos (pagos QR, PSE y tarjetas) tardan entre 15 y 30 minutos en detectarse, elevando el tiempo medio de respuesta (MTTR) ante incidentes operacionales.

### Formulación de la Pregunta Problema
> *¿De qué manera la implementación de una App Móvil desarrollada en Android Studio y conectada a servicios en la nube (APIs REST y Firebase Cloud Messaging) permite resolver los altos tiempos de latencia (>8 segundos) y la ausencia de alertas proactivas en el monitoreo de KPIs transaccionales bancarios en tiempo real?*

---

## 🎯 3. Objetivos SMART del Proyecto

### Objetivo General (SMART)
> Desarrollar e implementar una aplicación móvil con arquitectura en la nube (API REST y Firebase) que reduzca el tiempo de carga de indicadores transaccionales bancarios de más de 8 segundos a **menos de 1 segundo** y emita notificaciones push en tiempo real ante anomalías estadísticas (Z-Score), completando el prototipo funcional y las pruebas de rendimiento en un plazo de **8 semanas**.

### Objetivos Específicos (Estructura Metodológica Móvil de 3 Pasos)
1. **Paso 1 — Investigación de Usuario y Prototipado UX/UI**:
   Diseñar los wireframes de alta fidelidad y el prototipo interactivo de la interfaz móvil enfocados en la visualización ejecutiva de KPIs transaccionales y alertas críticas, validando heurísticas de usabilidad de Nielsen en la **semana 2**.
2. **Paso 2 — Desarrollo Frontend Móvil e Integración API/Backend**:
   Programar los módulos del cliente móvil en **Android Studio (Kotlin / Jetpack Compose)** e integrar el consumo de APIs REST en la nube (FastAPI / Firebase Base de Datos) junto con el motor estadístico de detección de anomalías y notificaciones push en tiempo real en la **semana 5**.
3. **Paso 3 — Pruebas en Dispositivos Reales y Medición de Rendimiento**:
   Realizar pruebas de carga, latencia en redes 4G/5G (<1 segundo), consumo de batería y evaluación de usabilidad (escala SUS > 85 puntos) bajo escenarios simulados de caída en canales PSE y QR en emuladores y dispositivos Android en la **semana 7**.

---

## ☁️ 4. Arquitectura, Simulador de Costos y Estrategia de Seguridad

### Diagrama de Arquitectura en la Nube

```
[ App Móvil Android (Kotlin Compose) / PWA ]
           │
           │  HTTPS TLS 1.3 + JWT Auth (<50 ms)
           ▼
[ API Gateway / Cloud Edge ]
           │
           ├──> [ Cache Edge L1: In-Memory / Firebase ] (Pre-agregaciones por minuto)
           │
           ▼
[ Microservicio Analítico: Python FastAPI (Cloud Run / Serverless) ]
           │
           ├──> [ Motor de Detección Estadística de Anomalías ] (Z-Score + Umbrales)
           │            │
           │            ▼ (Si Z-Score < -2.2 o Aprobación < 90%)
           │    [ Firebase Cloud Messaging (FCM) / Android NotificationManager ]
           │            │
           │            └──> [ Notificación Push Inmediata al Dispositivo ] 📲
           ▼
[ Base de Datos en Tiempo Real: Google Cloud Firebase / Firestore ]
```

### Simulador de Costos de Infraestructura, Servicios Móviles y Cuentas de Tienda

| Concepto / Servicio Móvil | Proveedor | Frecuencia / Estimación | Costo USD | Subtotal USD |
| :--- | :--- | :--- | :---: | :---: |
| **Google Play Console (Cuenta Desarrollador)** | Google | Pago Único de por vida | $25.00 | $25.00 |
| **Apple Developer Program** | Apple | Suscripción Anual | $99.00 | $99.00 |
| **Firebase Base de Datos y Cloud Messaging (FCM)** | Google Cloud | Estimado Mensual (Pago por uso) | $15.00 | $15.00 |
| **Cloud Run / Hosting Microservicio API Analítica** | Google Cloud / AWS | Estimado Mensual (Serverless) | $20.00 | $20.00 |
| **Total Estimado del Proyecto** | — | **Presupuesto Asignado: $300.00 USD** | — | **$159.00 USD (47% libre)** |

### Estrategia de Seguridad en el Dispositivo (Storage Seguro, HTTPS, JWT)
- **Transporte Cifrado (HTTPS / TLS 1.3)**: Toda la comunicación entre la aplicación móvil y los endpoints de la API en la nube se realiza sobre canales cifrados HTTPS con TLS 1.3 y validación de certificados (SSL Pinning).
- **Autenticación y Autorización con JWT (JSON Web Tokens)**: El acceso a las métricas transaccionales utiliza tokens JWT firmados digitalmente (RS256) con expiración corta (*Access Token* de 15 minutos) y rotación mediante *Refresh Tokens*.
- **Almacenamiento Seguro en el Dispositivo (Secure Storage)**: Los tokens de sesión y credenciales se almacenan cifrados mediante **Android Keystore (AES-256)** y nunca se persisten datos personales (PII) ni números de cuenta en la caché local del teléfono.

---

## 📂 5. Estructura del Repositorio

```text
├── android-app/                  # Proyecto Nativo de Android Studio (Kotlin + Jetpack Compose)
│   └── app/src/main/
│       ├── AndroidManifest.xml   # Permisos de INTERNET y POST_NOTIFICATIONS
│       └── java/com/bancolombia/finpulse/
│           └── MainActivity.kt   # Dashboard móvil en vivo, gráfico Canvas y Push Nativo
├── backend/                      # Microservicio en la Nube (Python FastAPI)
│   ├── main.py                   # API REST FastAPI y orquestación en la nube
│   ├── anomaly_detector.py       # Motor analítico (Z-Score y reglas bancarias)
│   ├── transaction_generator.py  # Simulador de flujo transaccional en tiempo real
│   └── requirements.txt          # Dependencias de Python
├── frontend/                     # Versión Web Móvil / PWA complementaria
│   ├── index.html                # Interfaz móvil responsiva (Dashboard KPIs)
│   ├── app.js                    # Consumo de API, graficación y notificaciones push
│   ├── manifest.json             # Manifiesto PWA
│   └── sw.js                     # Service Worker
├── docs/
│   ├── presentation.pptx         # Diapositivas ejecutivas y técnicas (Sustentación)
│   ├── presentation.pdf          # Versión PDF de la presentación
│   └── arquitectura_nube.png     # Diagrama de arquitectura
└── README.md                     # Documentación técnica completa
```

---

## 🚀 6. Guía Paso a Paso en Android Studio

1. En la pantalla de bienvenida de **Android Studio**, haz clic en **`+ New Project`**.
2. Selecciona **`Empty Activity`** (plantilla con Jetpack Compose) y haz clic en **`Next`**.
3. Configura los datos del proyecto:
   - **Name**: `FinPulse Mobile`
   - **Package name**: `com.bancolombia.finpulse`
   - **Minimum SDK**: `API 26` (o el que venga por defecto)
   - Haz clic en **`Finish`** y espera a que termine de sincronizar Gradle.
4. Abre el archivo `app/src/main/java/com/bancolombia/finpulse/MainActivity.kt` y reemplaza todo su contenido por el código de [`android-app/app/src/main/java/com/bancolombia/finpulse/MainActivity.kt`](./android-app/app/src/main/java/com/bancolombia/finpulse/MainActivity.kt).
5. Abre `app/src/main/AndroidManifest.xml` y agrega los dos permisos justo arriba de `<application`:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
   ```
6. Haz clic en el botón verde **`Run 'app' ▶️`** arriba en Android Studio para lanzar el emulador Android y probar el botón **`🚨 Caída PSE`** (verás caer la gráfica en vivo y saltar la notificación push en la barra superior de Android).

---

**Autor**: Juan José Raigoza Calderón  | Jorge Mario Vélez Osorio
**Curso**: Diseño de Aplicaciones Móviles — Taller ABP Entrega 1 (20%)
