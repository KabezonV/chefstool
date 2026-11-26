// Vendors
import { afterEach, describe, expect, it, vi } from 'vitest'

// Utils
import { descargarArchivo, mesEnCurso } from '@/lib/descargas'

describe('descargarArchivo', () => {
  afterEach(() => {
    delete URL.createObjectURL
    delete URL.revokeObjectURL
  })

  it('descarga el archivo con el nombre indicado', () => {
    URL.createObjectURL = vi.fn(() => 'blob:albaran')
    URL.revokeObjectURL = vi.fn()
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    descargarArchivo(new Blob(['x']), '2026-11-08_ALB-1.pdf')

    expect(URL.createObjectURL).toHaveBeenCalled()
    expect(click).toHaveBeenCalled()
    expect(document.querySelector('a[download="2026-11-08_ALB-1.pdf"]')).toBeNull()
    click.mockRestore()
  })
})

describe('mesEnCurso', () => {
  it('va del día 1 al día de hoy', () => {
    expect(mesEnCurso(new Date(2026, 10, 26))).toEqual({
      desde: '2026-11-01',
      hasta: '2026-11-26',
    })
  })

  it('rellena el mes con dos dígitos', () => {
    expect(mesEnCurso(new Date(2026, 0, 5))).toEqual({
      desde: '2026-01-01',
      hasta: '2026-01-05',
    })
  })
})
