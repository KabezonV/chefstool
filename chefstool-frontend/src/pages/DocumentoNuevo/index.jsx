// Vendors
import { ArrowLeft, Loader2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

// Utils
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

  async function enviar(archivos, origen) {
    setSubiendo(true)
    setError(null)

    try {
      if (archivos.length === 0) {
        const datos = new FormData()
        datos.append('origen', 'demo')
        const documento = await api.subir('/documentos', datos)
        navigate(`/documentos/${documento.id}`)
        return
      }

      const creados = []
      for (const archivo of archivos) {
        const datos = new FormData()
        datos.append('archivo', archivo)
        datos.append('origen', origen)
        creados.push(await api.subir('/documentos', datos))
      }

      navigate(creados.length === 1 ? `/documentos/${creados[0].id}` : '/documentos')
    } catch (err) {
      setError(err.message)
    } finally {
      setSubiendo(false)
    }
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
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <input
        ref={camaraRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(event) => alElegir(event, 'camara')}
      />
      <input
        ref={archivoRef}
        type="file"
        accept="image/*,application/pdf"
        multiple
        className="hidden"
        onChange={(event) => alElegir(event, 'subida')}
      />

      <div className="grid gap-4 md:grid-cols-3">
        {OPCIONES_ALTA.map(({ clave, icono: Icono, titulo, texto }) => (
          <Card key={clave} className="flex flex-col">
            <CardContent className="flex flex-1 flex-col gap-3 pt-5">
              <span className="grid size-10 place-items-center rounded-lg bg-black/[0.05]">
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
