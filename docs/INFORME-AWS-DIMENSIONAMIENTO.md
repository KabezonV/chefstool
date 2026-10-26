# Informe: qué servidor necesitamos en AWS (20 cocinas)

**Para:** Bernart · **Fecha:** 26/10/2026 · **Estado:** propuesta de dimensionamiento y coste

---

## 1. Resumen en un minuto

- **Con 20 cocinas necesitamos una sola instancia pequeña.** No hace falta ni balanceador ni
  clúster: el trabajo real son unos **6.000 albaranes al mes**, que es un 17 % de un núcleo de
  CPU en la hora punta.
- **Configuración propuesta:** 1 servidor `t4g.medium` (2 vCPU, 4 GB, ARM) + PostgreSQL
  gestionado `db.t4g.micro` + **las fotos de los albaranes en S3** (como pediste).
  Coste: **54,6 $/mes ≈ 50 €/mes** para las 20 cocinas, es decir **2,50 € por cocina al mes**.
- **La IA de lectura (OpenAI) añade 3,17 €/mes en total**, no por cocina. El total de la
  plataforma para 20 cocinas ronda **53 €/mes**.
- **El almacenamiento en S3 es barato**: unas 6.000 fotos al mes suponen 15 GB/mes (180 GB el
  primer año) y **4,14 $/mes al final del primer año**. Con ciclo de vida a almacenamiento frío
  baja casi a la mitad.
- **El código ya está preparado**: las fotos se guardan por S3 desde el primer día (bucket
  privado, enlaces firmados de 5 minutos) y el despliegue es un `docker compose` con las
  variables de entorno. Solo faltan la cuenta de AWS, el dominio y la clave de OpenAI.
- **Cuándo habría que crecer**: a partir de **~50 cocinas** se sube a `t4g.large` y la base a
  `db.t4g.small`; a partir de **~100 cocinas** se pasa a dos servidores con balanceador. El
  coste por cocina se mantiene en torno a 2 €.

---

## 2. Cuánto trabajo va a haber (el dato que decide)

Partimos de la estimación de uso real de una cocina:

| Concepto | Por cocina | 20 cocinas |
| --- | ---: | ---: |
| Albaranes al mes | 300 | **6.000** |
| Albaranes por día laborable (22 días) | 14 | **273** |
| Albaranes en la hora punta (8:00–11:00) | 2 | **~40/hora (picos de 60)** |
| Usuarios dados de alta | 3 | 60 |
| Usuarios a la vez en el pico | 1 | **~12–15** |
| Fotos subidas al mes (2,5 MB cada una) | 0,75 GB | **15 GB** |
| Líneas de compra al mes (~20 por albarán) | 6.000 | **120.000** |
| Líneas de compra al año | 72.000 | **1.440.000** |

**Lo que eso significa para el servidor:**

- **CPU**: cada albarán ocupa la máquina 10 segundos (llamada a OpenAI) o 15 segundos si es un
  PDF que hay que pasar por OCR local. A 60 albaranes/hora son **600 segundos de trabajo por
  hora**: un 17 % de un solo núcleo. Con **2 vCPU vamos 6 veces sobrados**.
- **Memoria**: la aplicación Java necesita 1 GB, el sistema 0,5 GB y los agentes de AWS 0,2 GB.
  **Con 2 GB iría justo** (sobre todo si entra el OCR local, que consume de golpe); **4 GB dan
  margen de sobra**.
- **Base de datos**: 1,4 millones de filas al año de tamaño pequeño (unos 700 MB con índices).
  **20 GB de disco dan para 3 años** sin tocar nada.
- **Almacenamiento**: 180 GB de fotos al año. Aquí sí conviene S3 desde el principio (ver §5).

> Si las cocinas hacen 500 albaranes/mes en vez de 300, la CPU sube un 65 % y sigue sobrando;
> lo único que crece de verdad es el almacenamiento (300 GB/año).

---

## 3. Arquitectura propuesta

```text
                    Internet
                       │
              Route 53 (dominio)  ──►  certificado TLS (ACM)
                       │
        ┌──────────────┴───────────────┐
        │   EC2 t4g.medium (2 vCPU/4GB)│
        │   Docker Compose:            │
        │   · nginx + React (frontend) │
        │   · API Java (Spring Boot)   │──►  OpenAI API (lectura de albaranes)
        │   · Tesseract (respaldo OCR) │
        └──────┬───────────────┬───────┘
               │               │
        ┌──────┴──────┐  ┌─────┴───────────────────────┐
        │ RDS Postgres│  │ S3 (bucket privado)         │
        │ db.t4g.micro│  │ fotos y PDF de albaranes    │
        │ 20 GB + 7d  │  │ cifrado + versionado        │
        └─────────────┘  └─────────────────────────────┘
               │
        CloudWatch (logs y alarmas)  ·  SSM (claves y secretos, sin SSH)
```

