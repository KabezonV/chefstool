import { ChevronDown, ChevronUp, ChevronsUpDown, Download } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

/**
 * Cabecera de tabla opcionalmente ordenable. `campo` es el nombre que entiende la API
 * (solo se permite un orden si la columna lo declara).
 */
export function CabeceraOrdenable({ columna, orden, direccion, onOrdenar, className }) {
  const activa = columna.orden && columna.orden === orden
  const Icono = activa ? (direccion === 'desc' ? ChevronDown : ChevronUp) : ChevronsUpDown

  if (!columna.orden) {
    return <th className={cn('px-5 py-2 font-medium', className)}>{columna.label}</th>
  }

  return (
    <th className={cn('px-5 py-2 font-medium', className)}>
      <button
        type="button"
        onClick={() => onOrdenar(columna.orden)}
        className={cn(
          'inline-flex items-center gap-1 hover:text-foreground',
          activa ? 'text-foreground' : 'text-muted',
        )}
        aria-label={`Ordenar por ${columna.label}`}
      >
        {columna.label}
        <Icono className="size-3.5" />
      </button>
    </th>
  )
}

export function BotonExportar({ onClick, exportando, etiqueta = 'Exportar CSV' }) {
  return (
    <Button variant="outline" size="sm" onClick={onClick} disabled={exportando}>
      <Download className="size-4" />
      {exportando ? 'Generando…' : etiqueta}
    </Button>
  )
}
