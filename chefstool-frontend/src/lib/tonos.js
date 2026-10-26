/**
 * Tono del Badge según el dato de negocio. Se separa de las vistas para que todas las
 * pantallas usen los mismos umbrales.
 */
export function tonoMargen(margen) {
  if (margen === null || margen === undefined) return 'neutral'
  if (margen >= 65) return 'success'
  if (margen >= 40) return 'warning'
  return 'danger'
}

export function tonoVariacion(valor) {
  if (valor === null || valor === undefined) return 'neutral'
  return Number(valor) >= 10 ? 'danger' : Number(valor) > 0 ? 'warning' : 'success'
}
