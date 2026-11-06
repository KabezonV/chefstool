// Vendors
import { BellRing, Check, ExternalLink } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

// Utils
import { fechaCorta, moneda } from '@/lib/formato'
import { COLUMNAS_COMPRAS } from './utils/columnas'

// Enumerations
import { TONO_ALERTA } from '@/constants/tonos'

// Handlers
import { api, listarTodo } from '@/lib/api'

// Hooks
import { useListado } from '@/lib/useListado'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select } from '@/components/ui/input'
import Paginador from '@/components/ui/paginador'
import { BotonesExportar, CabeceraOrdenable, Celda, Tabla } from '@/components/ui/tabla'

export default function Compras() {
  const [parametros] = useSearchParams()
  const filtros = {
    proveedorId: parametros.get('proveedorId') ?? '',
    desde: parametros.get('desde') ?? '',
    hasta: parametros.get('hasta') ?? '',
  }

  const {
    items: compras,
    datos,
    pagina,
    tamano,
    orden,
    direccion,
    cargando,
    error,
    setError,
    exportando,
    actualizar,
    ordenarPor,
    exportar,
  } = useListado('/compras', filtros)

  const [proveedores, setProveedores] = useState([])
  const [alertas, setAlertas] = useState([])
  const [detalle, setDetalle] = useState(null)

  useEffect(() => {
    listarTodo('/proveedores')
      .then(setProveedores)
      .catch(() => setProveedores([]))
  }, [])

  function actualizarFiltro(campo, valor) {
    actualizar({ [campo]: valor, pagina: 0 })
  }

  const columnasExport = [
    { label: 'Fecha', valor: (compra) => compra.fecha },
    { label: 'Proveedor', valor: (compra) => compra.proveedorNombre },
    { label: 'Documento', valor: (compra) => compra.numeroDocumento },
    { label: 'Base', valor: (compra) => compra.importeNeto },
    { label: 'IVA', valor: (compra) => compra.importeIva },
    { label: 'Total', valor: (compra) => compra.importeTotal },
  ]

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
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
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
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-hover px-3 py-2 text-sm"
              >
                <div className="flex items-center gap-2">
                  <Badge tone={TONO_ALERTA[alerta.severidad] ?? 'neutral'}>
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
          <div className="flex flex-wrap items-center gap-2">
            <BotonesExportar
              onCsv={() => exportar(columnasExport)}
              onExcel={() => exportar(columnasExport, 'xlsx')}
              exportando={exportando}
            />
            <Badge>{cargando ? 'Cargando…' : `${datos.totalElementos} compras`}</Badge>
          </div>
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
              <Tabla>
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <CabeceraOrdenable
                      columna={COLUMNAS_COMPRAS.fecha}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3"
                    />
                    <CabeceraOrdenable
                      columna={COLUMNAS_COMPRAS.proveedor}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3"
                    />
                    <CabeceraOrdenable
                      columna={COLUMNAS_COMPRAS.documento}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3"
                    />
                    <CabeceraOrdenable
                      columna={COLUMNAS_COMPRAS.total}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                    <th className="w-20 px-3 py-2" />
                  </tr>
                </thead>
                <tbody>
                  {compras.map((compra) => (
                    <tr key={compra.id} className="border-b border-border/60 last:border-0">
                      <Celda etiqueta="Fecha" className="px-4 py-2.5 sm:px-3 sm:py-2">
                        {fechaCorta(compra.fecha)}
                      </Celda>
                      <Celda etiqueta="Proveedor" className="px-4 py-2.5 sm:px-3 sm:py-2">
                        {compra.proveedorNombre ?? 'Sin proveedor'}
                      </Celda>
                      <Celda
                        etiqueta="Documento"
                        className="px-4 py-2.5 text-muted sm:px-3 sm:py-2"
                      >
                        {compra.numeroDocumento ?? '—'}
                      </Celda>
                      <Celda
                        etiqueta="Total"
                        className="px-4 py-2.5 text-right tabular-nums sm:px-3 sm:py-2"
                      >
                        {moneda(compra.importeTotal)}
                      </Celda>
                      <Celda className="px-4 py-2.5 text-right sm:px-3 sm:py-2">
                        <Button variant="ghost" size="sm" onClick={() => verDetalle(compra.id)}>
                          Ver
                        </Button>
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
              <Tabla>
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
                      <Celda
                        etiqueta="Producto"
                        className="px-4 py-2 font-medium sm:px-5 sm:py-2"
                      >
                        {linea.productoNombre ?? linea.descripcion ?? '—'}
                      </Celda>
                      <Celda etiqueta="Cantidad" className="px-4 py-2 sm:px-3 sm:py-2">
                        {linea.cantidad} {linea.unidad ?? ''}
                      </Celda>
                      <Celda etiqueta="Precio" className="px-4 py-2 tabular-nums sm:px-3 sm:py-2">
                        {moneda(linea.precioUnitario)}
                      </Celda>
                      <Celda
                        etiqueta="Importe"
                        className="px-4 py-2 text-right tabular-nums sm:px-3 sm:py-2"
                      >
                        {moneda(linea.importe)}
                      </Celda>
                    </tr>
                  ))}
                </tbody>
              </Tabla>
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
