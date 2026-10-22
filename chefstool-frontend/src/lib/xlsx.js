/**
 * Generador minimo de XLSX (Excel) sin dependencias: zip "store" + hoja con celdas
 * inline. Los numeros se escriben como numeros, asi que Excel los suma y los formatea
 * sin tener que convertir el texto.
 */

const TABLA_CRC = (() => {
  const tabla = new Uint32Array(256)
  for (let i = 0; i < 256; i += 1) {
    let valor = i
    for (let bit = 0; bit < 8; bit += 1) {
      valor = valor & 1 ? 0xedb88320 ^ (valor >>> 1) : valor >>> 1
    }
    tabla[i] = valor >>> 0
  }
  return tabla
})()

function crc32(bytes) {
  let crc = 0xffffffff
  for (let i = 0; i < bytes.length; i += 1) {
    crc = TABLA_CRC[(crc ^ bytes[i]) & 0xff] ^ (crc >>> 8)
  }
  return (crc ^ 0xffffffff) >>> 0
}

function textoBytes(texto) {
  return new TextEncoder().encode(texto)
}

/** Zip sin compresion: cabecera local + datos por fichero, y directorio central. */
function comprimir(ficheros) {
  const partes = []
  const central = []
  let desplazamiento = 0
  const hora = 0
  const fecha = 0x21 // 1980-01-01, fecha fija para que el fichero sea reproducible

  for (const fichero of ficheros) {
    const nombre = textoBytes(fichero.nombre)
    const datos = textoBytes(fichero.contenido)
    const crc = crc32(datos)

    const local = new Uint8Array(30 + nombre.length)
    const vista = new DataView(local.buffer)
    vista.setUint32(0, 0x04034b50, true)
    vista.setUint16(4, 20, true)
    vista.setUint16(6, 0x0800, true) // nombres en UTF-8
    vista.setUint16(8, 0, true) // sin compresion
    vista.setUint16(10, hora, true)
    vista.setUint16(12, fecha, true)
    vista.setUint32(14, crc, true)
    vista.setUint32(18, datos.length, true)
    vista.setUint32(22, datos.length, true)
    vista.setUint16(26, nombre.length, true)
    vista.setUint16(28, 0, true)
    local.set(nombre, 30)

    partes.push(local, datos)

    const entrada = new Uint8Array(46 + nombre.length)
    const vistaCentral = new DataView(entrada.buffer)
    vistaCentral.setUint32(0, 0x02014b50, true)
    vistaCentral.setUint16(4, 20, true)
    vistaCentral.setUint16(6, 20, true)
    vistaCentral.setUint16(8, 0x0800, true)
    vistaCentral.setUint16(10, 0, true)
    vistaCentral.setUint16(12, hora, true)
    vistaCentral.setUint16(14, fecha, true)
    vistaCentral.setUint32(16, crc, true)
    vistaCentral.setUint32(20, datos.length, true)
    vistaCentral.setUint32(24, datos.length, true)
    vistaCentral.setUint16(28, nombre.length, true)
    vistaCentral.setUint32(42, desplazamiento, true)
    entrada.set(nombre, 46)
    central.push(entrada)

    desplazamiento += local.length + datos.length
  }

  const tamanoCentral = central.reduce((total, entrada) => total + entrada.length, 0)
  const fin = new Uint8Array(22)
  const vistaFin = new DataView(fin.buffer)
  vistaFin.setUint32(0, 0x06054b50, true)
  vistaFin.setUint16(8, ficheros.length, true)
  vistaFin.setUint16(10, ficheros.length, true)
  vistaFin.setUint32(12, tamanoCentral, true)
  vistaFin.setUint32(16, desplazamiento, true)

  const total =
    desplazamiento + tamanoCentral + fin.length
  const salida = new Uint8Array(total)
  let posicion = 0
  for (const parte of partes) {
    salida.set(parte, posicion)
    posicion += parte.length
  }
  for (const entrada of central) {
    salida.set(entrada, posicion)
    posicion += entrada.length
  }
  salida.set(fin, posicion)
  return salida
}

function escaparXml(valor) {
  return String(valor)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function columna(indice) {
  let resto = indice
  let letra = ''
  do {
    letra = String.fromCharCode(65 + (resto % 26)) + letra
    resto = Math.floor(resto / 26) - 1
  } while (resto >= 0)
  return letra
}

function celda(valor, indice, fila) {
  const referencia = `${columna(indice)}${fila}`
  if (valor === null || valor === undefined || valor === '') {
    return ''
  }
  if (typeof valor === 'number' && Number.isFinite(valor)) {
    return `<c r="${referencia}" t="n"><v>${valor}</v></c>`
  }
  return `<c r="${referencia}" t="inlineStr"><is><t xml:space="preserve">${escaparXml(
    valor,
  )}</t></is></c>`
}

/** Construye el .xlsx de una sola hoja a partir de cabeceras y filas. */
export function construirXlsx({ hoja = 'Datos', columnas, filas }) {
  const filasXml = []

  const cabecera = columnas.map((columnaActual, indice) =>
    celda(columnaActual.label, indice, 1),
  )
  filasXml.push(`<row r="1">${cabecera.join('')}</row>`)

  filas.forEach((fila, numero) => {
    const celdas = columnas.map((columnaActual, indice) =>
      celda(columnaActual.valor(fila), indice, numero + 2),
    )
    filasXml.push(`<row r="${numero + 2}">${celdas.join('')}</row>`)
  })

  const anchos = columnas
    .map(
      (columnaActual, indice) =>
        `<col min="${indice + 1}" max="${indice + 1}" width="${
          Math.min(Math.max(columnaActual.label.length + 4, 12), 40)
        }" customWidth="1"/>`,
    )
    .join('')

  const worksheet = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<cols>${anchos}</cols>
<sheetData>${filasXml.join('')}</sheetData>
</worksheet>`

  const workbook = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="${escaparXml(hoja)}" sheetId="1" r:id="rId1"/></sheets>
</workbook>`

  const tipos = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>`

  const relaciones = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>`

  const relacionesLibro = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>`

  return comprimir([
    { nombre: '[Content_Types].xml', contenido: tipos },
    { nombre: '_rels/.rels', contenido: relaciones },
    { nombre: 'xl/workbook.xml', contenido: workbook },
    { nombre: 'xl/_rels/workbook.xml.rels', contenido: relacionesLibro },
    { nombre: 'xl/worksheets/sheet1.xml', contenido: worksheet },
  ])
}

export function descargarXlsx(nombre, { hoja, columnas, filas }) {
  const bytes = construirXlsx({ hoja, columnas, filas })
  const blob = new Blob([bytes], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre.endsWith('.xlsx') ? nombre : `${nombre}.xlsx`
  enlace.rel = 'noopener'
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}
