// Vendors
import { ShieldCheck } from 'lucide-react'

// Utils
import { fechaHora } from '@/lib/formato'
import { ACCIONES_AUDITORIA, ENTIDADES_AUDITORIA } from './utils/etiquetas'

// Hooks
import { useListado } from '@/lib/useListado'

// Components
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import Paginador from '@/components/ui/paginador'
import { Celda, Tabla } from '@/components/ui/tabla'

export default function Auditoria() {
  const { items, datos, pagina, tamano, cargando, error, actualizar } = useListado('/auditoria')

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <ShieldCheck className="size-5" />
          Auditoría
        </h1>
        <p className="mt-1 text-sm text-muted">
          Quién hizo qué y cuándo: altas, cambios, borrados, albaranes y aprobaciones.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Actividad</CardTitle>
          <Badge>{cargando ? 'Cargando…' : `${datos.totalElementos} movimientos`}</Badge>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {!cargando && items.length === 0 ? (
            <p className="px-5 pb-6 text-sm text-muted">
              Todavía no hay movimientos registrados en esta organización.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <Tabla>
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <th className="px-5 py-2 font-medium">Cuándo</th>
                    <th className="px-2 py-2 font-medium">Quién</th>
                    <th className="px-2 py-2 font-medium">Qué hizo</th>
                    <th className="px-2 py-2 font-medium">Sobre</th>
                    <th className="px-5 py-2 font-medium">Detalle</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((evento) => (
                    <tr key={evento.id} className="border-b border-border/60 last:border-0">
                      <Celda etiqueta="Cuándo" className="px-4 py-2 whitespace-nowrap sm:px-5">
                        {fechaHora(evento.createdAt)}
                      </Celda>
                      <Celda etiqueta="Quién" className="px-4 py-2 sm:px-2">
                        {evento.usuarioNombre ?? 'Sistema'}
                      </Celda>
                      <Celda etiqueta="Qué hizo" className="px-4 py-2 sm:px-2">
                        {ACCIONES_AUDITORIA[evento.accion] ?? evento.accion}
                      </Celda>
                      <Celda etiqueta="Sobre" className="px-4 py-2 sm:px-2">
                        {ENTIDADES_AUDITORIA[evento.entidad] ?? evento.entidad}
                      </Celda>
                      <Celda
                        etiqueta="Detalle"
                        className="max-w-xs truncate px-4 py-2 text-muted sm:px-5"
                      >
                        {evento.detalle ?? '—'}
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
