// Vendors
import { useCallback, useEffect, useMemo, useState } from 'react'

// Enumerations
import { CLAVE_TEMA } from '@/constants/claves'

// Hooks
import { TemaContext } from '@/lib/tema-context'

const CONSULTA_OSCURO = '(prefers-color-scheme: dark)'

function guardarTema(tema) {
  try {
    localStorage.setItem(CLAVE_TEMA, tema)
  } catch {
    /* navegación privada sin almacenamiento */
  }
}

/** Tema guardado por el usuario o, si no hay, el del sistema. */
function leerTema() {
  try {
    const guardado = localStorage.getItem(CLAVE_TEMA)
    if (guardado === 'claro' || guardado === 'oscuro') return guardado
  } catch {
    /* navegación privada sin almacenamiento */
  }
  return window.matchMedia?.(CONSULTA_OSCURO).matches ? 'oscuro' : 'claro'
}

/** Pinta el tema en el documento (el script de `index.html` hace lo mismo antes de pintar). */
function aplicarTema(tema) {
  const oscuro = tema === 'oscuro'
  document.documentElement.classList.toggle('dark', oscuro)
  document.documentElement.style.colorScheme = oscuro ? 'dark' : 'light'
}

export function TemaProvider({ children }) {
  const [tema, setTema] = useState(leerTema)

  useEffect(() => {
    aplicarTema(tema)
    guardarTema(tema)
  }, [tema])

  const alternar = useCallback(() => {
    setTema((actual) => (actual === 'oscuro' ? 'claro' : 'oscuro'))
  }, [])

  const valor = useMemo(() => ({ tema, setTema, alternar }), [tema, alternar])

  return <TemaContext.Provider value={valor}>{children}</TemaContext.Provider>
}
