// Vendors
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Components
import Auditoria from '@/pages/Auditoria'

vi.mock('@/lib/api', () => ({
  api: { get: vi.fn() },
  listarTodo: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/lib/useListado', () => ({
  useListado: () => ({
    items: [
      {
        id: 'evento-1',
        usuarioNombre: 'Marta Jefa',
        accion: 'CONFIRMAR',
        entidad: 'DOCUMENTO',
        detalle: 'albaran.pdf',
        createdAt: '2026-11-26T09:15:00Z',
      },
      {
        id: 'evento-2',
        usuarioNombre: null,
        accion: 'APROBAR',
        entidad: 'SOLICITUD_COMPRA',
        detalle: 'Horno nuevo',
        createdAt: '2026-11-25T18:00:00Z',
      },
    ],
    datos: { totalElementos: 2, totalPaginas: 1 },
    pagina: 0,
    tamano: 20,
    cargando: false,
    error: null,
    actualizar: vi.fn(),
  }),
}))

describe('Pantalla de auditoría', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra quién hizo qué en lenguaje claro', async () => {
    render(<Auditoria />)

    expect(await screen.findByText('Marta Jefa')).toBeInTheDocument()
    expect(screen.getByText('Confirmó una compra')).toBeInTheDocument()
    expect(screen.getByText('un albarán')).toBeInTheDocument()
    expect(screen.getByText('albaran.pdf')).toBeInTheDocument()
  })

  it('llama Sistema cuando la acción no tiene usuario', async () => {
    render(<Auditoria />)

    await screen.findByText('Marta Jefa')
    expect(screen.getByText('Sistema')).toBeInTheDocument()
    expect(screen.getByText('Aprobó una solicitud')).toBeInTheDocument()
  })
})
