/** Valores iniciales de un formulario generado a partir de la definición de campos. */
export function valoresIniciales(campos) {
  return Object.fromEntries(campos.map((campo) => [campo.name, '']))
}

/** Convierte el estado del formulario al JSON que espera la API. */
export function construirPayload(campos, valores) {
  const payload = {}
  for (const campo of campos) {
    const bruto = (valores[campo.name] ?? '').toString().trim()
    if (bruto === '') {
      payload[campo.name] = null
    } else if (campo.type === 'number') {
      payload[campo.name] = Number(bruto)
    } else {
      payload[campo.name] = bruto
    }
  }
  return payload
}
