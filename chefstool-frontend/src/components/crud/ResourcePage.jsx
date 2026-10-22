import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Pencil, Plus, Trash2, X } from 'lucide-react'
import { api, conParametros, listarTodo, normalizarPagina, paginaVacia } from '@/lib/api'
import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select, Textarea } from '@/components/ui/input'
import Paginador, { Buscador } from '@/components/ui/paginador'

function valoresIniciales(campos) {
  return Object.fromEntries(campos.map((campo) => [campo.name, '']))
}

function construirPayload(campos, valores) {
  const payload = {}
  for (const campo of campos) {
    const bruto = (valores[campo.name] ?? '').toString().trim()
    if (bruto === '') {
      payload[campo.name] = null
    } else if (campo.type === 'number') {
      payload[campo.name] = Number(bruto)
    } else {
      payload[campo.name] = bruto
    }
  }
  return payload
}

function Campo({ campo, valor, onChange, opciones }) {
  const id = `campo-${campo.name}`

  if (campo.type === 'select') {
    return (
      <Field label={campo.label} htmlFor={id}>
        <Select
          id={id}
          value={valor ?? ''}
          onChange={(event) => onChange(campo.name, event.target.value)}
        >
          <option value="">{campo.vacio ?? 'Sin asignar'}</option>
          {opciones.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {opcion.etiqueta}
            </option>
          ))}
        </Select>
      </Field>
    )
  }

  if (campo.type === 'textarea') {
    return (
      <Field label={campo.label} htmlFor={id}>
        <Textarea
          id={id}
          value={valor ?? ''}
          onChange={(event) => onChange(campo.name, event.target.value)}
        />
      </Field>
    )
  }

  return (
    <Field label={campo.label} htmlFor={id}>
      <Input
        id={id}
        type={campo.type ?? 'text'}
        required={campo.required}
        placeholder={campo.placeholder}
        value={valor ?? ''}
        onChange={(event) => onChange(campo.name, event.target.value)}
      />
    </Field>
  )
}

