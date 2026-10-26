// Vendors
import { describe, expect, it } from 'vitest'

// Enumerations
import { ESTADOS_DOCUMENTO } from '@/constants/documentos'

describe('estados de documento', () => {
  it('traduce los estados a texto legible', () => {
    expect(ESTADOS_DOCUMENTO.pendiente_revision.texto).toBe('Por revisar')
    expect(ESTADOS_DOCUMENTO.confirmado.texto).toBe('Confirmado')
    expect(ESTADOS_DOCUMENTO.descartado.texto).toBe('Descartado')
  })
})
