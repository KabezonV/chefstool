// Enumerations
import { CLAVE_REFRESH, CLAVE_SESION, CLAVE_TOKEN } from '@/constants/claves'
import { TAMANO_PAGINA, TAMANO_PETICION_MAXIMA } from '@/constants/paginacion'

const BASE_URL = import.meta.env.VITE_API_URL ?? '/api'

export function tokenGuardado() {
  return localStorage.getItem(CLAVE_TOKEN)
}

export function guardarToken(token) {
  localStorage.setItem(CLAVE_TOKEN, token)
}

export function borrarToken() {
  localStorage.removeItem(CLAVE_TOKEN)
}

async function manejar(respuesta, path) {
  if (respuesta.status === 401) {
    // Al entrar, un 401 son credenciales incorrectas, no una sesion caducada.
    if (path.startsWith('/auth/login') || path.startsWith('/auth/registro')) {
      const cuerpo = await respuesta.json().catch(() => null)
      throw new Error(cuerpo?.error ?? 'Correo o contraseña incorrectos')
    }
    borrarToken()
    localStorage.removeItem(CLAVE_REFRESH)
    localStorage.removeItem(CLAVE_SESION)
    if (window.location.pathname !== '/auth') {
      window.location.href = '/auth'
    }
    throw new Error('Tu sesión ha caducado. Vuelve a entrar.')
  }

  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => null)
    throw new Error(cuerpo?.error ?? `Error ${respuesta.status} en ${path}`)
  }

  if (respuesta.status === 204) {
    return null
  }

  return respuesta.json()
}

async function request(path, options = {}) {
  const headers = { ...options.headers }
  const token = tokenGuardado()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  let respuesta = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (
    respuesta.status === 401
    && !path.startsWith('/auth/login')
    && !path.startsWith('/auth/registro')
    && !path.startsWith('/auth/refrescar')
  ) {
    const refreshToken = localStorage.getItem(CLAVE_REFRESH)
    if (refreshToken) {
      try {
        const refrescada = await fetch(`${BASE_URL}/auth/refrescar`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
        if (refrescada.ok) {
          const datos = await refrescada.json()
          guardarToken(datos.token)
          localStorage.setItem(CLAVE_REFRESH, datos.refreshToken)
          localStorage.setItem(CLAVE_SESION, JSON.stringify(datos))
          headers.Authorization = `Bearer ${datos.token}`
          respuesta = await fetch(`${BASE_URL}${path}`, { ...options, headers })
        }
      } catch {
        // El flujo normal de manejar() cerrará la sesión si el retry falla.
      }
    }
  }

  return manejar(respuesta, path)
}

async function requestBlob(path) {
  const headers = {}
  const token = tokenGuardado()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const respuesta = await fetch(`${BASE_URL}${path}`, { headers })
  if (!respuesta.ok) {
    // El backend explica el motivo (periodo vacio, demasiados albaranes...)
    const cuerpo = await respuesta.json().catch(() => null)
    throw new Error(cuerpo?.error ?? `Error ${respuesta.status} al descargar ${path}`)
  }
  return respuesta.blob()
}

export const api = {
  descargar: (path) => requestBlob(path),
  get: (path) => request(path),
  post: (path, body) =>
    request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) =>
    request(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path, body) =>
    request(path, {
      method: 'DELETE',
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    }),
  subir: (path, formData) => request(path, { method: 'POST', body: formData }),
}

/** Envoltorio de paginacion que devuelve el backend. */
export function paginaVacia(tamano = TAMANO_PAGINA) {
  return { contenido: [], pagina: 0, tamano, totalElementos: 0, totalPaginas: 0 }
}

export function normalizarPagina(datos, tamano = TAMANO_PAGINA) {
  return Array.isArray(datos) ? { ...paginaVacia(tamano), contenido: datos, totalElementos: datos.length, totalPaginas: datos.length > 0 ? 1 : 0 } : datos
}

function conParametros(path, parametros) {
  const query = Object.entries(parametros)
    .filter(([, valor]) => valor !== undefined && valor !== null && valor !== '')
    .map(([clave, valor]) => `${clave}=${encodeURIComponent(valor)}`)
    .join('&')
  if (query === '') return path
  return path.includes('?') ? `${path}&${query}` : `${path}?${query}`
}

/** Lista completa, pagina a pagina, para alimentar desplegables. */
export async function listarTodo(path, tamano = TAMANO_PETICION_MAXIMA) {
  const primera = normalizarPagina(
    await api.get(conParametros(path, { pagina: 0, tamano })),
    tamano,
  )
  const items = [...primera.contenido]

  for (let pagina = 1; pagina < primera.totalPaginas; pagina += 1) {
    const siguiente = normalizarPagina(
      await api.get(conParametros(path, { pagina, tamano })),
      tamano,
    )
    items.push(...siguiente.contenido)
  }

  return items
}

export { conParametros }
