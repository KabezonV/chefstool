import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select } from '@/components/ui/input'

const roles = [
  { valor: 'propietario', texto: 'Propietario' },
  { valor: 'admin', texto: 'Administrador' },
  { valor: 'miembro', texto: 'Miembro' },
]

export default function Usuarios() {
  const [miembros, setMiembros] = useState([])
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    let activo = true

    api
      .get('/usuarios')
      .then((datos) => activo && setMiembros(datos))
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))

    return () => {
      activo = false
    }
  }, [])

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

      <Card>
        <CardHeader>
          <CardTitle>Miembros</CardTitle>
          <Badge>{cargando ? 'Cargando…' : `${miembros.length} miembros`}</Badge>
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
                    <td className="px-5 py-2 font-medium">
                      {miembro.nombre ?? '—'}
                    </td>
                    <td className="px-3 py-2 text-muted">{miembro.email}</td>
                    <td className="px-3 py-2">
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
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      <p className="text-xs text-muted">
        La organización necesita al menos un propietario. La invitación de nuevos
        miembros por correo llegará con el envío de emails transaccionales.
      </p>
    </div>
  )
}
