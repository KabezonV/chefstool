// Vendors
import { createContext, useContext } from 'react'

export const TemaContext = createContext(null)

export function useTema() {
  const contexto = useContext(TemaContext)
  if (!contexto) {
    throw new Error('useTema debe usarse dentro de TemaProvider')
  }
  return contexto
}
