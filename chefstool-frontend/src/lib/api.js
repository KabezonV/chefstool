const BASE_URL = import.meta.env.VITE_API_URL ?? '/api'

async function manejar(respuesta, path) {
  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => null)
    throw new Error(cuerpo?.error ?? `Error ${respuesta.status} en ${path}`)
  }

  if (respuesta.status === 204) {
    return null
  }

  return respuesta.json()
}

function request(path, options = {}) {
  const headers = { ...options.headers }
  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  return fetch(`${BASE_URL}${path}`, { ...options, headers }).then((respuesta) =>
    manejar(respuesta, path),
  )
}

export const api = {
  get: (path) => request(path),
  post: (path, body) =>
    request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) =>
    request(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
  subir: (path, formData) => request(path, { method: 'POST', body: formData }),
}
