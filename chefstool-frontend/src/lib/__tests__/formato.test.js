// Vendors
import { describe, expect, it } from 'vitest'

// Utils
import { fechaCorta, moneda } from '@/lib/formato'

describe('formato', () => {
  it('formatea importes en euros', () => {
    expect(moneda(12.5)).toContain('12,50')
    expect(moneda(12.5)).toContain('€')
  })

  it('trata importes vacios como cero', () => {
    expect(moneda(null)).toContain('0,00')
    expect(moneda(undefined)).toContain('0,00')
  })

  it('formatea fechas en formato español', () => {
    expect(fechaCorta('2026-10-05')).toBe('05/10/2026')
  })

  it('devuelve un guion si no hay fecha', () => {
    expect(fechaCorta(null)).toBe('—')
  })
})