Decisiones y por qué:

1. **Una sola instancia con Docker Compose** (lo que ya tenemos montado y probado). Para 20
   cocinas no hay motivo para contenedores gestionados: se ahorra complejidad y dinero.
2. **La base de datos sí va gestionada (RDS)**: copias automáticas, parcheo y restauración a un
   punto en el tiempo. Es lo único que no queremos administrar a mano.
3. **Las fotos en S3, nunca en el disco de la instancia** (tu indicación). Ventajas reales: no
   se llena el disco, sobrevive a la pérdida de la instancia, y una foto se sirve con un enlace
   firmado que caduca en 5 minutos.
4. **Sin SSH abierto**: administración con SSM Session Manager. Menos superficie de ataque y
   queda registro de cada sesión.
5. **Región**: `eu-west-1` (Irlanda) por catálogo y precio; `eu-south-2` (Aragón) si se exige
   que los datos estén físicamente en España (algo más caro y con menos servicios).

---

## 4. Qué servidor poner: las cuatro opciones

Precios verificados hoy en AWS para Irlanda (`eu-west-1`), Linux, bajo demanda, 730 h/mes,
sin IVA.

| Opción | Qué incluye | Coste/mes | Ventajas | Inconvenientes |
| --- | --- | ---: | --- | --- |
| **A. Recomendada** | EC2 `t4g.medium` + RDS `db.t4g.micro` 20 GB + S3 | **54,6 $** | Base gestionada, copias automáticas, sencilla de mantener | Un único servidor de aplicación |
| B. Económica | EC2 `t4g.small` + PostgreSQL dentro del contenedor + S3 | **27,1 $** | La mitad de coste | Copias manuales, base de datos sin gestión |
| C. Alta disponibilidad | 2 × `t4g.medium` + balanceador (ALB) + RDS Multi-AZ | **117 $** | Si cae un servidor, sigue funcionando | El doble de coste y de piezas |
| D. Contenedores gestionados | ECS Fargate ARM (1 vCPU/2 GB) + ALB + RDS `db.t4g.micro` | **77 $** | Sin servidor que administrar | Más piezas y más difícil de depurar |

**Recomiendo la A.** Con 20 cocinas, un fallo del servidor se resuelve restaurando una AMI en
10 minutos; montar alta disponibilidad ahora es pagar el doble por un riesgo pequeño. La C
tiene sentido cuando haya clientes que firmen un SLA o al pasar de ~100 cocinas.

### Desglose de la opción A (precios unitarios verificados)

| Concepto | Precio unitario | Cantidad | $/mes |
| --- | ---: | ---: | ---: |
| EC2 `t4g.medium` (2 vCPU, 4 GB, ARM) | 0,0368 $/hora | 730 h | 26,86 |
| Disco EBS gp3 de la instancia | 0,088 $/GB-mes* | 40 GB | 3,52 |
| RDS PostgreSQL `db.t4g.micro` (1 instancia) | 0,017 $/hora | 730 h | 12,41 |
| Almacenamiento RDS gp3 | 0,127 $/GB-mes | 20 GB | 2,54 |
| S3 Standard (año 2: 180 GB) | 0,023 $/GB-mes | 180 GB | 4,14 |
| Peticiones S3 (6.000 subidas/mes) | 0,005 $/1.000 | 6.000 | 0,03 |
| CloudWatch Logs | 0,57 $/GB | 5 GB | 2,85 |
| Route 53 (zona del dominio) | 0,50 $/mes | 1 | 0,50 |
| Transferencia de salida | 0,09 $/GB | ~20 GB | 1,80 |
| **Total** | | | **≈ 54,6 $** |

\* El precio de EBS gp3 es la tarifa estándar de la región; conviene confirmarlo en la
calculadora de AWS al crear la instancia (supone 3,5 $ sobre el total, no cambia la decisión).

Comparación con lo que ya conocemos: **Hetzner costaría 5,99 €/mes** por una máquina
equivalente. AWS cuesta ~9 veces más por el mismo tamaño, pero aporta copias gestionadas,
integración con S3, control de accesos y la comodidad de que la empresa ya factura con ellos.

---

## 5. El guardado de albaranes en S3 (tu indicación)

