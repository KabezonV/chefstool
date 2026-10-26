// Enumerations
import { clientes } from '@/config/recursos'

// Components
import ResourcePage from '@/components/crud/ResourcePage'

export default function Clientes() {
  return <ResourcePage recurso={clientes} />
}
