import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, TrendingDown } from 'lucide-react'
import { api } from '@/lib/api'
import { fechaCorta, moneda } from '@/lib/formato'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function ProductoDetalle() {
  const { id } = useParams()
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    let activo = true

    api
      .get(`/productos/${id}/precios`)
      .then((detalle) => {
        if (!activo) return
        setDatos(detalle)
        setError(null)
      })
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))

    return () => {
      activo = false
    }
  }, [id])

  if (cargando) {
    return <div className="mx-auto max-w-4xl"><p className="text-sm text-muted">Cargando producto…</p></div>
  }

  if (!datos) {
    return (
      <div className="mx-auto max-w-4xl space-y-4">
        <p className="text-sm text-danger">{error ?? 'Producto no encontrado'}</p>
        <Button asChild variant="outline">
          <Link to="/productos">Volver a productos</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <Link
          to="/productos"
          className="inline-flex items-center gap-1 text-sm text-muted hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Productos
        </Link>
        <div className="mt-2 flex flex-wrap items-center gap-3">
          <h1 className="text-xl font-semibold tracking-tight">{datos.nombre}</h1>
          <Badge>unidad base: {datos.unidadBase}</Badge>
        </div>
      </div>

      {datos.mejorProveedor ? (
        <Card>
          <CardContent className="flex flex-wrap items-center justify-between gap-3 pt-5">
            <div className="flex items-center gap-3">
              <span className="grid size-10 place-items-center rounded-lg bg-emerald-50 text-success">
                <TrendingDown className="size-5" />
              </span>
              <div>
                <p className="text-xs text-muted">Mejor precio actual</p>
                <p className="text-lg font-semibold">
                  {moneda(datos.mejorPrecio)} / {datos.unidadBase}
                </p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-xs text-muted">Proveedor</p>
              <p className="text-sm font-medium">{datos.mejorProveedor}</p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="pt-5 text-sm text-muted">
            Este producto todavía no tiene compras registradas.
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Comparativa por proveedor</CardTitle>
          <Badge>{datos.proveedores.length} proveedores</Badge>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {datos.proveedores.length === 0 ? (
            <p className="px-5 pb-5 text-sm text-muted">Sin datos todavía.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-y border-border text-left text-xs text-muted">
                    <th className="px-5 py-2 font-medium">Proveedor</th>
                    <th className="px-3 py-2 text-right font-medium">Precio actual</th>
                    <th className="px-3 py-2 text-right font-medium">Mínimo</th>
                    <th className="px-3 py-2 text-right font-medium">Máximo</th>
                    <th className="px-3 py-2 font-medium">Última compra</th>
                  </tr>
                </thead>
                <tbody>
                  {datos.proveedores.map((proveedor) => (
                    <tr
                      key={proveedor.proveedorId}
                      className="border-b border-border/60 last:border-0"
                    >
                      <td className="px-5 py-2 font-medium">
                        {proveedor.proveedorNombre}
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums">
                        {moneda(proveedor.precioActual)}
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums text-muted">
                        {moneda(proveedor.precioMinimo)}
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums text-muted">
                        {moneda(proveedor.precioMaximo)}
                      </td>
                      <td className="px-3 py-2 text-muted">
                        {fechaCorta(proveedor.ultimaCompra)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {datos.evolucion.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Evolución de precios</CardTitle>
            <Badge>{datos.evolucion.length} registros</Badge>
          </CardHeader>
          <CardContent>
            <ul className="space-y-1.5 text-sm">
              {datos.evolucion.map((punto, indice) => (
                <li
                  key={`${punto.fecha}-${indice}`}
                  className="flex items-center justify-between border-b border-border/50 pb-1.5 last:border-0"
                >
                  <span className="text-muted">{fechaCorta(punto.fecha)}</span>
                  <span className="text-muted">{punto.proveedorNombre}</span>
                  <span className="tabular-nums">{moneda(punto.precio)}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