**Ya está implementado así**: la aplicación sube las fotos y los PDF a un bucket privado y
guarda solo la ruta en la base de datos. Para ver una foto se genera un enlace firmado que
caduca en 5 minutos; nunca hay archivos públicos ni fotos en el disco del servidor. Si mañana
cambiamos de nube, solo hay que cambiar la configuración (`chefstool.almacenamiento.s3.*`) o
el endpoint, porque el código no depende de AWS.

Configuración propuesta del bucket:

| Ajuste | Valor | Motivo |
| --- | --- | --- |
| Nombre | `chefstool-albaranes-prod` | Claro y en la misma región (`eu-west-1`) |
| Acceso público | Bloqueado | Los albaranes llevan precios de proveedores |
| Cifrado | SSE-S3 (o SSE-KMS si se quiere auditar el acceso) | RGPD, datos de negocio |
| Versionado | Activado | Recuperar un archivo sobrescrito o borrado |
| Ciclo de vida | A los 365 días → S3 Standard-IA; a los 3 años → Glacier Instant | Rebaja el coste a la mitad a partir del segundo año |
| Borrado | Nunca automático mientras la cocina sea cliente | Los albaranes son documentación fiscal (4 años) |

**Coste del almacenamiento** (2,5 MB por albarán, 6.000 al mes):

| Momento | Fotos | Tamaño | Coste S3 |
| --- | ---: | ---: | ---: |
| Mes 1 | 6.000 | 15 GB | 0,35 $ |
| Mes 12 | 72.000 | 180 GB | 4,14 $ |
| Año 3 (con ciclo de vida) | 216.000 | 540 GB | ~6,8 $ (frente a 12,4 $ sin ciclo) |

---

## 6. Cuándo hay que crecer

| Cocinas | Albaranes/mes | Servidor | Base de datos | Coste/mes | €/cocina |
| ---: | ---: | --- | --- | ---: | ---: |
| **20** | 6.000 | EC2 `t4g.medium` | `db.t4g.micro` | **54,6 $** | **2,50 €** |
| 50 | 15.000 | EC2 `t4g.large` (4 vCPU/8 GB) | `db.t4g.small` | 105,6 $ | 1,95 € |
| 100 | 30.000 | 2 × `t4g.large` + balanceador | `db.t4g.medium` | 227,1 $ | 2,10 € |
| 250 | 75.000 | 3 instancias + balanceador | `db.t4g.large` + réplica de lectura | ~520 $ | ~1,90 € |

**Señales de que toca subir de tamaño** (para no hacerlo a ojo):

- CPU media por encima del 60 % durante dos semanas seguidas.
- Más de 60 conexiones simultáneas a la base de datos.
- Tiempo de respuesta de las pantallas por encima de 1 segundo de media.
- El disco por encima del 70 %.

Subir de `t4g.medium` a `t4g.large` es parar la instancia, cambiar el tipo y arrancar: **un
minuto de corte** y unos 27 $/mes más.

---

## 7. Escalado horizontal: cuándo y cómo

No hace falta hasta ~100 cocinas. Cuando llegue:

1. Se pone un **balanceador (ALB)** delante y dos instancias iguales.
2. La aplicación ya es **sin estado** (las sesiones van en un token firmado, no en memoria) y
   las fotos ya están en S3, así que basta con levantar una segunda copia del contenedor.
3. La base pasa a `db.t4g.medium` y, si hiciera falta, se añade una **réplica de lectura** para
   los informes.

Coste de dar ese paso: unos 60 $/mes más, sin tocar código.

---

## 8. Seguridad y datos personales

| Medida | Cómo |
| --- | --- |
| Aislamiento de red | La base de datos en subred privada; solo acepta conexiones desde el servidor de aplicación |
| Sin SSH | Administración por SSM Session Manager (queda registro de cada sesión) |
| Claves fuera del código | Clave de OpenAI, contraseña de la base y secreto del token en SSM Parameter Store (cifrados) |
| HTTPS | Certificado gratuito de AWS, renovación automática |
| Roles en vez de claves | La instancia accede a S3 con un rol de IAM: no hay claves de AWS guardadas en el servidor |
| Copias | RDS automáticas 7 días + copia mensual retenida 12 meses; AMI semanal del servidor |
| Objetivo de recuperación | Pérdida máxima de datos: 5 minutos. Tiempo de vuelta: menos de 1 hora |
| Contrato de datos (RGPD) | AWS firma el contrato de encargado del tratamiento; región europea |

---

