import { useState } from 'react'
import { Sparkles } from 'lucide-react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

/**
 * Tarjeta de bienvenida para organizaciones sin compras: rellena catalogo,
 * proveedores, albaranes de ejemplo y fichas para poder probar la app.
 */
export default function DatosEjemplo({ onCargado }) {
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)

  async function cargar() {
    setCargando(true)
    setError(null)
    try {
      await api.post('/demo/datos')
      onCargado?.()
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sparkles className="size-4" />
          Empezar con datos de ejemplo
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3 text-sm text-muted">
        <p>
          Todavía no hay compras. Puedes cargar un catálogo de prueba con 8 productos, 3
          proveedores, 2 clientes, 3 albaranes confirmados (con su alerta de subida de precio) y
          2 fichas técnicas.
        </p>
        <p>Es reversible: puedes borrar cada registro desde sus listados.</p>
        {error && <p className="text-danger">{error}</p>}
        <Button onClick={cargar} disabled={cargando} variant="accent">
          {cargando ? 'Cargando datos…' : 'Cargar datos de ejemplo'}
        </Button>
      </CardContent>
    </Card>
  )
}
