import { useCallback, useEffect, useMemo, useState } from 'react'
import { Pencil, Plus, Trash2, X } from 'lucide-react'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select, Textarea } from '@/components/ui/input'

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
  const { endpoint, titulo, descripcion, columnas, campos, singular, vacio } = recurso

  const [items, setItems] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [formAbierto, setFormAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [valores, setValores] = useState(() => valoresIniciales(campos))
  const [guardando, setGuardando] = useState(false)
  const [fuentes, setFuentes] = useState({})

  const camposSelect = useMemo(
    () => campos.filter((campo) => campo.type === 'select'),
    [campos],
  )

  const recargar = useCallback(async () => {
    setCargando(true)
    try {
      setItems(await api.get(endpoint))
      setError(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [endpoint])

  useEffect(() => {
    let activo = true

    api
      .get(endpoint)
      .then((datos) => {
        if (!activo) return
        setItems(datos)
        setError(null)
      })
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))

    return () => {
      activo = false
    }
  }, [endpoint])

  useEffect(() => {
    let activo = true

    async function cargarFuentes() {
      const entradas = await Promise.all(
        camposSelect.map(async (campo) => {
          try {
            const datos = await api.get(campo.fuente.endpoint)
            return [
              campo.name,
              datos.map((fila) => ({
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
      await recargar()
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
      await recargar()
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
        <CardHeader>
          <CardTitle>{titulo}</CardTitle>
          <Badge>{cargando ? 'Cargando…' : `${items.length} registros`}</Badge>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {!cargando && items.length === 0 ? (
            <p className="px-5 pb-5 text-sm text-muted">{vacio}</p>
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
                    <tr key={item.id} className="border-b border-border/60 last:border-0">
                      {columnas.map((columna) => (
                        <td key={columna.key} className="px-5 py-3">
                          {columna.format
                            ? columna.format(item[columna.key], item)
                            : (item[columna.key] ?? '—')}
                        </td>
                      ))}
                      <td className="px-5 py-3">
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
        </CardContent>
      </Card>
    </div>
  )
}
