# Chefstool

Control de compras y costes para cocinas profesionales: sube el albarán, revísalo y
obtén costes reales, márgenes y comparativas de proveedores.

- `chefstool-backend`: Java 25 (LTS) + Spring Boot 4 + Maven + JPA/PostgreSQL + Flyway.
- `chefstool-frontend`: React 19 + Vite + Tailwind CSS 4 + React Router (PWA instalable).

## Qué hace

| Módulo | Para qué sirve |
| --- | --- |
| Documentos | Foto o archivo del albarán → extracción → revisión → compra confirmada |
| Compras | Histórico de compras con filtros y detalle de líneas |
| Productos | Catálogo normalizado con histórico de precios y comparativa por proveedor |
| Proveedores / Clientes / Eventos | Imputación de compras y costes por servicio |
| Fichas técnicas | Escandallos con coste por ración y margen sobre precio de venta |
| Alertas | Aviso automático cuando un proveedor sube el precio ≥5 % |
| Dashboard e informes | Gasto mensual, gasto por proveedor, variaciones y ahorro potencial |
| Usuarios y configuración | Organización, miembros y roles |

## Arranque en local

### Requisitos

- JDK 25 (`java -version` debe mostrar 25.x)
- Node.js 20 o superior
- Una base de datos PostgreSQL (el proyecto usa Supabase)

### Backend

Las credenciales **no** están en el repositorio. Crea el archivo `.env` a partir
de la plantilla:

```bash
cd chefstool-backend
cp .env.example .env   # rellena DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080/api`. El esquema lo gestiona **Flyway**
(`src/main/resources/db/migration`): las migraciones se aplican solas al arrancar
y Hibernate solo valida que el modelo encaje.

### Frontend

```bash
cd chefstool-frontend
npm install
npm run dev
```

Vite sirve en `http://localhost:5173` y redirige `/api` al backend (`proxy` en
`vite.config.js`), así que en desarrollo no hay que configurar CORS.

### Primer uso

1. Abre la aplicación: te llevará a `/auth`.
2. Pulsa **Crear cuenta**. Al registrarte se crea tu organización y quedas como
   *propietario*.
3. Entra en **Documentos → Añadir documento → Probar con un ejemplo** para generar
   un albarán de demostración (no necesita cámara).
4. Revisa las líneas y pulsa **Confirmar y guardar**.
5. El dashboard y los informes se rellenan con la compra confirmada.

## Variables de entorno

| Variable | Dónde | Para qué |
| --- | --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `chefstool-backend/.env` | Conexión a PostgreSQL |
| `CHEFSTOOL_JWT_SECRET` | entorno del backend | Clave HMAC para firmar los JWT (mínimo 32 caracteres) |
| `CHEFSTOOL_SEGURIDAD` | entorno del backend | `false` desactiva la autenticación (solo para pruebas) |
| `CHEFSTOOL_UPLOADS` | entorno del backend | Carpeta donde se guardan los albaranes (por defecto `./uploads`) |
| `VITE_API_URL` | `chefstool-frontend/.env` | Base de la API (por defecto `/api`, que usa el proxy) |

## Autenticación y roles

- **JWT firmado con HS256**, válido 12 horas. El token viaja en
  `Authorization: Bearer …`.
- Todas las rutas `/api/**` exigen token salvo `/api/auth/login` y
  `/api/auth/registro`.
- Cada usuario pertenece a una **organización**; todos los datos se filtran por
  `organizacion_id`, así que dos cocinas nunca ven datos ajenas.
- Roles: `propietario`, `admin`, `miembro`. La organización no puede quedarse sin
  al menos un propietario.
- En los tests la seguridad se desactiva con el perfil `test`
  (`application-test.properties`), salvo en `AuthControllerTest`, que la ejercita
  de verdad.

## Arquitectura

El backend sigue una **arquitectura hexagonal pragmática**:

