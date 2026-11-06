// Vendors
import { Check, Loader2, ShieldCheck, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

// Utils
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

export default function Configuracion() {
  const { sesion, salir } = useAuth()
  const navigate = useNavigate()
  const puedeEditar = esGestor(sesion?.rol)
  const [organizacion, setOrganizacion] = useState(null)
  const [nombre, setNombre] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [guardado, setGuardado] = useState(false)
  const [error, setError] = useState(null)
  const [passwordBaja, setPasswordBaja] = useState('')
  const [bajaAbierta, setBajaAbierta] = useState(false)
  const [dandoBaja, setDandoBaja] = useState(false)

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

  async function descargarDatos() {
    setError(null)
    try {
      const datos = await api.get('/auth/datos')
      const blob = new Blob([JSON.stringify(datos, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = 'chefstool-mis-datos.json'
      document.body.appendChild(enlace)
      enlace.click()
      enlace.remove()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message)
    }
  }

  async function darseDeBaja(event) {
    event.preventDefault()
    setError(null)
    setDandoBaja(true)
    try {
      await api.delete('/auth/cuenta', { password: passwordBaja.trim() })
      salir()
      navigate('/auth', { replace: true, state: { aviso: 'Tu cuenta se ha eliminado' } })
    } catch (err) {
      setError(err.message)
      setDandoBaja(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Configuración</h1>
        <p className="mt-1 text-sm text-muted">Datos de tu organización y tu cuenta.</p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
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
                disabled={!puedeEditar}
                value={nombre}
                onChange={(event) => {
                  setNombre(event.target.value)
                  setGuardado(false)
                }}
              />
            </Field>
            <div className="flex items-center gap-3">
              <Button type="submit" disabled={!puedeEditar || guardando || !nombre.trim()}>
                {guardando ? <Loader2 className="size-4 animate-spin" /> : null}
                Guardar
              </Button>
              {!puedeEditar && (
                <span className="text-xs text-muted">
                  Solo propietarios y administradores pueden cambiar el nombre.
                </span>
              )}
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

      <Card>
        <CardHeader>
          <CardTitle>
            <span className="inline-flex items-center gap-2">
              <ShieldCheck className="size-4" />
              Tus datos y privacidad
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4 text-sm">
          <div className="flex flex-wrap items-center gap-3">
            <Button variant="outline" onClick={descargarDatos}>
              Descargar mis datos (JSON)
            </Button>
            <span className="text-xs text-muted">
              Incluye tu cuenta y las organizaciones a las que perteneces. El resto de datos
              operativos se exportan desde cada listado (Excel o CSV).
            </span>
          </div>

          <div className="rounded-xl border border-danger/30 bg-danger-soft/60 p-4">
            <p className="font-medium text-danger">Darte de baja</p>
            <p className="mt-1 text-xs text-danger/80">
              Se elimina tu cuenta y tu acceso. Si eres el único miembro de la organización se
              borran también sus proveedores, productos, compras, albaranes y fichas. Esta acción
              no se puede deshacer. Si hay más personas, nombra antes otro propietario.
            </p>

            {!bajaAbierta ? (
              <Button
                variant="outline"
                className="mt-3 border-danger/40 text-danger hover:bg-danger-soft"
                onClick={() => setBajaAbierta(true)}
              >
                <Trash2 className="size-4" />
                Quiero darme de baja
              </Button>
            ) : (
              <form onSubmit={darseDeBaja} className="mt-3 space-y-3">
                <Field label="Confirma tu contraseña" htmlFor="baja-password">
                  <Input
                    id="baja-password"
                    type="password"
                    autoComplete="current-password"
                    value={passwordBaja}
                    onChange={(event) => setPasswordBaja(event.target.value)}
                  />
                </Field>
                <div className="flex items-center gap-2">
                  <Button
                    type="submit"
                    className="bg-danger text-white hover:bg-danger/90"
                    disabled={dandoBaja || !passwordBaja.trim()}
                  >
                    {dandoBaja ? <Loader2 className="size-4 animate-spin" /> : null}
                    Eliminar mi cuenta
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => {
                      setBajaAbierta(false)
                      setPasswordBaja('')
                    }}
                  >
                    Cancelar
                  </Button>
                </div>
              </form>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
