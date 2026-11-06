// Vendors
import { Pencil, Plus, Trash2, Upload, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

// Utils
import { cn } from '@/lib/utils'

// Handlers
import { api, listarTodo } from '@/lib/api'

// Hooks
import { useDebounce, useListado } from '@/lib/useListado'

// Components
import { construirPayload, valoresIniciales } from '@/components/crud/utils/formulario'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select, Textarea } from '@/components/ui/input'
import Paginador, { Buscador } from '@/components/ui/paginador'
import { BotonesExportar, CabeceraOrdenable, Celda, Tabla } from '@/components/ui/tabla'

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

  const {
    items,
    datos,
    pagina,
    tamano,
    orden,
    direccion,
    q: busquedaAplicada,
    cargando,
    error,
    setError,
    exportando,
    actualizar,
    ordenarPor,
    refrescar,
    exportar,
  } = useListado(endpoint)

  const [busqueda, setBusqueda] = useState(busquedaAplicada)
  const [formAbierto, setFormAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [valores, setValores] = useState(() => valoresIniciales(campos))
  const [guardando, setGuardando] = useState(false)
  const [importando, setImportando] = useState(false)
  const [importacion, setImportacion] = useState(null)
  const [fuentes, setFuentes] = useState({})

  const camposSelect = useMemo(
    () => campos.filter((campo) => campo.type === 'select'),
    [campos],
  )

  const aplicarBusqueda = useDebounce(
    (valor) => actualizar({ q: valor.trim(), pagina: 0 }),
    300,
  )

  function cambiarBusqueda(valor) {
    setBusqueda(valor)
    aplicarBusqueda(valor)
  }

  async function importar(event) {
    const archivo = event.target.files?.[0]
    if (!archivo) return
    setImportando(true)
    setImportacion(null)
    try {
      const datos = new FormData()
      datos.append('archivo', archivo)
      const resumen = await api.subir(recurso.importar.endpoint, datos)
      setImportacion(resumen)
      refrescar()
    } catch (err) {
      setError(err.message)
    } finally {
      setImportando(false)
      event.target.value = ''
    }
  }

  const columnasExport = columnas.map((columna) => ({
    label: columna.label,
    valor: (fila) => fila[columna.key],
  }))

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
        actualizar({ pagina: 0 })
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
        actualizar({ pagina: pagina - 1 })
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
        <div className="flex flex-wrap items-center gap-2">
          {recurso.importar && (
            <label
              className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg border border-border bg-card px-4 text-sm hover:bg-hover"
              title={recurso.importar.ayuda}
            >
              <Upload className="size-4" />
              {importando ? 'Importando…' : 'Importar CSV'}
              <input
                type="file"
                accept=".csv,text/csv"
                className="hidden"
                onChange={importar}
                disabled={importando}
              />
            </label>
          )}
          <Button onClick={abrirNuevo} variant="accent">
            <Plus className="size-4" />
            Nuevo {singular}
          </Button>
        </div>
      </div>

      {importacion && (
        <div className="flex items-start justify-between gap-3 rounded-xl border border-success-border bg-success-soft px-4 py-3 text-sm text-success">
          <div>
            <p className="font-medium">
              Importación terminada: {importacion.creados} creados
              {importacion.omitidos > 0 && `, ${importacion.omitidos} omitidos`}
            </p>
            {importacion.errores?.length > 0 && (
              <ul className="mt-1 list-disc pl-5 text-xs">
                {importacion.errores.slice(0, 5).map((mensaje) => (
                  <li key={mensaje}>{mensaje}</li>
                ))}
              </ul>
            )}
          </div>
          <button type="button" onClick={() => setImportacion(null)} aria-label="Cerrar aviso">
            <X className="size-4" />
          </button>
        </div>
      )}

      {error && (
        <div className="flex items-start justify-between gap-3 rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
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
          <div className="flex flex-wrap items-center gap-2">
            <Buscador
              valor={busqueda}
              onChange={cambiarBusqueda}
              placeholder="Buscar por nombre…"
            />
            <BotonesExportar
              onCsv={() => exportar(columnasExport)}
              onExcel={() => exportar(columnasExport, 'xlsx')}
              exportando={exportando}
            />
            <Badge>{cargando ? 'Cargando…' : `${datos.totalElementos} registros`}</Badge>
          </div>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {!cargando && items.length === 0 ? (
            <p className="px-5 pb-5 text-sm text-muted">
              {busquedaAplicada ? `Sin resultados para «${busquedaAplicada}».` : vacio}
            </p>
          ) : (
            <div className="overflow-x-auto">
              <Tabla>
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    {columnas.map((columna) => (
                      <CabeceraOrdenable
                        key={columna.key}
                        columna={columna}
                        orden={orden}
                        direccion={direccion}
                        onOrdenar={ordenarPor}
                      />
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
                        detalle && 'cursor-pointer hover:bg-hover',
                      )}
                    >
                      {columnas.map((columna) => (
                        <Celda
                          key={columna.key}
                          etiqueta={columna.label}
                          className="px-4 py-2.5 sm:px-5 sm:py-3"
                        >
                          {columna.format
                            ? columna.format(item[columna.key], item)
                            : (item[columna.key] ?? '—')}
                        </Celda>
                      ))}
                      <Celda
                        className="px-4 py-2.5 sm:px-5 sm:py-3"
                        onClick={(event) => event.stopPropagation()}
                      >
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
                      </Celda>
                    </tr>
                  ))}
                </tbody>
              </Tabla>
            </div>
          )}
          <Paginador
            pagina={pagina}
            tamano={tamano}
            totalElementos={datos.totalElementos}
            totalPaginas={datos.totalPaginas}
            onCambiar={({ pagina: nuevaPagina, tamano: nuevoTamano }) =>
              actualizar({ pagina: nuevaPagina, tamano: nuevoTamano })
            }
          />
        </CardContent>
      </Card>
    </div>
  )
}
