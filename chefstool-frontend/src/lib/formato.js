const eur = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
})

const fecha = new Intl.DateTimeFormat('es-ES', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

export function moneda(valor) {
  return eur.format(Number(valor ?? 0))
}

export function fechaCorta(valor) {
  if (!valor) return '—'
  return fecha.format(new Date(valor))
}

export const estados = {
  pendiente_revision: { texto: 'Por revisar', tono: 'warning' },
  confirmado: { texto: 'Confirmado', tono: 'success' },
  descartado: { texto: 'Descartado', tono: 'neutral' },
}
