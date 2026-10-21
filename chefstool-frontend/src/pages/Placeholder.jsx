import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

export default function Placeholder({ title, description }) {
  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="flex items-center gap-3">
        <h1 className="text-xl font-semibold tracking-tight">{title}</h1>
        <Badge tone="warning">En construcción</Badge>
      </div>
      <Card>
        <CardContent className="pt-5">
          <p className="text-sm text-muted">
            {description ?? 'Este módulo se implementará en una etapa posterior.'}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
