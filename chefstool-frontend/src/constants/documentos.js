/** Estados por los que pasa un albarán subido. */
export const ESTADOS_DOCUMENTO = {
  pendiente_revision: { texto: 'Por revisar', tono: 'warning' },
  confirmado: { texto: 'Confirmado', tono: 'success' },
  descartado: { texto: 'Descartado', tono: 'neutral' },
}
