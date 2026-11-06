// Vendors
import { ChefHat, LogOut, Menu, Plus, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'

// Utils
import { cn } from '@/lib/utils'

// Enumerations
import { navItems } from '@/config/navigation'

// Hooks
import { useAuth } from '@/lib/auth-context'

// Components
import { Button } from '@/components/ui/button'
import BotonTema from '@/components/ui/tema'

export default function AppShell() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { sesion, salir } = useAuth()

  useEffect(() => {
    if (!menuOpen) return

    const { overflow } = document.body.style
    document.body.style.overflow = 'hidden'

    function cerrarConEscape(event) {
      if (event.key === 'Escape') setMenuOpen(false)
    }

    window.addEventListener('keydown', cerrarConEscape)

    return () => {
      document.body.style.overflow = overflow
      window.removeEventListener('keydown', cerrarConEscape)
    }
  }, [menuOpen])

  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[16rem_1fr]">
      {menuOpen && (
        <button
          type="button"
          aria-label="Cerrar menú"
          onClick={() => setMenuOpen(false)}
          className="fixed inset-0 z-30 bg-black/20 lg:hidden"
        />
      )}

      <aside
        id="menu-lateral"
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-64 max-w-[85vw] flex-col border-r border-border bg-card transition-transform lg:static lg:translate-x-0',
          menuOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center justify-between gap-2 px-4 py-4">
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
                        ? 'bg-active font-medium text-foreground'
                        : 'text-muted hover:bg-hover hover:text-foreground',
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
          <div className="mt-2 flex items-center justify-between gap-2">
            <BotonTema variante="texto" />
            <button
              type="button"
              onClick={salir}
              className="inline-flex items-center gap-1 text-xs text-muted hover:text-foreground"
            >
              <LogOut className="size-3.5" />
              Cerrar sesión
            </button>
          </div>
        </div>
      </aside>

      <div className="flex min-h-dvh min-w-0 flex-col">
        <header className="sticky top-0 z-20 flex items-center justify-between gap-3 border-b border-border bg-background/85 px-4 py-3 backdrop-blur lg:px-8">
          <div className="flex min-w-0 items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              className="lg:hidden"
              onClick={() => setMenuOpen(true)}
              aria-label="Abrir menú"
              aria-expanded={menuOpen}
              aria-controls="menu-lateral"
            >
              <Menu className="size-4" />
            </Button>
            <span className="truncate text-sm text-muted lg:hidden">Chefstool</span>
          </div>
          <Button asChild size="sm" className="shrink-0 lg:hidden">
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
