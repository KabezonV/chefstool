// Vendors
import { ChefHat, Loader2 } from 'lucide-react'
import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'

// Handlers
import { api } from '@/lib/api'

// Hooks
import { useAuth } from '@/lib/auth-context'

// Components
import { Button } from '@/components/ui/button'
import { Field, Input } from '@/components/ui/input'

export default function Auth() {
  const navigate = useNavigate()
  const { state } = useLocation()
  const { sesion, entrar, registrar } = useAuth()
  const [modo, setModo] = useState('login')
  const [aviso, setAviso] = useState(state?.aviso ?? null)
  const [datos, setDatos] = useState({
    email: '',
    password: '',
    nombre: '',
    organizacion: '',
  })
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)

  if (sesion) {
    return <Navigate to="/dashboard" replace />
  }

  function cambiar(campo, valor) {
    setDatos((previos) => ({ ...previos, [campo]: valor }))
  }

  async function enviar(event) {
    event.preventDefault()
    setCargando(true)
    setError(null)
    setAviso(null)

    try {
      if (modo === 'recuperar') {
        const respuesta = await api.post('/auth/recuperar', { email: datos.email.trim() })
        setAviso({
          mensaje: respuesta.mensaje,
          token: respuesta.token,
        })
        return
      }

      if (modo === 'login') {
        await entrar(datos.email.trim(), datos.password.trim())
      } else {
        await registrar({
          email: datos.email.trim(),
          password: datos.password.trim(),
          nombre: datos.nombre || null,
          organizacion: datos.organizacion || null,
        })
      }
      navigate('/dashboard', { replace: true })
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
            <p className="text-xs text-muted">
              Control de compras y costes para cocinas profesionales
            </p>
          </div>
        </div>

        <div className="rounded-xl border border-border bg-card p-5">
          <div
            className={
              modo === 'recuperar'
                ? 'hidden'
                : 'mb-4 grid grid-cols-2 gap-1 rounded-lg bg-black/[0.04] p-1'
            }
          >
            {[
              { clave: 'login', texto: 'Iniciar sesión' },
              { clave: 'registro', texto: 'Crear cuenta' },
            ].map((opcion) => (
              <button
                key={opcion.clave}
                type="button"
                onClick={() => {
                  setModo(opcion.clave)
                  setError(null)
                }}
                className={
                  modo === opcion.clave
                    ? 'rounded-md bg-card px-3 py-1.5 text-sm font-medium shadow-sm'
                    : 'rounded-md px-3 py-1.5 text-sm text-muted'
                }
              >
                {opcion.texto}
              </button>
            ))}
          </div>

          {error && (
            <div className="mb-4 rounded-lg border border-danger/30 bg-red-50 px-3 py-2 text-sm text-danger">
              {error}
            </div>
          )}

          {aviso && (
            <div className="mb-4 space-y-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-success">
              <p>{aviso.mensaje}</p>
              {aviso.token && (
                <p>
                  Modo demostración (no hay correo configurado):{' '}
                  <Link
                    to={`/auth/restablecer?token=${aviso.token}`}
                    className="font-medium underline"
                  >
                    abrir el enlace de recuperación
                  </Link>
                </p>
              )}
            </div>
          )}

          <form onSubmit={enviar} className="space-y-3">
            {modo === 'registro' && (
              <>
                <Field label="Tu nombre" htmlFor="nombre">
                  <Input
                    id="nombre"
                    placeholder="Ana García"
                    value={datos.nombre}
                    onChange={(event) => cambiar('nombre', event.target.value)}
                  />
                </Field>
                <Field label="Nombre de la cocina u organización" htmlFor="organizacion">
                  <Input
                    id="organizacion"
                    placeholder="Restaurante La Sal"
                    value={datos.organizacion}
                    onChange={(event) => cambiar('organizacion', event.target.value)}
                  />
                </Field>
              </>
            )}

            <Field label="Correo electrónico" htmlFor="email">
              <Input
                id="email"
                type="email"
                required
                autoComplete="email"
                autoCapitalize="none"
                autoCorrect="off"
                spellCheck={false}
                inputMode="email"
                placeholder="tu@cocina.es"
                value={datos.email}
                onChange={(event) => cambiar('email', event.target.value)}
              />
            </Field>

            {modo !== 'recuperar' && (
              <Field label="Contraseña" htmlFor="password">
                <Input
                  id="password"
                  type="password"
                  required
                  minLength={modo === 'registro' ? 8 : undefined}
                  autoComplete={modo === 'login' ? 'current-password' : 'new-password'}
                  autoCapitalize="none"
                  autoCorrect="off"
                  spellCheck={false}
                  placeholder="Mínimo 8 caracteres"
                  value={datos.password}
                  onChange={(event) => cambiar('password', event.target.value)}
                />
              </Field>
            )}

            {modo === 'login' && (
              <button
                type="button"
                onClick={() => {
                  setModo('recuperar')
                  setError(null)
                  setAviso(null)
                }}
                className="text-xs font-medium text-muted hover:text-foreground"
              >
                ¿Has olvidado la contraseña?
              </button>
            )}

            <Button type="submit" variant="accent" className="w-full" disabled={cargando}>
              {cargando ? <Loader2 className="size-4 animate-spin" /> : null}
              {modo === 'login' && 'Entrar'}
              {modo === 'registro' && 'Crear cuenta'}
              {modo === 'recuperar' && 'Enviar enlace'}
            </Button>

            {modo === 'recuperar' && (
              <button
                type="button"
                onClick={() => {
                  setModo('login')
                  setAviso(null)
                  setError(null)
                }}
                className="w-full text-xs font-medium text-muted hover:text-foreground"
              >
                Volver a iniciar sesión
              </button>
            )}
          </form>
        </div>

        <p className="text-center text-xs text-muted">
          Al crear una cuenta se crea también tu organización y quedas como propietario.{' '}
          <Link to="/privacidad" className="font-medium hover:text-foreground hover:underline">
            Aviso de privacidad
          </Link>
        </p>
      </div>
    </div>
  )
}
