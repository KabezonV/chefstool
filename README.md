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

## Requisitos

- JDK 25 (`java -version` debe mostrar 25.x)
- Node.js 20 o superior

## Cronograma

| Etapa | Alcance | Estado |
| --- | --- | --- |
| 0 | Java 25, git, secretos fuera del código, esqueleto del frontend | Completada |
| 1 | Modelo de datos (Flyway) y entidades JPA | Completada |
| 2 | CRUDs de productos, proveedores, clientes y eventos | Pendiente |
| 3 | Documentos + OCR (cámara, extracción, revisión y confirmación) | Pendiente |
| 4 | Compras, histórico de precios y alertas de subida | Pendiente |
| 5 | Fichas técnicas y escandallos | Pendiente |
| 6 | Dashboard e informes | Pendiente |
| 7 | Autenticación, organizaciones, roles y PWA | Pendiente |
