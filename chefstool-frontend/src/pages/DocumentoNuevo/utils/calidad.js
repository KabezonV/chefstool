/** Por debajo de este ancho el texto pequeño del albarán deja de ser legible para el OCR. */
export const ANCHO_MINIMO_LEGIBLE = 1600

/**
 * Ancho en píxeles del archivo, o null si no es una imagen que se pueda medir en este
 * navegador (por ejemplo, sin soporte de createImageBitmap).
 */
async function anchoDeImagen(archivo) {
  if (!archivo.type?.startsWith('image/') || typeof createImageBitmap !== 'function') {
    return null
  }
  try {
    const mapa = await createImageBitmap(archivo)
    const ancho = mapa.width
    mapa.close?.()
    return ancho
  } catch {
    return null
  }
}

/** Avisos de las imágenes que llegan con tan poca resolución que el OCR puede fallar. */
export function avisosDeResolucion(mediciones) {
  return mediciones
    .filter((medicion) => medicion?.ancho != null && medicion.ancho < ANCHO_MINIMO_LEGIBLE)
    .map((medicion) => ({ nombre: medicion.nombre, ancho: medicion.ancho }))
}

/**
 * Mide las imágenes elegidas y devuelve los avisos que el usuario debe ver antes de
 * procesarlas. Los PDF no se miden: llevan texto o se rasterizan en el servidor.
 */
export async function revisarCalidad(archivos) {
  const mediciones = await Promise.all(
    archivos.map(async (archivo) => ({
      nombre: archivo.name,
      ancho: await anchoDeImagen(archivo),
    })),
  )
  return avisosDeResolucion(mediciones)
}
