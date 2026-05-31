const CACHE_NAME = "vokabeltrainer-v1";
const ASSETS_TO_CACHE = [
  "./",
  "./index.html",
  "./book.png"
];

// 1. Installieren
self.addEventListener("install", (event) => {
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

// 3. Fokus auf bestehende App-Instanz, statt neu laden
async function focusOrOpenApp() {
  const allClients = await self.clients.matchAll({
    type: "window",
    includeUncontrolled: true
  });

  for (const client of allClients) {
    if (client.url.includes("index.html") || client.url.endsWith("/")) {
      return client.focus();
    }
  }

  return self.clients.openWindow("./index.html");
}

// Wenn ein Notification-Button geklickt wird
self.addEventListener("notificationclick", (event) => {
  event.waitUntil(focusOrOpenApp());
});

// Wenn die App per Message-Event angesprochen wird (z.B. vom Tile)
self.addEventListener("message", (event) => {
  if (event.data === "focus-app") {
    event.waitUntil(focusOrOpenApp());
  }
});

// 4. Fetch: Cache First für App-Shell, Network First für Daten
self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);

  // Firebase/Firestore: komplett ignorieren (immer live)
  if (url.hostname.includes("googleapis") || url.hostname.includes("firestore")) {
    return;
  }

  // Externe Ressourcen (CDN, andere Domains): Cache First
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

  // Eigene App-Dateien: Cache First + Stale-While-Revalidate
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        event.waitUntil(
          fetch(event.request).then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              return caches.open(CACHE_NAME).then((cache) => {
                cache.put(event.request, networkResponse.clone());
              });
            }
          }).catch(() => {})
        );
        return cachedResponse;
      }

      return fetch(event.request).then((response) => {
        return caches.open(CACHE_NAME).then((cache) => {
          cache.put(event.request, response.clone());
          return response;
        });
      });
    })
  );
});