```
com.chefstool.backend
├── domain/model        Entidades y conceptos del negocio (Producto, Compra, Ficha…)
├── domain/port/out     Puertos de salida: repositorios, almacenamiento y OCR
├── application         Casos de uso; solo dependen del dominio y de los puertos
├── adapter/in/web      Controladores REST, DTOs y manejo de errores
├── adapter/out         Implementaciones: Spring Data, OCR y almacenamiento
└── config              Seguridad (JWT, BCrypt, filtros)
```

Las dependencias apuntan siempre hacia dentro: `adapter → application → domain`.
Cambiar el motor de OCR, sustituir el disco por Supabase Storage o migrar de base
de datos no obliga a tocar los casos de uso.

Dos compromisos conscientes para no añadir indirección innecesaria a este tamaño:

- Las entidades JPA son también el modelo de dominio. Separarlas exigiría duplicar
  el mapeo de 13 entidades; se hará el día que cambie el motor de persistencia.
- Los repositorios de Spring Data viven en `domain/port/out` como puertos; Spring
  genera las implementaciones, que actúan como adaptador implícito.

### Modelo de datos

`organizaciones`, `usuarios`, `miembros_organizacion`, `productos`,
`proveedores`, `proveedor_productos`, `clientes`, `eventos`,
`documentos_compra`, `compras`, `lineas_compra`, `fichas`, `ficha_lineas` y
`alertas_precio`.

### OCR

La extracción está detrás de la interfaz `ExtractorDocumento`. Hoy se usa
`ExtractorDemo`, que **no lee el archivo**: genera una extracción realista con el
catálogo de la organización para poder recorrer el flujo completo. Para conectar
un motor real (Mindee, Google Vision, un modelo de visión…) basta con añadir otra
implementación y marcarla como primaria.

## API

Referencia completa con ejemplos en [docs/API.md](docs/API.md).

| Recurso | Ruta |
| --- | --- |
| Autenticación | `/api/auth/registro`, `/api/auth/login`, `/api/auth/yo` |
| Productos | `/api/productos` (+ `/api/productos/{id}/precios`) |
| Proveedores | `/api/proveedores` |
| Clientes | `/api/clientes` |
| Eventos | `/api/eventos` |
| Documentos | `/api/documentos` (+ `/confirmar`, `/archivo`) |
| Compras | `/api/compras` |
| Alertas de precio | `/api/alertas` |
| Fichas técnicas | `/api/fichas` |
| Panel e informes | `/api/informes/panel` |
| Usuarios y organización | `/api/usuarios`, `/api/organizacion` |

Los errores se devuelven como `{"error": "mensaje"}` con 400 (datos no válidos o
duplicados), 401 (credenciales o token), 404 (registro inexistente) o 409
(documento ya confirmado).

## PWA

La app es instalable en tablet y móvil: manifest (`public/manifest.webmanifest`),
iconos 192/512 e instalación de service worker en `src/main.jsx`.

El service worker cachea **solo el shell**; las llamadas a `/api/**` nunca se
sirven desde caché, para no mostrar datos financieros obsoletos.

## Calidad

```bash
cd chefstool-backend && ./mvnw clean test   # 45 tests
cd chefstool-frontend && npm run lint && npm run build
```

## Cronograma

| Etapa | Alcance | Estado |
| --- | --- | --- |
| 0 | Java 25, git, secretos fuera del código, esqueleto del frontend | Completada |
| 1 | Modelo de datos (Flyway) y entidades JPA | Completada |
| 2 | CRUDs de productos, proveedores, clientes y eventos | Completada |
| 3 | Documentos + OCR (cámara, extracción, revisión y confirmación) | Completada |
| 4 | Compras, histórico de precios y alertas de subida | Completada |
| 5 | Fichas técnicas y escandallos | Completada |
| 6 | Dashboard e informes | Completada |
| 7 | Autenticación, organizaciones, roles y PWA | Completada |

## Siguientes pasos

1. **OCR real**: elegir proveedor, implementar `ExtractorDocumento` y probar con
   albaranes de verdad. Es lo único que separa la demo de una herramienta real.
2. **Despliegue**: ver [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).
3. **Invitaciones por correo**: hoy un miembro nuevo debe registrarse con su propia
   organización; falta el envío de invitaciones y la comprobación de permisos por
   rol en cada endpoint.
