// Vendors
import { Loader2, MapPin, Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'

// Utils
import { activarLocal } from '@/lib/locales'
import { esGestor } from '@/lib/permisos'

// Handlers
import { api } from '@/lib/api'

// Hooks
import { useAuth } from '@/lib/auth-context'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input } from '@/components/ui/input'

export default function Locales() {
  const { sesion } = useAuth()
  const gestor = esGestor(sesion?.rol)
  const [locales, setLocales] = useState([])
  const [nombre, setNombre] = useState('')
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true
    api
      .get('/locales')
      .then((lista) => activo && setLocales(lista))
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [])

  async function crear(event) {
    event.preventDefault()
    setGuardando(true)
    setError(null)
    try {
      const creado = await api.post('/locales', { nombre: nombre.trim() })
      setLocales((previos) => [...previos, creado].sort((a, b) => a.nombre.localeCompare(b.nombre)))
      setNombre('')
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function eliminar(local) {
    setError(null)
    try {
      await api.delete(`/locales/${local.id}`)
      setLocales((previos) => previos.filter((otro) => otro.id !== local.id))
    } catch (err) {
      setError(err.message)
    }
  }

  async function usar(local) {
    setError(null)
    try {
      await activarLocal(local.id)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="flex items-center gap-2 text-xl font-semibold tracking-tight">
          <MapPin className="size-5" />
          Locales
        </h1>
        <p className="mt-1 text-sm text-muted">
          Cada local lleva sus propios albaranes, compras y productos en uso. Los datos no se
          mezclan entre locales.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      {gestor && (
        <Card>
          <CardHeader>
            <CardTitle>Añadir un local</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={crear} className="flex flex-wrap items-end gap-3">
              <Field label="Nombre del local" htmlFor="local-nombre">
                <Input
                  id="local-nombre"
                  value={nombre}
                  onChange={(event) => setNombre(event.target.value)}
                  placeholder="Terraza, Cocina central…"
                  required
                />
              </Field>
              <Button type="submit" disabled={guardando || nombre.trim() === ''}>
                {guardando ? <Loader2 className="size-4 animate-spin" /> : <Plus className="size-4" />}
                Añadir local
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Tus locales</CardTitle>
          <Badge>{cargando ? 'Cargando…' : `${locales.length} locales`}</Badge>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {!cargando && locales.length === 0 ? (
            <p className="px-5 pb-6 text-sm text-muted">Todavía no hay locales.</p>
          ) : (
            <ul className="divide-y divide-border">
              {locales.map((local) => (
                <li
                  key={local.id}
                  className="flex flex-wrap items-center justify-between gap-3 px-5 py-3"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{local.nombre}</p>
                    {local.id === sesion?.localId && (
                      <p className="text-xs text-success">Local activo</p>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={local.id === sesion?.localId}
                      onClick={() => usar(local)}
                    >
                      Usar este local
                    </Button>
                    {gestor && locales.length > 1 && local.id !== sesion?.localId && (
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={`Eliminar ${local.nombre}`}
                        onClick={() => eliminar(local)}
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
