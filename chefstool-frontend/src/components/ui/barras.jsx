// Utils
import { moneda } from '@/lib/formato'

const formatoMes = new Intl.DateTimeFormat('es-ES', { month: 'short', year: 'numeric' })

function etiquetaLegible(valor) {
  const partes = /^(\d{4})-(\d{2})$/.exec(valor)
  if (!partes) return valor
  return formatoMes.format(new Date(Number(partes[1]), Number(partes[2]) - 1, 1))
}

/** Lista de barras horizontales proporcionales, para gasto por proveedor o por mes. */
export default function Barras({ datos, etiquetaVacia = 'Sin datos' }) {
  if (!datos || datos.length === 0) {
    return <p className="text-sm text-muted">{etiquetaVacia}</p>
  }

  const maximo = Math.max(...datos.map((dato) => Number(dato.importe) || 0), 1)

  return (
    <ul className="space-y-2">
      {datos.map((dato) => (
        <li key={dato.etiqueta} className="text-sm">
          <div className="mb-1 flex items-center justify-between gap-2">
            <span className="min-w-0 truncate">{etiquetaLegible(dato.etiqueta)}</span>
            <span className="shrink-0 tabular-nums text-muted">
              {moneda(dato.importe)}
            </span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-active">
            <div
              className="h-full rounded-full bg-accent/80"
              style={{ width: `${Math.max(2, (Number(dato.importe) / maximo) * 100)}%` }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}
