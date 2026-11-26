// Enumerations
import { CLAVE_REFRESH, CLAVE_SESION } from '@/constants/claves'

// Handlers
import { api, guardarToken } from '@/lib/api'

/**
 * Cambia el local activo de la sesión. El backend devuelve una sesión nueva con el local
 * dentro, así que se guarda igual que al entrar y se recarga para que todo pida los datos
 * del local nuevo.
 */
export async function activarLocal(id) {
  const datos = await api.post(`/locales/${id}/seleccionar`, {})
  guardarToken(datos.token)
  localStorage.setItem(CLAVE_REFRESH, datos.refreshToken)
  localStorage.setItem(CLAVE_SESION, JSON.stringify(datos))
  window.location.reload()
}