export default function ResourcePage({ recurso }) {
  const { endpoint, titulo, descripcion, columnas, campos, singular, vacio, detalle } = recurso
  const navigate = useNavigate()

  const [datos, setDatos] = useState(() => paginaVacia())
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(20)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [formAbierto, setFormAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [valores, setValores] = useState(() => valoresIniciales(campos))
  const [guardando, setGuardando] = useState(false)
  const [fuentes, setFuentes] = useState({})
  const [recarga, setRecarga] = useState(0)
  const temporizadorBusqueda = useRef(null)

  const items = datos.contenido

  const camposSelect = useMemo(
    () => campos.filter((campo) => campo.type === 'select'),
    [campos],
  )

  const cargar = useCallback(
    () =>
      api
        .get(conParametros(endpoint, { pagina, tamano, q: busquedaAplicada }))
        .then((datos) => normalizarPagina(datos, tamano)),
    [endpoint, pagina, tamano, busquedaAplicada],
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

  useEffect(() => () => clearTimeout(temporizadorBusqueda.current), [])

  function cambiarBusqueda(valor) {
    setBusqueda(valor)
    setPagina(0)
    setCargando(true)
    clearTimeout(temporizadorBusqueda.current)
    temporizadorBusqueda.current = setTimeout(() => setBusquedaAplicada(valor.trim()), 300)
  }

  function refrescar() {
    setCargando(true)
    setRecarga((valor) => valor + 1)
  }

  useEffect(() => {
    let activo = true

    async function cargarFuentes() {
      const entradas = await Promise.all(
        camposSelect.map(async (campo) => {
          try {
            const filas = await listarTodo(campo.fuente.endpoint)
            return [
              campo.name,
              filas.map((fila) => ({
                valor: fila[campo.fuente.valor],
                etiqueta: fila[campo.fuente.etiqueta],
              })),
            ]
          } catch {
            return [campo.name, []]
          }
        }),
      )
      if (activo) {
        setFuentes(Object.fromEntries(entradas))
      }
    }

    if (camposSelect.length > 0) {
      cargarFuentes()
    }

    return () => {
      activo = false
    }
  }, [camposSelect])

  function abrirNuevo() {
    setEditando(null)
    setValores(valoresIniciales(campos))
    setError(null)
    setFormAbierto(true)
  }

  function abrirEdicion(item) {
    setEditando(item)
    setValores(
      Object.fromEntries(
        campos.map((campo) => [
          campo.name,
          item[campo.name] === null || item[campo.name] === undefined
            ? ''
            : String(item[campo.name]),
        ]),
      ),
    )
    setError(null)
    setFormAbierto(true)
  }

  function cerrarFormulario() {
    setFormAbierto(false)
    setEditando(null)
    setValores(valoresIniciales(campos))
  }

  function cambiar(nombre, valor) {
    setValores((previos) => ({ ...previos, [nombre]: valor }))
  }

  async function guardar(event) {
    event.preventDefault()
    setGuardando(true)
    setError(null)

    try {
      const payload = construirPayload(campos, valores)
      if (editando) {
        await api.put(`${endpoint}/${editando.id}`, payload)
      } else {
        await api.post(endpoint, payload)
      }
      cerrarFormulario()
      if (pagina !== 0) {
        setPagina(0)
        setCargando(true)
      } else {
        refrescar()
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function eliminar(item) {
    const nombre = item[columnas[0].key] ?? singular
    if (!window.confirm(`¿Eliminar "${nombre}"?`)) {
      return
    }
    try {
      await api.delete(`${endpoint}/${item.id}`)
      if (items.length === 1 && pagina > 0) {
        setCargando(true)
        setPagina(pagina - 1)
      } else {
        refrescar()
      }
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">{titulo}</h1>
          {descripcion && <p className="mt-1 text-sm text-muted">{descripcion}</p>}
        </div>
        <Button onClick={abrirNuevo} variant="accent">
          <Plus className="size-4" />
          Nuevo {singular}
        </Button>
      </div>

      {error && (
        <div className="flex items-start justify-between gap-3 rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
          <span>{error}</span>
          <button type="button" onClick={() => setError(null)} aria-label="Descartar aviso">
            <X className="size-4" />
          </button>
        </div>
      )}

      {formAbierto && (
        <Card>
          <CardHeader>
            <CardTitle>{editando ? `Editar ${singular}` : `Nuevo ${singular}`}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={guardar} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                {campos.map((campo) => (
                  <Campo
                    key={campo.name}
                    campo={campo}
                    valor={valores[campo.name]}
                    onChange={cambiar}
                    opciones={fuentes[campo.name] ?? []}
                  />
                ))}
              </div>
              <div className="flex gap-2">
                <Button type="submit" disabled={guardando}>
                  {guardando ? 'Guardando…' : 'Guardar'}
                </Button>
                <Button type="button" variant="outline" onClick={cerrarFormulario}>
                  Cancelar
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="flex-wrap">
          <CardTitle>{titulo}</CardTitle>
          <Buscador
            valor={busqueda}
            onChange={cambiarBusqueda}
            placeholder="Buscar por nombre…"
          />
          <Badge>{cargando ? 'Cargando…' : `${datos.totalElementos} registros`}</Badge>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {!cargando && items.length === 0 ? (
            <p className="px-5 pb-5 text-sm text-muted">
              {busquedaAplicada ? `Sin resultados para «${busquedaAplicada}».` : vacio}
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    {columnas.map((columna) => (
                      <th key={columna.key} className="px-5 py-2 font-medium">
                        {columna.label}
                      </th>
                    ))}
                    <th className="w-24 px-5 py-2" />
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr
                      key={item.id}
                      onClick={detalle ? () => navigate(detalle(item)) : undefined}
                      className={cn(
                        'border-b border-border/60 last:border-0',
                        detalle && 'cursor-pointer hover:bg-black/[0.02]',
                      )}
                    >
                      {columnas.map((columna) => (
                        <td key={columna.key} className="px-5 py-3">
                          {columna.format
                            ? columna.format(item[columna.key], item)
                            : (item[columna.key] ?? '—')}
                        </td>
                      ))}
                      <td className="px-5 py-3" onClick={(event) => event.stopPropagation()}>
                        <div className="flex justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label={`Editar ${singular}`}
                            onClick={() => abrirEdicion(item)}
                          >
                            <Pencil className="size-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label={`Eliminar ${singular}`}
                            onClick={() => eliminar(item)}
                          >
                            <Trash2 className="size-4" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <Paginador
            pagina={pagina}
            tamano={tamano}
            totalElementos={datos.totalElementos}
            totalPaginas={datos.totalPaginas}
            onCambiar={({ pagina: nuevaPagina, tamano: nuevoTamano }) => {
              setCargando(true)
              setPagina(nuevaPagina)
              setTamano(nuevoTamano)
            }}
          />
        </CardContent>
      </Card>
    </div>
  )
}
