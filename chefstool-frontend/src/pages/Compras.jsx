import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BellRing, Check, ExternalLink } from 'lucide-react'
import { api } from '@/lib/api'
import { fechaCorta, moneda } from '@/lib/formato'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select } from '@/components/ui/input'

const tonos = { info: 'neutral', aviso: 'warning', critico: 'danger' }

export default function Compras() {
  const [compras, setCompras] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [alertas, setAlertas] = useState([])
  const [detalle, setDetalle] = useState(null)
  const [filtros, setFiltros] = useState({ proveedorId: '', desde: '', hasta: '' })
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    api
      .get('/proveedores')
      .then(setProveedores)
      .catch(() => setProveedores([]))
  }, [])

  useEffect(() => {
    let activo = true
    const parametros = new URLSearchParams()
    if (filtros.proveedorId) parametros.set('proveedorId', filtros.proveedorId)
    if (filtros.desde) parametros.set('desde', filtros.desde)
    if (filtros.hasta) parametros.set('hasta', filtros.hasta)

    api
      .get(`/compras?${parametros.toString()}`)
      .then((datos) => {
        if (!activo) return
        setCompras(datos)
        setError(null)
      })
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))

    return () => {
      activo = false
    }
  }, [filtros])

  function actualizarFiltro(campo, valor) {
    setCargando(true)
    setFiltros((previos) => ({ ...previos, [campo]: valor }))
  }

  function cargarAlertas() {
    api
      .get('/alertas')
      .then(setAlertas)
      .catch(() => setAlertas([]))
  }

  useEffect(cargarAlertas, [])

  async function verDetalle(id) {
    try {
      setDetalle(await api.get(`/compras/${id}`))
    } catch (err) {
      setError(err.message)
    }
  }

  async function reconocer(id) {
    try {
      await api.post(`/alertas/${id}/reconocer`)
      cargarAlertas()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Compras</h1>
        <p className="mt-1 text-sm text-muted">
          Compras confirmadas a partir de tus albaranes.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      {alertas.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BellRing className="size-4" />
              Alertas de precio
            </CardTitle>
            <Badge tone="warning">{alertas.length} sin revisar</Badge>
          </CardHeader>
          <CardContent className="space-y-2">
            {alertas.map((alerta) => (
              <div
                key={alerta.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-black/[0.02] px-3 py-2 text-sm"
              >
                <div className="flex items-center gap-2">
                  <Badge tone={tonos[alerta.severidad] ?? 'neutral'}>
                    {alerta.severidad}
                  </Badge>
                  <span>{alerta.mensaje}</span>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => reconocer(alerta.id)}
                >
                  <Check className="size-4" />
                  Reconocer
                </Button>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Histórico</CardTitle>
          <Badge>{cargando ? 'Cargando…' : `${compras.length} compras`}</Badge>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label="Proveedor" htmlFor="filtro-proveedor">
              <Select
                id="filtro-proveedor"
                value={filtros.proveedorId}
                onChange={(event) => actualizarFiltro('proveedorId', event.target.value)}
              >
                <option value="">Todos</option>
                {proveedores.map((proveedor) => (
                  <option key={proveedor.id} value={proveedor.id}>
                    {proveedor.nombre}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Desde" htmlFor="filtro-desde">
              <Input
                id="filtro-desde"
                type="date"
                value={filtros.desde}
                onChange={(event) => actualizarFiltro('desde', event.target.value)}
              />
            </Field>
            <Field label="Hasta" htmlFor="filtro-hasta">
              <Input
                id="filtro-hasta"
                type="date"
                value={filtros.hasta}
                onChange={(event) => actualizarFiltro('hasta', event.target.value)}
              />
            </Field>
          </div>

          {compras.length === 0 && !cargando ? (
            <p className="text-sm text-muted">
              Todavía no hay compras confirmadas. Sube un albarán desde{' '}
              <Link to="/documentos/nuevo" className="font-medium text-foreground hover:underline">
                Añadir documento
              </Link>
              .
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <th className="px-3 py-2 font-medium">Fecha</th>
                    <th className="px-3 py-2 font-medium">Proveedor</th>
                    <th className="px-3 py-2 font-medium">Documento</th>
                    <th className="px-3 py-2 text-right font-medium">Total</th>
                    <th className="w-20 px-3 py-2" />
                  </tr>
                </thead>
                <tbody>
                  {compras.map((compra) => (
                    <tr key={compra.id} className="border-b border-border/60 last:border-0">
                      <td className="px-3 py-2">{fechaCorta(compra.fecha)}</td>
                      <td className="px-3 py-2">
                        {compra.proveedorNombre ?? 'Sin proveedor'}
                      </td>
                      <td className="px-3 py-2 text-muted">
                        {compra.numeroDocumento ?? '—'}
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums">
                        {moneda(compra.importeTotal)}
                      </td>
                      <td className="px-3 py-2 text-right">
                        <Button variant="ghost" size="sm" onClick={() => verDetalle(compra.id)}>
                          Ver
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {detalle && (
        <Card>
          <CardHeader>
            <CardTitle>
              {detalle.numeroDocumento ?? 'Compra'} ·{' '}
              {detalle.proveedorNombre ?? 'Sin proveedor'}
            </CardTitle>
            <div className="flex items-center gap-2">
              {detalle.documentoId && (
                <Link
                  to={`/documentos/${detalle.documentoId}`}
                  className="inline-flex items-center gap-1 text-xs font-medium text-muted hover:text-foreground"
                >
                  <ExternalLink className="size-3.5" />
                  Ver documento
                </Link>
              )}
              <Button variant="ghost" size="sm" onClick={() => setDetalle(null)}>
                Cerrar
              </Button>
            </div>
          </CardHeader>
          <CardContent className="px-0 pb-0">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <th className="px-5 py-2 font-medium">Producto</th>
                    <th className="px-3 py-2 font-medium">Cantidad</th>
                    <th className="px-3 py-2 font-medium">Precio</th>
                    <th className="px-3 py-2 text-right font-medium">Importe</th>
                  </tr>
                </thead>
                <tbody>
                  {detalle.lineas.map((linea) => (
                    <tr key={linea.id} className="border-b border-border/60 last:border-0">
                      <td className="px-5 py-2">
                        {linea.productoNombre ?? linea.descripcion ?? '—'}
                      </td>
                      <td className="px-3 py-2">
                        {linea.cantidad} {linea.unidad ?? ''}
                      </td>
                      <td className="px-3 py-2 tabular-nums">{moneda(linea.precioUnitario)}</td>
                      <td className="px-3 py-2 text-right tabular-nums">
                        {moneda(linea.importe)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="ml-auto grid w-64 grid-cols-2 gap-y-1 px-5 py-4 text-sm">
              <span className="text-muted">Base imponible</span>
              <span className="text-right tabular-nums">{moneda(detalle.importeNeto)}</span>
              <span className="text-muted">IVA</span>
              <span className="text-right tabular-nums">{moneda(detalle.importeIva)}</span>
              <span className="border-t border-border pt-1 font-semibold">Total</span>
              <span className="border-t border-border pt-1 text-right font-semibold tabular-nums">
                {moneda(detalle.importeTotal)}
              </span>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
