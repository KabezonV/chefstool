export const ROLES = [
  { valor: 'propietario', texto: 'Propietario' },
  { valor: 'admin', texto: 'Administrador' },
  { valor: 'miembro', texto: 'Miembro' },
]

export const ROL_POR_DEFECTO = 'miembro'

/** Roles que pueden gestionar equipo, organización y borrados de catálogo. */
export const ROLES_DE_GESTION = ['propietario', 'admin']

export const ETIQUETA_ROL = Object.fromEntries(ROLES.map(({ valor, texto }) => [valor, texto]))
