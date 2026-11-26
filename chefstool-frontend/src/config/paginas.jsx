// Components
import Auditoria from '@/pages/Auditoria'
import Clientes from '@/pages/Clientes'
import Compras from '@/pages/Compras'
import Configuracion from '@/pages/Configuracion'
import Documentos from '@/pages/Documentos'
import Eventos from '@/pages/Eventos'
import Fichas from '@/pages/Fichas'
import Informes from '@/pages/Informes'
import Locales from '@/pages/Locales'
import Productos from '@/pages/Productos'
import Proveedores from '@/pages/Proveedores'
import SolicitudesCompra from '@/pages/SolicitudesCompra'
import Usuarios from '@/pages/Usuarios'

/** Ruta del menú -> pantalla. El menú vive en config/navigation.jsx. */
export const PAGINAS = {
  '/productos': <Productos />,
  '/proveedores': <Proveedores />,
  '/clientes': <Clientes />,
  '/eventos': <Eventos />,
  '/documentos': <Documentos />,
  '/compras': <Compras />,
  '/solicitudes-compra': <SolicitudesCompra />,
  '/fichas': <Fichas />,
  '/informes': <Informes />,
  '/locales': <Locales />,
  '/usuarios': <Usuarios />,
  '/auditoria': <Auditoria />,
  '/configuracion': <Configuracion />,
}
