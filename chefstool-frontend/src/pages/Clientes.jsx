import ResourcePage from '@/components/crud/ResourcePage'
import { clientes } from '@/config/recursos'

export default function Clientes() {
  return <ResourcePage recurso={clientes} />
}
