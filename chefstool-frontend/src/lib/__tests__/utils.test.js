// Vendors
import { describe, expect, it } from 'vitest'

// Utils
import { cn } from '@/lib/utils'

describe('cn', () => {
  it('junta clases', () => {
    expect(cn('a', 'b')).toBe('a b')
  })

  it('resuelve conflictos de tailwind quedandose con la ultima', () => {
    expect(cn('p-2', 'p-4')).toBe('p-4')
  })

  it('ignora valores falsos', () => {
    expect(cn('a', null, undefined, false, 'b')).toBe('a b')
  })
})
