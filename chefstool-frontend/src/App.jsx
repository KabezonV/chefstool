import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '@/components/layout/AppShell'
import { navItems } from '@/config/navigation'
import Clientes from '@/pages/Clientes'
import Compras from '@/pages/Compras'
import Dashboard from '@/pages/Dashboard'
import DocumentoDetalle from '@/pages/DocumentoDetalle'
import DocumentoNuevo from '@/pages/DocumentoNuevo'
import Documentos from '@/pages/Documentos'
import FichaDetalle from '@/pages/FichaDetalle'
import FichaNuevo from '@/pages/FichaNuevo'
import Fichas from '@/pages/Fichas'
import Eventos from '@/pages/Eventos'
import Placeholder from '@/pages/Placeholder'
import ProductoDetalle from '@/pages/ProductoDetalle'
import Productos from '@/pages/Productos'
import Proveedores from '@/pages/Proveedores'

const paginas = {
  '/documentos': <Documentos />,
  '/compras': <Compras />,
  '/fichas': <Fichas />,
  '/productos': <Productos />,
  '/proveedores': <Proveedores />,
  '/clientes': <Clientes />,
  '/eventos': <Eventos />,
}

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />

        {navItems
          .filter(({ to }) => to !== '/dashboard')
          .map(({ to, label }) => (
            <Route
              key={to}
              path={to}
              element={paginas[to] ?? <Placeholder title={label} />}
            />
          ))}

        <Route path="/productos/:id" element={<ProductoDetalle />} />
        <Route path="/fichas/nuevo" element={<FichaNuevo />} />
        <Route path="/fichas/:id" element={<FichaDetalle />} />
        <Route path="/documentos/nuevo" element={<DocumentoNuevo />} />
        <Route path="/documentos/:id" element={<DocumentoDetalle />} />

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
