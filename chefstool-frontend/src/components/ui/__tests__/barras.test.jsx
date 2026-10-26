// Vendors
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

// Components
import Barras from '@/components/ui/barras'

describe('Barras', () => {
  it('muestra el mensaje cuando no hay datos', () => {
    render(<Barras datos={[]} etiquetaVacia="Sin compras" />)
    expect(screen.getByText('Sin compras')).toBeInTheDocument()
  })

  it('pinta una linea por dato con su importe', () => {
    render(
      <Barras
        datos={[
          { etiqueta: 'Frutas Garcia', importe: 145.2 },
          { etiqueta: 'Verduras Lopez', importe: 97.9 },
        ]}
      />,
    )

    expect(screen.getByText('Frutas Garcia')).toBeInTheDocument()
    expect(screen.getByText(/145,20/)).toBeInTheDocument()
    expect(screen.getByText(/97,90/)).toBeInTheDocument()
  })

  it('traduce las etiquetas de mes a un formato legible', () => {
    render(<Barras datos={[{ etiqueta: '2026-05', importe: 10 }]} />)
    expect(screen.getByText(/may/i)).toBeInTheDocument()
  })

  it('no divide por cero cuando todos los importes son cero', () => {
    render(<Barras datos={[{ etiqueta: 'Sin gasto', importe: 0 }]} />)
    expect(screen.getByText('Sin gasto')).toBeInTheDocument()
  })
})
