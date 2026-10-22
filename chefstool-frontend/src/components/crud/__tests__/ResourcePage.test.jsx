import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { api } from '@/lib/api'
import ResourcePage from '@/components/crud/ResourcePage'

vi.mock('@/lib/api', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  }
})

function pagina(contenido, { pagina: numero = 0, tamano = 20, total = contenido.length } = {}) {
  return {
    contenido,
    pagina: numero,
    tamano,
    totalElementos: total,
    totalPaginas: Math.ceil(total / tamano),
  }
}

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
    api.get.mockResolvedValue(
      pagina([
        { id: '1', nombre: 'Tomate pera', unidadBase: 'kg' },
        { id: '2', nombre: 'Cebolla', unidadBase: 'kg' },
      ]),
    )

    montar()

    expect(await screen.findByText('Tomate pera')).toBeInTheDocument()
    expect(screen.getByText('Cebolla')).toBeInTheDocument()
    expect(screen.getByText('2 registros')).toBeInTheDocument()
  })

  it('muestra el estado vacio', async () => {
    api.get.mockResolvedValue(pagina([]))
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
    api.get.mockResolvedValue(pagina([]))
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
    api.get.mockResolvedValue(pagina([]))
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

  it('pagina los resultados y pide la siguiente pagina', async () => {
    api.get.mockResolvedValueOnce(
      pagina([{ id: '1', nombre: 'Tomate pera', unidadBase: 'kg' }], { total: 30 }),
    )
    api.get.mockResolvedValueOnce(
      pagina([{ id: '21', nombre: 'Azafran', unidadBase: 'g' }], {
        pagina: 1,
        total: 30,
      }),
    )
    const usuario = userEvent.setup()

    montar()

    expect(await screen.findByText('Tomate pera')).toBeInTheDocument()
    expect(screen.getByText('1–20 de 30')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Página siguiente' }))

    expect(await screen.findByText('Azafran')).toBeInTheDocument()
    expect(api.get).toHaveBeenLastCalledWith('/productos?pagina=1&tamano=20')
  })

  it('busca por nombre y vuelve a la primera pagina', async () => {
    api.get.mockResolvedValue(pagina([{ id: '1', nombre: 'Tomate pera', unidadBase: 'kg' }]))
    const usuario = userEvent.setup()

    montar()

    await screen.findByText('Tomate pera')
    await usuario.type(screen.getByRole('searchbox'), 'cebolla')

    await waitFor(
      () => {
        expect(api.get).toHaveBeenLastCalledWith('/productos?pagina=0&tamano=20&q=cebolla')
      },
      { timeout: 2000 },
    )
  })
})
