import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ChefHat, KeyRound, Loader2 } from 'lucide-react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Field, Input } from '@/components/ui/input'

export default function Restablecer() {
  const navigate = useNavigate()
  const [parametros] = useSearchParams()
  const token = parametros.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [repetida, setRepetida] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)

  async function enviar(event) {
    event.preventDefault()
    setError(null)

    if (password !== repetida) {
      setError('Las dos contraseñas no coinciden')
      return
    }

    setCargando(true)
    try {
      const respuesta = await api.post('/auth/restablecer', { token, password })
      navigate('/auth', { replace: true, state: { aviso: respuesta.mensaje } })
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="grid min-h-screen place-items-center bg-background px-4">
      <div className="w-full max-w-md space-y-6">
        <div className="flex items-center gap-2">
          <span className="grid size-10 place-items-center rounded-xl bg-primary text-primary-foreground">
            <ChefHat className="size-5" />
          </span>
          <div>
            <p className="text-lg font-semibold tracking-tight">Chefstool</p>
            <p className="text-xs text-muted">Nueva contraseña</p>
          </div>
        </div>

        <div className="rounded-xl border border-border bg-card p-5">
          {!token ? (
            <div className="space-y-3 text-sm">
              <p className="font-medium">El enlace no es válido</p>
              <p className="text-muted">
                Falta el código de recuperación. Pide un enlace nuevo desde la pantalla de
                acceso.
              </p>
              <Link to="/auth" className="inline-block font-medium text-foreground hover:underline">
                Volver al acceso
              </Link>
            </div>
          ) : (
            <form onSubmit={enviar} className="space-y-3">
              <p className="flex items-center gap-2 text-sm font-medium">
                <KeyRound className="size-4" />
                Elige una contraseña nueva
              </p>

              {error && (
                <div className="rounded-lg border border-danger/30 bg-red-50 px-3 py-2 text-sm text-danger">
                  {error}
                </div>
              )}

              <Field label="Contraseña nueva" htmlFor="password">
                <Input
                  id="password"
                  type="password"
                  required
                  minLength={8}
                  autoComplete="new-password"
                  placeholder="Mínimo 8 caracteres"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </Field>

              <Field label="Repite la contraseña" htmlFor="repetida">
                <Input
                  id="repetida"
                  type="password"
                  required
                  minLength={8}
                  autoComplete="new-password"
                  value={repetida}
                  onChange={(event) => setRepetida(event.target.value)}
                />
              </Field>

              <Button type="submit" variant="accent" className="w-full" disabled={cargando}>
                {cargando ? <Loader2 className="size-4 animate-spin" /> : null}
                Guardar contraseña
              </Button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
