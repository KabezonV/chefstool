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

/** Fecha y hora completas (auditoría): 26/11/2026 10:21. */
export function fechaHora(valor) {
  if (!valor) return '-'
  const momento = new Date(valor)
  if (Number.isNaN(momento.getTime())) return '-'
  return momento.toLocaleString('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