## 9. Plan de puesta en marcha (1–2 días de trabajo)

| Paso | Qué se hace | Tiempo |
| --- | --- | --- |
| 1 | Crear la cuenta/VPC, el bucket S3 y la base RDS | 1 h |
| 2 | Abrir el servidor con Docker y desplegar el `docker-compose` (ya probado) | 2 h |
| 3 | Dominio en Route 53 + certificado + HTTPS | 1 h |
| 4 | Claves en SSM y primer arranque con la migración de base de datos (Flyway) | 1 h |
| 5 | Activar la clave de OpenAI y probar 3 albaranes reales de extremo a extremo | 2 h |
| 6 | Alarmas de CPU, disco, errores y copias; prueba de restauración | 2 h |
| 7 | Automatizar el despliegue con GitHub Actions (sin abrir SSH) | 3 h |
| 8 | Documento de explotación: qué hacer si algo falla | 1 h |

Lo que **ya está hecho** y se reutiliza: `docker-compose.yml`, `Dockerfile` con Tesseract,
migraciones automáticas, guardado en S3, alertas de salud y el manual de arranque.

---

## 10. Coste total de la plataforma para 20 cocinas

| Concepto | Coste/mes |
| --- | ---: |
| Infraestructura AWS (opción A) | 54,6 $ ≈ **50 €** |
| Lectura de albaranes con OpenAI (6.000 albaranes × 0,0005 €) | **3,17 €** |
| Dominio (Route 53, prorrateado) | incluido |
| Correo transaccional (recuperar contraseña) | 0 € con el plan gratuito de SES (hasta 3.000 correos/mes) |
| **Total** | **≈ 53 €/mes** |
| **Por cocina** | **≈ 2,65 €/mes** |

Si la empresa prefiere que todo vaya por AWS también para la IA (Bedrock), el coste de lectura
subiría a unos 8 €/mes en total: sigue siendo calderilla frente a los 50 € de infraestructura.

---

## 11. Qué necesito para arrancar

1. **Cuenta de AWS** con un usuario/rol para mí con permisos sobre EC2, RDS, S3, Route 53,
   CloudWatch y SSM (o que alguien de sistemas cree los recursos y me pase los accesos).
2. **Clave de OpenAI** (me la vas a pasar) y confirmación de que se puede usar para datos de
   clientes con el contrato de encargado del tratamiento.
3. **Dominio**: ¿usamos uno nuevo tipo `app.chefstool.es` o uno que ya tengáis?
4. **Región**: por defecto Irlanda (`eu-west-1`); dime si preferís Aragón (`eu-south-2`).
5. **Presupuesto**: con la opción A son ~50 €/mes de AWS. Si quieres, se puede empezar con la
   opción B (25 $/mes) y pasar a la A cuando haya clientes reales.

---

## 12. Fuentes (consultadas el 26/10/2026)

- Precios de EC2, RDS, S3, ALB, Fargate, CloudWatch y Route 53: API pública de precios de AWS
  (`pricing.us-east-1.amazonaws.com`) y mapas de tarifas de la web de AWS, región `eu-west-1`
  (Irlanda), Linux, bajo demanda.
  - EC2: `t4g.small` 0,0184 $/h · `t4g.medium` 0,0368 $/h · `t4g.large` 0,0736 $/h · `m7g.medium` 0,0455 $/h
  - RDS PostgreSQL: `db.t4g.micro` 0,017 $/h · `db.t4g.small` 0,035 $/h · `db.t4g.medium` 0,069 $/h
  - RDS almacenamiento gp3: 0,127 $/GB-mes · Multi-AZ: 0,254 $/GB-mes · copias extra: 0,095 $/GB-mes
  - S3: 0,023 $/GB-mes · 0,005 $ por 1.000 subidas · 0,0004 $ por 1.000 descargas
  - Balanceador de aplicación (ALB): 0,0252 $/h + 0,008 $/unidad de capacidad-hora
  - Fargate: 0,04048 $/vCPU-h y 0,004445 $/GB-h (x86); 0,03238 $/vCPU-h (ARM)
  - CloudWatch Logs: 0,57 $/GB ingerido
- Precio de la lectura con IA: [COMPARATIVA-COSTES-IA.md](COMPARATIVA-COSTES-IA.md)
- Cálculo reproducible en la [calculadora de AWS](https://calculator.aws/) con los precios de
  arriba; los importes son **sin IVA** y con cambio 1 $ = 0,92 €.

> Estimación de costes, no una oferta. Antes de contratar hay que confirmar precios y región.
