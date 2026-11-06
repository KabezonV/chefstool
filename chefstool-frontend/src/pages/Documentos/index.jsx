// Vendors
import { FileText, Plus } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

// Utils
import { fechaCorta } from '@/lib/formato'
import { COLUMNAS_DOCUMENTOS } from './utils/columnas'
import { formatearConfianza } from './utils/confianza'

// Enumerations
import { ESTADOS_DOCUMENTO } from '@/constants/documentos'

// Hooks
import { useListado } from '@/lib/useListado'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import Paginador from '@/components/ui/paginador'
import { BotonesExportar, CabeceraOrdenable, Celda, Tabla } from '@/components/ui/tabla'

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

  const columnasExport = [
    { label: 'Archivo', valor: (documento) => documento.nombreOriginal },
    { label: 'Estado', valor: (documento) => documento.estado },
    { label: 'Origen', valor: (documento) => documento.origen },
    {
      label: 'Confianza (%)',
      valor: (documento) =>
        documento.confianzaExtraccion === null
          ? null
          : Math.round(Number(documento.confianzaExtraccion) * 100),
    },
    { label: 'Subido', valor: (documento) => documento.createdAt },
  ]

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
        <div className="rounded-xl border border-success-border bg-success-soft px-4 py-3 text-sm text-success">
          {state.aviso}
        </div>
      )}

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Bandeja de entrada</CardTitle>
          <div className="flex flex-wrap items-center gap-2">
            <BotonesExportar
              onCsv={() => exportar(columnasExport)}
              onExcel={() => exportar(columnasExport, 'xlsx')}
              exportando={exportando}
            />
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
              <Tabla>
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    {Object.values(COLUMNAS_DOCUMENTOS).map((columna) => (
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
                    const estado = ESTADOS_DOCUMENTO[documento.estado] ?? ESTADOS_DOCUMENTO.pendiente_revision
                    return (
                      <tr
                        key={documento.id}
                        onClick={() => navigate(`/documentos/${documento.id}`)}
                        className="cursor-pointer border-b border-border/60 last:border-0 hover:bg-hover"
                      >
                        <Celda
                          etiqueta="Archivo"
                          className="px-4 py-2.5 font-medium sm:px-5 sm:py-3"
                        >
                          {documento.nombreOriginal}
                        </Celda>
                        <Celda etiqueta="Estado" className="px-4 py-2.5 sm:px-5 sm:py-3">
                          <Badge tone={estado.tono}>{estado.texto}</Badge>
                        </Celda>
                        <Celda
                          etiqueta="Origen"
                          className="px-4 py-2.5 capitalize text-muted sm:px-5 sm:py-3"
                        >
                          {documento.origen}
                        </Celda>
                        <Celda etiqueta="Confianza" className="px-4 py-2.5 text-muted sm:px-5 sm:py-3">
                          {formatearConfianza(documento.confianzaExtraccion)}
                        </Celda>
                        <Celda etiqueta="Subido" className="px-4 py-2.5 text-muted sm:px-5 sm:py-3">
                          {fechaCorta(documento.createdAt)}
                        </Celda>
                      </tr>
                    )
                  })}
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
