// Vendors
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Components
import Documentos from '@/pages/Documentos'

vi.mock('@/lib/api', () => ({
  api: { descargar: vi.fn(), get: vi.fn() },
  listarTodo: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/lib/useListado', () => ({
  useListado: () => ({
    items: [],
    datos: { totalElementos: 0 },
    pagina: 0,
    tamano: 20,
    orden: null,
    direccion: null,
    cargando: false,
    error: null,
    exportando: false,
    actualizar: vi.fn(),
    ordenarPor: vi.fn(),
    exportar: vi.fn(),
  }),
}))

function pintar() {
  return render(
    <MemoryRouter>
      <Documentos />
    </MemoryRouter>,
  )
}

describe('Descarga de albaranes en ZIP', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    URL.createObjectURL = vi.fn(() => 'blob:zip')
    URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    api.descargar.mockResolvedValue(new Blob(['zip']))
  })

  afterEach(() => {
    delete URL.createObjectURL
    delete URL.revokeObjectURL
  })

  it('pide el ZIP del periodo elegido', async () => {
    const user = userEvent.setup()
    pintar()

    await user.clear(screen.getByLabelText('Albaranes desde'))
    await user.type(screen.getByLabelText('Albaranes desde'), '2026-10-01')
    await user.clear(screen.getByLabelText('Albaranes hasta'))
    await user.type(screen.getByLabelText('Albaranes hasta'), '2026-10-31')

    await user.click(screen.getByRole('button', { name: /ZIP/i }))

    expect(api.descargar).toHaveBeenCalledWith(
      '/documentos/zip?desde=2026-10-01&hasta=2026-10-31',
    )
  })

  it('enseña el motivo cuando no hay albaranes en el periodo', async () => {
    const user = userEvent.setup()
    api.descargar.mockRejectedValue(new Error('No hay albaranes con archivo en ese periodo'))
    pintar()

    await user.click(screen.getByRole('button', { name: /ZIP/i }))

    expect(
      await screen.findByText('No hay albaranes con archivo en ese periodo'),
    ).toBeInTheDocument()
  })
})
