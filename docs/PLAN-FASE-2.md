# Plan de la fase 2 — Salir de Supabase, OCR real y puesta en producción

**Fecha:** 23/10/2026 · **Punto de partida:** la app funciona completa sobre PostgreSQL
propio (Docker), sin Supabase en el camino de ejecución.

---

## 1. Respuesta rápida a las dudas

### ¿Hay que instalar algo para la base de datos?

**No hace falta instalar nada.** Hay tres formas y ninguna requiere instalar software en
los equipos de trabajo:

| Forma | Qué hay que hacer | Coste |
| --- | --- | --- |
| **PostgreSQL en Docker** (lo de ahora) | Nada: `docker compose up --build` ya lo levanta | 0 € |
| **Amazon RDS** (recomendado para producción) | Crear la instancia en la consola de AWS | 15–25 €/mes (free tier 12 meses) |
| PostgreSQL en el servidor de la empresa | Instalarlo una vez en ese servidor | 0 € |

En los tres casos la aplicación se conecta igual: solo cambian `DB_HOST`, `DB_USER`,
`DB_PASSWORD` y `DB_SSLMODE`.

### ¿SQL o MySQL?

**PostgreSQL ya es SQL.** Si lo que piden es "no depender de Supabase", está resuelto: hoy
la base es nuestra. Si lo que piden es **MySQL concretamente**, hay que saber esto:

- Nuestras 6 migraciones usan cosas que MySQL no tiene igual: `timestamptz`,
  **índices parciales** (`where deleted_at is null`), `gen_random_uuid()` como valor por
  defecto y columnas `jsonb`.
- Cambiar a MySQL implica **reescribir las migraciones y parte de las consultas** y
  revalidar todo: 3–4 días de trabajo y riesgo de diferencias sutiles (fechas con zona
  horaria, ordenaciones, colaciones de acentos).

**Recomendación:** quedarse en **PostgreSQL** (es el motor que usan Amazon RDS, Azure y
Google; cumple cualquier requisito de "base de datos SQL" y es lo que ya está probado).
Si dirección exige MySQL por política de la empresa, se planifica aparte.

### ¿OCR con Amazon?

Sí, **Amazon Textract** es la opción correcta: es el servicio de AWS para leer documentos,
tiene un modo específico para **facturas y albaranes** (`AnalyzeExpense`) que devuelve
proveedor, fecha, totales, impuestos y **líneas de detalle** ya estructuradas.

- **No hay que instalar nada**: es una API. Solo se necesita una cuenta de AWS y unas
  credenciales con permiso de Textract.
- **Coste orientativo** (a confirmar en la calculadora de AWS): `AnalyzeExpense` ≈ 10 $ por
  cada 1.000 páginas. Con 300 albaranes al mes ≈ **3 $/mes**. `DetectDocumentText` (solo
  texto) es más barato, ≈ 1,50 $/1.000.
- **Requisito técnico**: para **PDF de varias páginas** hay que pasar por S3 (Textract
  asíncrono). Ya tenemos que usar S3 para guardar los albaranes, así que encaja.
- **Alternativas** si se quiere comparar: Google Document AI, Azure Document Intelligence y
  Mindee. Todas funcionan igual de bien; la ventaja de Textract es que ya estamos en AWS.

---

## 2. Decisiones que hay que tomar

| # | Decisión | Opciones | Recomendación |
| --- | --- | --- | --- |
| D1 | Motor de base de datos | PostgreSQL / MySQL | **PostgreSQL** |
| D2 | Dónde vive la base | Docker local / RDS / servidor propio | **RDS** en producción, Docker en local |
| D3 | Almacén de albaranes | Volumen del servidor / **S3** | **S3** (necesario para Textract con PDF) |
| D4 | Motor de OCR | Textract / Google / Mindee | **Amazon Textract** |
| D5 | Correo (invitaciones y contraseñas) | SES / Resend / SendGrid | **Amazon SES** (mismo proveedor) |
| D6 | Region de AWS | `eu-south-2` (Zaragoza) / `eu-west-1` (Irlanda) | **`eu-south-2`** (datos en España) |

---

## 3. Tareas, por bloques y prioridad

### Bloque A — Quitar Supabase del todo (P0)

| # | Tarea | Detalle | Esfuerzo |
| --- | --- | --- | --- |
| A1 | Eliminar el adaptador de almacenamiento de Supabase | Borrar `AlmacenamientoSupabase` y su test | 1 h |
| A2 | Crear adaptador de **S3** para los albaranes | Mismo puerto `AlmacenamientoArchivos`; SDK de AWS v2; bucket privado y URL firmada para descargar | 3–4 h |
| A3 | Limpiar variables y documentación | Quitar `SUPABASE_*` de `application.properties`, `docker-compose` y `.env.ejemplo`; dejar solo S3 | 1 h |
| A4 | Revisar que no queda ninguna referencia | `grep` en código, docs y config; el documento de migración se conserva como histórico | 30 min |
| A5 | Borrar el proyecto de Supabase | Cuando los datos no hagan falta (son de demo) | 15 min |

### Bloque B — OCR real con Amazon Textract (P0)

