// Vendors
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Components
import SolicitudesCompra from '@/pages/SolicitudesCompra'

const contexto = vi.hoisted(() => ({
  sesion: {
    usuarioId: 'miembro-1',
    rol: 'miembro',
  },
}))

vi.mock('@/lib/api', () => ({
  api: { get: vi.fn(), post: vi.fn() },
}))

vi.mock('@/lib/auth-context', () => ({
  useAuth: () => contexto,
}))

describe('Solicitudes de compra', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    contexto.sesion = { usuarioId: 'miembro-1', rol: 'miembro' }
    api.get.mockResolvedValue([])
  })

  it('envía una solicitud sin crear ni modificar una compra confirmada', async () => {
    const user = userEvent.setup()
    render(<SolicitudesCompra />)

    await user.type(screen.getByLabelText('¿Qué necesitas?'), 'Horno nuevo')
    await user.type(screen.getByLabelText('Importe estimado (€)'), '1250')
    await user.click(screen.getByRole('button', { name: 'Enviar para aprobación' }))

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/solicitudes-compra', {
        concepto: 'Horno nuevo',
        descripcion: '',
        proveedor: '',
        importe: 1250,
      })
    })
  })

  it('permite a un administrador aprobar solicitudes de otras personas', async () => {
    contexto.sesion = { usuarioId: 'admin-1', rol: 'admin' }
    api.get.mockResolvedValue([
      {
        id: 'solicitud-1',
        concepto: 'Lavavajillas',
        proveedor: 'Proveedor',
        importe: 890,
        estado: 'PENDIENTE',
        solicitanteId: 'miembro-1',
        solicitanteNombre: 'Ana',
        createdAt: '2026-11-25T10:00:00Z',
      },
    ])
    const user = userEvent.setup()
    render(<SolicitudesCompra />)

    await user.click(await screen.findByRole('button', { name: 'Aprobar' }))

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/solicitudes-compra/solicitud-1/aprobar', {})
    })
  })

  it('no muestra acciones de aprobación a los miembros', async () => {
    api.get.mockResolvedValue([
      {
        id: 'solicitud-1',
        concepto: 'Lavavajillas',
        importe: 890,
        estado: 'PENDIENTE',
        solicitanteId: 'otro-miembro',
        solicitanteNombre: 'Ana',
        createdAt: '2026-11-25T10:00:00Z',
      },
    ])
    render(<SolicitudesCompra />)

    await screen.findByText('Lavavajillas')
    expect(screen.queryByRole('button', { name: 'Aprobar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar' })).not.toBeInTheDocument()
  })

  it('muestra el error de carga sin presentarlo como un listado vacío', async () => {
    api.get.mockRejectedValue(new Error('No se pudo cargar'))
    render(<SolicitudesCompra />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar')
    expect(screen.queryByText('Todavía no hay solicitudes de compra.')).not.toBeInTheDocument()
  })
})
