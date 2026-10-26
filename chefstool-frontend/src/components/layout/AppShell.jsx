// Vendors
import { ChefHat, LogOut, Menu, Plus, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'

// Utils
import { cn } from '@/lib/utils'

// Enumerations
import { navItems } from '@/config/navigation'

// Hooks
import { useAuth } from '@/lib/auth-context'

// Components
import { Button } from '@/components/ui/button'

export default function AppShell() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { sesion, salir } = useAuth()

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[16rem_1fr]">
      {menuOpen && (
        <button
          type="button"
          aria-label="Cerrar menú"
          onClick={() => setMenuOpen(false)}
          className="fixed inset-0 z-30 bg-black/20 lg:hidden"
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-border bg-card transition-transform lg:static lg:translate-x-0',
          menuOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center justify-between px-4 py-4">
          <Link to="/dashboard" className="flex items-center gap-2">
            <span className="grid size-8 place-items-center rounded-lg bg-primary text-primary-foreground">
              <ChefHat className="size-4" />
            </span>
            <span className="text-sm font-semibold tracking-tight">Chefstool</span>
          </Link>
          <Button
            variant="ghost"
            size="icon"
            className="lg:hidden"
            onClick={() => setMenuOpen(false)}
            aria-label="Cerrar menú"
          >
            <X className="size-4" />
          </Button>
        </div>

        <div className="px-3 pb-3">
          <Button asChild variant="accent" className="w-full justify-start">
            <Link to="/documentos/nuevo" onClick={() => setMenuOpen(false)}>
              <Plus className="size-4" />
              Añadir documento
            </Link>
          </Button>
        </div>

        <nav className="flex-1 overflow-y-auto px-2 pb-4">
          <ul className="space-y-0.5">
            {navItems.map(({ to, label, icon: Icon }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  onClick={() => setMenuOpen(false)}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors',
                      isActive
                        ? 'bg-black/[0.06] font-medium text-foreground'
                        : 'text-muted hover:bg-black/[0.03] hover:text-foreground',
                    )
                  }
                >
                  <Icon className="size-4 shrink-0" />
                  {label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="border-t border-border px-4 py-3">
          <p className="truncate text-sm font-medium">{sesion?.nombre ?? 'Invitado'}</p>
          <p className="truncate text-xs text-muted">
            {sesion?.organizacion ?? 'Sin organizacion'} · {sesion?.rol ?? ''}
          </p>
          <button
            type="button"
            onClick={salir}
            className="mt-2 inline-flex items-center gap-1 text-xs text-muted hover:text-foreground"
          >
            <LogOut className="size-3.5" />
            Cerrar sesión
          </button>
        </div>
      </aside>

      <div className="flex min-h-screen flex-col">
        <header className="sticky top-0 z-20 flex items-center justify-between gap-3 border-b border-border bg-background/85 px-4 py-3 backdrop-blur lg:px-8">
          <Button
            variant="ghost"
            size="icon"
            className="lg:hidden"
            onClick={() => setMenuOpen(true)}
            aria-label="Abrir menú"
          >
            <Menu className="size-4" />
          </Button>
          <span className="text-sm text-muted lg:hidden">Chefstool</span>
          <div className="hidden lg:block" />
          <Button asChild size="sm" className="lg:hidden">
            <Link to="/documentos/nuevo">
              <Plus className="size-4" />
              Añadir
            </Link>
          </Button>
        </header>

        <main className="flex-1 px-4 py-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
