// Vendors
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Components
import DocumentoDetalle from '@/pages/DocumentoDetalle'

vi.mock('@/lib/api', () => ({
  api: { get: vi.fn(), subir: vi.fn(), put: vi.fn(), post: vi.fn(), descargar: vi.fn() },
  listarTodo: vi.fn().mockResolvedValue([]),
}))

vi.mock('react-router-dom', async () => {
  const real = await vi.importActual('react-router-dom')
  return { ...real, useParams: () => ({ id: 'doc-1' }), useNavigate: () => vi.fn() }
})

function documento(extra = {}) {
  return {
    id: 'doc-1',
    nombreOriginal: 'albaran-hoja1.jpg',
    estado: 'pendiente_revision',
    origen: 'camara',
    confianzaExtraccion: 0.9,
    createdAt: '2026-11-26T09:00:00Z',
    tieneArchivo: true,
    errorExtraccion: null,
    extraccion: {
      proveedorNombre: 'Distribuciones Ejemplo',
      numeroDocumento: 'A-1',
      fecha: '2026-11-08',
      moneda: 'EUR',
      importeNeto: 37.8,
      importeIva: 3.78,
      importeTotal: 41.58,
      lineas: [
        {
          descripcion: 'LECHUGA ROMANA',
          cantidad: 2,
          unidad: 'kg',
          precioUnitario: 2.5,
          importe: 5,
        },
      ],
    },
    ...extra,
  }
}

function pintar() {
  return render(
    <MemoryRouter>
      <DocumentoDetalle />
    </MemoryRouter>,
  )
}

describe('Revisión del albarán con varias hojas', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('añade la segunda hoja y reemplaza las líneas por la lectura nueva', async () => {
    const user = userEvent.setup()
    api.get.mockResolvedValue(documento())
    api.subir.mockResolvedValue(
      documento({
        nombreOriginal: 'albaran-hoja1-2-hojas.pdf',
        extraccion: {
          ...documento().extraccion,
          lineas: [
            ...documento().extraccion.lineas,
            {
              descripcion: 'TOMATE PERA',
              cantidad: 4,
              unidad: 'kg',
              precioUnitario: 1.85,
              importe: 7.4,
            },
          ],
        },
      }),
    )

    const { container } = pintar()
    await screen.findByText('albaran-hoja1.jpg')

    const entrada = container.querySelector('input[type="file"]')
    await user.upload(entrada, new File(['x'], 'hoja2.jpg', { type: 'image/jpeg' }))

    await user.click(await screen.findByRole('button', { name: 'Añadir y releer' }))

    await waitFor(() => expect(api.subir).toHaveBeenCalledTimes(1))
    expect(api.subir.mock.calls[0][0]).toBe('/documentos/doc-1/hojas')

    await waitFor(() =>
      expect(screen.getByDisplayValue('TOMATE PERA')).toBeInTheDocument(),
    )
  })

  it('no ofrece añadir hojas cuando el documento ya está confirmado', async () => {
    api.get.mockResolvedValue(documento({ estado: 'confirmado' }))
    pintar()

    await screen.findByText('albaran-hoja1.jpg')
    expect(screen.queryByRole('button', { name: 'Añadir otra hoja' })).not.toBeInTheDocument()
  })

  it('no ofrece añadir hojas a un documento sin archivo', async () => {
    api.get.mockResolvedValue(documento({ tieneArchivo: false, origen: 'demo' }))
    pintar()

    await screen.findByText('albaran-hoja1.jpg')
    expect(screen.queryByRole('button', { name: 'Añadir otra hoja' })).not.toBeInTheDocument()
  })

  it('permite corregir el importe de una línea y recalcula el total', async () => {
    const user = userEvent.setup()
    api.get.mockResolvedValue(documento())
    api.post.mockResolvedValue({ id: 'compra-1' })

    pintar()
    await screen.findByText('albaran-hoja1.jpg')

    // La línea trae 2 x 2,50 = 5,00 y el documento dice 9,50: se escribe el importe real
    const importe = screen.getByLabelText('Importe de la línea 1')
    await user.clear(importe)
    await user.type(importe, '9.5')

    // Base 9,50 + IVA 0,95 (10 %) = 10,45, en vez de los 5,50 que salían antes
    expect(await screen.findByText('10,45 €')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Confirmar y guardar/i }))

    await waitFor(() => expect(api.post).toHaveBeenCalled())
    const [, enviado] = api.post.mock.calls.at(-1)
    expect(enviado.lineas[0].importe).toBe(9.5)
    expect(enviado.importeTotal).toBe(10.45)
  })

  it('descarga el original con su nombre desde la revisión', async () => {
    const user = userEvent.setup()
    api.get.mockResolvedValue(documento())
    api.descargar.mockResolvedValue(new Blob(['pdf']))
    URL.createObjectURL = vi.fn(() => 'blob:original')
    URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    pintar()
    await screen.findByText('albaran-hoja1.jpg')

    await user.click(screen.getByRole('button', { name: 'Descargar' }))

    expect(api.descargar).toHaveBeenCalledWith('/documentos/doc-1/descarga')

    delete URL.createObjectURL
    delete URL.revokeObjectURL
  })
})
