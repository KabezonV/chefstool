import ResourcePage from '@/components/crud/ResourcePage'
import { productos } from '@/config/recursos'

export default function Productos() {
  return <ResourcePage recurso={productos} />
}
