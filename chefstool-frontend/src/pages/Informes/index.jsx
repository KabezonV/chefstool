// Vendors
import { TrendingUp } from 'lucide-react'
import { useEffect, useState } from 'react'

// Utils
import { descargarCsv, fechaArchivo } from '@/lib/csv'
import { fechaCorta, moneda } from '@/lib/formato'
import { tonoVariacion } from '@/lib/tonos'
import { descargarXlsx } from '@/lib/xlsx'
import { COLUMNAS_PROVEEDORES } from './utils/columnas'

// Handlers
import { api } from '@/lib/api'

// Components
import { Badge } from '@/components/ui/badge'
import Barras from '@/components/ui/barras'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { BotonesExportar, Celda, Tabla } from '@/components/ui/tabla'

export default function Informes() {
  const [panel, setPanel] = useState(null)
  const [proveedores, setProveedores] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true

    Promise.all([api.get('/informes/panel'), api.get('/informes/proveedores')])
      .then(([datosPanel, datosProveedores]) => {
        if (!activo) return
        setPanel(datosPanel)
        setProveedores(datosProveedores)
      })
      .catch((err) => activo && setError(err.message))

    return () => {
      activo = false
    }
  }, [])

  if (error) {
    return (
      <div className="mx-auto max-w-6xl rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
        {error}
      </div>
    )
  }

  if (!panel) {
    return <p className="text-sm text-muted">Calculando informes…</p>
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Informes</h1>
        <p className="mt-1 text-sm text-muted">
          Evolución del gasto y oportunidades de ahorro.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="pt-5">
            <p className="text-xs font-medium text-muted">Gasto total</p>
            <p className="mt-1 text-2xl font-semibold tracking-tight">
              {moneda(panel.gastoTotal)}
            </p>
            <p className="mt-1 text-xs text-muted">
              {panel.comprasAnalizadas} compras analizadas
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-5">
            <p className="text-xs font-medium text-muted">Gasto del mes</p>
            <p className="mt-1 text-2xl font-semibold tracking-tight">
              {moneda(panel.kpis.gastoMes)}
            </p>
            <p className="mt-1 text-xs text-muted">
              Mes anterior: {moneda(panel.kpis.gastoMesAnterior)}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-5">
            <p className="text-xs font-medium text-muted">Ahorro potencial estimado</p>
            <p className="mt-1 text-2xl font-semibold tracking-tight text-success">
              {moneda(panel.ahorroPotencial)}
            </p>
            <p className="mt-1 text-xs text-muted">
              Comparando cada compra con el mejor precio histórico del mismo producto
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Gasto por mes</CardTitle>
            <Badge>{panel.gastoPorMes.length} meses</Badge>
          </CardHeader>
          <CardContent>
            <Barras datos={panel.gastoPorMes} etiquetaVacia="Sin histórico." />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Gasto por proveedor</CardTitle>
            <Badge>{panel.gastoPorProveedor.length} proveedores</Badge>
          </CardHeader>
          <CardContent>
            <Barras
              datos={panel.gastoPorProveedor}
              etiquetaVacia="Sin compras este mes."
            />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex-wrap">
          <CardTitle>Coste histórico por proveedor</CardTitle>
          <div className="flex flex-wrap items-center gap-2">
            <BotonesExportar
              onCsv={() => descargarCsv(`proveedores-${fechaArchivo()}`, COLUMNAS_PROVEEDORES, proveedores)}
              onExcel={() =>
                descargarXlsx(`proveedores-${fechaArchivo()}`, {
                  hoja: 'Proveedores',
                  columnas: COLUMNAS_PROVEEDORES,
                  filas: proveedores,
                })
              }
            />
            <Badge>{proveedores.length} proveedores</Badge>
          </div>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {proveedores.length === 0 ? (
            <p className="px-5 pb-5 text-sm text-muted">
              Todavía no hay compras confirmadas con proveedor.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <Tabla>
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <th className="px-5 py-2 font-medium">Proveedor</th>
                    <th className="px-3 py-2 text-right font-medium">Compras</th>
                    <th className="px-3 py-2 text-right font-medium">Gasto total</th>
                    <th className="px-3 py-2 text-right font-medium">Ticket medio</th>
                    <th className="w-32 px-3 py-2 font-medium">Peso</th>
                    <th className="px-3 py-2 text-right font-medium">Var. precios</th>
                    <th className="px-3 py-2 text-right font-medium">Última compra</th>
                  </tr>
                </thead>
                <tbody>
                  {proveedores.map((proveedor) => (
                    <tr
                      key={proveedor.proveedorId}
                      className="border-b border-border/60 last:border-0"
                    >
                      <Celda
                        etiqueta="Proveedor"
                        className="px-4 py-2 font-medium sm:px-5 sm:py-2"
                      >
                        {proveedor.nombre}
                      </Celda>
                      <Celda
                        etiqueta="Compras"
                        className="px-4 py-2 text-right tabular-nums sm:px-3 sm:py-2"
                      >
                        {proveedor.compras}
                      </Celda>
                      <Celda
                        etiqueta="Gasto total"
                        className="px-4 py-2 text-right tabular-nums sm:px-3 sm:py-2"
                      >
                        {moneda(proveedor.gastoTotal)}
                      </Celda>
                      <Celda
                        etiqueta="Ticket medio"
                        className="px-4 py-2 text-right tabular-nums text-muted sm:px-3 sm:py-2"
                      >
                        {moneda(proveedor.ticketMedio)}
                      </Celda>
                      <Celda etiqueta="Peso" className="px-4 py-2 sm:px-3 sm:py-2">
                        <div className="flex items-center justify-end gap-2 sm:justify-start">
                          <div className="h-1.5 w-16 overflow-hidden rounded-full bg-track">
                            <div
                              className="h-full rounded-full bg-foreground"
                              style={{ width: `${Math.min(Number(proveedor.porcentajeGasto), 100)}%` }}
                            />
                          </div>
                          <span className="text-xs tabular-nums text-muted">
                            {Number(proveedor.porcentajeGasto).toFixed(1)} %
                          </span>
                        </div>
                      </Celda>
                      <Celda
                        etiqueta="Var. precios"
                        className="px-4 py-2 text-right sm:px-3 sm:py-2"
                      >
                        {proveedor.variacionMediaPct === null ? (
                          <span className="text-xs text-muted">—</span>
                        ) : (
                          <Badge tone={tonoVariacion(proveedor.variacionMediaPct)}>
                            {Number(proveedor.variacionMediaPct) > 0 ? '+' : ''}
                            {Number(proveedor.variacionMediaPct).toFixed(1)} %
                          </Badge>
                        )}
                      </Celda>
                      <Celda
                        etiqueta="Última compra"
                        className="px-4 py-2 text-right text-muted sm:px-3 sm:py-2"
                      >
                        {proveedor.ultimaCompra ? fechaCorta(proveedor.ultimaCompra) : '—'}
                      </Celda>
                    </tr>
                  ))}
                </tbody>
              </Tabla>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="size-4" />
            Mayores variaciones de precio
          </CardTitle>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {panel.mayoresVariaciones.length === 0 ? (
            <p className="px-5 pb-5 text-sm text-muted">
              Necesitamos al menos dos compras del mismo producto para comparar precios.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <Tabla>
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <th className="px-5 py-2 font-medium">Producto</th>
                    <th className="px-3 py-2 font-medium">Proveedor</th>
                    <th className="px-3 py-2 text-right font-medium">Antes</th>
                    <th className="px-3 py-2 text-right font-medium">Ahora</th>
                    <th className="px-3 py-2 text-right font-medium">Variación</th>
                  </tr>
                </thead>
                <tbody>
                  {panel.mayoresVariaciones.map((variacion, indice) => (
                    <tr
                      key={variacion.producto + '-' + indice}
                      className="border-b border-border/60 last:border-0"
                    >
                      <Celda
                        etiqueta="Producto"
                        className="px-4 py-2 font-medium sm:px-5 sm:py-2"
                      >
                        {variacion.producto}
                      </Celda>
                      <Celda etiqueta="Proveedor" className="px-4 py-2 text-muted sm:px-3 sm:py-2">
                        {variacion.proveedor}
                      </Celda>
                      <Celda
                        etiqueta="Antes"
                        className="px-4 py-2 text-right tabular-nums text-muted sm:px-3 sm:py-2"
                      >
                        {moneda(variacion.precioAnterior)}
                      </Celda>
                      <Celda
                        etiqueta="Ahora"
                        className="px-4 py-2 text-right tabular-nums sm:px-3 sm:py-2"
                      >
                        {moneda(variacion.precioNuevo)}
                      </Celda>
                      <Celda
                        etiqueta="Variación"
                        className="px-4 py-2 text-right sm:px-3 sm:py-2"
                      >
                        <Badge
                          tone={Number(variacion.porcentaje) >= 20 ? 'danger' : 'warning'}
                        >
                          +{Number(variacion.porcentaje).toFixed(1)} %
                        </Badge>
                      </Celda>
                    </tr>
                  ))}
                </tbody>
              </Tabla>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
