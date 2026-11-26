// Vendors
import { Check, Clock3, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'

// Utils
import { fechaCorta, moneda } from '@/lib/formato'

// Handlers
import { api } from '@/lib/api'

// Hooks
import { useAuth } from '@/lib/auth-context'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Textarea } from '@/components/ui/input'

const ESTADO = {
  PENDIENTE: { texto: 'Pendiente', tono: 'warning' },
  APROBADA: { texto: 'Aprobada', tono: 'success' },
  RECHAZADA: { texto: 'Rechazada', tono: 'danger' },
}

const FORMULARIO_INICIAL = {
  concepto: '',
  descripcion: '',
  proveedor: '',
  importe: '',
}

export default function SolicitudesCompra() {
  const { sesion } = useAuth()
  const [solicitudes, setSolicitudes] = useState([])
  const [formulario, setFormulario] = useState(FORMULARIO_INICIAL)
  const [motivos, setMotivos] = useState({})
  const [rechazando, setRechazando] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const puedeResolver = ['propietario', 'admin'].includes(sesion?.rol)

  const cargar = useCallback(async () => {
    try {
      setSolicitudes(await api.get('/solicitudes-compra'))
      setError('')
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    let activa = true
    api.get('/solicitudes-compra')
      .then((datos) => {
        if (activa) {
          setSolicitudes(datos)
          setError('')
        }
      })
      .catch((err) => {
        if (activa) setError(err.message)
      })
      .finally(() => {
        if (activa) setCargando(false)
      })

    return () => {
      activa = false
    }
  }, [])

  function cambiarCampo(event) {
    const { name, value } = event.target
    setFormulario((actual) => ({ ...actual, [name]: value }))
  }

  async function crear(event) {
    event.preventDefault()
    setGuardando(true)
    setError('')
    try {
      await api.post('/solicitudes-compra', {
        ...formulario,
        importe: Number(formulario.importe),
      })
      setFormulario(FORMULARIO_INICIAL)
      await cargar()
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function aprobar(id) {
    setError('')
    try {
      await api.post(`/solicitudes-compra/${id}/aprobar`, {})
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  async function rechazar(id) {
    setError('')
    try {
      await api.post(`/solicitudes-compra/${id}/rechazar`, {
        motivo: motivos[id] ?? '',
      })
      setRechazando(null)
      setMotivos((actual) => ({ ...actual, [id]: '' }))
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">Solicitudes de compra</h1>
        <p className="mt-1 text-sm text-muted">
          Pide autorización antes de comprometer un gasto. Aprobar una solicitud no realiza
          automáticamente ningún pedido ni pago.
        </p>
      </header>

      {error && (
        <div role="alert" className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Nueva solicitud</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={crear}>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="¿Qué necesitas?" htmlFor="solicitud-concepto">
                <Input
                  id="solicitud-concepto"
                  name="concepto"
                  value={formulario.concepto}
                  onChange={cambiarCampo}
                  maxLength={160}
                  required
                />
              </Field>
              <Field label="Importe estimado (€)" htmlFor="solicitud-importe">
                <Input
                  id="solicitud-importe"
                  name="importe"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={formulario.importe}
                  onChange={cambiarCampo}
                  required
                />
              </Field>
              <Field label="Proveedor (opcional)" htmlFor="solicitud-proveedor">
                <Input
                  id="solicitud-proveedor"
                  name="proveedor"
                  value={formulario.proveedor}
                  onChange={cambiarCampo}
                  maxLength={200}
                />
              </Field>
              <Field label="Detalles (opcional)" htmlFor="solicitud-descripcion">
                <Textarea
                  id="solicitud-descripcion"
                  name="descripcion"
                  value={formulario.descripcion}
                  onChange={cambiarCampo}
                  maxLength={2000}
                />
              </Field>
            </div>
            <Button type="submit" disabled={guardando}>
              {guardando ? 'Enviando…' : 'Enviar para aprobación'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Solicitudes de este local</CardTitle>
          <Badge>{cargando ? 'Cargando…' : `${solicitudes.length} solicitudes`}</Badge>
        </CardHeader>
        <CardContent className="space-y-3">
          {!cargando && !error && solicitudes.length === 0 && (
            <p className="text-sm text-muted">Todavía no hay solicitudes de compra.</p>
          )}
          {solicitudes.map((solicitud) => {
            const estado = ESTADO[solicitud.estado] ?? ESTADO.PENDIENTE
            const puedeActuar =
              puedeResolver &&
              solicitud.estado === 'PENDIENTE' &&
              solicitud.solicitanteId !== sesion?.usuarioId

            return (
              <article key={solicitud.id} className="rounded-xl border border-border p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h2 className="font-medium">{solicitud.concepto}</h2>
                    <p className="mt-1 text-sm text-muted">
                      {solicitud.proveedor || 'Sin proveedor'} · Solicitó{' '}
                      {solicitud.solicitanteNombre || 'usuario'} · {fechaCorta(solicitud.createdAt)}
                    </p>
                    {solicitud.descripcion && (
                      <p className="mt-2 whitespace-pre-wrap text-sm">{solicitud.descripcion}</p>
                    )}
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <strong>{moneda(solicitud.importe)}</strong>
                    <Badge tone={estado.tono}>{estado.texto}</Badge>
                  </div>
                </div>

                {solicitud.motivoRechazo && (
                  <p className="mt-3 rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">
                    Rechazada por {solicitud.revisadaPorNombre || 'un administrador'}:{' '}
                    {solicitud.motivoRechazo}
                  </p>
                )}
                {solicitud.estado === 'APROBADA' && (
                  <p className="mt-2 text-xs text-muted">
                    Aprobada por {solicitud.revisadaPorNombre || 'un administrador'}.
                  </p>
                )}

                {puedeActuar && (
                  <div className="mt-4 flex flex-wrap items-center gap-2">
                    <Button size="sm" onClick={() => aprobar(solicitud.id)}>
                      <Check className="size-4" />
                      Aprobar
                    </Button>
                    {rechazando === solicitud.id ? (
                      <div className="flex w-full flex-wrap items-end gap-2">
                        <Field label="Motivo del rechazo" htmlFor={`motivo-${solicitud.id}`}>
                          <Input
                            id={`motivo-${solicitud.id}`}
                            value={motivos[solicitud.id] ?? ''}
                            onChange={(event) =>
                              setMotivos((actual) => ({
                                ...actual,
                                [solicitud.id]: event.target.value,
                              }))
                            }
                            maxLength={1000}
                            required
                          />
                        </Field>
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={!(motivos[solicitud.id] ?? '').trim()}
                          onClick={() => rechazar(solicitud.id)}
                        >
                          <X className="size-4" />
                          Confirmar rechazo
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => setRechazando(null)}
                        >
                          Cancelar
                        </Button>
                      </div>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setRechazando(solicitud.id)}
                      >
                        <X className="size-4" />
                        Rechazar
                      </Button>
                    )}
                  </div>
                )}
                {puedeResolver &&
                  solicitud.estado === 'PENDIENTE' &&
                  solicitud.solicitanteId === sesion?.usuarioId && (
                    <p className="mt-3 flex items-center gap-1 text-xs text-muted">
                      <Clock3 className="size-3.5" />
                      No puedes aprobar tu propia solicitud.
                    </p>
                  )}
              </article>
            )
          })}
        </CardContent>
      </Card>
    </div>
  )
}
