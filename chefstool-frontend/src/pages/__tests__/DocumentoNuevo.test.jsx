// Vendors
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// Handlers
import { api } from '@/lib/api'

// Components
import DocumentoNuevo from '@/pages/DocumentoNuevo'

const navegar = vi.fn()

vi.mock('@/lib/api', () => ({
  api: { subir: vi.fn(), get: vi.fn() },
}))

vi.mock('react-router-dom', async () => {
  const real = await vi.importActual('react-router-dom')
  return { ...real, useNavigate: () => navegar }
})

function foto(nombre = 'albaran.jpg') {
  return new File([new Uint8Array([0xff, 0xd8, 0xff])], nombre, { type: 'image/jpeg' })
}

function elegirArchivo(contenedor, archivos) {
  const entrada = contenedor.querySelectorAll('input[type="file"]')[1]
  return userEvent.upload(entrada, archivos)
}

describe('Alta de documento con foto', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.subir.mockResolvedValue({ id: 'doc-1' })
  })

  afterEach(() => {
    delete globalThis.createImageBitmap
  })

  it('sube sin preguntar cuando la foto tiene resolución suficiente', async () => {
    globalThis.createImageBitmap = vi.fn().mockResolvedValue({ width: 3000, height: 4000 })
    const { container } = render(
      <MemoryRouter>
        <DocumentoNuevo />
      </MemoryRouter>,
    )

    await elegirArchivo(container, foto())

    await waitFor(() => expect(api.subir).toHaveBeenCalledTimes(1))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(navegar).toHaveBeenCalledWith('/documentos/doc-1')
  })

  it('avisa y no sube nada cuando la foto es demasiado pequeña', async () => {
    globalThis.createImageBitmap = vi.fn().mockResolvedValue({ width: 768, height: 1365 })
    const { container } = render(
      <MemoryRouter>
        <DocumentoNuevo />
      </MemoryRouter>,
    )

    await elegirArchivo(container, foto('whatsapp-albaran.jpg'))

    const aviso = await screen.findByRole('alert')
    expect(aviso).toHaveTextContent('poca resolución')
    expect(aviso).toHaveTextContent('whatsapp-albaran.jpg')
    expect(aviso).toHaveTextContent('768 px')
    expect(api.subir).not.toHaveBeenCalled()
    expect(navegar).not.toHaveBeenCalled()
  })

  it('sube la foto pequeña solo si el usuario lo confirma', async () => {
    globalThis.createImageBitmap = vi.fn().mockResolvedValue({ width: 900, height: 1600 })
    const user = userEvent.setup()
    const { container } = render(
      <MemoryRouter>
        <DocumentoNuevo />
      </MemoryRouter>,
    )

    await elegirArchivo(container, foto())
    await screen.findByRole('alert')

    await user.click(screen.getByRole('button', { name: 'Subir igualmente' }))

    await waitFor(() => expect(api.subir).toHaveBeenCalledTimes(1))
    expect(navegar).toHaveBeenCalledWith('/documentos/doc-1')
  })

  it('deja elegir otra imagen y retira el aviso', async () => {
    globalThis.createImageBitmap = vi.fn().mockResolvedValue({ width: 800, height: 1200 })
    const user = userEvent.setup()
    const { container } = render(
      <MemoryRouter>
        <DocumentoNuevo />
      </MemoryRouter>,
    )

    await elegirArchivo(container, foto())
    await screen.findByRole('alert')

    await user.click(screen.getByRole('button', { name: 'Elegir otra imagen' }))

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(api.subir).not.toHaveBeenCalled()
  })

  it('no bloquea un PDF aunque no se pueda medir como imagen', async () => {
    globalThis.createImageBitmap = vi.fn()
    const { container } = render(
      <MemoryRouter>
        <DocumentoNuevo />
      </MemoryRouter>,
    )

    const pdf = new File([new Uint8Array([0x25, 0x50, 0x44, 0x46])], 'albaran.pdf', {
      type: 'application/pdf',
    })
    await elegirArchivo(container, pdf)

    await waitFor(() => expect(api.subir).toHaveBeenCalledTimes(1))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
