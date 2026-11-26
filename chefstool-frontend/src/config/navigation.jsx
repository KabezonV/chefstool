// Vendors
import {
  BarChart3,
  BookOpen,
  CalendarDays,
  FileText,
  LayoutDashboard,
  MapPin,
  Package,
  ReceiptText,
  Settings,
  ShieldCheck,
  ShoppingCart,
  Truck,
  UserCog,
  Users,
} from 'lucide-react'

/**
 * Entradas del menú. `soloGestores` la oculta a los miembros: el backend también
 * responde 403, así que la pantalla no es accesible aunque se escriba la URL.
 */
export const navItems = [
  { to: '/dashboard', label: 'Inicio', icon: LayoutDashboard },
  { to: '/documentos', label: 'Documentos', icon: FileText },
  { to: '/compras', label: 'Compras', icon: ShoppingCart },
  { to: '/solicitudes-compra', label: 'Solicitudes', icon: ReceiptText },
  { to: '/productos', label: 'Productos', icon: Package },
  { to: '/fichas', label: 'Fichas técnicas', icon: BookOpen },
  { to: '/proveedores', label: 'Proveedores', icon: Truck },
  { to: '/clientes', label: 'Clientes', icon: Users },
  { to: '/eventos', label: 'Eventos', icon: CalendarDays },
  { to: '/informes', label: 'Informes', icon: BarChart3 },
  { to: '/locales', label: 'Locales', icon: MapPin },
  { to: '/usuarios', label: 'Usuarios', icon: UserCog },
  { to: '/auditoria', label: 'Auditoría', icon: ShieldCheck, soloGestores: true },
  { to: '/configuracion', label: 'Configuración', icon: Settings },
]
