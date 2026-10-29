# Lo que falta para seguir — bloqueantes del proyecto

**Fecha:** 29/10/2026 · **Para:** Bernart · **Estado:** la aplicación está terminada y
funcionando en local; quedan **dos decisiones que no dependen de mí** para poder avanzar.

---

## 1. Resumen en treinta segundos

| # | Bloqueante | Qué es | Qué necesito | Tiempo de trabajo después | Coste |
| --- | --- | --- | --- | --- | --- |
| 1 | **Servicio de OCR con IA** | El lector de albaranes con modelo de visión **ya está programado**, pero está apagado porque no tiene clave | La **clave de API** del proveedor que elijamos (+ 5 albaranes reales) | Medio día (si es OpenAI) · 0 si es Gemini | 0 – 3,17 €/mes |
| 2 | **Servidor** | El despliegue está preparado y probado en local | **Cuenta de AWS** (o Render) con un rol para mí, **dominio** y **región** | 1 – 2 días | 27 – 50 €/mes |

Todo lo demás (correo, contratos, copias de seguridad) **no bloquea la demo**, pero hay que
cerrarlo antes de meter datos reales de compras. Está en el punto 4.

---

## 2. Bloqueante 1 — El servicio de OCR

### Qué hay hecho ya

| Entrada | Cómo se lee hoy | Coste | Estado |
| --- | --- | --- | --- |
| PDF digital | Se extrae el texto embebido (PDFBox), sin OCR | 0 € | ✅ Funcionando |
| Foto o escaneo | Tesseract en el servidor, con diccionario español | 0 € | ✅ Funcionando (precisión media) |
| Foto o escaneo (con IA) | `ExtractorVision`: modelo de visión por API | Según proveedor | ✅ **Programado y probado**, apagado por falta de clave |
| Demostración | Datos de ejemplo generados con el catálogo | 0 € | ✅ Funcionando |

El lector con IA entra **antes** que Tesseract y, si falla (sin cuota, sin red, respuesta
rara), la aplicación **cae sola** al lector local: nunca se queda sin leer el albarán
(`ExtractorHibrido`).

### Qué falta exactamente

1. **La clave de API** (o que me autoricéis a generarla con la cuenta de la empresa).
2. **Decidir proveedor** con la comparativa de costes que ya tienes.
3. **5 albaranes reales** de proveedores distintos para medir la calidad antes de cerrar la
   decisión. Es medio día de trabajo y no cuesta nada.

### Opciones

| Opción | Precisión en fotos | Coste con 20 cocinas | Trabajo para activarlo |
| --- | --- | --- | --- |
| **Tesseract** (lo de ahora) | Media | 0 € | Ninguno |
| **Gemini** (Google AI Studio) | Buena | 2,40 €/mes (Flash-Lite) · 16,74 €/mes (Gemini 3 Flash) | **Ninguno**: ya está integrado, solo poner la clave |
| **OpenAI** (recomendado en la comparativa de costes) | Buena | 3,17 €/mes | Medio día para el adaptador + token |
| **AWS Textract** | Buena en facturas | 8,30 €/mes en modo texto · 55 €/mes en modo facturas | 2 días |

> ⚠️ **Ojo con un documento antiguo:** en `PLAN-FASE-2.md` (23/10) propuse Amazon Textract
> porque daba por hecho que había que ir a AWS de todas formas. El análisis de costes del
> 26/10 lo descarta por precio: **Textract cuesta 2,6 veces más que OpenAI** en modo texto y
> 17 veces más en modo facturas. **La recomendación vigente es OpenAI**, y Gemini como
> alternativa gratuita para empezar.

### Cómo se activa (para que no haya dudas de esfuerzo)

```bash
# Opción gratuita, hoy mismo, sin tocar código:
CHEFSTOOL_OCR_VISION_CLAVE=<clave de Google AI Studio>
CHEFSTOOL_OCR_VISION_MODELO=gemini-3.5-flash
```

Con OpenAI hace falta un adaptador nuevo (`ExtractorOpenAi`) que implemente el mismo puerto
`ExtractorDocumento`: no cambia ni una pantalla ni un caso de uso. Medio día.

---

## 3. Bloqueante 2 — El servidor

### Qué hay hecho ya

- **Imagen Docker del backend** con Tesseract y español dentro, `HEALTHCHECK` incluido.
- **`docker-compose` completo** (backend + PostgreSQL 17 + frontend): `docker compose up --build`
  y la aplicación entera funciona.
- **`render.yaml`**: despliegue en Render en un clic (backend Docker + frontend estático).
- **Almacenamiento de albaranes en S3** ya programado: bucket privado y enlaces firmados de 5
  minutos. En local funciona con disco o con MinIO.
- **Migraciones automáticas** con Flyway: el esquema se crea solo al arrancar.
- **Plan de despliegue en 8 pasos** (`DESPLIEGUE.md`) e **informe de dimensionamiento AWS** con
  precios y umbrales de crecimiento.

### Qué falta exactamente

