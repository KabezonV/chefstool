const TOLERANCIA = 0.02

function numero(valor) {
  const n = Number(valor)
  return Number.isFinite(n) ? n : 0
}

export function euros(valor) {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(numero(valor))
}

/**
 * Comprueba que los números del documento cuadren entre sí. Sirve sobre todo para
 * detectar fallos de lectura: si el OCR se come una línea, las cuentas no salen.
 */
export function avisosDeImportes(cabecera = {}, lineas = []) {
  const avisos = []
  const neto = numero(cabecera.importeNeto)
  const iva = numero(cabecera.importeIva)
  const total = numero(cabecera.importeTotal)

  const conImporte = lineas.filter((linea) => Number.isFinite(Number(linea.importe)))
  const sumaLíneas = conImporte.reduce((suma, linea) => suma + numero(linea.importe), 0)

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
