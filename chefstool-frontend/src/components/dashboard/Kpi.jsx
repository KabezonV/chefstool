// Components
import { Card, CardContent } from '@/components/ui/card'

/** Tarjeta de indicador del panel: valor grande con un detalle opcional en color. */
export default function Kpi({ etiqueta, valor, detalle, tono }) {
  return (
    <Card>
      <CardContent className="pt-5">
        <p className="text-xs font-medium text-muted">{etiqueta}</p>
        <p className="mt-1 text-2xl font-semibold tracking-tight">{valor}</p>
        {detalle && (
          <p
            className={
              tono === 'danger'
                ? 'mt-1 flex items-center gap-1 text-xs text-danger'
                : tono === 'success'
                  ? 'mt-1 flex items-center gap-1 text-xs text-success'
                  : 'mt-1 text-xs text-muted'
            }
          >
            {detalle}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
