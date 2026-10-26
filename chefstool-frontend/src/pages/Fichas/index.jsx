// Vendors
import { Plus } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'

// Utils
import { moneda } from '@/lib/formato'
import { tonoMargen } from '@/lib/tonos'
import { COLUMNAS_FICHAS } from './utils/columnas'

// Hooks
import { useListado } from '@/lib/useListado'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import Paginador from '@/components/ui/paginador'
import { BotonesExportar, CabeceraOrdenable } from '@/components/ui/tabla'

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

  const columnasExport = [
    { label: 'Ficha', valor: (ficha) => ficha.nombre },
    { label: 'Raciones', valor: (ficha) => ficha.raciones },
    { label: 'Coste por ración', valor: (ficha) => ficha.costePorRacion },
    { label: 'Precio venta', valor: (ficha) => ficha.precioVenta },
    { label: 'Margen (%)', valor: (ficha) => ficha.margenPct },
  ]

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
            <BotonesExportar
              onCsv={() => exportar(columnasExport)}
              onExcel={() => exportar(columnasExport, 'xlsx')}
              exportando={exportando}
            />
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
                      columna={COLUMNAS_FICHAS.nombre}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                    />
                    <CabeceraOrdenable
                      columna={COLUMNAS_FICHAS.raciones}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                    <CabeceraOrdenable
                      columna={COLUMNAS_FICHAS.coste}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                    <CabeceraOrdenable
                      columna={COLUMNAS_FICHAS.precioVenta}
                      orden={orden}
                      direccion={direccion}
                      onOrdenar={ordenarPor}
                      className="px-3 text-right"
                    />
                    <CabeceraOrdenable
                      columna={COLUMNAS_FICHAS.margen}
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
