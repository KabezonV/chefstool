function escapar(valor) {
  if (valor === null || valor === undefined) return ''
  const texto = String(valor)
  return /[";\r\n]/.test(texto) ? `"${texto.replaceAll('"', '""')}"` : texto
}

/** Cabecera + filas en CSV con punto y coma (Excel en español). */
export function aCsv(columnas, filas) {
  const cabecera = columnas.map((columna) => escapar(columna.label)).join(';')
  const cuerpo = filas.map((fila) =>
    columnas.map((columna) => escapar(columna.valor(fila))).join(';'),
  )
  return [cabecera, ...cuerpo].join('\r\n')
}

export function fechaArchivo(fecha = new Date()) {
  const dos = (numero) => String(numero).padStart(2, '0')
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`
}

/** Descarga un CSV con BOM para que Excel respete los acentos. */
export function descargarCsv(nombre, columnas, filas) {
  const contenido = `\uFEFF${aCsv(columnas, filas)}`
  const blob = new Blob([contenido], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre.endsWith('.csv') ? nombre : `${nombre}.csv`
  enlace.rel = 'noopener'
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}
