// Utils
import { cn } from '@/lib/utils'

// Components
import { CLASE_BASE_CAMPO } from '@/components/ui/utils/input'

export function Input({ className, ...props }) {
  return <input className={cn(CLASE_BASE_CAMPO, 'h-9', className)} {...props} />
}

export function Textarea({ className, ...props }) {
  return <textarea className={cn(CLASE_BASE_CAMPO, 'min-h-20 py-2', className)} {...props} />
}

export function Select({ className, children, ...props }) {
  return (
    <select className={cn(CLASE_BASE_CAMPO, 'h-9', className)} {...props}>
      {children}
    </select>
  )
}

export function Label({ className, ...props }) {
  return (
    <label
      className={cn('mb-1 block text-xs font-medium text-muted', className)}
      {...props}
    />
  )
}

export function Field({ label, htmlFor, children }) {
  return (
    <div>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
    </div>
  )
}
