const CACHE_NAME = 'fitu-v4';
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/logo.svg',
  '/manifest.json'
];

// Domains that should always be cached heavily (Cache First)
const CACHE_FIRST_DOMAINS = [
  'cdn.jsdelivr.net',
  'storage.googleapis.com',
  'tfhub.dev',
  'esm.sh',
  'cdn.tailwindcss.com',
  'fonts.googleapis.com',
  'fonts.gstatic.com'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;

  const url = new URL(event.request.url);

  // 1. Navigation Requests (SPA Support)
  // CRITICAL: If network fails, ALWAYS return index.html from cache
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request).catch(() => {
        return caches.match('/index.html').then((response) => {
             // If index.html is missing from cache for some reason, we are in trouble,
             // but returning it here fixes the "You're offline" browser screen.
             return response;
        });
      })
    );
    return;
  }

  // 2. Heavy Assets & Libraries (Cache First Strategy)
  const isHeavyAsset = CACHE_FIRST_DOMAINS.some(domain => url.hostname.includes(domain)) || 
                       url.href.includes('tfjs-models') ||
                       url.pathname.endsWith('.json') || 
                       url.pathname.endsWith('.bin') ||
                       url.pathname.endsWith('.svg') ||
                       url.pathname.endsWith('.png') ||
                       url.pathname.endsWith('.jpg') ||
                       url.pathname.endsWith('.woff2');
  
  if (isHeavyAsset) {
    event.respondWith(
      caches.match(event.request).then((cached) => {
        if (cached) return cached;
        return fetch(event.request).then((response) => {
          if (response.ok) {
             const clone = response.clone();
             caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
          }
          return response;
        }).catch(() => {
           // If heavy asset fails (offline) and not in cache, nothing we can do.
           // However, avoiding the crash is priority.
           return new Response('', { status: 408, statusText: 'Offline' });
        });
      })
    );
    return;
  }

  // 3. Stale-While-Revalidate for everything else
  event.respondWith(
    caches.match(event.request).then((cached) => {
      const networkFetch = fetch(event.request).then((response) => {
        if (response.ok) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
        }
        return response;
      }).catch(() => {
         // Network failed
      });
      return cached || networkFetch;
    })
  );
});