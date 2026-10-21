# Chefstool

Control de compras y costes para cocinas profesionales.

- `chefstool-backend`: Java 25 (LTS) + Spring Boot 4 + Maven + JPA/PostgreSQL.
- `chefstool-frontend`: React 19 + Vite + Tailwind CSS 4 + React Router.

## Arranque en local

### Backend

Las credenciales de Supabase **no** están en el repositorio. Crea el archivo
`chefstool-backend/.env` a partir de la plantilla:

```bash
cd chefstool-backend
cp .env.example .env   # rellena DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080/api`, por ejemplo `GET /api/productos`.

El esquema de base de datos lo gestiona Flyway (`src/main/resources/db/migration`).
Las migraciones se aplican solas al arrancar; Hibernate solo valida el modelo.

### Frontend

```bash
cd chefstool-frontend
npm install
npm run dev
```

Vite sirve en `http://localhost:5173` y redirige `/api` al backend (`proxy` en
`vite.config.js`), así que no hay que configurar CORS en desarrollo.

## Arquitectura

El backend sigue una **arquitectura hexagonal pragmática**:

```
com.chefstool.backend
├── domain/model        Entidades y conceptos del negocio (Producto, Compra, Ficha…)
├── domain/port/out     Puertos de salida: repositorios, almacenamiento y OCR
├── application         Casos de uso; solo dependen del dominio y de los puertos
├── adapter/in/web      Controladores REST, DTOs y manejo de errores
└── adapter/out         Implementaciones: Spring Data, OCR y almacenamiento
```

Las dependencias apuntan siempre hacia dentro: `adapter → application → domain`.
Cambiar el motor de OCR, sustituir el disco por Supabase Storage o migrar de base
de datos no obliga a tocar los casos de uso.

Dos compromisos conscientes para no añadir indirección innecesaria a este tamaño:

- Las entidades JPA son también el modelo de dominio. Separarlas exigiría duplicar
  el mapeo de 13 entidades; se hará el día que cambie el motor de persistencia.
- Los repositorios de Spring Data viven en `domain/port/out` como puertos; Spring
  genera las implementaciones, que actúan como adaptador implícito.

## API

Todos los recursos exponen el mismo contrato: `GET` (listado), `POST` (alta),
`PUT /{id}` (edición) y `DELETE /{id}` (borrado lógico, responde 204).

| Recurso | Ruta |
| --- | --- |
| Productos | `/api/productos` |
| Proveedores | `/api/proveedores` |
| Clientes | `/api/clientes` |
| Eventos | `/api/eventos` |
| Compras | `/api/compras` (filtros `proveedorId`, `desde`, `hasta`) |
| Alertas de precio | `/api/alertas` (`todas=true` incluye reconocidas) |
| Fichas técnicas | `/api/fichas` |
| Panel e informes | `/api/informes/panel` |

Los errores se devuelven como `{"error": "mensaje"}` con 400 (datos no válidos o
duplicados), 404 (registro inexistente) o 409 (documento ya confirmado).

### Documentos y OCR

| Método | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/documentos` | Multipart con `archivo` y `origen` (`subida`/`camara`) |
| `POST` | `/api/documentos` | Con solo `origen=demo` genera un albarán de ejemplo |
| `GET` | `/api/documentos/{id}/archivo` | Abre el original |
| `PUT` | `/api/documentos/{id}` | Guarda el borrador revisado |
| `POST` | `/api/documentos/{id}/confirmar` | Crea la compra y actualiza precios |
| `DELETE` | `/api/documentos/{id}` | Descarta el documento |

### Panel

`GET /api/informes/panel` devuelve en una sola llamada los KPIs del mes, la
variación frente al mes anterior, el gasto por proveedor y por mes, las alertas,
los documentos pendientes y el ahorro potencial (diferencia entre lo pagado y el
mejor precio histórico de cada producto).

### Fichas técnicas

`/api/fichas` permite crear escandallos con sus ingredientes. El coste de cada
línea se calcula automáticamente con el **mejor precio vigente** del producto
entre todos los proveedores, y el margen sale del precio de venta por ración.

### Precios

`GET /api/productos/{id}/precios` devuelve el histórico del producto: precio actual
por proveedor, mínimo y máximo, mejor proveedor y la evolución cronológica.

`POST /api/alertas/{id}/reconocer` marca una alerta como revisada.

### OCR

El OCR está detrás de la interfaz `ExtractorDocumento`. Hoy se usa
`ExtractorDemo`, que genera una extracción realista con el catálogo actual; para
conectar un motor real basta con añadir otra implementación.

## Requisitos

- JDK 25 (`java -version` debe mostrar 25.x)
- Node.js 20 o superior

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
| 7 | Autenticación, organizaciones, roles y PWA | Pendiente |
