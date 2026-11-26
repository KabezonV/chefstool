// Vendors
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Components
import Compras from '@/pages/Compras'

vi.mock('@/lib/api', () => ({
  api: { descargar: vi.fn(), get: vi.fn().mockResolvedValue([]), post: vi.fn() },
  listarTodo: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/lib/useListado', () => ({
  useListado: () => ({
    items: [
      {
        id: 'compra-1',
        proveedorNombre: 'Mayorista Uno',
        numeroDocumento: 'A-100',
        fecha: '2026-11-10',
        importeTotal: 41.58,
      },
    ],
    datos: { totalElementos: 1, totalPaginas: 1 },
    pagina: 0,
    tamano: 20,
    orden: null,
    direccion: null,
    cargando: false,
    error: null,
    setError: vi.fn(),
    exportando: false,
    actualizar: vi.fn(),
    ordenarPor: vi.fn(),
    exportar: vi.fn(),
  }),
}))

function pintar() {
  return render(
    <MemoryRouter initialEntries={['/compras?desde=2026-11-01&hasta=2026-11-30']}>
      <Compras />
    </MemoryRouter>,
  )
}

describe('Descargas de compras', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    URL.createObjectURL = vi.fn(() => 'blob:compra')
    URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    api.descargar.mockResolvedValue(new Blob(['x']))
  })

  afterEach(() => {
    delete URL.createObjectURL
    delete URL.revokeObjectURL
  })

  it('descarga el ZIP de los albaranes con el periodo de los filtros', async () => {
    const user = userEvent.setup()
    pintar()

    await user.click(screen.getByRole('button', { name: /ZIP albaranes/i }))

    expect(api.descargar).toHaveBeenCalledWith(
      '/compras/zip?desde=2026-11-01&hasta=2026-11-30',
    )
  })

  it('descarga el PDF de la compra abierta', async () => {
    const user = userEvent.setup()
    api.get.mockResolvedValue({
      id: 'compra-1',
      numeroDocumento: 'A-100',
      proveedorNombre: 'Mayorista Uno',
      lineas: [],
      importeNeto: 37.8,
      importeIva: 3.78,
      importeTotal: 41.58,
    })
    pintar()

    const abrir = await screen.findAllByRole('button', { name: /ver/i })
    await user.click(abrir[0])
    await user.click(await screen.findByRole('button', { name: /PDF de la compra/i }))

    expect(api.descargar).toHaveBeenCalledWith('/compras/compra-1/pdf')
  })

  it('pide el periodo antes de bajar un ZIP sin fechas', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/compras']}>
        <Compras />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: /ZIP albaranes/i }))

    expect(api.descargar).not.toHaveBeenCalled()
    expect(
      await screen.findByText(/Elige el periodo/i),
    ).toBeInTheDocument()
  })
})
