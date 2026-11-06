// Vendors
import { ArrowLeft, Loader2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

// Handlers
import { api } from '@/lib/api'

// Components
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Field, Input, Textarea } from '@/components/ui/input'

export default function FichaNuevo() {
  const navigate = useNavigate()
  const [datos, setDatos] = useState({
    nombre: '',
    descripcion: '',
    raciones: '1',
    precioVenta: '',
  })
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)

  function cambiar(campo, valor) {
    setDatos((previos) => ({ ...previos, [campo]: valor }))
  }

  async function crear(event) {
    event.preventDefault()
    setGuardando(true)
    setError(null)

    try {
      const ficha = await api.post('/fichas', {
        nombre: datos.nombre,
        descripcion: datos.descripcion || null,
        raciones: Number(datos.raciones || 1),
        precioVenta: datos.precioVenta === '' ? null : Number(datos.precioVenta),
        margenObjetivo: null,
        lineas: [],
      })
      navigate(`/fichas/${ficha.id}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <Link
          to="/fichas"
          className="inline-flex items-center gap-1 text-sm text-muted hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Fichas técnicas
        </Link>
        <h1 className="mt-2 text-xl font-semibold tracking-tight">Nueva ficha técnica</h1>
        <p className="mt-1 text-sm text-muted">
          Crea la ficha y después añade los ingredientes con sus cantidades.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <Card>
        <CardContent className="pt-5">
          <form onSubmit={crear} className="space-y-4">
            <Field label="Nombre de la elaboración" htmlFor="nombre">
              <Input
                id="nombre"
                required
                placeholder="Salsa de tomate"
                value={datos.nombre}
                onChange={(event) => cambiar('nombre', event.target.value)}
              />
            </Field>
            <Field label="Descripción" htmlFor="descripcion">
              <Textarea
                id="descripcion"
                placeholder="Base para pizzas y pastas"
                value={datos.descripcion}
                onChange={(event) => cambiar('descripcion', event.target.value)}
              />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Raciones" htmlFor="raciones">
                <Input
                  id="raciones"
                  type="number"
                  min="1"
                  value={datos.raciones}
                  onChange={(event) => cambiar('raciones', event.target.value)}
                />
              </Field>
              <Field label="Precio de venta por ración (€)" htmlFor="precio">
                <Input
                  id="precio"
                  type="number"
                  step="0.01"
                  placeholder="8.50"
                  value={datos.precioVenta}
                  onChange={(event) => cambiar('precioVenta', event.target.value)}
                />
              </Field>
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={guardando}>
                {guardando ? <Loader2 className="size-4 animate-spin" /> : null}
                Crear ficha
              </Button>
              <Button asChild variant="outline">
                <Link to="/fichas">Cancelar</Link>
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
