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

/** Importe de una línea redondeado a céntimos, sin depender de la vista. */
export function importeLinea(linea) {
  const cantidad = Number(linea.cantidad || 0)
  const precio = Number(linea.precioUnitario || 0)
  return Math.round(cantidad * precio * 100) / 100
}
