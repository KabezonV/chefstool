// Vendors
import { Slot } from '@radix-ui/react-slot'

// Utils
import { cn } from '@/lib/utils'

// Components
import { variantesBoton } from '@/components/ui/utils/button'

export function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}) {
  const Comp = asChild ? Slot : 'button'
  return (
    <Comp
      className={cn(variantesBoton({ variant, size }), className)}
      {...props}
    />
  )
}
