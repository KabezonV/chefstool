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
| Fichas técnicas | Escandallos con coste por ración y margen sobre precio de venta, exportables a PDF A4 |
| RGPD | Aviso de privacidad, descarga de datos personales y baja de cuenta con borrado en cascada |
| Alertas | Aviso automático cuando un proveedor sube el precio ≥5 % |
| Dashboard e informes | Gasto mensual, coste histórico por proveedor, variaciones y ahorro potencial |
| Usuarios y configuración | Organización, miembros y roles |
| Datos de ejemplo | Rellena una organización nueva con catálogo, albaranes y fichas para probar |

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

### Todo junto con Docker

```bash
cp .env.ejemplo .env      # ya viene configurado para el PostgreSQL local
docker compose up --build # PostgreSQL + API + web
```

No necesita ningún servicio externo: el `docker-compose` levanta su propio
**PostgreSQL 17** y Flyway crea las tablas al arrancar. Para usar una base gestionada
(Amazon RDS) solo cambian `DB_HOST`, `DB_USER`, `DB_PASSWORD` y `DB_SSLMODE=require`;
ver [docs/BASE-DE-DATOS.md](docs/BASE-DE-DATOS.md).

Abre <http://localhost:8081>. La imagen del backend incluye Tesseract con español, así
que la lectura de fotos de albarán funciona sin instalar nada más.

### Primer uso

1. Abre la aplicación: te llevará a `/auth`.
2. Pulsa **Crear cuenta**. Al registrarte se crea tu organización y quedas como
   *propietario*.
3. La primera vez, el dashboard ofrece **Cargar datos de ejemplo**: crea 8 productos,
   3 proveedores, 2 clientes, 2 eventos, 3 albaranes confirmados (con una alerta de
   subida de precio) y 2 fichas técnicas. También puedes hacerlo con
   `POST /api/demo/datos`.
4. Si prefieres ir paso a paso: **Documentos → Añadir documento → Probar con un ejemplo**,
   revisa las líneas y pulsa **Confirmar y guardar**.
5. El dashboard y los informes se rellenan con las compras confirmadas.

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
  (`application-test.properties`), salvo en los tests de autenticación y equipo,
  que la ejercitan de verdad.

### Lectura de albaranes con IA (opcional)

Si se define `CHEFSTOOL_OCR_VISION_CLAVE` (Google AI Studio: gratis y sin tarjeta), los
documentos se leen con un modelo de visión —incluidas fotos torcidas y tablas— y Tesseract
queda como respaldo automático si falla, no hay clave o se agota la cuota.

### Recuperar la contraseña

En la pantalla de acceso, *¿Has olvidado la contraseña?* genera un enlace de un solo
uso válido una hora. Mientras no haya proveedor de correo, el enlace aparece en el log
del backend; con `CHEFSTOOL_ENLACE_DIRECTO=true` (solo demostración) también se muestra
en la propia pantalla.

### Invitar a un compañero

En **Usuarios → Invitar a tu equipo** se invita por correo:

- Si la persona **ya tiene cuenta**, se añade a la organización al momento.
- Si **no la tiene**, la invitación queda pendiente (con su rol) y se resuelve sola
  cuando se registre con ese correo: entra en tu organización en vez de crear una
  propia.

No hace falta servicio de email: la invitación se guarda y se aplica en el
registro. Se puede cancelar desde la misma pantalla.

### Permisos por rol

| Acción | miembro | admin | propietario |
| --- | --- | --- | --- |
| Ver y trabajar con productos, albaranes, compras y fichas | ✅ | ✅ | ✅ |
| Invitar, cancelar invitaciones y cambiar roles | ❌ | ✅ | ✅ |
| Cambiar el nombre de la organización | ❌ | ✅ | ✅ |

Los permisos se comprueban en el backend con `@PreAuthorize` a partir del rol
firmado en el token, no solo ocultando botones en la interfaz.

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

La extracción está detrás de la interfaz `ExtractorDocumento` y hoy funciona **sin
contratar ningún servicio**:

| Entrada | Cómo se lee |
| --- | --- |
| PDF digital | Se extrae el texto embebido con PDFBox (máxima precisión, sin OCR) |
| Imagen o PDF escaneado | OCR con **Tesseract** si está instalado en el servidor |
| Sin archivo (`origen=demo`) | `ExtractorDemo` genera un ejemplo con tu catálogo |

El texto se interpreta con un parser heurístico (`ParserAlbaran`): proveedor,
fecha, número de documento, líneas (cantidad, unidad, precio, importe), IVA y
total. Cada línea se empareja automáticamente con el catálogo de productos.

Si el documento no se puede leer, queda **por revisar** con el aviso
correspondiente y se completa a mano: nunca se inventan datos.

Para imágenes hace falta Tesseract (una sola vez en el servidor):

```bash
brew install tesseract tesseract-lang   # macOS
apt install tesseract-ocr tesseract-ocr-spa   # Debian/Ubuntu
```

