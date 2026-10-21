import { useEffect, useState } from 'react'
import { Check, Loader2 } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth-context'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input } from '@/components/ui/input'

export default function Configuracion() {
  const { sesion } = useAuth()
  const [organizacion, setOrganizacion] = useState(null)
  const [nombre, setNombre] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [guardado, setGuardado] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true

    api
      .get('/organizacion')
      .then((datos) => {
        if (!activo) return
        setOrganizacion(datos)
        setNombre(datos.nombre)
      })
      .catch((err) => activo && setError(err.message))

    return () => {
      activo = false
    }
  }, [])

  async function guardar(event) {
    event.preventDefault()
    setGuardando(true)
    setError(null)
    setGuardado(false)

    try {
      const actualizada = await api.put('/organizacion', { nombre })
      setOrganizacion(actualizada)
      setGuardado(true)
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Configuración</h1>
        <p className="mt-1 text-sm text-muted">Datos de tu organización y tu cuenta.</p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Organización</CardTitle>
          {organizacion && <Badge>{organizacion.id.slice(0, 8)}</Badge>}
        </CardHeader>
        <CardContent>
          <form onSubmit={guardar} className="space-y-4">
            <Field label="Nombre" htmlFor="organizacion-nombre">
              <Input
                id="organizacion-nombre"
                value={nombre}
                onChange={(event) => {
                  setNombre(event.target.value)
                  setGuardado(false)
                }}
              />
            </Field>
            <div className="flex items-center gap-3">
              <Button type="submit" disabled={guardando || !nombre.trim()}>
                {guardando ? <Loader2 className="size-4 animate-spin" /> : null}
                Guardar
              </Button>
              {guardado && (
                <span className="inline-flex items-center gap-1 text-sm text-success">
                  <Check className="size-4" />
                  Guardado
                </span>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Tu cuenta</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <div className="flex items-center justify-between">
            <span className="text-muted">Nombre</span>
            <span>{sesion?.nombre ?? '—'}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-muted">Correo</span>
            <span>{sesion?.email ?? '—'}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-muted">Rol</span>
            <Badge>{sesion?.rol ?? '—'}</Badge>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
