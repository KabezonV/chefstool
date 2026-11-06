// Vendors
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Hooks
import { AuthProvider } from '@/lib/auth'
import { TemaProvider } from '@/lib/tema'

// Components
import Auth from '@/pages/Auth'

vi.mock('@/lib/api', async () => {
  const real = await vi.importActual('@/lib/api')
  return {
    ...real,
    api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  }
})

const sesion = {
  token: 'token-de-prueba',
  usuarioId: 'u1',
  email: 'chef@cocina.es',
  nombre: 'Chef',
  organizacionId: 'o1',
  organizacion: 'Cocina',
  rol: 'propietario',
}

function montar() {
  return render(
    <MemoryRouter>
      <TemaProvider>
        <AuthProvider>
          <Auth />
        </AuthProvider>
      </TemaProvider>
    </MemoryRouter>,
  )
}

describe('Pantalla de acceso', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('entra con correo y contraseña', async () => {
    api.post.mockResolvedValue(sesion)
    const usuario = userEvent.setup()
    montar()

    await usuario.type(screen.getByLabelText('Correo electrónico'), 'chef@cocina.es')
    await usuario.type(screen.getByLabelText('Contraseña'), 'secreto123')
    await usuario.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        email: 'chef@cocina.es',
        password: 'secreto123',
      })
    })
    expect(localStorage.getItem('chefstool.token')).toBe('token-de-prueba')
  })

  it('muestra el error si las credenciales fallan', async () => {
    api.post.mockRejectedValue(new Error('Correo o contrasena incorrectos'))
    const usuario = userEvent.setup()
    montar()

    await usuario.type(screen.getByLabelText('Correo electrónico'), 'chef@cocina.es')
    await usuario.type(screen.getByLabelText('Contraseña'), 'mala')
    await usuario.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Correo o contrasena incorrectos')).toBeInTheDocument()
    expect(localStorage.getItem('chefstool.token')).toBeNull()
  })

  it('registra una cuenta nueva con organización', async () => {
    api.post.mockResolvedValue(sesion)
    const usuario = userEvent.setup()
    montar()

    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))
    await usuario.type(screen.getByLabelText('Tu nombre'), 'Ana')
    await usuario.type(screen.getByLabelText(/organización/i), 'La Sal')
    await usuario.type(screen.getByLabelText('Correo electrónico'), 'ana@lacocina.es')
    await usuario.type(screen.getByLabelText('Contraseña'), 'secreto123')
    // Hay dos botones con ese texto: la pestaña y el submit. Pulsamos el submit.
    const botones = screen.getAllByRole('button', { name: 'Crear cuenta' })
    await usuario.click(botones[botones.length - 1])

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/auth/registro', {
        email: 'ana@lacocina.es',
        password: 'secreto123',
        nombre: 'Ana',
        organizacion: 'La Sal',
      })
    })
  })

  it('deja ver la contraseña escrita', async () => {
    const usuario = userEvent.setup()
    montar()

    const campo = screen.getByLabelText('Contraseña')
    await usuario.type(campo, 'secreto123')
    expect(campo).toHaveAttribute('type', 'password')

    await usuario.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))

    expect(campo).toHaveAttribute('type', 'text')
    expect(campo).toHaveValue('secreto123')
    expect(screen.getByRole('button', { name: 'Ocultar contraseña' })).toBeInTheDocument()
  })

  it('cambia entre tema claro y oscuro', async () => {
    const usuario = userEvent.setup()
    montar()

    expect(document.documentElement).not.toHaveClass('dark')

    await usuario.click(screen.getByRole('button', { name: 'Cambiar a tema oscuro' }))

    expect(document.documentElement).toHaveClass('dark')
    expect(localStorage.getItem('chefstool.tema')).toBe('oscuro')
  })
})
