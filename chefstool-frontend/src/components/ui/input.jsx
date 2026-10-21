import { cn } from '@/lib/utils'

const base =
  'w-full rounded-lg border border-border bg-card px-3 text-sm outline-none transition-colors placeholder:text-muted/60 focus-visible:ring-2 focus-visible:ring-foreground/20'

export function Input({ className, ...props }) {
  return <input className={cn(base, 'h-9', className)} {...props} />
}

export function Textarea({ className, ...props }) {
  return <textarea className={cn(base, 'min-h-20 py-2', className)} {...props} />
}

export function Select({ className, children, ...props }) {
  return (
    <select className={cn(base, 'h-9', className)} {...props}>
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
