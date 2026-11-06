// Vendors
import { ArrowLeft, ChefHat } from 'lucide-react'
import { Link } from 'react-router-dom'

// Utils
import { SECCIONES_PRIVACIDAD } from './utils/secciones'

export default function Privacidad() {
  return (
    <div className="min-h-dvh bg-background px-4 py-10">
      <div className="mx-auto max-w-2xl space-y-6">
        <div className="flex items-center gap-2">
          <span className="grid size-10 place-items-center rounded-xl bg-primary text-primary-foreground">
            <ChefHat className="size-5" />
          </span>
          <div>
            <p className="text-lg font-semibold tracking-tight">Chefstool</p>
            <p className="text-xs text-muted">Aviso de privacidad</p>
          </div>
        </div>

        <div className="space-y-4 rounded-xl border border-border bg-card p-5">
          <h1 className="text-xl font-semibold tracking-tight">Protección de datos</h1>
          <p className="text-sm text-muted">
            Resumen en lenguaje claro de cómo tratamos los datos personales, conforme al
            Reglamento (UE) 2016/679 (RGPD) y a la LOPDGDD.
          </p>

          {SECCIONES_PRIVACIDAD.map(({ titulo, texto }) => (
            <section key={titulo} className="space-y-1">
              <h2 className="text-sm font-semibold">{titulo}</h2>
              <p className="text-sm text-muted">{texto}</p>
            </section>
          ))}

          <p className="border-t border-border pt-3 text-xs text-muted">
            Última actualización: pendiente de revisión legal antes de la puesta en producción.
          </p>
        </div>

        <Link
          to="/auth"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Volver al acceso
        </Link>
      </div>
    </div>
  )
}
