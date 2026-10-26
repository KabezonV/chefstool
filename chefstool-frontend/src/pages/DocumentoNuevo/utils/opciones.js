// Vendors
import { Camera, Sparkles, Upload } from 'lucide-react'

/** Formas de dar de alta un albarán. */
export const OPCIONES_ALTA = [
  {
    clave: 'camara',
    icono: Camera,
    titulo: 'Hacer foto',
    texto: 'Abre la cámara del móvil o la tablet y captura el albarán.',
  },
  {
    clave: 'archivo',
    icono: Upload,
    titulo: 'Subir archivo',
    texto: 'Fotos o PDF ya guardados. Puedes seleccionar varios a la vez.',
  },
  {
    clave: 'demo',
    icono: Sparkles,
    titulo: 'Probar con un ejemplo',
    texto: 'Genera un albarán de demostración con tu catálogo actual.',
  },
]
