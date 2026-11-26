/** Descarga un archivo que ya viene del servidor (PDF, JPG o ZIP) con su nombre. */
export function descargarArchivo(blob, nombre) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  enlace.rel = 'noopener'
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}

/** Primer y último día del mes en curso, en formato AAAA-MM-DD (para el periodo por defecto). */
export function mesEnCurso(hoy = new Date()) {
  const dos = (numero) => String(numero).padStart(2, '0')
  const primero = new Date(hoy.getFullYear(), hoy.getMonth(), 1)
  return {
    desde: `${primero.getFullYear()}-${dos(primero.getMonth() + 1)}-01`,
    hasta: `${hoy.getFullYear()}-${dos(hoy.getMonth() + 1)}-${dos(hoy.getDate())}`,
  }
}
