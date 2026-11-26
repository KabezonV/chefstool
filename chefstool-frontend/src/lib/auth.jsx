// Vendors
import { useCallback, useMemo, useState } from 'react'

// Enumerations
import { CLAVE_REFRESH, CLAVE_SESION } from '@/constants/claves'

// Handlers
import { api, borrarToken, guardarToken } from '@/lib/api'

// Hooks
import { AuthContext } from '@/lib/auth-context'

function leerSesion() {
  try {
    const guardada = localStorage.getItem(CLAVE_SESION)
    return guardada ? JSON.parse(guardada) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(leerSesion)

  const iniciar = useCallback((datos) => {
    guardarToken(datos.token)
    if (datos.refreshToken) {
      localStorage.setItem(CLAVE_REFRESH, datos.refreshToken)
    }
    localStorage.setItem(CLAVE_SESION, JSON.stringify(datos))
    setSesion(datos)
  }, [])

  const entrar = useCallback(
    async (email, password) => {
      const datos = await api.post('/auth/login', { email, password })
      iniciar(datos)
      return datos
    },
    [iniciar],
  )

  const registrar = useCallback(
    async (datos) => {
      const creada = await api.post('/auth/registro', datos)
      iniciar(creada)
      return creada
    },
    [iniciar],
  )

  const salir = useCallback(() => {
    const refresh = localStorage.getItem(CLAVE_REFRESH)
    if (refresh) {
      api.post('/auth/logout', { refreshToken: refresh }).catch(() => null)
    }
    borrarToken()
    localStorage.removeItem(CLAVE_REFRESH)
    localStorage.removeItem(CLAVE_SESION)
    setSesion(null)
  }, [])

  const valor = useMemo(
    () => ({ sesion, entrar, registrar, salir }),
    [sesion, entrar, registrar, salir],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
