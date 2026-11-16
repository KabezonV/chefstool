// Vendors
import { describe, expect, it } from 'vitest'

// Utils
import { avisosDeImportes, lineasDescuadradas } from '@/lib/importes'

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

  it('cuenta las líneas del formulario, que solo traen cantidad y precio', () => {
    const avisos = avisosDeImportes({ importeNeto: 10 }, [
      { cantidad: '2', precioUnitario: '2.5' },
      { cantidad: '1', precioUnitario: '3' },
    ])
    expect(avisos.some((aviso) => aviso.includes('Las líneas suman'))).toBe(true)
  })

  it('no avisa si faltan datos', () => {
    expect(avisosDeImportes({}, [])).toEqual([])
    expect(avisosDeImportes({ importeNeto: 0, importeIva: 0, importeTotal: 0 }, lineas(10))).toEqual(
      [],
    )
  })
})

describe('lineasDescuadradas', () => {
  it('no avisa cuando cantidad por precio da el importe del documento', () => {
    expect(
      lineasDescuadradas([
        { cantidad: '2', precioUnitario: '2.5', importeLeido: 5 },
        { cantidad: '10', precioUnitario: '0.95', importeLeido: 9.5 },
      ]),
    ).toEqual([])
  })

  it('señala la línea cuyo importe no sale de la cantidad y el precio', () => {
    const descuadres = lineasDescuadradas([
      { cantidad: '2', precioUnitario: '2.5', importeLeido: 5 },
      { cantidad: '2', precioUnitario: '1.2', importeLeido: 3.6 },
    ])
    expect(descuadres).toHaveLength(1)
    expect(descuadres[0]).toMatchObject({ indice: 1, leido: 3.6, calculado: 2.4 })
  })

  it('ignora las líneas escritas a mano, que no traen importe leído', () => {
    expect(lineasDescuadradas([{ cantidad: '3', precioUnitario: '1.2' }])).toEqual([])
    expect(
      lineasDescuadradas([{ cantidad: '3', precioUnitario: '1.2', importeLeido: null }]),
    ).toEqual([])
  })

  it('tolera céntimos de redondeo', () => {
    expect(
      lineasDescuadradas([{ cantidad: '3', precioUnitario: '0.3333', importeLeido: 1 }]),
    ).toEqual([])
  })
})
