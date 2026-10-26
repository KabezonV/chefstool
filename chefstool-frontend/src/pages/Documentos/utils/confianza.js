/** Confianza de la extracción (0..1) en porcentaje legible. */
export function formatearConfianza(valor) {
  if (valor === null || valor === undefined) return '—'
  return `${Math.round(Number(valor) * 100)} %`
}
