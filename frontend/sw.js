// Service Worker para FinPulse Mobile PWA
const CACHE_NAME = 'finpulse-cache-v1';
const ASSETS_TO_CACHE = [
  '/',
  '/static/manifest.json',
  '/static/index.html',
  '/static/app.js'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE).catch(() => {});
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Estrategia Network-First para métricas de tiempo real con fallback a caché
  if (event.request.url.includes('/api/')) {
    event.respondWith(
      fetch(event.request).catch(() => caches.match(event.request))
    );
  } else {
    event.respondWith(
      caches.match(event.request).then((cachedResponse) => {
        return cachedResponse || fetch(event.request);
      })
    );
  }
});

// Listener para notificaciones push en segundo plano
self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : { title: 'FinPulse Alerta', body: 'Anomalía detectada' };
  const options = {
    body: data.body,
    icon: 'https://cdn-icons-png.flaticon.com/512/2830/2830284.png',
    badge: 'https://cdn-icons-png.flaticon.com/512/2830/2830284.png',
    vibrate: [200, 100, 200],
    data: data.data || {}
  };
  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});
