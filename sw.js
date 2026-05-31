const CACHE_NAME = "vokabeltrainer-v2";
const ASSETS_TO_CACHE = [
  "./",
  "./index.html",
  "./book.png"
];

// 1. Installieren
self.addEventListener("install", (event) => {
  // KEIN skipWaiting() mehr – verhindert unerwartetes Neuladen
  // während die App gerade offen ist
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE);
    })
  );
});

// 2. Aktivieren
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keyList) =>
      Promise.all(
        keyList.map((key) => {
          if (key !== CACHE_NAME) return caches.delete(key);
        })
      )
    ).then(() => self.clients.claim())
  );
});

// 3. Fetch: Cache First für App-Shell, Network First für Daten
self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);

  // Firebase/Firestore: komplett ignorieren (immer live)
  if (url.hostname.includes("googleapis") || url.hostname.includes("firestore")) {
    return;
  }

  // Externe Ressourcen (CDN, andere Domains): nur cachen, nie blockieren
  if (url.origin !== self.location.origin) {
    event.respondWith(
      caches.match(event.request).then((cached) => {
        return cached || fetch(event.request).then((response) => {
          return caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, response.clone());
            return response;
          });
        });
      })
    );
    return;
  }

  // Eigene App-Dateien (index.html, book.png, etc.): Cache First
  // → App startet sofort aus dem Cache, kein Warten aufs Netzwerk
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        // Cache-Treffer: sofort zurückgeben UND im Hintergrund aktualisieren
        // (Stale-While-Revalidate-Muster)
        event.waitUntil(
          fetch(event.request).then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              return caches.open(CACHE_NAME).then((cache) => {
                cache.put(event.request, networkResponse.clone());
              });
            }
          }).catch(() => { /* offline – kein Problem */ })
        );
        return cachedResponse; // ← sofort aus Cache, kein Warten
      }

      // Noch nicht im Cache: normal laden und dabei cachen
      return fetch(event.request).then((response) => {
        return caches.open(CACHE_NAME).then((cache) => {
          cache.put(event.request, response.clone());
          return response;
        });
      });
    })
  );
});
