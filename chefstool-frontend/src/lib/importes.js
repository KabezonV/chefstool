const TOLERANCIA = 0.02

function numero(valor) {
  const n = Number(valor)
  return Number.isFinite(n) ? n : 0
}

export function euros(valor) {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(numero(valor))
}

/**
 * Líneas en las que la cantidad por el precio no da el importe que traía el documento.
 * Es la señal más fina de un fallo de lectura: dice exactamente qué renglón revisar.
 */
export function lineasDescuadradas(lineas = []) {
  return lineas.flatMap((linea, indice) => {
    if (linea.importeLeido === null || linea.importeLeido === undefined || linea.importeLeido === '') {
      return []
    }
    const leido = numero(linea.importeLeido)
    const cantidad = numero(linea.cantidad)
    const precio = numero(linea.precioUnitario)
    const calculado = Math.round(cantidad * precio * 100) / 100
    if (leido <= 0 || cantidad <= 0 || precio <= 0) return []
    return Math.abs(leido - calculado) > TOLERANCIA
      ? [{ indice, leido, calculado, cantidad, precio }]
      : []
  })
}

/**
 * Comprueba que los números del documento cuadren entre sí. Sirve sobre todo para
 * detectar fallos de lectura: si el OCR se come una línea, las cuentas no salen.
 */
/** Importe de la línea: el que trae, o cantidad por precio si viene del formulario. */
function importeDe(linea) {
  if (Number.isFinite(Number(linea.importe))) return numero(linea.importe)
  return Math.round(numero(linea.cantidad) * numero(linea.precioUnitario) * 100) / 100
}

export function avisosDeImportes(cabecera = {}, lineas = []) {
  const avisos = []
  const neto = numero(cabecera.importeNeto)
  const iva = numero(cabecera.importeIva)
  const total = numero(cabecera.importeTotal)

  const conImporte = lineas.filter(
    (linea) =>
      Number.isFinite(Number(linea.importe)) ||
      Number.isFinite(Number(linea.precioUnitario)) ||
      Number.isFinite(Number(linea.cantidad)),
  )
  const sumaLíneas = conImporte.reduce((suma, linea) => suma + importeDe(linea), 0)

  if (conImporte.length > 0 && neto > 0 && Math.abs(sumaLíneas - neto) > TOLERANCIA) {
    avisos.push(
      `Las líneas suman ${euros(sumaLíneas)} y la base imponible dice ${euros(neto)}`,
    )
  }

  if (total > 0 && neto + iva > 0 && Math.abs(neto + iva - total) > TOLERANCIA) {
    avisos.push(
      `Base imponible más IVA son ${euros(neto + iva)} y el total dice ${euros(total)}`,
    )
  }

  return avisos
}
