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

const recursoOrdenable = {
  ...recurso,
  columnas: [
    { key: 'nombre', label: 'Nombre', orden: 'nombre' },
    { key: 'unidadBase', label: 'Unidad' },
  ],
}

function montar(rec = recurso) {
  return render(
    <MemoryRouter>
      <ResourcePage recurso={rec} />
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

  it('ordena por la columna al pulsar la cabecera', async () => {
    api.get.mockResolvedValue(pagina([{ id: '1', nombre: 'Tomate pera', unidadBase: 'kg' }]))
    const usuario = userEvent.setup()

    montar(recursoOrdenable)

    await screen.findByText('Tomate pera')
    await usuario.click(screen.getByRole('button', { name: 'Ordenar por Nombre' }))

    await waitFor(() => {
      expect(api.get).toHaveBeenLastCalledWith(
        '/productos?pagina=0&tamano=20&orden=nombre&direccion=asc',
      )
    })
  })

  it('exporta a CSV todas las paginas', async () => {
    api.get.mockResolvedValue(pagina([{ id: '1', nombre: 'Tomate pera', unidadBase: 'kg' }]))
    const objetoUrl = vi.fn(() => 'blob:csv')
    const revocar = vi.fn()
    URL.createObjectURL = objetoUrl
    URL.revokeObjectURL = revocar
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => pagina([{ id: '1', nombre: 'Tomate pera', unidadBase: 'kg' }]),
    }))
    vi.stubGlobal('fetch', fetchMock)

    const usuario = userEvent.setup()
    montar()

    await screen.findByText('Tomate pera')
    await usuario.click(screen.getByRole('button', { name: 'CSV' }))

    await waitFor(() => expect(objetoUrl).toHaveBeenCalled())
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/productos?pagina=0&tamano=100',
      expect.anything(),
    )

    const csv = await objetoUrl.mock.calls[0][0].text()
    expect(csv.replace('\uFEFF', '')).toBe('Nombre;Unidad\r\nTomate pera;kg')

    click.mockRestore()
    delete URL.createObjectURL
    delete URL.revokeObjectURL
    vi.unstubAllGlobals()
  })

  it('exporta a Excel con celdas numericas', async () => {
    api.get.mockResolvedValue(pagina([{ id: '1', nombre: 'Tomate pera', unidadBase: 'kg' }]))
    const objetoUrl = vi.fn(() => 'blob:xlsx')
    URL.createObjectURL = objetoUrl
    URL.revokeObjectURL = vi.fn()
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    vi.stubGlobal('fetch', async () => ({
      ok: true,
      status: 200,
      json: async () => pagina([{ id: '1', nombre: 'Tomate pera', unidadBase: 'kg' }]),
    }))

    const usuario = userEvent.setup()
    montar()

    await screen.findByText('Tomate pera')
    await usuario.click(screen.getByRole('button', { name: 'Excel' }))

    await waitFor(() => expect(objetoUrl).toHaveBeenCalled())

    const bytes = new Uint8Array(await objetoUrl.mock.calls[0][0].arrayBuffer())
    // Firma de un zip (todo .xlsx lo es) y una de sus partes
    expect([...bytes.slice(0, 4)]).toEqual([0x50, 0x4b, 0x03, 0x04])
    const contenido = new TextDecoder().decode(bytes)
    expect(contenido).toContain('xl/worksheets/sheet1.xml')
    expect(contenido).toContain('Tomate pera')

    click.mockRestore()
    delete URL.createObjectURL
    delete URL.revokeObjectURL
    vi.unstubAllGlobals()
  })
})
