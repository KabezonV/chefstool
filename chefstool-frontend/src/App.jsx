import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '@/components/layout/AppShell'
import { navItems } from '@/config/navigation'
import { AuthProvider } from '@/lib/auth'
import { useAuth } from '@/lib/auth-context'
import Auth from '@/pages/Auth'
import Restablecer from '@/pages/Restablecer'
import Clientes from '@/pages/Clientes'
import Compras from '@/pages/Compras'
import Configuracion from '@/pages/Configuracion'
import Dashboard from '@/pages/Dashboard'
import DocumentoDetalle from '@/pages/DocumentoDetalle'
import DocumentoNuevo from '@/pages/DocumentoNuevo'
import Documentos from '@/pages/Documentos'
import Eventos from '@/pages/Eventos'
import FichaDetalle from '@/pages/FichaDetalle'
import FichaNuevo from '@/pages/FichaNuevo'
import Fichas from '@/pages/Fichas'
import Informes from '@/pages/Informes'
import Placeholder from '@/pages/Placeholder'
import Privacidad from '@/pages/Privacidad'
import ProductoDetalle from '@/pages/ProductoDetalle'
import Productos from '@/pages/Productos'
import Proveedores from '@/pages/Proveedores'
import Usuarios from '@/pages/Usuarios'

const paginas = {
  '/productos': <Productos />,
  '/proveedores': <Proveedores />,
  '/clientes': <Clientes />,
  '/eventos': <Eventos />,
  '/documentos': <Documentos />,
  '/compras': <Compras />,
  '/fichas': <Fichas />,
  '/informes': <Informes />,
  '/usuarios': <Usuarios />,
  '/configuracion': <Configuracion />,
}

function RutaPrivada() {
  const { sesion } = useAuth()
  return sesion ? <AppShell /> : <Navigate to="/auth" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/auth" element={<Auth />} />
        <Route path="/auth/restablecer" element={<Restablecer />} />
        <Route path="/privacidad" element={<Privacidad />} />

        <Route element={<RutaPrivada />}>
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
    </AuthProvider>
  )
}
