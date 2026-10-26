// Enumerations
import { ROLES_DE_GESTION } from '@/constants/roles'

/** Propietarios y administradores pueden gestionar equipo y borrar del catálogo. */
export function esGestor(rol) {
  return ROLES_DE_GESTION.includes(rol)
}
