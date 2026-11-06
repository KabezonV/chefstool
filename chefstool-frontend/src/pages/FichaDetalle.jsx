// Vendors
import { ArrowLeft, FileDown, Loader2, Plus, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

// Utils
import { moneda } from '@/lib/formato'
import { tonoMargen } from '@/lib/tonos'

// Handlers
import { api, listarTodo } from '@/lib/api'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select, Textarea } from '@/components/ui/input'
import { Celda, TablaFormulario } from '@/components/ui/tabla'

export default function FichaDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [ficha, setFicha] = useState(null)
  const [productos, setProductos] = useState([])
  const [cabecera, setCabecera] = useState({
    nombre: '',
    descripcion: '',
    raciones: '1',
    precioVenta: '',
  })
  const [lineas, setLineas] = useState([])
  const [precios, setPrecios] = useState({})
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [descargandoPdf, setDescargandoPdf] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true

    Promise.all([api.get(`/fichas/${id}`), listarTodo('/productos')])
      .then(([detalle, catalogo]) => {
        if (!activo) return
        setFicha(detalle)
        setProductos(catalogo)
        setCabecera({
          nombre: detalle.nombre ?? '',
          descripcion: detalle.descripcion ?? '',
          raciones: String(detalle.raciones ?? 1),
          precioVenta:
            detalle.precioVenta === null || detalle.precioVenta === undefined
              ? ''
              : String(detalle.precioVenta),
        })
        setLineas(
          detalle.lineas.map((linea) => ({
            productoId: linea.productoId ?? '',
            descripcion: linea.descripcion ?? '',
            cantidad: String(linea.cantidad ?? 0),
            unidad: linea.unidad ?? '',
          })),
        )
        setPrecios(
          Object.fromEntries(
            detalle.lineas
              .filter((linea) => linea.productoId && linea.precioUnitario !== null)
              .map((linea) => [linea.productoId, Number(linea.precioUnitario)]),
          ),
        )
        setError(null)
      })
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))

    return () => {
      activo = false
    }
  }, [id])

  const costeTotal = useMemo(
    () =>
      lineas.reduce((total, linea) => {
        const precio = precios[linea.productoId]
        return precio === undefined ? total : total + Number(linea.cantidad || 0) * precio
      }, 0),
    [lineas, precios],
  )

  const raciones = Number(cabecera.raciones || 1)
  const costePorRacion = costeTotal / (raciones > 0 ? raciones : 1)
  const precioVenta = Number(cabecera.precioVenta || 0)
  const margen =
    precioVenta > 0 ? ((precioVenta - costePorRacion) / precioVenta) * 100 : null

  async function descargarPdf() {
    setDescargandoPdf(true)
    try {
      const blob = await api.descargar(`/fichas/${id}/pdf`)
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = `ficha-${(ficha?.nombre ?? 'escandallo')
        .replace(/[^\p{L}\p{N}]+/gu, '-')
        .replace(/^-|-$/g, '')
        .toLowerCase()}.pdf`
      document.body.appendChild(enlace)
      enlace.click()
      enlace.remove()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message)
    } finally {
      setDescargandoPdf(false)
    }
  }

  function cambiarCabecera(campo, valor) {
    setCabecera((previos) => ({ ...previos, [campo]: valor }))
  }

  function cambiarLinea(indice, campo, valor) {
    setLineas((previas) =>
      previas.map((linea, i) => (i === indice ? { ...linea, [campo]: valor } : linea)),
    )
  }

  function anadirLinea() {
    setLineas((previas) => [
      ...previas,
      { productoId: '', descripcion: '', cantidad: '1', unidad: '' },
    ])
  }

  function quitarLinea(indice) {
    setLineas((previas) => previas.filter((_, i) => i !== indice))
  }

  function payload() {
    return {
      nombre: cabecera.nombre,
      descripcion: cabecera.descripcion || null,
      raciones,
      margenObjetivo: null,
      precioVenta: cabecera.precioVenta === '' ? null : precioVenta,
      lineas: lineas.map((linea) => ({
        productoId: linea.productoId || null,
        descripcion: linea.descripcion || null,
        cantidad: Number(linea.cantidad || 0),
        unidad: linea.unidad || null,
      })),
    }
  }

  async function guardar() {
    setGuardando(true)
    setError(null)
    try {
      const actualizada = await api.put(`/fichas/${id}`, payload())
      setFicha(actualizada)
      setPrecios(
        Object.fromEntries(
          actualizada.lineas
            .filter((linea) => linea.productoId && linea.precioUnitario !== null)
            .map((linea) => [linea.productoId, Number(linea.precioUnitario)]),
        ),
      )
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function eliminar() {
    if (!window.confirm('¿Eliminar esta ficha técnica?')) return
    setGuardando(true)
    try {
      await api.delete(`/fichas/${id}`)
      navigate('/fichas')
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) {
    return <p className="text-sm text-muted">Cargando ficha…</p>
  }

  if (!ficha) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <p className="text-sm text-danger">{error ?? 'Ficha no encontrada'}</p>
        <Button asChild variant="outline">
          <Link to="/fichas">Volver a fichas</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <Link
          to="/fichas"
          className="inline-flex items-center gap-1 text-sm text-muted hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Fichas técnicas
        </Link>
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="mt-2 min-w-0 break-words text-xl font-semibold tracking-tight">{ficha.nombre}</h1>
          <Button variant="outline" size="sm" onClick={descargarPdf} disabled={descargandoPdf}>
            <FileDown className="size-4" />
            {descargandoPdf ? 'Generando…' : 'PDF'}
          </Button>
        </div>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      {ficha.productosSinPrecio > 0 && (
        <div className="rounded-xl border border-warning-border bg-warning-soft px-4 py-3 text-sm text-warning">
          {ficha.productosSinPrecio} producto(s) no tienen precio de compra, así que su
          coste no se cuenta todavía. Registra una compra para completar el escandallo.
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Datos de la ficha</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Nombre" htmlFor="nombre">
                <Input
                  id="nombre"
                  value={cabecera.nombre}
                  onChange={(event) => cambiarCabecera('nombre', event.target.value)}
                />
              </Field>
              <Field label="Raciones" htmlFor="raciones">
                <Input
                  id="raciones"
                  type="number"
                  min="1"
                  value={cabecera.raciones}
                  onChange={(event) => cambiarCabecera('raciones', event.target.value)}
                />
              </Field>
              <div className="sm:col-span-2">
                <Field label="Descripción" htmlFor="descripcion">
                  <Textarea
                    id="descripcion"
                    value={cabecera.descripcion}
                    onChange={(event) => cambiarCabecera('descripcion', event.target.value)}
                  />
                </Field>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Coste y margen</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="text-muted">Coste total</span>
              <span className="tabular-nums">{moneda(costeTotal)}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-muted">Por ración</span>
              <span className="text-base font-semibold tabular-nums">
                {moneda(costePorRacion)}
              </span>
            </div>
            <Field label="Precio de venta por ración (€)" htmlFor="precio-venta">
              <Input
                id="precio-venta"
                type="number"
                step="0.01"
                value={cabecera.precioVenta}
                onChange={(event) => cambiarCabecera('precioVenta', event.target.value)}
              />
            </Field>
            <div className="flex items-center justify-between">
              <span className="text-muted">Margen</span>
              {margen === null ? (
                <span className="text-muted">—</span>
              ) : (
                <Badge tone={tonoMargen(margen)}>{margen.toFixed(1)} %</Badge>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Ingredientes</CardTitle>
          <Badge>{lineas.length} líneas</Badge>
        </CardHeader>
        <CardContent className="space-y-3 px-0 pb-0">
          <div className="overflow-x-auto">
            <TablaFormulario>
              <thead>
                <tr className="border-y border-border text-left text-xs text-muted">
                  <th className="px-5 py-2 font-medium">Producto</th>
                  <th className="w-40 px-2 py-2 font-medium">Nota</th>
                  <th className="w-28 px-2 py-2 font-medium">Cantidad</th>
                  <th className="w-20 px-2 py-2 font-medium">Unidad</th>
                  <th className="w-28 px-2 py-2 text-right font-medium">Coste</th>
                  <th className="w-12 px-2 py-2" />
                </tr>
              </thead>
              <tbody>
                {lineas.map((linea, indice) => {
                  const precio = precios[linea.productoId]
                  const coste =
                    precio === undefined ? null : Number(linea.cantidad || 0) * precio
                  return (
                    <tr key={indice} className="border-b border-border/60 last:border-0">
                      <Celda etiqueta="Producto" className="px-4 py-2 sm:px-5 sm:py-2">
                        <Select
                          value={linea.productoId}
                          onChange={(event) =>
                            cambiarLinea(indice, 'productoId', event.target.value)
                          }
                        >
                          <option value="">Sin relacionar</option>
                          {productos.map((producto) => (
                            <option key={producto.id} value={producto.id}>
                              {producto.nombre}
                            </option>
                          ))}
                        </Select>
                      </Celda>
                      <Celda etiqueta="Nota" className="px-4 py-2 sm:px-2 sm:py-2">
                        <Input
                          placeholder="Opcional"
                          value={linea.descripcion}
                          onChange={(event) =>
                            cambiarLinea(indice, 'descripcion', event.target.value)
                          }
                        />
                      </Celda>
                      <Celda etiqueta="Cantidad" className="px-4 py-2 sm:px-2 sm:py-2">
                        <Input
                          type="number"
                          step="0.001"
                          value={linea.cantidad}
                          onChange={(event) =>
                            cambiarLinea(indice, 'cantidad', event.target.value)
                          }
                        />
                      </Celda>
                      <Celda etiqueta="Unidad" className="px-4 py-2 sm:px-2 sm:py-2">
                        <Input
                          value={linea.unidad}
                          onChange={(event) =>
                            cambiarLinea(indice, 'unidad', event.target.value)
                          }
                        />
                      </Celda>
                      <Celda
                        etiqueta="Coste"
                        className="px-4 py-2 text-right tabular-nums sm:px-2 sm:py-2"
                      >
                        {coste === null ? '—' : moneda(coste)}
                      </Celda>
                      <Celda className="px-4 py-2 sm:px-2 sm:py-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Quitar ingrediente"
                          onClick={() => quitarLinea(indice)}
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      </Celda>
                    </tr>
                  )
                })}
              </tbody>
            </TablaFormulario>
          </div>
          <div className="px-4 pb-5 sm:px-5">
            <Button variant="outline" onClick={anadirLinea}>
              <Plus className="size-4" />
              Añadir ingrediente
            </Button>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap items-center justify-end gap-2">
        <Button variant="ghost" disabled={guardando} onClick={eliminar}>
          Eliminar ficha
        </Button>
        <Button variant="accent" disabled={guardando} onClick={guardar}>
          {guardando ? <Loader2 className="size-4 animate-spin" /> : null}
          Guardar cambios
        </Button>
      </div>
    </div>
  )
}
