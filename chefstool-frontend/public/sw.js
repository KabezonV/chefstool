const CACHE = 'chefstool-v1'
const SHELL = ['/', '/index.html', '/manifest.webmanifest']

self.addEventListener('install', (evento) => {
  evento.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(SHELL))
      .then(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (evento) => {
  evento.waitUntil(
    caches
      .keys()
      .then((claves) =>
        Promise.all(claves.filter((clave) => clave !== CACHE).map((clave) => caches.delete(clave))),
      )
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (evento) => {
  const peticion = evento.request
  const url = new URL(peticion.url)

  // La API siempre va a red: nunca se sirven datos financieros cacheados.
  if (peticion.method !== 'GET' || url.pathname.startsWith('/api/')) {
    return
  }

  evento.respondWith(
    caches.match(peticion).then((cacheada) => {
      if (cacheada) return cacheada
      return fetch(peticion)
        .then((respuesta) => {
          const copia = respuesta.clone()
          caches.open(CACHE).then((cache) => cache.put(peticion, copia))
          return respuesta
        })
        .catch(() => caches.match('/index.html'))
    }),
  )
})
