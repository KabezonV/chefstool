// Vendors
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Components
import Locales from '@/pages/Locales'

const contexto = vi.hoisted(() => ({
  sesion: { rol: 'propietario', localId: 'local-1' },
}))

vi.mock('@/lib/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), delete: vi.fn() },
  guardarToken: vi.fn(),
}))

vi.mock('@/lib/auth-context', () => ({
  useAuth: () => contexto,
}))

const locales = [
  { id: 'local-1', nombre: 'Cocina' },
  { id: 'local-2', nombre: 'Terraza' },
]

describe('Gestión de locales', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    contexto.sesion = { rol: 'propietario', localId: 'local-1' }
    api.get.mockResolvedValue(locales)
  })

  it('lista los locales y marca el activo', async () => {
    render(<Locales />)

    expect(await screen.findByText('Cocina')).toBeInTheDocument()
    expect(screen.getByText('Terraza')).toBeInTheDocument()
    expect(screen.getByText('Local activo')).toBeInTheDocument()
  })

  it('crea un local nuevo', async () => {
    const user = userEvent.setup()
    api.post.mockResolvedValue({ id: 'local-3', nombre: 'Almacén' })
    render(<Locales />)
    await screen.findByText('Cocina')

    await user.type(screen.getByLabelText('Nombre del local'), 'Almacén')
    await user.click(screen.getByRole('button', { name: /Añadir local/i }))

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/locales', { nombre: 'Almacén' }),
    )
    expect(await screen.findByText('Almacén')).toBeInTheDocument()
  })

  it('enseña el motivo si el nombre ya existe', async () => {
    const user = userEvent.setup()
    api.post.mockRejectedValue(new Error('Ya existe un local llamado Cocina'))
    render(<Locales />)
    await screen.findByText('Cocina')

    await user.type(screen.getByLabelText('Nombre del local'), 'Cocina')
    await user.click(screen.getByRole('button', { name: /Añadir local/i }))

    expect(await screen.findByText('Ya existe un local llamado Cocina')).toBeInTheDocument()
  })

  it('elimina un local que no está activo', async () => {
    const user = userEvent.setup()
    api.delete.mockResolvedValue(null)
    render(<Locales />)
    await screen.findByText('Terraza')

    await user.click(screen.getByRole('button', { name: 'Eliminar Terraza' }))

    await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/locales/local-2'))
    expect(screen.queryByText('Terraza')).not.toBeInTheDocument()
  })

  it('un miembro ve los locales pero no puede crearlos ni borrarlos', async () => {
    contexto.sesion = { rol: 'miembro', localId: 'local-1' }
    render(<Locales />)

    await screen.findByText('Terraza')
    expect(screen.queryByLabelText('Nombre del local')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Eliminar Terraza' })).not.toBeInTheDocument()
  })
})
