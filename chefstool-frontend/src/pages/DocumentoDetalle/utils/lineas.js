// Utils
import { importeDeLinea } from '@/lib/importes'

/** Línea en blanco del editor de albaranes. */
export function lineaVacia() {
  return {
    descripcion: '',
    cantidad: '1',
    unidad: '',
    precioUnitario: '0',
    productoId: '',
  }
}

/**
 * Importe de una línea redondeado a céntimos. Si en la revisión se ha escrito un importe a
 * mano (descuento por línea, precio cerrado), manda ese.
 */
export function importeLinea(linea) {
  return importeDeLinea(linea)
}

/** Importe que saldría de cantidad por precio, para avisar cuando no coincide. */
export function importeCalculado(linea) {
  const cantidad = Number(linea.cantidad || 0)
  const precio = Number(linea.precioUnitario || 0)
  return Math.round(cantidad * precio * 100) / 100
}
