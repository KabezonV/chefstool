// Utils
import { cn } from '@/lib/utils'

// Components
import { TONOS_BADGE } from '@/components/ui/utils/badge'

export function Badge({ className, tone = 'neutral', ...props }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
        TONOS_BADGE[tone],
        className,
      )}
      {...props}
    />
  )
}
