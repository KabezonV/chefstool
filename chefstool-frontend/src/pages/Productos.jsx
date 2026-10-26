// Enumerations
import { productos } from '@/config/recursos'

// Components
import ResourcePage from '@/components/crud/ResourcePage'

export default function Productos() {
  return <ResourcePage recurso={productos} />
}
