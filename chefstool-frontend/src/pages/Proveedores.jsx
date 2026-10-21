import ResourcePage from '@/components/crud/ResourcePage'
import { proveedores } from '@/config/recursos'

export default function Proveedores() {
  return <ResourcePage recurso={proveedores} />
}
