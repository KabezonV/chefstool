// Vendors
import { Moon, Sun } from 'lucide-react'

// Utils
import { cn } from '@/lib/utils'

// Hooks
import { useTema } from '@/lib/tema-context'

// Components
import { Button } from '@/components/ui/button'

/** Botón para alternar entre tema claro y oscuro. */
export default function BotonTema({ variante = 'icono', className }) {
  const { tema, alternar } = useTema()
  const oscuro = tema === 'oscuro'
  const Icono = oscuro ? Sun : Moon
  const etiqueta = oscuro ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'

  if (variante === 'texto') {
    return (
      <button
        type="button"
        onClick={alternar}
        aria-label={etiqueta}
        aria-pressed={oscuro}
        className={cn(
          'inline-flex items-center gap-1 text-xs text-muted hover:text-foreground',
          className,
        )}
      >
        <Icono className="size-3.5" />
        {oscuro ? 'Tema claro' : 'Tema oscuro'}
      </button>
    )
  }

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={alternar}
      aria-label={etiqueta}
      aria-pressed={oscuro}
      title={etiqueta}
      className={className}
    >
      <Icono className="size-4" />
    </Button>
  )
}
