# Despliegue

## Resumen

Son dos piezas independientes:

| Pieza | Qué es | Dónde va |
| --- | --- | --- |
| `chefstool-backend` | JAR de Spring Boot (Java 25) | Cualquier hosting con JVM: Railway, Fly.io, Render, Azure App Service, un VPS… |
| `chefstool-frontend` | Sitio estático (HTML/JS/CSS) | Netlify, Vercel, Cloudflare Pages, Nginx… |
| Base de datos | PostgreSQL (Supabase) | Ya existe |
| Albaranes | Archivos | Disco del backend (hoy) o almacenamiento de objetos |

## 1. Base de datos

El esquema lo aplica Flyway al arrancar el backend. No hay que ejecutar SQL a mano.

- Usa una base de datos **distinta** para producción y para desarrollo.
- Si el proyecto Supabase es nuevo, crea el `.env` con sus credenciales y arranca
  el backend una vez: las migraciones V1–V4 se aplican solas.
- **Copia de seguridad**: activa los backups automáticos de Supabase y prueba una
  restauración antes de dar acceso a usuarios reales.

> El pooler de Supabase en modo sesión limita a **15 conexiones**. Por eso el
> backend usa `maximum-pool-size=3` y `minimum-idle=0`. Si lo despliegas con más
> de una instancia, baja aún más el pool o usa el pooler en modo transacción.

## 2. Backend

### Variables de entorno obligatorias

```bash
DB_HOST=...
DB_PORT=5432
DB_NAME=postgres
DB_USER=...
DB_PASSWORD=...
CHEFSTOOL_JWT_SECRET=<cadena aleatoria de 32+ caracteres>
```

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

Hoy los archivos se guardan en disco (`CHEFSTOOL_UPLOADS`, por defecto `./uploads`).

- En plataformas con sistema de archivos efímero (Heroku, Render, Fly sin volumen),
  los albaranes **se pierden en cada despliegue**.
- Soluciones: montar un volumen persistente, o implementar la interfaz
  `AlmacenamientoArchivos` para S3 / Supabase Storage. Es la única clase que hay
  que escribir: el resto del código no cambia.

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

- [ ] `CHEFSTOOL_JWT_SECRET` propio y fuera del repositorio
- [ ] HTTPS en frontend y API
- [ ] Base de datos de producción separada y con backups probados
- [ ] Credenciales del `.env` rotadas (las de desarrollo nunca van a producción)
- [ ] Almacenamiento de albaranes persistente
- [ ] Revisar el pool de conexiones según el número de instancias
- [ ] Probar el flujo completo: registro → subida → confirmación → dashboard

## 5. Limitaciones conocidas

- **El OCR de fotos requiere Tesseract**: sin él solo se leen PDF con texto.
- **El parser es heurístico**: entiende formatos habituales de albarán español,
  pero conviene probarlo con los documentos reales de cada proveedor.
- **Las invitaciones no envían email**: se guardan y se aplican cuando la persona
  se registra con ese correo. Falta integrar un proveedor de email si se quiere
  avisar.
- **Permisos por rol básicos**: separan gestión de equipo/configuración de trabajo
  diario; no distinguen operaciones sobre datos (por ejemplo, borrar un producto).
- **Sin rate limiting** en `/api/auth/login`.
- **Los tests apuntan a la base de datos real**: son transaccionales (hacen
  rollback), pero conviene moverlos a Testcontainers antes de crecer.
