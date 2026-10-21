import { useCallback, useMemo, useState } from 'react'
import { api, borrarToken, guardarToken } from '@/lib/api'
import { AuthContext } from '@/lib/auth-context'

const CLAVE_SESION = 'chefstool.sesion'

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
    borrarToken()
    localStorage.removeItem(CLAVE_SESION)
    setSesion(null)
  }, [])

  const valor = useMemo(
    () => ({ sesion, entrar, registrar, salir }),
    [sesion, entrar, registrar, salir],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
