import { useEffect, useState } from 'react'
import { Check, Loader2, MailPlus, X } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth-context'
import { fechaCorta } from '@/lib/formato'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select } from '@/components/ui/input'

const roles = [
  { valor: 'propietario', texto: 'Propietario' },
  { valor: 'admin', texto: 'Administrador' },
  { valor: 'miembro', texto: 'Miembro' },
]

export default function Usuarios() {
  const { sesion } = useAuth()
  const puedeGestionar = sesion?.rol === 'propietario' || sesion?.rol === 'admin'

  const [miembros, setMiembros] = useState([])
  const [invitaciones, setInvitaciones] = useState([])
  const [invitacion, setInvitacion] = useState({ email: '', rol: 'miembro' })
  const [cargando, setCargando] = useState(true)
  const [recarga, setRecarga] = useState(0)
  const [enviando, setEnviando] = useState(false)
  const [aviso, setAviso] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true

    const equipo = api.get('/usuarios')
    const pendientes = puedeGestionar
      ? api.get('/usuarios/invitaciones')
      : Promise.resolve([])

    Promise.all([equipo, pendientes])
      .then(([lista, invitacionesPendientes]) => {
        if (!activo) return
        setMiembros(lista)
        setInvitaciones(invitacionesPendientes)
        setError(null)
      })
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))

    return () => {
      activo = false
    }
  }, [puedeGestionar, recarga])

  async function invitar(event) {
    event.preventDefault()
    setEnviando(true)
    setError(null)
    setAviso(null)

    try {
      const resultado = await api.post('/usuarios/invitar', invitacion)
      setAviso(
        resultado.estado === 'anadido'
          ? `${resultado.email} ya tenía cuenta: se ha añadido al equipo.`
          : `Invitación guardada para ${resultado.email}. Entrará en tu organización cuando se registre con ese correo.`,
      )
      setInvitacion({ email: '', rol: 'miembro' })
      setRecarga((valor) => valor + 1)
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  async function cancelar(id) {
    setError(null)
    try {
      await api.delete(`/usuarios/invitaciones/${id}`)
      setRecarga((valor) => valor + 1)
    } catch (err) {
      setError(err.message)
    }
  }

  async function cambiarRol(id, rol) {
    setError(null)
    try {
      const actualizado = await api.put(`/usuarios/${id}/rol`, { rol })
      setMiembros((previos) =>
        previos.map((miembro) => (miembro.id === id ? actualizado : miembro)),
      )
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Usuarios</h1>
        <p className="mt-1 text-sm text-muted">
          Quién tiene acceso a esta organización y con qué permisos.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-red-50 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      {aviso && (
        <div className="flex items-start justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-success">
          <span className="flex items-center gap-2">
            <Check className="size-4" />
            {aviso}
          </span>
          <button type="button" onClick={() => setAviso(null)} aria-label="Cerrar aviso">
            <X className="size-4" />
          </button>
        </div>
      )}

      {puedeGestionar && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <MailPlus className="size-4" />
              Invitar a tu equipo
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={invitar} className="flex flex-wrap items-end gap-3">
              <div className="min-w-56 flex-1">
                <Field label="Correo electrónico" htmlFor="invitar-email">
                  <Input
                    id="invitar-email"
                    type="email"
                    required
                    placeholder="compañero@cocina.es"
                    value={invitacion.email}
                    onChange={(event) =>
                      setInvitacion((previos) => ({ ...previos, email: event.target.value }))
                    }
                  />
                </Field>
              </div>
              <div className="w-40">
                <Field label="Rol" htmlFor="invitar-rol">
                  <Select
                    id="invitar-rol"
                    value={invitacion.rol}
                    onChange={(event) =>
                      setInvitacion((previos) => ({ ...previos, rol: event.target.value }))
                    }
                  >
                    {roles.map((rol) => (
                      <option key={rol.valor} value={rol.valor}>
                        {rol.texto}
                      </option>
                    ))}
                  </Select>
                </Field>
              </div>
              <Button type="submit" disabled={enviando}>
                {enviando ? <Loader2 className="size-4 animate-spin" /> : null}
                Invitar
              </Button>
            </form>
            <p className="mt-3 text-xs text-muted">
              Si ya tiene cuenta, se añade al momento. Si no, entrará en tu organización
              automáticamente cuando se registre con ese correo.
            </p>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Miembros</CardTitle>
          <Badge>{cargando ? 'Cargando…' : miembros.length + ' miembros'}</Badge>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-y border-border text-left text-xs text-muted">
                  <th className="px-5 py-2 font-medium">Nombre</th>
                  <th className="px-3 py-2 font-medium">Correo</th>
                  <th className="w-48 px-3 py-2 font-medium">Rol</th>
                </tr>
              </thead>
              <tbody>
                {miembros.map((miembro) => (
                  <tr key={miembro.id} className="border-b border-border/60 last:border-0">
                    <td className="px-5 py-2 font-medium">{miembro.nombre ?? '—'}</td>
                    <td className="px-3 py-2 text-muted">{miembro.email}</td>
                    <td className="px-3 py-2">
                      {puedeGestionar ? (
                        <Select
                          value={miembro.rol}
                          onChange={(event) => cambiarRol(miembro.id, event.target.value)}
                        >
                          {roles.map((rol) => (
                            <option key={rol.valor} value={rol.valor}>
                              {rol.texto}
                            </option>
                          ))}
                        </Select>
                      ) : (
                        <Badge>{miembro.rol}</Badge>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {puedeGestionar && invitaciones.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Invitaciones pendientes</CardTitle>
            <Badge tone="warning">{invitaciones.length}</Badge>
          </CardHeader>
          <CardContent className="space-y-2">
            {invitaciones.map((pendiente) => (
              <div
                key={pendiente.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-black/[0.02] px-3 py-2 text-sm"
              >
                <div>
                  <p className="font-medium">{pendiente.email}</p>
                  <p className="text-xs text-muted">
                    {pendiente.rol} · invitado el {fechaCorta(pendiente.createdAt)}
                  </p>
                </div>
                <Button variant="ghost" size="sm" onClick={() => cancelar(pendiente.id)}>
                  <X className="size-4" />
                  Cancelar
                </Button>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <p className="text-xs text-muted">
        Los miembros pueden trabajar con productos, albaranes y fichas; administradores y
        propietarios además gestionan el equipo y la configuración.
      </p>
    </div>
  )
}
