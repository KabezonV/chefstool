// Vendors
import { ArrowLeft, Loader2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

// Utils
import { revisarCalidad } from './utils/calidad'
import { OPCIONES_ALTA } from './utils/opciones'

// Handlers
import { api } from '@/lib/api'

// Components
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

export default function DocumentoNuevo() {
  const navigate = useNavigate()
  const camaraRef = useRef(null)
  const archivoRef = useRef(null)
  const [subiendo, setSubiendo] = useState(false)
  const [error, setError] = useState(null)
  const [avisos, setAvisos] = useState([])
  const [pendiente, setPendiente] = useState(null)

  async function subir(archivos, origen) {
    setSubiendo(true)
    setError(null)

    try {
      // Varias fotos van en una sola subida: el backend las junta en un PDF y la
      // revision es una unica compra, no una por hoja.
      const datos = new FormData()
      for (const archivo of archivos) datos.append('archivo', archivo)
      datos.append('origen', origen)

      const documento = await api.subir('/documentos', datos)
      navigate(`/documentos/${documento.id}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubiendo(false)
    }
  }

  async function enviar(archivos, origen, { confirmado = false } = {}) {
    setError(null)
    setAvisos([])
    setPendiente(null)

    if (archivos.length === 0) {
      setSubiendo(true)
      try {
        const datos = new FormData()
        datos.append('origen', 'demo')
        const documento = await api.subir('/documentos', datos)
        navigate(`/documentos/${documento.id}`)
      } catch (err) {
        setError(err.message)
      } finally {
        setSubiendo(false)
      }
      return
    }

    // El aviso tiene que verse antes de procesar: si se muestra mientras se sube, la
    // navegación a la revisión lo esconde y el usuario nunca llega a enterarse.
    if (!confirmado) {
      setSubiendo(true)
      const encontrados = await revisarCalidad(archivos)
      setSubiendo(false)
      if (encontrados.length > 0) {
        setAvisos(encontrados)
        setPendiente({ archivos, origen })
        return
      }
    }

    await subir(archivos, origen)
  }

  function alElegir(event, origen) {
    const archivos = Array.from(event.target.files ?? [])
    event.target.value = ''
    if (archivos.length > 0) {
      enviar(archivos, origen)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <Link
          to="/documentos"
          className="inline-flex items-center gap-1 text-sm text-muted hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Documentos
        </Link>
        <h1 className="mt-2 text-xl font-semibold tracking-tight">Añadir documento</h1>
        <p className="mt-1 text-sm text-muted">
          Elige cómo quieres incorporar el albarán. El texto se extrae automáticamente
          y podrás revisarlo antes de confirmar.
        </p>
        <p className="mt-2 text-xs text-muted">
          Para que se lea bien: apoya el móvil, haz la foto recta y que se vea la hoja
          entera con buena luz.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      {avisos.length > 0 && (
        <div
          role="alert"
          className="rounded-xl border border-warning-border bg-warning-soft px-4 py-3 text-sm text-warning"
        >
          <p className="font-medium">Estas imágenes tienen poca resolución</p>
          <ul className="mt-1 list-disc pl-5">
            {avisos.map((imagen) => (
              <li key={imagen.nombre}>
                {imagen.nombre} ({imagen.ancho} px de ancho)
              </li>
            ))}
          </ul>
          <p className="mt-1">
            El texto pequeño puede no leerse bien. Sube la foto original (no reenviada por
            mensajería) o, mejor, el PDF del proveedor.
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setAvisos([])
                setPendiente(null)
                archivoRef.current?.click()
              }}
            >
              Elegir otra imagen
            </Button>
            <Button
              size="sm"
              disabled={subiendo}
              onClick={() => enviar(pendiente.archivos, pendiente.origen, { confirmado: true })}
            >
              Subir igualmente
            </Button>
          </div>
        </div>
      )}

      <input
        ref={camaraRef}
        type="file"
        accept="image/jpeg,image/png"
        capture="environment"
        className="hidden"
        onChange={(event) => alElegir(event, 'camara')}
      />
      <input
        ref={archivoRef}
        type="file"
        accept="image/jpeg,image/png,application/pdf"
        multiple
        className="hidden"
        onChange={(event) => alElegir(event, 'subida')}
      />

      <div className="grid gap-4 md:grid-cols-3">
        {OPCIONES_ALTA.map(({ clave, icono: Icono, titulo, texto }) => (
          <Card key={clave} className="flex flex-col">
            <CardContent className="flex flex-1 flex-col gap-3 pt-5">
              <span className="grid size-10 place-items-center rounded-lg bg-sunken">
                <Icono className="size-5" />
              </span>
              <div>
                <p className="text-sm font-semibold">{titulo}</p>
                <p className="mt-1 text-xs text-muted">{texto}</p>
              </div>
              <Button
                className="mt-auto w-full"
                variant={clave === 'demo' ? 'outline' : 'default'}
                disabled={subiendo}
                onClick={() => {
                  if (clave === 'camara') camaraRef.current?.click()
                  else if (clave === 'archivo') archivoRef.current?.click()
                  else enviar([], 'demo')
                }}
              >
                {subiendo ? (
                  <>
                    <Loader2 className="size-4 animate-spin" />
                    Procesando…
                  </>
                ) : (
                  titulo
                )}
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      {subiendo && (
        <p className="text-sm text-muted">
          Subiendo y extrayendo los datos del documento…
        </p>
      )}
    </div>
  )
}