1. **Cuenta de AWS** de la empresa, o confirmación de que la creo yo (y quién paga).
2. **Rol o usuario IAM para mí** con permisos de EC2, RDS, S3, Route 53, CloudWatch y SSM.
3. **Dominio**: ¿`app.chefstool.es` nuevo, o un subdominio de la web corporativa?
4. **Región**: por defecto **Irlanda (`eu-west-1`)**; si queréis los datos en España,
   **Aragón (`eu-south-2`)** (algo más caro).
5. **Aprobación del gasto**: **50 €/mes** para las 20 cocinas (opción recomendada) o **27 €/mes**
   empezando por la económica.

### Si AWS tarda

Para la demo privada **no hace falta AWS**: con el `render.yaml` la aplicación se publica en
Render en una tarde con plan gratuito. Sirve para enseñarla y para decidir con algo delante.
Lo que no haría es meter datos reales de compras ahí.

---

## 4. Antes de datos reales (no bloquean la demo)

| Tema | Qué falta | Dónde está el detalle |
| --- | --- | --- |
| **Correo transaccional** | Contratar proveedor (Resend, SES o SendGrid) y verificar el dominio. Hoy las invitaciones y la recuperación generan el enlace, pero no envían correo | `INFORME-BERNART.md` → Decisión 2 |
| **Contratos RGPD (art. 28)** | Firmar el encargo del tratamiento con el proveedor de alojamiento y con el de OCR si se activa | `RGPD.md` → punto 2 |
| **Copias de seguridad** | Snapshot y **prueba de restauración** hecha de verdad, no solo programada | `DESPLIEGUE.md` → checklist |
| **Plazo de conservación** | Definir cuántos años se guardan albaranes y compras (fiscal: 4 años) | `RGPD.md` → punto 2 |
| **Aviso de privacidad** | Poner la identidad real del responsable (NIF, correo) | `RGPD.md` → punto 2 |
| **Varias instancias** | El límite de intentos de login está en memoria: con más de un servidor hace falta Redis o una tabla | `DESPLIEGUE.md` → limitaciones |
| **Albaranes reales** | Formatos, descuentos y maquetas de cada proveedor | `QA.md` → pendiente de verificar a mano |

---

## 5. Lo que ya está terminado

- Registro, acceso, invitaciones, recuperación de contraseña, roles y permisos.
- Albaranes: subir, leer, revisar, confirmar compra y guardar el archivo.
- Productos con histórico de precios, proveedores, clientes y eventos.
- Fichas técnicas con coste por ración, margen y exportación a PDF A4.
- Panel, informes de coste por proveedor, variaciones de precio, alertas y exportación a Excel.
- RGPD: aviso de privacidad, descarga de datos y baja de cuenta con borrado en cascada.
- **116 pruebas de backend** (con su propio PostgreSQL en Testcontainers) y **25 de frontend**,
  todas en verde.
- Documentación completa en `docs/` (ver índice en el `README.md`).

---

## 6. Lo que necesito de ti, por orden

1. **OK a la demo privada** y, si quieres, tres personas para probarla.
2. **Clave de OCR**: o la de OpenAI (con token), o me autorizas a generar una gratuita de Google
   AI Studio para empezar hoy mismo.
3. **5 albaranes reales** de proveedores distintos.
4. **Cuenta de AWS** (o Render) + rol IAM + dominio + región. Si AWS se alarga, publicamos la
   demo en Render y seguimos.
5. **Correo**: ¿contratamos ya un proveedor (0–15 €/mes) o lo dejamos para el piloto?
6. **Una cocina piloto** con su jefe de cocina.

Con los puntos 1 y 2 puedo tener la lectura de albaranes con IA funcionando **el mismo día**.

---

## 7. Mensaje listo para enviar

> Hola Bernart:
>
> Estoy parado en dos cosas que dependen de vosotros; el resto está terminado.
>
> **1. Lectura de albaranes (OCR).** El lector con IA ya está programado y probado, solo le falta
> la clave. Necesito el **token de OpenAI** (o que me confirméis que puedo generarlo con la cuenta
> de la empresa). Si prefieres verlo antes de decidir, dime y activo una clave **gratuita de Google
> AI Studio** —no pide tarjeta— y lo tienes funcionando hoy. Con cualquiera de las dos, mándame
> también **5 albaranes reales** de proveedores distintos para medir la calidad.
>
> **2. Servidor.** Necesito la **cuenta de AWS** y un usuario o rol para mí con permisos de EC2,
> RDS, S3, Route 53, CloudWatch y SSM; el **dominio** (uno nuevo tipo `app.chefstool.es` o un
> subdominio de la web corporativa) y la **región** (Irlanda por defecto; Aragón si queréis los
> datos en España). El coste es de **50 €/mes** para las 20 cocinas, o **27 €/mes** si empezamos
> por la opción económica. Si AWS tarda, publico la demo privada en Render en una tarde y no
> perdemos tiempo.
>
> Y cuando puedas: ¿contratamos ya el correo (0–15 €/mes) y qué cocina hacemos piloto?
