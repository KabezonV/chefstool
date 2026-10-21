import { useEffect, useState } from 'react'
import { TrendingUp } from 'lucide-react'
import { api } from '@/lib/api'
import { moneda } from '@/lib/formato'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import Barras from '@/components/ui/barras'

export default function Informes() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true

    api
      .get('/informes/panel')
      .then((datos) => activo && setPanel(datos))
      .catch((err) => activo && setError(err.message))

    return () => {
      activo = false
    }
  }, [])

  if (error) {
    return (
      <div className="mx-auto max-w-6xl rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
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
              <table className="w-full text-sm">
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
                      <td className="px-5 py-2 font-medium">{variacion.producto}</td>
                      <td className="px-3 py-2 text-muted">{variacion.proveedor}</td>
                      <td className="px-3 py-2 text-right tabular-nums text-muted">
                        {moneda(variacion.precioAnterior)}
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums">
                        {moneda(variacion.precioNuevo)}
                      </td>
                      <td className="px-3 py-2 text-right">
                        <Badge
                          tone={Number(variacion.porcentaje) >= 20 ? 'danger' : 'warning'}
                        >
                          +{Number(variacion.porcentaje).toFixed(1)} %
                        </Badge>
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
