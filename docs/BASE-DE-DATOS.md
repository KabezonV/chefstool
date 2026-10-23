# Base de datos: PostgreSQL propio o en Amazon

**Fecha:** 22/10/2026 · **Estado:** Supabase eliminado del código y de la configuración

---

## 1. Conclusión rápida

**Hecho: Chefstool ya no depende de Supabase.** Usa **PostgreSQL estándar** por JDBC y la
base la pone el `docker-compose` (o Amazon RDS, o cualquier PostgreSQL propio) sin tocar
una línea de lógica de negocio. Las referencias que quedaban en el código, en la
configuración y en el almacenamiento de archivos se han eliminado.

Lo he verificado hoy: la aplicación arranca con Docker sobre un **PostgreSQL 17** limpio,
Flyway crea las **16 tablas** sola y todo funciona (login, albaranes, compras, fichas,
informes).

> El código no contiene nada específico de Supabase. La única función "moderna" que usan
> las migraciones es `gen_random_uuid()`, que viene **incluida en PostgreSQL desde la 13**.
> No hay extensiones de Supabase, ni `auth.*`, ni `storage.*`.

---

## 2. Qué había que cambiar (y qué ya está cambiado)

| Cambio | Estado | Dónde |
| --- | --- | --- |
| URL de conexión con SSL configurable (`DB_SSLMODE`) | ✅ hecho | `application.properties` |
| Pool de conexiones configurable (`DB_POOL_MAX`) | ✅ hecho | `application.properties` |
| PostgreSQL en el `docker-compose` (sin servicio externo) | ✅ hecho | `docker-compose.yml` |
| Plantilla `.env.ejemplo` apuntando a la base local | ✅ hecho | `.env.ejemplo` |
| Volumen persistente para los datos (`chefstool_datos`) | ✅ hecho | `docker-compose.yml` |
| Guía de migración de datos actuales | ✅ este documento | — |
| Almacenamiento de albaranes: adaptador de **S3** en lugar del de Supabase | ✅ hecho | `AlmacenamientoS3` |

**Cerco del cambio:** configuración y despliegue. **Cero cambios** en entidades,
servicios, controladores, migraciones o frontend.

---

## 3. Qué se pierde al dejar Supabase y cómo se cubre

| Lo que daba Supabase | Sin Supabase | Cómo se cubre en Amazon |
| --- | --- | --- |
| Copias de seguridad automáticas | No | **RDS Automated backups** (retención 7–35 días) + snapshot manual antes de cada despliegue |
| Punto de recuperación en el tiempo (PITR) | No | RDS lo incluye con retención > 0 |
| Pooler de conexiones | No | Pool propio pequeño (ya configurado) o **RDS Proxy** si se escala a varias instancias |
| Panel web para mirar datos | No | DBeaver / pgAdmin / `psql`, o el propio informe de la app |
| Almacenamiento de archivos (Storage) | Sí, había adaptador | **S3** o volumen EBS (hoy el almacén local funciona con volumen) |
| Autenticación de usuarios (no la usábamos) | — | No aplica: la app tiene su propio JWT |
| Métricas y logs | Básico | **CloudWatch** (CPU, conexiones, alarmas de espacio) |

Ninguna de esas pérdidas es funcional para el usuario de la aplicación: son servicios
de operación, y RDS los cubre de serie.

---

## 4. Opciones y coste (precios orientativos, a confirmar en la calculadora de AWS)

| Opción | Coste mensual aprox. | Ventajas | Inconvenientes |
| --- | --- | --- | --- |
| **RDS PostgreSQL `db.t4g.micro`** (recomendada) | 0 € los primeros 12 meses (free tier) · luego **15–25 €** con 20 GB y backups de 7 días | Gestionada: parches, backups y PITR incluidos; SSL; región `eu-south-2` (España) | Una instancia para todo; Multi-AZ duplica el coste |
| RDS `db.t4g.small` | 30–45 € | Más margen para varias cocinas | Sobredimensionado al principio |
| **Aurora Serverless v2** | desde ~45 € | Escala sola, muy tolerante a fallos | Caro para este volumen |
| **EC2 + PostgreSQL en Docker** | 8–12 € (t4g.small) | Lo más barato | Tú haces parches, backups y monitorización |
| PostgreSQL en el mismo servidor de la app | 0 € extra | Trivial | Riesgo: si cae el servidor, caen app y datos |
| Seguir en Supabase Pro | 25 $ (~23 €) | Cero trabajo | Es lo que dirección quiere evitar |

**Mi recomendación:** **RDS PostgreSQL 17 `db.t4g.micro`, single-AZ, 20 GB, backups 7
días, en `eu-south-2` (Zaragoza)**. Se paga poco, cumple con que los datos estén en
España y quita de encima la operación de la base de datos.

