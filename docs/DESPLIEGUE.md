# Despliegue

## Resumen

Son dos piezas independientes:

| Pieza | Qué es | Dónde va |
| --- | --- | --- |
| `chefstool-backend` | JAR de Spring Boot (Java 25) | Cualquier hosting con JVM: Railway, Fly.io, Render, Azure App Service, un VPS… |
| `chefstool-frontend` | Sitio estático (HTML/JS/CSS) | Netlify, Vercel, Cloudflare Pages, Nginx… |
| Base de datos | PostgreSQL (Supabase) | Ya existe |
| Albaranes | Archivos | Disco del backend (por defecto) o Supabase Storage |

## 1. Base de datos

Cualquier **PostgreSQL 14 o superior** sirve: un contenedor propio, un servidor con Docker
o una instancia gestionada (Amazon RDS, Aurora, Azure Database…). El esquema lo aplica
Flyway al arrancar el backend, no hay que ejecutar SQL a mano.

Para Amazon RDS: instancia PostgreSQL 17 `db.t4g.micro` (free tier 12 meses), 20 GB,
copias automáticas 7 días, security group que solo permita el servidor de la aplicación y
`DB_SSLMODE=require`. Guía completa y comparativa de costes:
[MIGRACION-BASE-DE-DATOS.md](MIGRACION-BASE-DE-DATOS.md).

- Usa una base de datos **distinta** para producción y para desarrollo.
- Si la base es nueva, arranca el backend una vez: las migraciones V1–V6 se aplican solas.
- **Copia de seguridad**: activa los backups automáticos de Supabase y prueba una
  restauración antes de dar acceso a usuarios reales.

> El pool es pequeño a propósito (`DB_POOL_MAX`, 5 por defecto) para no agotar las
> conexiones de instancias gestionadas pequeñas. Con varias instancias, reparte ese
> número o usa un pooler (RDS Proxy).

## 2. Backend

### Variables de entorno

```bash
# Obligatorias
DB_HOST=...
DB_PORT=5432
DB_NAME=chefstool
DB_SSLMODE=require
DB_USER=...
DB_PASSWORD=...
CHEFSTOOL_JWT_SECRET=<cadena aleatoria de 32+ caracteres>

# Segun el entorno
CHEFSTOOL_CORS=https://app.tudominio.com      # origenes del frontend, separados por comas
CHEFSTOOL_ENLACE_DIRECTO=false                # true solo en demostracion
CHEFSTOOL_ALMACEN=supabase                    # local | supabase
SUPABASE_URL=https://TU_PROYECTO.supabase.co
SUPABASE_SERVICE_KEY=...                      # service_role, nunca en el frontend
SUPABASE_BUCKET=documentos
CHEFSTOOL_OCR_TESSERACT=tesseract
CHEFSTOOL_OCR_IDIOMA=spa
```

> El backend **no arranca** si `CHEFSTOOL_JWT_SECRET` tiene menos de 32 caracteres, ni
> con el secreto de desarrollo cuando `SPRING_PROFILES_ACTIVE=prod`.

Puedes generar el secreto con:

```bash
openssl rand -base64 48
```

> **Importante**: si cambias `CHEFSTOOL_JWT_SECRET`, todos los usuarios tendrán que
> volver a iniciar sesión.

### Build

```bash
cd chefstool-backend
./mvnw clean package -DskipTests
java -jar target/chefstool-backend-0.0.1-SNAPSHOT.jar
```

### Docker (recomendado)

El `Dockerfile` del backend compila con JDK 25 e incluye **Tesseract con español**, así
que las fotos de albarán funcionan tal cual:

```bash
docker build -t chefstool-backend chefstool-backend
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=... -e DB_PORT=5432 -e DB_NAME=postgres \
  -e DB_USER=... -e DB_PASSWORD=... \
  -e CHEFSTOOL_JWT_SECRET=$(openssl rand -base64 48) \
  -e CHEFSTOOL_CORS=https://app.tudominio.com \
  -e CHEFSTOOL_ALMACEN=supabase \
  -e SUPABASE_URL=https://TU_PROYECTO.supabase.co \
  -e SUPABASE_SERVICE_KEY=... \
  chefstool-backend
```

Incluye `HEALTHCHECK` contra `/api/auth/yo` (responde 401 sin token: prueba de vida).

### OCR de imágenes (opcional)

Los PDF con texto se leen sin dependencias extra. Para **fotos y escaneos** el
servidor necesita Tesseract con el idioma español:

