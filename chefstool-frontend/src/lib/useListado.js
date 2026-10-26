// Vendors
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

// Utils
import { descargarCsv, fechaArchivo } from '@/lib/csv'
import { descargarXlsx } from '@/lib/xlsx'

// Enumerations
import { PAGINA_INICIAL, TAMANO_PAGINA } from '@/constants/paginacion'

// Handlers
import { api, conParametros, listarTodo, normalizarPagina, paginaVacia } from '@/lib/api'

/**
 * Estado de un listado paginado guardado en la URL (pagina, tamano, orden, direccion y q),
 * de forma que al volver de un detalle se recupera la misma vista.
 */
export function useListado(endpoint, filtros = {}) {
  const [parametros, setParametros] = useSearchParams()
  const pagina = Math.max(
    Number(parametros.get('pagina') ?? PAGINA_INICIAL) || PAGINA_INICIAL,
    PAGINA_INICIAL,
  )
  const tamano = Number(parametros.get('tamano') ?? TAMANO_PAGINA) || TAMANO_PAGINA
  const orden = parametros.get('orden') ?? ''
  const direccion = parametros.get('direccion') ?? 'asc'
  const q = parametros.get('q') ?? ''

  const [datos, setDatos] = useState(() => paginaVacia())
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)
  const [exportando, setExportando] = useState(false)

  const filtrosExtra = JSON.stringify(filtros)

  const consulta = useMemo(
    () => ({ ...JSON.parse(filtrosExtra), q, orden, direccion: orden ? direccion : '' }),
    [filtrosExtra, q, orden, direccion],
  )

  const cargar = useCallback(
    () =>
      api
        .get(conParametros(endpoint, { pagina, tamano, ...consulta }))
        .then((respuesta) => normalizarPagina(respuesta, tamano)),
    [endpoint, pagina, tamano, consulta],
  )

  useEffect(() => {
    let activo = true

    cargar()
      .then((resultado) => {
        if (!activo) return
        setDatos(resultado)
        setError(null)
        setCargando(false)
      })
      .catch((err) => {
        if (!activo) return
        setError(err.message)
        setCargando(false)
      })

    return () => {
      activo = false
    }
  }, [cargar, recarga])

  const actualizar = useCallback(
    (cambios) => {
      setCargando(true)
      setParametros(
        (previos) => {
          const siguientes = new URLSearchParams(previos)
          for (const [clave, valor] of Object.entries(cambios)) {
            if (valor === '' || valor === null || valor === undefined) {
              siguientes.delete(clave)
            } else {
              siguientes.set(clave, String(valor))
            }
          }
          return siguientes
        },
        { replace: true },
      )
    },
    [setParametros],
  )

  const ordenarPor = useCallback(
    (campo) =>
      actualizar({
        orden: campo,
        direccion: campo === orden && direccion === 'asc' ? 'desc' : 'asc',
        pagina: 0,
      }),
    [actualizar, orden, direccion],
  )

  const refrescar = useCallback(() => {
    setCargando(true)
    setRecarga((valor) => valor + 1)
  }, [])

  const exportar = useCallback(
    async (columnas, formato = 'csv') => {
      setExportando(true)
      try {
        const filas = await listarTodo(conParametros(endpoint, consulta), 100)
        const nombre = `${endpoint.replace('/', '')}-${fechaArchivo()}`
        if (formato === 'xlsx') {
          descargarXlsx(nombre, { hoja: 'Datos', columnas, filas })
        } else {
          descargarCsv(nombre, columnas, filas)
        }
      } catch (err) {
        setError(err.message)
      } finally {
        setExportando(false)
      }
    },
    [endpoint, consulta],
  )

  return {
    items: datos.contenido,
    datos,
    pagina,
    tamano,
    orden,
    direccion,
    q,
    cargando,
    error,
    setError,
    exportando,
    actualizar,
    ordenarPor,
    refrescar,
    exportar,
  }
}

/** Debounce sencillo para la caja de busqueda. */
export function useDebounce(callback, espera = 300) {
  const temporizador = useRef(null)

  useEffect(() => () => clearTimeout(temporizador.current), [])

  return useCallback(
    (valor) => {
      clearTimeout(temporizador.current)
      temporizador.current = setTimeout(() => callback(valor), espera)
    },
    [callback, espera],
  )
}
