import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '@/components/layout/AppShell'
import { navItems } from '@/config/navigation'
import Dashboard from '@/pages/Dashboard'
import Placeholder from '@/pages/Placeholder'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />

        {navItems
          .filter(({ to }) => to !== '/dashboard')
          .map(({ to, label }) => (
            <Route key={to} path={to} element={<Placeholder title={label} />} />
          ))}

        <Route
          path="/documentos/nuevo"
          element={
            <Placeholder
              title="Nuevo documento"
              description="Captura con cámara, subida de albarán y extracción OCR: Etapa 3."
            />
          }
        />

        <Route
          path="*"
          element={
            <Placeholder
              title="Página no encontrada"
              description="La ruta solicitada no existe."
            />
          }
        />
      </Route>
    </Routes>
  )
}
