#!/usr/bin/env node
/**
 * Ordena y etiqueta los imports de los ficheros del frontend.
 *
 *   node scripts/orden-imports.mjs src        # reescribe los ficheros
 *   node scripts/orden-imports.mjs src --check # falla (exit 1) si algo no está en orden
 *
 * Grupos (siempre en este orden, separados por una línea en blanco):
 *   Vendors / Types / Utils / Enumerations / Handlers / Hooks / Components / Styles
 */
import { readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs'
import { join, relative } from 'node:path'

const GRUPOS = [
  'Vendors',
  'Types',
  'Utils',
  'Enumerations',
  'Handlers',
  'Hooks',
  'Components',
  'Styles',
]

const MARCADOR = new RegExp(`^//\\s*(${GRUPOS.join('|')})\\s*$`)
const FIN_IMPORT = /from\s+['"][^'"]+['"];?\s*$/
const IMPORT_SUELTO = /^import\s+['"][^'"]+['"];?\s*$/
const FUENTE = /['"]([^'"]+)['"]/

function clasificar(origen, primeraLinea) {
  if (primeraLinea.trimStart().startsWith('import type')) return 'Types'
  if (origen.endsWith('.css')) return 'Styles'
  if (origen.startsWith('@/')) {
    if (origen.startsWith('@/constants/') || origen.startsWith('@/config/')) return 'Enumerations'
    if (origen === '@/lib/api' || origen.startsWith('@/lib/api/')) return 'Handlers'
    if (['@/lib/useListado', '@/lib/auth-context', '@/lib/auth'].includes(origen)) return 'Hooks'
    if (origen.startsWith('@/lib/')) return 'Utils'
    if (origen.startsWith('@/components/') || origen.startsWith('@/pages/')) return 'Components'
    return 'Utils'
  }
  return origen.startsWith('.') ? 'Utils' : 'Vendors'
}

/** Devuelve el fichero reordenado, o null si no tiene imports. */
function reordenar(texto) {
  const lineas = texto.split('\n')
  const bloques = []
  let i = 0
  let comentarios = []

  while (i < lineas.length) {
    const linea = lineas[i]

    if (linea.trim() === '') {
      i += 1
      continue
    }
    if (linea.trim().startsWith('//')) {
      comentarios.push(linea)
      i += 1
      continue
    }
    if (!linea.startsWith('import ')) break

    const sentencia = [linea]
    while (!FIN_IMPORT.test(sentencia.at(-1)) && !IMPORT_SUELTO.test(sentencia.at(-1))) {
      i += 1
      if (i >= lineas.length) return null
      sentencia.push(lineas[i])
    }

    const origen = FUENTE.exec(sentencia.at(-1))[1]
    bloques.push({ origen, sentencia, comentarios: comentarios.filter((c) => !MARCADOR.test(c.trim())) })
    comentarios = []
    i += 1
  }

  if (bloques.length === 0) return null

  const grupos = Object.fromEntries(GRUPOS.map((grupo) => [grupo, []]))
  for (const bloque of bloques) {
    grupos[clasificar(bloque.origen, bloque.sentencia[0])].push(bloque)
  }

  const salida = []
  for (const grupo of GRUPOS) {
    const items = grupos[grupo]
    if (items.length === 0) continue

    if (salida.length > 0) salida.push('')
    salida.push(`// ${grupo}`)
    items.sort((a, b) => {
      const pesoA = a.origen.startsWith('@/') ? 0 : 1
      const pesoB = b.origen.startsWith('@/') ? 0 : 1
      return pesoA - pesoB || a.origen.localeCompare(b.origen)
    })
    for (const { sentencia, comentarios: previos } of items) {
      salida.push(...previos, ...sentencia)
    }
  }

  const resto = lineas.slice(i)
  while (resto.length > 0 && resto[0].trim() === '') resto.shift()

  const resultado = [...salida, ...(resto.length > 0 ? ['', ...resto] : [''])].join('\n')
  return resultado.endsWith('\n') ? resultado : `${resultado}\n`
}

function ficheros(directorio) {
  return readdirSync(directorio).flatMap((entrada) => {
    const ruta = join(directorio, entrada)
    if (statSync(ruta).isDirectory()) return ficheros(ruta)
    return /\.(js|jsx)$/.test(entrada) ? [ruta] : []
  })
}

const [objetivo = 'src', ...opciones] = process.argv.slice(2)
const soloComprobar = opciones.includes('--check')
const desordenados = []

for (const fichero of ficheros(objetivo)) {
  const texto = readFileSync(fichero, 'utf8')
  const resultado = reordenar(texto)
  if (resultado === null || resultado === texto) continue

  if (soloComprobar) {
    desordenados.push(relative(process.cwd(), fichero))
  } else {
    writeFileSync(fichero, resultado)
  }
}

if (soloComprobar) {
  if (desordenados.length > 0) {
    console.error('Imports fuera de orden en:')
    for (const fichero of desordenados) console.error(` - ${fichero}`)
    console.error('\nEjecuta "npm run imports" para ordenarlos.')
    process.exit(1)
  }
  console.log('Imports en orden.')
} else {
  console.log('Imports ordenados.')
}
