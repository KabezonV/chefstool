// Enumerations
import { proveedores } from '@/config/recursos'

// Components
import ResourcePage from '@/components/crud/ResourcePage'

export default function Proveedores() {
  return <ResourcePage recurso={proveedores} />
}
