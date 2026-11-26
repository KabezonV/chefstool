// Vendors
import { describe, expect, it } from 'vitest'

// Utils
import {
  avisosDeImportes,
  importeDeLinea,
  ivaDe,
  lineasDescuadradas,
  tipoDeIva,
} from '@/lib/importes'

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

  it('deja de avisar cuando el usuario escribe el importe que dice el documento', () => {
    const lineas = [
      { cantidad: '2', precioUnitario: '1.2', importeLeido: 3.6, importeManual: '3.6' },
    ]
    expect(lineasDescuadradas(lineas)).toEqual([])
    expect(importeDeLinea(lineas[0])).toBe(3.6)
  })
})

describe('importeDeLinea', () => {
  it('usa el importe escrito a mano aunque no salga de cantidad por precio', () => {
    expect(
      importeDeLinea({ cantidad: '2', precioUnitario: '10', importeManual: '17.5' }),
    ).toBe(17.5)
  })

  it('si no hay importe a mano usa el del documento', () => {
    expect(importeDeLinea({ cantidad: '2', precioUnitario: '10', importe: 18 })).toBe(18)
  })

  it('si no hay nada calcula cantidad por precio', () => {
    expect(importeDeLinea({ cantidad: '3', precioUnitario: '1.2' })).toBe(3.6)
    expect(importeDeLinea({})).toBe(0)
  })

  it('con el campo vacío vuelve a calcular', () => {
    expect(importeDeLinea({ cantidad: '2', precioUnitario: '10', importeManual: '' })).toBe(20)
  })
})

describe('tipoDeIva', () => {
  it('deduce el tipo de los importes del documento', () => {
    expect(tipoDeIva({ importeNeto: 37.8, importeIva: 3.78 })).toBeCloseTo(0.1, 4)
    expect(tipoDeIva({ importeNeto: 142.5, importeIva: 7.13 })).toBeCloseTo(0.05, 4)
    expect(tipoDeIva({ importeNeto: 100, importeIva: 21 })).toBeCloseTo(0.21, 4)
  })

  it('ajusta al tipo habitual mas cercano si la base viene mal leida', () => {
    expect(tipoDeIva({ importeNeto: 152.8, importeIva: 8.92 })).toBeCloseTo(0.05, 4)
    expect(tipoDeIva({ importeNeto: 152.8, importeIva: 15.5 })).toBeCloseTo(0.1, 4)
    expect(tipoDeIva({ importeNeto: 100, importeIva: 21.4 })).toBeCloseTo(0.21, 4)
  })

  it('no inventa un tipo si faltan importes o no tienen sentido', () => {
    expect(tipoDeIva({ importeNeto: 0, importeIva: 0 })).toBeNull()
    expect(tipoDeIva({ importeNeto: 100 })).toBeNull()
    expect(tipoDeIva({ importeNeto: 10, importeIva: 90 })).toBeNull()
  })
})

describe('ivaDe', () => {
  it('aplica el tipo sobre la base de las líneas', () => {
    expect(ivaDe(114, 0.1)).toBe(11.4)
    expect(ivaDe(113.8, 0.1)).toBe(11.38)
    expect(ivaDe(142.5, 0.05)).toBe(7.13)
  })

  it('sin tipo no calcula nada', () => {
    expect(ivaDe(100, null)).toBe(0)
  })
})
