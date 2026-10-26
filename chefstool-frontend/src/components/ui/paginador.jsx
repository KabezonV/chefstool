// Vendors
import { ChevronLeft, ChevronRight } from 'lucide-react'

// Enumerations
import { TAMANOS_PAGINA } from '@/constants/paginacion'

// Components
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/input'

export function Buscador({ valor, onChange, placeholder = 'Buscar…' }) {
  return (
    <input
      type="search"
      value={valor}
      onChange={(event) => onChange(event.target.value)}
      placeholder={placeholder}
      aria-label={placeholder}
      className="h-9 w-full max-w-xs rounded-lg border border-border bg-card px-3 text-sm outline-none transition-colors placeholder:text-muted/60 focus-visible:ring-2 focus-visible:ring-foreground/20"
    />
  )
}

export default function Paginador({ pagina, tamano, totalElementos, totalPaginas, onCambiar }) {
  if (!totalElementos) return null

  const desde = pagina * tamano + 1
  const hasta = Math.min((pagina + 1) * tamano, totalElementos)

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border px-5 py-3 text-sm">
      <span className="text-muted">
        {desde}–{hasta} de {totalElementos}
      </span>

      <div className="flex items-center gap-2">
        <span className="text-xs text-muted">Por página</span>
        <Select
          aria-label="Registros por página"
          value={tamano}
          onChange={(event) => onCambiar({ pagina: 0, tamano: Number(event.target.value) })}
          className="h-8 w-20"
        >
          {TAMANOS_PAGINA.map((valor) => (
            <option key={valor} value={valor}>
              {valor}
            </option>
          ))}
        </Select>

        <Button
          variant="outline"
          size="icon"
          aria-label="Página anterior"
          disabled={pagina <= 0}
          onClick={() => onCambiar({ pagina: pagina - 1, tamano })}
        >
          <ChevronLeft className="size-4" />
        </Button>
        <span className="min-w-16 text-center text-xs text-muted">
          {pagina + 1} / {Math.max(totalPaginas, 1)}
        </span>
        <Button
          variant="outline"
          size="icon"
          aria-label="Página siguiente"
          disabled={pagina + 1 >= totalPaginas}
          onClick={() => onCambiar({ pagina: pagina + 1, tamano })}
        >
          <ChevronRight className="size-4" />
        </Button>
      </div>
    </div>
  )
}
