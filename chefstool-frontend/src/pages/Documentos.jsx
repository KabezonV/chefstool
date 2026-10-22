import { Link, useLocation, useNavigate } from 'react-router-dom'
import { FileText, Plus } from 'lucide-react'
import { useListado } from '@/lib/useListado'
import { estados, fechaCorta } from '@/lib/formato'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import Paginador from '@/components/ui/paginador'
import { BotonExportar, CabeceraOrdenable } from '@/components/ui/tabla'

function confianza(valor) {
  if (valor === null || valor === undefined) return '—'
  return `${Math.round(Number(valor) * 100)} %`
}

const columnas = {
  archivo: { label: 'Archivo', orden: 'nombreOriginal' },
  estado: { label: 'Estado', orden: 'estado' },
  origen: { label: 'Origen' },
  confianza: { label: 'Confianza', orden: 'confianzaExtraccion' },
  subido: { label: 'Subido', orden: 'createdAt' },
}

export default function Documentos() {
  const navigate = useNavigate()
  const { state } = useLocation()

  const {
    items: documentos,
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
  } = useListado('/documentos')

  function exportarCsv() {
    exportar([
      { label: 'Archivo', valor: (documento) => documento.nombreOriginal },
      { label: 'Estado', valor: (documento) => documento.estado },
      { label: 'Origen', valor: (documento) => documento.origen },
      { label: 'Confianza', valor: (documento) => documento.confianzaExtraccion },
      { label: 'Subido', valor: (documento) => documento.createdAt },
    ])
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Documentos</h1>
          <p className="mt-1 text-sm text-muted">
            Albaranes y facturas: desde la foto hasta la compra confirmada.
          </p>
        </div>
        <Button asChild variant="accent">
          <Link to="/documentos/nuevo">
            <Plus className="size-4" />
            Añadir documento
          </Link>
        </Button>
      </div>

      {state?.aviso && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-success">
          {state.aviso}
        </div>
      )}

      {error && (
        <div className="rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Bandeja de entrada</CardTitle>
          <div className="flex flex-wrap items-center gap-2">
            <BotonExportar onClick={exportarCsv} exportando={exportando} />
            <Badge>{cargando ? 'Cargando…' : `${datos.totalElementos} documentos`}</Badge>
          </div>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {!cargando && documentos.length === 0 ? (
            <div className="px-5 pb-6 text-sm text-muted">
              <p>Todavía no has subido ningún albarán.</p>
              <Link
                to="/documentos/nuevo"
                className="mt-2 inline-flex items-center gap-1 font-medium text-foreground hover:underline"
              >
                <FileText className="size-4" />
                Sube el primero
              </Link>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    {Object.values(columnas).map((columna) => (
                      <CabeceraOrdenable
                        key={columna.label}
                        columna={columna}
                        orden={orden}
                        direccion={direccion}
                        onOrdenar={ordenarPor}
                      />
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {documentos.map((documento) => {
                    const estado = estados[documento.estado] ?? estados.pendiente_revision
                    return (
                      <tr
                        key={documento.id}
                        onClick={() => navigate(`/documentos/${documento.id}`)}
                        className="cursor-pointer border-b border-border/60 last:border-0 hover:bg-black/[0.02]"
                      >
                        <td className="px-5 py-3 font-medium">
                          {documento.nombreOriginal}
                        </td>
                        <td className="px-5 py-3">
                          <Badge tone={estado.tono}>{estado.texto}</Badge>
                        </td>
                        <td className="px-5 py-3 capitalize text-muted">
                          {documento.origen}
                        </td>
                        <td className="px-5 py-3 text-muted">
                          {confianza(documento.confianzaExtraccion)}
                        </td>
                        <td className="px-5 py-3 text-muted">
                          {fechaCorta(documento.createdAt)}
                        </td>
                      </tr>
                    )
                  })}
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
