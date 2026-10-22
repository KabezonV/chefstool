import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { api } from '@/lib/api'
import ResourcePage from '@/components/crud/ResourcePage'

vi.mock('@/lib/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const recurso = {
  endpoint: '/productos',
  titulo: 'Productos',
  singular: 'producto',
  descripcion: 'Catálogo de prueba',
  vacio: 'No hay productos',
  columnas: [
    { key: 'nombre', label: 'Nombre' },
    { key: 'unidadBase', label: 'Unidad' },
  ],
  campos: [
    { name: 'nombre', label: 'Nombre', required: true },
    { name: 'unidadBase', label: 'Unidad' },
  ],
}

function montar() {
  return render(
    <MemoryRouter>
      <ResourcePage recurso={recurso} />
    </MemoryRouter>,
  )
}

describe('ResourcePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra los registros que devuelve la api', async () => {
    api.get.mockResolvedValue([
      { id: '1', nombre: 'Tomate pera', unidadBase: 'kg' },
      { id: '2', nombre: 'Cebolla', unidadBase: 'kg' },
    ])

    montar()

    expect(await screen.findByText('Tomate pera')).toBeInTheDocument()
    expect(screen.getByText('Cebolla')).toBeInTheDocument()
    expect(screen.getByText('2 registros')).toBeInTheDocument()
  })

  it('muestra el estado vacio', async () => {
    api.get.mockResolvedValue([])
    montar()

    expect(await screen.findByText('No hay productos')).toBeInTheDocument()
    expect(screen.getByText('0 registros')).toBeInTheDocument()
  })

  it('muestra el error si la api falla', async () => {
    api.get.mockRejectedValue(new Error('Sin conexión'))
    montar()

    expect(await screen.findByText('Sin conexión')).toBeInTheDocument()
  })

  it('abre el formulario al pulsar nuevo y envia los datos', async () => {
    api.get.mockResolvedValue([])
    api.post.mockResolvedValue({ id: '9' })
    const usuario = userEvent.setup()

    montar()

    await usuario.click(await screen.findByRole('button', { name: /Nuevo producto/i }))
    await usuario.type(screen.getByLabelText('Nombre'), 'Pimienta')
    await usuario.type(screen.getByLabelText('Unidad'), 'kg')
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/productos', {
        nombre: 'Pimienta',
        unidadBase: 'kg',
      })
    })
  })

  it('envia null en los campos vacios', async () => {
    api.get.mockResolvedValue([])
    api.post.mockResolvedValue({ id: '9' })
    const usuario = userEvent.setup()

    montar()

    await usuario.click(await screen.findByRole('button', { name: /Nuevo producto/i }))
    await usuario.type(screen.getByLabelText('Nombre'), 'Sal')
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/productos', {
        nombre: 'Sal',
        unidadBase: null,
      })
    })
  })
})