```bash
apt install tesseract-ocr tesseract-ocr-spa   # Debian/Ubuntu
brew install tesseract tesseract-lang         # macOS
```

Si no está instalado, la app sigue funcionando: los documentos quedan por revisar
con un aviso claro en lugar de inventar datos.

### Puerto

El servidor escucha en el puerto 8080 (`PORT` no está mapeado todavía; si tu
plataforma lo impone, añade `server.port=${PORT:8080}`).

### Almacenamiento de albaranes

Dos modos, se eligen con `CHEFSTOOL_ALMACEN`:

- `local` (por defecto): disco, en `CHEFSTOOL_UPLOADS`. Solo vale si el disco es
  persistente (volumen montado). **En plataformas con disco efímero los albaranes se
  pierden en cada despliegue.**
- `supabase`: object storage. Pasos en el panel de Supabase:

  1. **Storage → New bucket**, nombre `documentos`, privado.
  2. **Project Settings → API → service_role key** y cópiala en `SUPABASE_SERVICE_KEY`
     (dato sensible: solo en el backend, nunca en el frontend).
  3. `SUPABASE_URL=https://TU_PROYECTO.supabase.co`.

  La ruta en el bucket es `organizacionId/documentoId/nombre`, la misma que se guarda
  en base de datos. La descarga la sigue sirviendo el backend (con su control de
  permisos), así que el bucket puede seguir siendo privado.

### Recuperación de contraseña

`POST /api/auth/recuperar` genera un enlace de un solo uso válido una hora y guarda
solo su hash. Como **todavía no hay proveedor de correo**, el enlace se escribe en el
log del backend (`Enlace de recuperacion para …`).

Para una demostración, `CHEFSTOOL_ENLACE_DIRECTO=true` hace que el enlace vuelva
también en la respuesta y la interfaz lo muestre como enlace directo. **Déjalo en
`false` en producción**: en cuanto se contrate un proveedor de email (Resend,
SendGrid, SES…) hay que enviarlo por correo y quitar esta opción.

## 3. Frontend

```bash
cd chefstool-frontend
npm ci
npm run build       # genera dist/
```

Dos formas de conectarlo con la API:

1. **Proxy en el mismo dominio** (recomendado): sirve `dist/` con Nginx y redirige
   `/api` al backend. El frontend no necesita configuración extra.
2. **Dominio distinto**: define `VITE_API_URL=https://api.tudominio.com/api` en el
   `.env` **antes de compilar** y habilita CORS en el backend.

La aplicación es una SPA: configura el servidor para que cualquier ruta desconocida
devuelva `index.html` (si no, recargar en `/compras` da 404).

## 4. Checklist antes de publicar

- [ ] `CHEFSTOOL_JWT_SECRET` propio (32+ caracteres) y fuera del repositorio
- [ ] `SPRING_PROFILES_ACTIVE=prod` (activa las comprobaciones estrictas)
- [ ] HTTPS en frontend y API
- [ ] `CHEFSTOOL_CORS` con el dominio real, sin localhost
- [ ] Base de datos de producción separada y con backups probados
- [ ] Credenciales del `.env` rotadas (las de desarrollo nunca van a producción)
- [ ] `CHEFSTOOL_ALMACEN=supabase` con el bucket privado creado
- [ ] Tesseract con español dentro de la imagen (`docker run … tesseract --list-langs`)
- [ ] `CHEFSTOOL_ENLACE_DIRECTO=false`
- [ ] Revisar el pool de conexiones según el número de instancias
- [ ] Probar el flujo completo: registro → recuperación de contraseña → subida →
      confirmación → dashboard → export a Excel

## 5. Limitaciones conocidas

- **El OCR de fotos requiere Tesseract**: sin él solo se leen PDF con texto.
- **El parser es heurístico**: entiende formatos habituales de albarán español,
  pero conviene probarlo con los documentos reales de cada proveedor.
- **Invitaciones y recuperación no envían email**: las invitaciones se aplican cuando
  la persona se registra con ese correo, y el enlace de recuperación va al log. Falta
  integrar un proveedor de email.
- **Permisos por rol básicos**: separan gestión de equipo/configuración de trabajo
  diario; no distinguen operaciones sobre datos (por ejemplo, borrar un producto).
- **Rate limiting en memoria**: 5 intentos fallidos por correo + IP cada 15 minutos en
  login y recuperación. Con varias instancias haría falta Redis o una tabla.
- **Los tests apuntan a la base de datos real**: son transaccionales (hacen
  rollback), pero conviene moverlos a Testcontainers antes de crecer.
