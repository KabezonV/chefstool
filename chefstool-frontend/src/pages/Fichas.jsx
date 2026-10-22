import { Link, useNavigate } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { useListado } from '@/lib/useListado'
import { moneda } from '@/lib/formato'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import Paginador from '@/components/ui/paginador'
import { BotonExportar, CabeceraOrdenable } from '@/components/ui/tabla'

function tonoMargen(margen) {
  if (margen === null || margen === undefined) return 'neutral'
  if (margen >= 65) return 'success'
  if (margen >= 40) return 'warning'
  return 'danger'
}

const columnas = {
  nombre: { label: 'Ficha', orden: 'nombre' },
  raciones: { label: 'Raciones', orden: 'raciones' },
  coste: { label: 'Coste/ración' },
  precioVenta: { label: 'Precio venta', orden: 'precioVenta' },
  margen: { label: 'Margen' },
}

export default function Fichas() {
  const navigate = useNavigate()

  const {
    items: fichas,
    datos,
    pagina,
    tamano,
    orden,
    direccion,
    cargando,
    error,
    exportando,
    actualizar,
    ordenarPor,
    exportar,
  } = useListado('/fichas')

  function exportarCsv() {
    exportar([
      { label: 'Ficha', valor: (ficha) => ficha.nombre },
      { label: 'Raciones', valor: (ficha) => ficha.raciones },
      { label: 'Coste por ración', valor: (ficha) => ficha.costePorRacion },
      { label: 'Precio venta', valor: (ficha) => ficha.precioVenta },
      { label: 'Margen', valor: (ficha) => ficha.margenPct },
    ])
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Fichas técnicas</h1>
          <p className="mt-1 text-sm text-muted">
            Escandallos con coste real por ración y margen.
          </p>
        </div>
        <Button asChild variant="accent">
          <Link to="/fichas/nuevo">
            <Plus className="size-4" />
            Nueva ficha
          </Link>
        </Button>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Recetario</CardTitle>
          <div className="flex flex-wrap items-center gap-2">
            <BotonExportar onClick={exportarCsv} exportando={exportando} />
            <Badge>{cargando ? 'Cargando…' : `${datos.totalElementos} fichas`}</Badge>
          </div>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {!cargando && fichas.length === 0 ? (
            <p className="px-5 pb-6 text-sm text-muted">
              Todavía no hay fichas técnicas. Crea la primera para calcular costes.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <CabeceraOrdenable
                      columna={columnas.nombre}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                    />
                    <CabeceraOrdenable
                      columna={columnas.raciones}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                    <CabeceraOrdenable
                      columna={columnas.coste}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                    <CabeceraOrdenable
                      columna={columnas.precioVenta}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                    <CabeceraOrdenable
                      columna={columnas.margen}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                  </tr>
                </thead>
                <tbody>
                  {fichas.map((ficha) => (
                    <tr
                      key={ficha.id}
                      onClick={() => navigate(`/fichas/${ficha.id}`)}
                      className="cursor-pointer border-b border-border/60 last:border-0 hover:bg-black/[0.02]"
                    >
                      <td className="px-5 py-3 font-medium">{ficha.nombre}</td>
                      <td className="px-3 py-3 text-right tabular-nums">{ficha.raciones}</td>
                      <td className="px-3 py-3 text-right tabular-nums">
                        {moneda(ficha.costePorRacion)}
                      </td>
                      <td className="px-3 py-3 text-right tabular-nums">
                        {ficha.precioVenta ? moneda(ficha.precioVenta) : '—'}
                      </td>
                      <td className="px-3 py-3 text-right">
                        {ficha.margenPct === null || ficha.margenPct === undefined ? (
                          '—'
                        ) : (
                          <Badge tone={tonoMargen(Number(ficha.margenPct))}>
                            {Number(ficha.margenPct).toFixed(1)} %
                          </Badge>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
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
