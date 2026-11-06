// Vendors
import { Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'

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

/** Campo de contraseña con botón para mostrarla u ocultarla. */
export function InputContrasena({ className, ...props }) {
  const [visible, setVisible] = useState(false)

  return (
    <div className="relative">
      <Input
        type={visible ? 'text' : 'password'}
        className={cn('pr-11', className)}
        {...props}
      />
      <button
        type="button"
        onClick={() => setVisible((valor) => !valor)}
        aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        aria-pressed={visible}
        title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        className="absolute inset-y-0 right-0 grid w-11 place-items-center rounded-r-lg text-muted transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-foreground/20 focus-visible:outline-none"
      >
        {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
      </button>
    </div>
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
