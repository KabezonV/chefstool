// Vendors
import { describe, expect, it } from 'vitest'

// Utils
import { avisosDeImportes } from '@/lib/importes'

const lineas = (...importes) => importes.map((importe) => ({ importe }))

describe('avisosDeImportes', () => {
  it('no avisa cuando todo cuadra', () => {
    const avisos = avisosDeImportes(
      { importeNeto: 29.8, importeIva: 2.98, importeTotal: 32.78 },
      lineas(21, 8.8),
    )
    expect(avisos).toEqual([])
  })

  it('avisa si las líneas no suman la base imponible', () => {
    const avisos = avisosDeImportes(
      { importeNeto: 50, importeIva: 5, importeTotal: 55 },
      lineas(21, 8.8),
    )
    expect(avisos[0]).toContain('Las líneas suman')
  })

  it('avisa si base más IVA no da el total', () => {
    const avisos = avisosDeImportes(
      { importeNeto: 100, importeIva: 10, importeTotal: 200 },
      lineas(100),
    )
    expect(avisos.some((aviso) => aviso.includes('IVA'))).toBe(true)
  })

  it('tolera céntimos de redondeo', () => {
    const avisos = avisosDeImportes(
      { importeNeto: 29.8, importeIva: 2.98, importeTotal: 32.78 },
      lineas(21.0, 8.799),
    )
    expect(avisos).toEqual([])
  })

  it('no avisa si faltan datos', () => {
    expect(avisosDeImportes({}, [])).toEqual([])
    expect(avisosDeImportes({ importeNeto: 0, importeIva: 0, importeTotal: 0 }, lineas(10))).toEqual(
      [],
    )
  })
})