| # | Tarea | Detalle | Esfuerzo |
| --- | --- | --- | --- |
| B1 | Cuenta AWS + usuario IAM | Permisos: `textract:*` (Analyze/Start/Get) y `s3:PutObject/GetObject` en el bucket | 1 h (lo hace dirección/IT) |
| B2 | Adaptador `ExtractorTextract` | Implementa el puerto `ExtractorDocumento`; PDF/imagen → `AnalyzeExpense`; mapear a `ExtraccionDocumento` | 6–8 h |
| B3 | Camino asíncrono para PDF de varias páginas | Subida a S3 + `StartExpenseAnalysis` + sondeo del resultado | 4 h |
| B4 | Confianza y avisos | Traducir la confianza de Textract a la pantalla de revisión; avisar si viene vacío | 2 h |
| B5 | Modo híbrido | Si Textract falla o no hay credenciales, seguir con el lector local actual (ya funciona) | 2 h |
| B6 | Pruebas con albaranes reales | 5–10 de cada proveedor de la cocina piloto; medir aciertos y ajustar | 1 día (con datos) |

### Bloque C — Puesta en producción en AWS (P0/P1)

| # | Tarea | Detalle | Esfuerzo |
| --- | --- | --- | --- |
| C1 | RDS PostgreSQL 17 | `db.t4g.micro`, 20 GB, backups 7 días, `eu-south-2`, security group cerrado | 2 h |
| C2 | Desplegar el backend | Imagen ya publicada en `ghcr.io` → App Runner o ECS Fargate con variables de entorno | 4 h |
| C3 | Desplegar el frontend | S3 + CloudFront (o el hosting que indique IT) con HTTPS y dominio | 3 h |
| C4 | Bucket S3 de albaranes | Privado, cifrado, ciclo de vida; permisos mínimos | 1 h |
| C5 | Dominio y certificados | `app.chefstool.es` / `api.chefstool.es`, ACM | 2 h |
| C6 | Alarmas y presupuesto | CloudWatch (CPU, conexiones, espacio) + AWS Budgets con aviso | 2 h |
| C7 | Copias verificadas | Snapshot manual + restaurar en una base de prueba (probar de verdad) | 2 h |

### Bloque D — Seguridad, datos y correo (P1)

| # | Tarea | Detalle | Esfuerzo |
| --- | --- | --- | --- |
| D1 | Amazon SES | Verificar dominio, DKIM; enviar invitaciones y recuperación por correo real | 4 h |
| D2 | Borrado de cuenta y de organización | Endpoint + botón; hoy solo hay borrado lógico de datos | 4 h |
| D3 | Aviso de privacidad y textos legales | Páginas en la app + registro de consentimiento | 3 h |
| D4 | Roles más finos | Que "miembro" no pueda borrar catálogo; auditoría básica de quién cambia qué | 4 h |
| D5 | Caducidad y refresco de sesión | Hoy 12 h y hay que volver a entrar | 3 h |

### Bloque E — Calidad y producto (P2)

| # | Tarea | Detalle | Esfuerzo |
| --- | --- | --- | --- |
| E1 | Tests con Testcontainers | Que las 88 pruebas no dependan de una base compartida | 4 h |
| E2 | Importar productos desde Excel | Muy pedido en cocinas: catálogo inicial sin teclearlo | 1 día |
| E3 | Informe de escandallos en PDF | Enviar la ficha técnica al jefe de cocina | 4 h |
| E4 | Alertas por correo | Aviso semanal de subidas de precio | 3 h |
| E5 | Multi-cocina / multi-empresa | Si una empresa tiene varios locales con compras separadas | 2 días |
| E6 | Pruebas E2E automatizadas | Playwright sobre los flujos críticos | 1 día |

---

## 4. Qué necesito de vosotros (bloqueantes)

1. **Cuenta de AWS** de la empresa (o confirmación de que se crea una) y **región** elegida.
2. **Usuario/rol IAM** con permisos de Textract y S3 (o que me den acceso para crearlo).
3. **Aprobación de coste** de Textract y RDS (≈ 20–35 €/mes en total al principio).
4. **Dominio** para la aplicación (¿subdominio de la web corporativa?).
5. **Decisión de MySQL vs PostgreSQL** (recomiendo PostgreSQL).
6. **Albaranes reales** de 2–3 proveedores para medir la precisión del OCR.

---

## 5. Orden propuesto

**Hoy / mañana (no necesitan credenciales):**
A1, A2, A3, A4, B2, B5 → Supabase fuera del código, adaptador S3 y Textract listos
(probadlos con un modo simulado; se activan en cuanto lleguen las claves).

**En cuanto haya cuenta AWS:**
B1, B3, C1, C2, C4 → base en RDS, backend desplegado y albaranes en S3.

**Después:**
C3, C5, C6, C7, D1, D2, D3 → dominio, HTTPS, correo, avisos y privacidad.

**Cuando haya albaranes reales:**
B6 y ajustes de precisión.

**Con tiempo:**
Bloque E (calidad y funcionalidades que piden las cocinas).

---

## 6. Riesgos

| Riesgo | Mitigación |
| --- | --- |
| Textract lee mal albaranes manuscritos o fotos malas | Pantalla de revisión siempre disponible; modo híbrido con el lector actual; probar antes de prometer |
| Coste de OCR se dispara con muchas fotos | Límite por organización/mes y aviso al superarlo |
| Bloqueo con AWS | El puerto `ExtractorDocumento` permite cambiar a Google/Mindee sin tocar la app |
| Pérdida de datos al migrar de Supabase | Los datos actuales son de demo: se regeneran. Si se quieren conservar, `pg_dump` documentado |
| Dependencia de una sola persona | Todo documentado y en repositorio; 88 pruebas automáticas y CI en verde |