Para contratar un proveedor en la nube (Mindee, Google Vision, un modelo de
visión…) basta con añadir otra implementación de `ExtractorDocumento`, marcarla
como `@Primary` y añadir su clave como variable de entorno. Ni los casos de uso ni
la interfaz cambian.

## API

Referencia completa con ejemplos en [docs/API.md](docs/API.md).

Resumen del proyecto y propuesta de puesta en marcha para dirección:
[docs/INFORME-BERNART.md](docs/INFORME-BERNART.md).
Guion y preparación para enseñar la aplicación: [docs/DEMO.md](docs/DEMO.md).
Arranque paso a paso (para compartir con otra persona): [docs/ARRANQUE.md](docs/ARRANQUE.md).
Base de datos (PostgreSQL propio, Amazon RDS y migración): [docs/BASE-DE-DATOS.md](docs/BASE-DE-DATOS.md).
Manual de usuario para cocina: [docs/MANUAL-USUARIO.md](docs/MANUAL-USUARIO.md).
Cumplimiento y pendientes RGPD: [docs/RGPD.md](docs/RGPD.md).
Comparativa de costes de IA y servidor (para decidir proveedor): [docs/COMPARATIVA-COSTES-IA.md](docs/COMPARATIVA-COSTES-IA.md) · [versión PDF](docs/COMPARATIVA-COSTES-IA.pdf) · [versión en texto sin tablas](docs/COMPARATIVA-COSTES-IA.txt) · [anexo técnico](docs/COMPARATIVA-COSTES-IA-ANEXO.md).
Estructura del frontend y convenciones de código (constantes, utils, orden de imports):
[docs/FRONTEND-ESTRUCTURA.md](docs/FRONTEND-ESTRUCTURA.md).
Plan de la fase 2 (OCR con Amazon Textract, AWS y tareas pendientes):
[docs/PLAN-FASE-2.md](docs/PLAN-FASE-2.md).

| Recurso | Ruta |
| --- | --- |
| Autenticación | `/api/auth/registro`, `/api/auth/login`, `/api/auth/yo`, `/api/auth/recuperar`, `/api/auth/restablecer` |
| Productos | `/api/productos` (+ `/api/productos/{id}/precios`, `/api/productos/importar`) |
| Proveedores | `/api/proveedores` |
| Clientes | `/api/clientes` |
| Eventos | `/api/eventos` |
| Documentos | `/api/documentos` (+ `/confirmar`, `/archivo`) |
| Compras | `/api/compras` |
| Alertas de precio | `/api/alertas` |
| Fichas técnicas | `/api/fichas` |
| Panel e informes | `/api/informes/panel`, `/api/informes/proveedores` |
| Usuarios y organización | `/api/usuarios`, `/api/organizacion` |
| Datos de ejemplo | `/api/demo/datos` (POST) |

Los listados de productos, proveedores, clientes, eventos, documentos, compras y
fichas están **paginados** (`?pagina=0&tamano=20`), se pueden **ordenar**
(`?orden=fecha&direccion=desc`) y devuelven
`{contenido, pagina, tamano, totalElementos, totalPaginas}`. Productos, proveedores,
clientes y eventos admiten además `q` para buscar por nombre.

En la interfaz, cada listado recuerda página, tamaño, búsqueda y orden en la URL (al
volver de un detalle sigues donde estabas) y ofrece **Excel** y **CSV** con todas las
filas del filtro actual. El `.xlsx` se genera sin dependencias externas y escribe los
importes como números, así que Excel los suma y los formatea directamente.

El informe `GET /api/informes/proveedores` añade el coste histórico por proveedor:
compras, gasto total, ticket medio, peso sobre el gasto, productos distintos, última
compra y variación media de sus precios.

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
cd chefstool-backend && ./mvnw clean test          # 116 tests (necesita Docker: usa Testcontainers)
cd chefstool-frontend && npm test                  # 25 tests
cd chefstool-frontend && npm run lint && npm run build
./scripts/qa-volumen.sh                            # prueba de volumen (crea una org nueva)
```

Detalle de qué cubre cada test y la pasada de QA en navegador:
[docs/QA.md](docs/QA.md).

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
| 8 | OCR local real (PDF + Tesseract) y parser de albaranes | Completada |
| 9 | Invitaciones de equipo y permisos por rol | Completada |

## Siguientes pasos

1. **Probar con albaranes reales** y ajustar el parser a los formatos que use cada
   proveedor. Si la precisión con fotos no basta, conectar un OCR en la nube
   (el código ya está preparado: una implementación nueva y su clave).
2. **Despliegue**: ver [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).
3. **Aviso por email de las invitaciones**: hoy se guardan y se aplican al
   registrarse, pero no se envía ningún correo de aviso.
4. **Recuperar contraseña** y permisos más finos sobre los datos (por ejemplo, que
   un miembro no pueda borrar).