> Nota: si el objetivo es **coste cero en la demo**, hoy ya funciona sin coste con el
> PostgreSQL del `docker-compose` en cualquier portátil o en un servidor pequeño.

---

## 5. Pendiente decidir: los albaranes (archivos)

La base de datos son datos; los PDFs e imágenes son archivos. Opciones:

| Opción | Coste | Comentario |
| --- | --- | --- |
| Volumen local (lo de ahora) | 0 € | Vale si el servidor tiene disco persistente (EBS) |
| **S3** | ~1–3 €/mes para miles de albaranes | Lo natural en AWS; hay que escribir un adaptador (el puerto ya existe) |
| Mantener Supabase solo para Storage | Mínimo del plan | No tiene sentido pagar Supabase por esto |

Recomendación: **S3**, con una tarde de trabajo. Mientras tanto, el volumen local
funciona (está ya configurado en Docker con el volumen `chefstool_albaranes`).

---

## 6. Migrar los datos que ya tenemos (si se quieren conservar)

Son datos de demostración, así que **se pueden regenerar** con el botón *Cargar datos de
ejemplo*. Aun así, si se quiere conservar lo que hay en Supabase:

```bash
# 1. Volcar desde Supabase (sin dueños ni privilegios)
pg_dump "postgresql://postgres.PROYECTO:PASSWORD@aws-0-eu-west-1.pooler.supabase.com:5432/postgres" \
  --no-owner --no-privileges --no-acl --clean --if-exists \
  -f chefstool.sql

# 2. Restaurar en la base nueva (RDS o el contenedor local)
psql "postgresql://chefstool:PASSWORD@ENDPOINT:5432/chefstool" -f chefstool.sql

# 3. Comprobar
psql "postgresql://chefstool:PASSWORD@ENDPOINT:5432/chefstool" \
  -c "select (select count(*) from productos) productos, (select count(*) from compras) compras, (select count(*) from usuarios) usuarios"
```

Detalles importantes:

- El `pg_dump` incluye la tabla `flyway_schema_history`, así que la base nueva queda con
  las migraciones ya marcadas y **Flyway no volverá a crearlas**.
- Si se empieza de cero (recomendado), no hay que hacer nada: Flyway crea todo al
  arrancar y luego se carga la demo desde la aplicación.
- El usuario de la base nueva debe ser dueño del esquema (RDS: el usuario maestro).

---

## 7. Plan para mañana (2–3 horas en total)

1. **Decidir** RDS vs. PostgreSQL en el servidor (yo recomiendo RDS).
2. Crear la instancia RDS (PostgreSQL 17, `db.t4g.micro`, 20 GB, backups 7 días,
   `eu-south-2`), security group que solo permita el servidor de la aplicación.
3. Crear la base `chefstool` y el usuario; apuntar `DB_HOST`, `DB_NAME`, `DB_USER`,
   `DB_PASSWORD`, `DB_SSLMODE=require` en las variables del despliegue.
4. Desplegar el backend (contenedor ya publicado en `ghcr.io`) y comprobar que Flyway
   aplica las migraciones contra RDS.
5. Cargar datos de ejemplo, probar el flujo completo y el OCR.
6. Definir el adaptador de **S3** para los albaranes (o dejar volumen EBS y decidirlo
   después).
7. Programar la alarma de espacio en disco y la de conexiones en CloudWatch.

---

## 8. Riesgos y cómo se controlan

| Riesgo | Mitigación |
| --- | --- |
| Perder los datos actuales al migrar | Snapshot de RDS antes de importar + probar el `pg_dump` en local primero |
| RDS inaccesible desde el servidor | Mismo VPC/security group; probar `psql` desde el contenedor antes de desplegar |
| Límite de conexiones | Pool a 5 por instancia (ya configurado); RDS Proxy si se añaden instancias |
| Coste inesperado | Instancia micro single-AZ + alarmas de facturación en AWS Budgets |
| Bloqueo con un proveedor | Es PostgreSQL estándar: RDS → Aurora → cualquier Postgres sin cambiar código |
| SSL obligatorio en RDS | `DB_SSLMODE=require` (ya soportado; por defecto `prefer` para local) |

---

## 9. Estado actual del repositorio

- `docker compose up --build` levanta **PostgreSQL 17 + API + web** sin ningún servicio
  externo de pago. Probado hoy: 16 tablas creadas por Flyway, login correcto y panel con
  datos.
- Los datos viven en el volumen `chefstool_datos` y los albaranes en `chefstool_albaranes`.
- Para usar RDS solo cambian cinco variables de entorno; el `docker-compose` puede
  quedarse como está (el servicio `postgres` se elimina o se ignora en producción).
