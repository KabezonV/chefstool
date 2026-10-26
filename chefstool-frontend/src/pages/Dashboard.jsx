// Vendors
import { ArrowDownRight, ArrowUpRight, BellRing, FileText, PiggyBank } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

// Utils
import { fechaCorta, moneda } from '@/lib/formato'

// Handlers
import { api } from '@/lib/api'

// Components
import Kpi from '@/components/dashboard/Kpi'
import DatosEjemplo from '@/components/DatosEjemplo'
import { Badge } from '@/components/ui/badge'
import Barras from '@/components/ui/barras'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function Dashboard() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    let activo = true

    api
      .get('/informes/panel')
      .then((datos) => activo && setPanel(datos))
      .catch((err) => activo && setError(err.message))

    return () => {
      activo = false
    }
  }, [recarga])

  if (error) {
    return (
      <div className="mx-auto max-w-6xl rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
        {error}
      </div>
    )
  }

  if (!panel) {
    return <p className="text-sm text-muted">Cargando panel…</p>
  }

  const { kpis } = panel
  const variacion = kpis.variacionPct === null ? null : Number(kpis.variacionPct)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Resumen de tu operación</h1>
        <p className="mt-1 text-sm text-muted">Datos reales de tus compras confirmadas.</p>
      </div>

      {panel.comprasAnalizadas === 0 && (
        <DatosEjemplo onCargado={() => setRecarga((valor) => valor + 1)} />
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi
          etiqueta="Gasto del mes"
          valor={moneda(kpis.gastoMes)}
          detalle={
            variacion === null ? (
              'Sin mes anterior para comparar'
            ) : (
              <span className="inline-flex items-center gap-1">
                {variacion >= 0 ? (
                  <ArrowUpRight className="size-3.5" />
                ) : (
                  <ArrowDownRight className="size-3.5" />
                )}
                {variacion >= 0 ? '+' : ''}
                {variacion.toFixed(1)} % vs mes anterior
              </span>
            )
          }
          tono={variacion === null ? null : variacion > 0 ? 'danger' : 'success'}
        />
        <Kpi
          etiqueta="Compras del mes"
          valor={kpis.comprasMes}
          detalle={panel.comprasAnalizadas + ' compras históricas'}
        />
        <Kpi
          etiqueta="Documentos por revisar"
          valor={kpis.documentosPendientes}
          detalle="Albaranes pendientes de confirmar"
        />
        <Kpi
          etiqueta="Ahorro potencial"
          valor={moneda(panel.ahorroPotencial)}
          detalle={panel.lineasMejorables + ' líneas por encima del mejor precio'}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Gasto por proveedor (mes)</CardTitle>
          </CardHeader>
          <CardContent>
            <Barras datos={panel.gastoPorProveedor} etiquetaVacia="Sin compras este mes." />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Gasto de los últimos meses</CardTitle>
          </CardHeader>
          <CardContent>
            <Barras datos={panel.gastoPorMes} etiquetaVacia="Sin histórico." />
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BellRing className="size-4" />
              Alertas de precio
            </CardTitle>
            <Link to="/compras" className="text-xs font-medium text-muted hover:text-foreground">
              Ver compras
            </Link>
          </CardHeader>
          <CardContent>
            {panel.alertas.length === 0 ? (
              <p className="text-sm text-muted">No hay subidas de precio pendientes.</p>
            ) : (
              <ul className="space-y-2 text-sm">
                {panel.alertas.map((alerta) => (
                  <li key={alerta.id} className="flex items-start gap-2">
                    <Badge tone={alerta.severidad === 'critico' ? 'danger' : 'warning'}>
                      {alerta.severidad}
                    </Badge>
                    <span>{alerta.mensaje}</span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileText className="size-4" />
              Documentos por revisar
            </CardTitle>
            <Link to="/documentos" className="text-xs font-medium text-muted hover:text-foreground">
              Ver documentos
            </Link>
          </CardHeader>
          <CardContent>
            {panel.documentosPorRevisar.length === 0 ? (
              <p className="text-sm text-muted">Todo revisado.</p>
            ) : (
              <ul className="space-y-2 text-sm">
                {panel.documentosPorRevisar.map((documento) => (
                  <li key={documento.id}>
                    <Link
                      to={'/documentos/' + documento.id}
                      className="flex items-center justify-between gap-2 hover:underline"
                    >
                      <span className="truncate">{documento.nombreOriginal}</span>
                      <span className="shrink-0 text-xs text-muted">
                        {fechaCorta(documento.createdAt)}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <PiggyBank className="size-4" />
            Ahorro potencial
          </CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted">
          <p>
            Estimación comparando cada compra con el mejor precio histórico del mismo
            producto. Ahora mismo asciende a{' '}
            <span className="font-semibold text-foreground">
              {moneda(panel.ahorroPotencial)}
            </span>{' '}
            en {panel.lineasMejorables} líneas.
          </p>
          <Button asChild variant="outline" size="sm" className="mt-3">
            <Link to="/informes">Ver informe completo</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
