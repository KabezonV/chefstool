import { cn } from '@/lib/utils'

export function Card({ className, ...props }) {
  return (
    <div
      className={cn('rounded-xl border border-border bg-card', className)}
      {...props}
    />
  )
}

export function CardHeader({ className, ...props }) {
  return (
    <div
      className={cn('flex items-center justify-between gap-3 px-5 py-4', className)}
      {...props}
    />
  )
}

export function CardTitle({ className, ...props }) {
  return (
    <h2
      className={cn('text-sm font-semibold tracking-tight', className)}
      {...props}
    />
  )
}

export function CardContent({ className, ...props }) {
  return <div className={cn('px-5 pb-5', className)} {...props} />
}
