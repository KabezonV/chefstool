import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, PlugZap } from 'lucide-react'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

function Kpi({ label, value, hint }) {
  return (
    <Card>
      <CardContent className="pt-5">
        <p className="text-xs font-medium text-muted">{label}</p>
        <p className="mt-1 text-2xl font-semibold tracking-tight">{value}</p>
        {hint && <p className="mt-1 text-xs text-muted">{hint}</p>}
      </CardContent>
    </Card>
  )
}

function BackendStatus() {
  const [state, setState] = useState({ status: 'loading', data: null, error: null })

  useEffect(() => {
    let active = true

    api
      .get('/productos')
      .then((data) => active && setState({ status: 'ok', data, error: null }))
      .catch((error) =>
        active && setState({ status: 'error', data: null, error: error.message }),
      )

    return () => {
      active = false
    }
  }, [])

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <PlugZap className="size-4" />
          Conexión con el backend
        </CardTitle>
        {state.status === 'loading' && <Badge>Comprobando…</Badge>}
        {state.status === 'ok' && <Badge tone="success">Conectado</Badge>}
        {state.status === 'error' && <Badge tone="danger">Sin conexión</Badge>}
      </CardHeader>
      <CardContent className="space-y-3">
        {state.status === 'error' && (
          <p className="text-sm text-muted">
            {state.error}. Arranca el backend con{' '}
            <code className="rounded bg-black/[0.05] px-1.5 py-0.5 text-xs">
              ./mvnw spring-boot:run
            </code>{' '}
            en <code className="text-xs">chefstool-backend</code>.
          </p>
        )}

        {state.status === 'ok' && (
          <>
            <p className="text-sm text-muted">
              GET <code className="text-xs">/api/productos</code> devuelve{' '}
              {state.data?.length ?? 0} productos del catálogo.
            </p>
            <ul className="flex flex-wrap gap-2">
              {(state.data ?? []).map((producto) => (
                <li key={producto.id}>
                  <Badge>
                    {producto.nombre} · {producto.unidadBase}
                  </Badge>
                </li>
              ))}
            </ul>
          </>
        )}
      </CardContent>
    </Card>
  )
}

export default function Dashboard() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">
          Resumen de tu operación
        </h1>
        <p className="mt-1 text-sm text-muted">
          Los indicadores se activarán cuando registres compras.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi label="Gasto del mes" value="—" hint="Sin compras confirmadas" />
        <Kpi label="Gasto mes anterior" value="—" hint="Sin histórico" />
        <Kpi label="Variación" value="—" hint="Comparativa mensual" />
        <Kpi label="Documentos por revisar" value="0" hint="Albaranes pendientes" />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <BackendStatus />

        <Card>
          <CardHeader>
            <CardTitle>Siguiente paso</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm text-muted">
            <p>
              Etapa 1: modelo de datos (productos, proveedores, compras, líneas,
              fichas y alertas de precio).
            </p>
            <Link
              to="/documentos"
              className="inline-flex items-center gap-1 text-sm font-medium text-foreground hover:underline"
            >
              Ver documentos
              <ArrowRight className="size-4" />
            </Link>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
