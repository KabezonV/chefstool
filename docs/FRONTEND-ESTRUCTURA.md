# Estructura del frontend

Convenciones para que el código siga siendo fácil de leer cuando crezca. No hace falta
memorizarlas: `npm run lint` (que se ejecuta también en CI) falla si los imports no están
en el orden acordado, y `npm run imports` los ordena solo.

```bash
cd chefstool-frontend
npm run imports         # ordena los imports de src/
npm run imports:check   # comprueba sin tocar nada (lo que hace el lint)
npm run lint            # oxlint + comprobación de imports
```

## 1. Dónde va cada cosa

| Qué es | Dónde va | Ejemplo |
| --- | --- | --- |
| Valores fijos de toda la app (roles, tamaños de página, claves de localStorage, estados) | `src/constants/` | `constants/roles.js` |
| Funciones puras reutilizables (formato, cálculo, permisos) | `src/lib/` | `lib/formato.js`, `lib/tonos.js` |
| Acceso a la API (el único sitio que hace `fetch`) | `src/lib/api.js` | `api.get('/productos')` |
| Hooks propios | `src/lib/useListado.js` | `useListado`, `useDebounce` |
| Configuración de navegación y de rutas | `src/config/` | `config/navigation.jsx`, `config/paginas.jsx` |
| Componentes reutilizables | `src/components/` | `components/ui/button.jsx` |
| Pantallas | `src/pages/` | `pages/Compras/index.jsx` |

### Reglas

1. **Nada fijo dentro de un componente.** Si un objeto o array no cambia nunca, va a
   `src/constants/`. Si es una función que no usa estado, va a `src/lib/` o al `utils/` de
   la vista.
2. **Dentro de un componente solo quedan**: el componente, sus handlers y sus hooks
   (`useState`, `useEffect`, `useMemo`).
3. **Una vista con helpers propios pasa a carpeta**:

   ```text
   src/pages/Compras/
   ├── index.jsx            # la pantalla
   └── utils/
       └── columnas.js      # columnas y demás constantes de esa pantalla
   ```

   Si la vista no necesita nada (por ejemplo `pages/Clientes.jsx`, que solo renderiza
   `ResourcePage`), se queda como fichero suelto. `@/pages/Compras` sigue funcionando en
   los dos casos porque Vite resuelve el `index.jsx`.

4. **Los `utils/` de una vista no se importan desde otra vista.** Si dos pantallas
   necesitan lo mismo, sube la función a `src/lib/` (así se hizo con `tonoMargen`, que la
   usan `Fichas` y `FichaDetalle`) o la constante a `src/constants/`.

## 2. Orden de las importaciones

Siete grupos, siempre en este orden, con una línea en blanco entre ellos y la etiqueta
delante. `npm run imports` lo deja así:

```js
// Vendors
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BellRing } from 'lucide-react'

// Types
// (import type … si algún día se pasa a TypeScript)

// Utils
import { fechaCorta, moneda } from '@/lib/formato'
import { COLUMNAS_COMPRAS } from './utils/columnas'

// Enumerations
import { TONO_ALERTA } from '@/constants/tonos'

// Handlers
import { api, listarTodo } from '@/lib/api'

// Hooks
import { useListado } from '@/lib/useListado'

// Components
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

// Styles
import './index.css'
```

Qué entra en cada grupo:

| Grupo | Contenido |
| --- | --- |
| **Vendors** | Paquetes de `node_modules` (`react`, `react-router-dom`, `lucide-react`…) |
| **Types** | `import type` (cuando se añada TypeScript o JSDoc con tipos) |
| **Utils** | `@/lib/*` que no sean API ni hooks, y los `./utils/*` de la propia vista |
| **Enumerations** | `@/constants/*` y `@/config/*` |
| **Handlers** | `@/lib/api` (cliente HTTP, `listarTodo`, `conParametros`) |
| **Hooks** | `useListado`, `useDebounce`, `useAuth`, `AuthProvider` |
| **Components** | `@/components/*` y `@/pages/*` |
| **Styles** | `*.css` |

Dentro de cada grupo se ordena alfabéticamente y los `@/…` van antes que los `./…`.

## 3. Mapa rápido

```text
src/
├── constants/          # valores fijos (roles, paginación, tonos, claves, estados)
├── lib/                # api, formato, cálculo, permisos, tonos, hooks
├── config/             # menú y mapa de rutas
├── components/
│   ├── ui/             # button, input, card, badge, tabla, paginador…
│   │   └── utils/      # clases y variantes de esos componentes
│   ├── crud/           # ResourcePage genérica + utils del formulario
│   ├── dashboard/      # tarjeta Kpi
│   └── layout/         # AppShell
└── pages/
    ├── Compras/{index.jsx,utils/}
    ├── Documentos/{index.jsx,utils/}
    ├── DocumentoDetalle/{index.jsx,utils/}
    ├── DocumentoNuevo/{index.jsx,utils/}
    ├── Fichas/{index.jsx,utils/}
    ├── Informes/{index.jsx,utils/}
    ├── Privacidad/{index.jsx,utils/}
    └── … (las vistas simples siguen siendo un fichero suelto)
```
