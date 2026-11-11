// Vendors
import { ArrowLeft, ExternalLink, Loader2, Plus, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

// Utils
import { fechaCorta, moneda } from '@/lib/formato'
import { avisosDeImportes } from '@/lib/importes'
import { importeLinea, lineaVacia } from './utils/lineas'

// Enumerations
import { ESTADOS_DOCUMENTO } from '@/constants/documentos'

// Handlers
import { api, listarTodo } from '@/lib/api'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, Input, Select } from '@/components/ui/input'
import { Celda, TablaFormulario } from '@/components/ui/tabla'

const PROVEEDOR_NUEVO = '__proveedor_nuevo__'

export default function DocumentoDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [documento, setDocumento] = useState(null)
  const [productos, setProductos] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [lineas, setLineas] = useState([])
  const [cabecera, setCabecera] = useState({
    proveedorId: '',
    proveedorNuevo: '',
    numeroDocumento: '',
    fecha: '',
    moneda: 'EUR',
    iva: '0',
  })
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true

    Promise.all([
      api.get(`/documentos/${id}`),
      listarTodo('/productos'),
      listarTodo('/proveedores'),
    ])
      .then(([detalle, catalogo, listaProveedores]) => {
        if (!activo) return
        setDocumento(detalle)
        setProductos(catalogo)
        setProveedores(listaProveedores)

        const extraccion = detalle.extraccion
        const nombreDetectado = (extraccion?.proveedorNombre ?? '').trim()
        const yaExiste =
          nombreDetectado !== '' &&
          listaProveedores.some(
            (proveedor) => proveedor.nombre.trim().toLowerCase() === nombreDetectado.toLowerCase(),
          )
        const proponerNuevo = nombreDetectado !== '' && !yaExiste && !extraccion?.proveedorId
        setCabecera({
          proveedorId: proponerNuevo ? PROVEEDOR_NUEVO : (extraccion?.proveedorId ?? ''),
          proveedorNuevo: proponerNuevo ? nombreDetectado : '',
          numeroDocumento: extraccion?.numeroDocumento ?? '',
          fecha: extraccion?.fecha ?? '',
          moneda: extraccion?.moneda ?? 'EUR',
          iva: String(extraccion?.importeIva ?? 0),
        })
        setLineas(
          (extraccion?.lineas ?? []).map((linea) => ({
            descripcion: linea.descripcion ?? '',
            cantidad: String(linea.cantidad ?? 1),
            unidad: linea.unidad ?? '',
            precioUnitario: String(linea.precioUnitario ?? 0),
            productoId: linea.productoId ?? '',
          })),
        )
        setError(null)
      })
      .catch((err) => activo && setError(err.message))
      .finally(() => activo && setCargando(false))

    return () => {
      activo = false
    }
  }, [id])

  const neto = useMemo(
    () => lineas.reduce((total, linea) => total + importeLinea(linea), 0),
    [lineas],
  )
  const iva = Number(cabecera.iva || 0)
  const total = Math.round((neto + iva) * 100) / 100

  const esProveedorNuevo = cabecera.proveedorId === PROVEEDOR_NUEVO

  const soloLectura =
    documento?.estado === 'confirmado' || documento?.estado === 'descartado'

  function cambiarCabecera(campo, valor) {
    setCabecera((previos) => ({ ...previos, [campo]: valor }))
  }

  function cambiarLinea(indice, campo, valor) {
    setLineas((previas) =>
      previas.map((linea, i) => (i === indice ? { ...linea, [campo]: valor } : linea)),
    )
  }

  function quitarLinea(indice) {
    setLineas((previas) => previas.filter((_, i) => i !== indice))
  }

  function anadirLinea() {
    setLineas((previas) => [...previas, lineaVacia()])
  }

  function payload() {
    const proveedor = proveedores.find((p) => p.id === cabecera.proveedorId)
    return {
      proveedorId: esProveedorNuevo ? null : cabecera.proveedorId || null,
      proveedorNombre: esProveedorNuevo
        ? cabecera.proveedorNuevo.trim() || null
        : (proveedor?.nombre ?? documento?.extraccion?.proveedorNombre ?? null),
      numeroDocumento: cabecera.numeroDocumento || null,
      fecha: cabecera.fecha || null,
      moneda: cabecera.moneda || 'EUR',
      importeNeto: neto,
      importeIva: iva,
      importeTotal: total,
      lineas: lineas.map((linea) => ({
        descripcion: linea.descripcion || null,
        cantidad: Number(linea.cantidad || 0),
        unidad: linea.unidad || null,
        precioUnitario: Number(linea.precioUnitario || 0),
        importe: importeLinea(linea),
        productoId: linea.productoId || null,
        clienteId: null,
        eventoId: null,
      })),
    }
  }

  async function guardarBorrador() {
    setGuardando(true)
    setError(null)
    try {
      const actualizado = await api.put(`/documentos/${id}`, payload())
      setDocumento(actualizado)
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function confirmar() {
    setGuardando(true)
    setError(null)
    try {
      await api.post(`/documentos/${id}/confirmar`, payload())
      navigate('/documentos', { state: { aviso: 'Compra registrada correctamente.' } })
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  async function descartar() {
    if (!window.confirm('¿Descartar este documento?')) return
    setGuardando(true)
    try {
      await api.delete(`/documentos/${id}`)
      navigate('/documentos')
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) {
    return <p className="text-sm text-muted">Cargando documento…</p>
  }

  if (!documento) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <p className="text-sm text-danger">{error ?? 'Documento no encontrado'}</p>
        <Button asChild variant="outline">
          <Link to="/documentos">Volver a documentos</Link>
        </Button>
      </div>
    )
  }

  const estado =
    ESTADOS_DOCUMENTO[documento.estado] ?? ESTADOS_DOCUMENTO.pendiente_revision
  const avisosImportes = avisosDeImportes(
    { importeNeto: documento.importeNeto, importeIva: documento.importeIva, importeTotal: documento.importeTotal },
    lineas,
  )
  const confianzaBaja =
    documento.confianzaExtraccion === null ||
    Number(documento.confianzaExtraccion) < 0.5

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <Link
          to="/documentos"
          className="inline-flex items-center gap-1 text-sm text-muted hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Documentos
        </Link>
        <div className="mt-2 flex flex-wrap items-center gap-3">
          <h1 className="min-w-0 break-words text-xl font-semibold tracking-tight">
            {documento.nombreOriginal}
          </h1>
          <Badge tone={estado.tono}>{estado.texto}</Badge>
          {documento.confianzaExtraccion !== null && (
            <Badge
              tone={Number(documento.confianzaExtraccion) < 0.85 ? 'warning' : 'success'}
              title="Porcentaje de campos e importes que la lectura automática ha reconocido con seguridad"
            >
              Confianza {Math.round(Number(documento.confianzaExtraccion) * 100)} %
            </Badge>
          )}
          {documento.tieneArchivo && (
            <a
              href={`/api/documentos/${documento.id}/archivo`}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-xs font-medium text-muted hover:text-foreground"
            >
              <ExternalLink className="size-3.5" />
              Abrir original
            </a>
          )}
        </div>
        <p className="mt-1 text-sm text-muted">
          Subido el {fechaCorta(documento.createdAt)} · origen {documento.origen}
        </p>
        {documento.confianzaExtraccion !== null &&
          Number(documento.confianzaExtraccion) < 0.85 && (
            <p className="mt-1 text-xs text-warning">
              La lectura automática no ha reconocido todo con seguridad: revisa proveedor,
              importes y líneas antes de confirmar.
            </p>
          )}
      </div>

      {error && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      {avisosImportes.length > 0 && documento.estado === 'pendiente_revision' && (
        <div className="rounded-xl border border-warning-border bg-warning-soft px-4 py-3 text-sm text-warning">
          <p className="font-medium">Los importes no cuadran</p>
          <ul className="mt-1 list-disc pl-5">
            {avisosImportes.map((aviso) => (
              <li key={aviso}>{aviso}</li>
            ))}
          </ul>
          <p className="mt-1">
            Puede ser una línea que no se ha leído. Revísalo antes de confirmar la compra.
          </p>
        </div>
      )}

      {!documento.errorExtraccion &&
        confianzaBaja &&
        lineas.length === 0 &&
        documento.estado === 'pendiente_revision' && (
          <div className="rounded-xl border border-warning-border bg-warning-soft px-4 py-3 text-sm text-warning">
            <p className="font-medium">No se han reconocido las líneas del documento</p>
            <p className="mt-1">
              Puede ser una foto movida, poco iluminada o un formato poco habitual. Prueba
              con una foto más recta y con buena luz, o escribe las líneas a mano: el
              original queda guardado y la compra se puede confirmar igualmente.
            </p>
          </div>
        )}

      {documento.errorExtraccion && (
        <div className="rounded-xl border border-warning-border bg-warning-soft px-4 py-3 text-sm text-warning">
          <p className="font-medium">No se ha podido leer el documento</p>
          <p className="mt-1">{documento.errorExtraccion}</p>
          <p className="mt-1">
            Puedes añadir las líneas a mano y confirmar la compra igualmente.
          </p>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Datos de la compra</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label="Proveedor" htmlFor="proveedor">
              <Select
                id="proveedor"
                disabled={soloLectura}
                value={cabecera.proveedorId}
                onChange={(event) => cambiarCabecera('proveedorId', event.target.value)}
              >
                <option value="">Sin asignar</option>
                {proveedores.map((proveedor) => (
                  <option key={proveedor.id} value={proveedor.id}>
                    {proveedor.nombre}
                  </option>
                ))}
                {!soloLectura && (
                  <option value={PROVEEDOR_NUEVO}>Dar de alta un proveedor nuevo…</option>
                )}
              </Select>
              {esProveedorNuevo && (
                <Input
                  className="mt-2"
                  placeholder="Nombre del proveedor"
                  aria-label="Nombre del proveedor nuevo"
                  disabled={soloLectura}
                  value={cabecera.proveedorNuevo}
                  onChange={(event) => cambiarCabecera('proveedorNuevo', event.target.value)}
                />
              )}
            </Field>
            <Field label="Número de documento" htmlFor="numero">
              <Input
                id="numero"
                disabled={soloLectura}
                value={cabecera.numeroDocumento}
                onChange={(event) =>
                  cambiarCabecera('numeroDocumento', event.target.value)
                }
              />
            </Field>
            <Field label="Fecha" htmlFor="fecha">
              <Input
                id="fecha"
                type="date"
                disabled={soloLectura}
                value={cabecera.fecha}
                onChange={(event) => cambiarCabecera('fecha', event.target.value)}
              />
            </Field>
            <Field label="Moneda" htmlFor="moneda">
              <Input
                id="moneda"
                disabled={soloLectura}
                value={cabecera.moneda}
                onChange={(event) => cambiarCabecera('moneda', event.target.value)}
              />
            </Field>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Líneas del documento</CardTitle>
          <Badge>{lineas.length} líneas</Badge>
        </CardHeader>
        <CardContent className="space-y-3 px-0 pb-0">
          <div className="overflow-x-auto">
            <TablaFormulario>
              <thead>
                <tr className="border-y border-border text-left text-xs text-muted">
                  <th className="px-5 py-2 font-medium">Descripción</th>
                  <th className="w-24 px-2 py-2 font-medium">Cantidad</th>
                  <th className="w-20 px-2 py-2 font-medium">Unidad</th>
                  <th className="w-28 px-2 py-2 font-medium">Precio</th>
                  <th className="w-24 px-2 py-2 font-medium">Importe</th>
                  <th className="w-48 px-2 py-2 font-medium">Producto</th>
                  <th className="w-12 px-2 py-2" />
                </tr>
              </thead>
              <tbody>
                {lineas.map((linea, indice) => (
                  <tr key={indice} className="border-b border-border/60 last:border-0">
                    <Celda etiqueta="Descripción" className="px-4 py-2 sm:px-5 sm:py-2">
                      <Input
                        disabled={soloLectura}
                        value={linea.descripcion}
                        onChange={(event) =>
                          cambiarLinea(indice, 'descripcion', event.target.value)
                        }
                      />
                    </Celda>
                    <Celda etiqueta="Cantidad" className="px-4 py-2 sm:px-2 sm:py-2">
                      <Input
                        type="number"
                        step="0.001"
                        disabled={soloLectura}
                        value={linea.cantidad}
                        onChange={(event) =>
                          cambiarLinea(indice, 'cantidad', event.target.value)
                        }
                      />
                    </Celda>
                    <Celda etiqueta="Unidad" className="px-4 py-2 sm:px-2 sm:py-2">
                      <Input
                        disabled={soloLectura}
                        value={linea.unidad}
                        onChange={(event) =>
                          cambiarLinea(indice, 'unidad', event.target.value)
                        }
                      />
                    </Celda>
                    <Celda etiqueta="Precio" className="px-4 py-2 sm:px-2 sm:py-2">
                      <Input
                        type="number"
                        step="0.0001"
                        disabled={soloLectura}
                        value={linea.precioUnitario}
                        onChange={(event) =>
                          cambiarLinea(indice, 'precioUnitario', event.target.value)
                        }
                      />
                    </Celda>
                    <Celda
                      etiqueta="Importe"
                      className="px-4 py-2 tabular-nums sm:px-2 sm:py-2"
                    >
                      {moneda(importeLinea(linea))}
                    </Celda>
                    <Celda etiqueta="Producto" className="px-4 py-2 sm:px-2 sm:py-2">
                      <Select
                        disabled={soloLectura}
                        value={linea.productoId}
                        onChange={(event) =>
                          cambiarLinea(indice, 'productoId', event.target.value)
                        }
                      >
                        <option value="">Sin relacionar</option>
                        {productos.map((producto) => (
                          <option key={producto.id} value={producto.id}>
                            {producto.nombre}
                          </option>
                        ))}
                      </Select>
                    </Celda>
                    <Celda className="px-4 py-2 sm:px-2 sm:py-2">
                      {!soloLectura && (
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Quitar línea"
                          onClick={() => quitarLinea(indice)}
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      )}
                    </Celda>
                  </tr>
                ))}
              </tbody>
            </TablaFormulario>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-4 px-4 pb-5 sm:px-5">
            {!soloLectura ? (
              <Button variant="outline" onClick={anadirLinea}>
                <Plus className="size-4" />
                Añadir línea
              </Button>
            ) : (
              <span />
            )}

            <div className="ml-auto grid w-64 grid-cols-2 gap-x-4 gap-y-1 text-sm">
              <span className="text-muted">Base imponible</span>
              <span className="text-right tabular-nums">{moneda(neto)}</span>
              <span className="flex items-center justify-end gap-2 text-muted">
                IVA
                {!soloLectura && (
                  <Input
                    type="number"
                    step="0.01"
                    className="h-7 w-20 text-right"
                    value={cabecera.iva}
                    onChange={(event) => cambiarCabecera('iva', event.target.value)}
                  />
                )}
              </span>
              <span className="text-right tabular-nums">{moneda(iva)}</span>
              <span className="border-t border-border pt-1 font-semibold">Total</span>
              <span className="border-t border-border pt-1 text-right font-semibold tabular-nums">
                {moneda(total)}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap items-center justify-end gap-2">
        {!soloLectura && (
          <>
            <Button variant="ghost" disabled={guardando} onClick={descartar}>
              Descartar
            </Button>
            <Button variant="outline" disabled={guardando} onClick={guardarBorrador}>
              {guardando ? <Loader2 className="size-4 animate-spin" /> : null}
              Guardar borrador
            </Button>
            <Button variant="accent" disabled={guardando} onClick={confirmar}>
              Confirmar y guardar
            </Button>
          </>
        )}
        {soloLectura && (
          <Button asChild variant="outline">
            <Link to="/documentos">Volver a documentos</Link>
          </Button>
        )}
      </div>
    </div>
  )
}
