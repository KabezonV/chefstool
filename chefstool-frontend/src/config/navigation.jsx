import {
  BarChart3,
  BookOpen,
  CalendarDays,
  FileText,
  LayoutDashboard,
  Package,
  Settings,
  ShoppingCart,
  Truck,
  UserCog,
  Users,
} from 'lucide-react'

export const navItems = [
  { to: '/dashboard', label: 'Inicio', icon: LayoutDashboard },
  { to: '/documentos', label: 'Documentos', icon: FileText },
  { to: '/compras', label: 'Compras', icon: ShoppingCart },
  { to: '/productos', label: 'Productos', icon: Package },
  { to: '/fichas', label: 'Fichas técnicas', icon: BookOpen },
  { to: '/proveedores', label: 'Proveedores', icon: Truck },
  { to: '/clientes', label: 'Clientes', icon: Users },
  { to: '/eventos', label: 'Eventos', icon: CalendarDays },
  { to: '/informes', label: 'Informes', icon: BarChart3 },
  { to: '/usuarios', label: 'Usuarios', icon: UserCog },
  { to: '/configuracion', label: 'Configuración', icon: Settings },
]
