# Lo que falta para seguir — bloqueantes del proyecto

**Fecha:** 29/10/2026 · **Actualizado:** 03/11/2026 · **Para:** Bernart · **Estado:** la
aplicación está terminada y funcionando en local. **El OCR ya no es un bloqueante**: con
Tesseract funciona hoy y a coste cero. Queda **una sola decisión** (el servidor) para avanzar.

---

## 1. Resumen en treinta segundos

| # | Bloqueante | Qué es | Qué necesito | Tiempo de trabajo después | Coste |
| --- | --- | --- | --- | --- | --- |
| 1 | ~~Servicio de OCR~~ **Resuelto** | Se lee con **Tesseract**, que va dentro de la imagen y no cuesta nada. Probado con un albarán real el 03/11 | Nada. Solo **5 albaranes reales** para afinar las reglas | 0 | **0 €** |
| 2 | **Servidor** | El despliegue está preparado y probado en local | **Cuenta de AWS** (o Render) con un rol para mí, **dominio** y **región** | 1 – 2 días | 6 €/mes (VPS) · 27 – 50 €/mes si es AWS |

Todo lo demás (correo, contratos, copias de seguridad) **no bloquea la demo**, pero hay que
cerrarlo antes de meter datos reales de compras. Está en el punto 4.

---

## 2. Bloqueante 1 — El servicio de OCR (resuelto sin coste)

### Qué hay hecho ya

| Entrada | Cómo se lee hoy | Coste | Estado |
| --- | --- | --- | --- |
| PDF digital | Se extrae el texto embebido (PDFBox), sin OCR | 0 € | ✅ Funcionando |
| Foto o escaneo | Tesseract en el servidor, con diccionario español | 0 € | ✅ **Probado el 03/11 con un albarán real** |
| Foto o escaneo (con IA) | `ExtractorVision`: modelo de visión por API | Según proveedor | ✅ Programado y probado; **opcional**, se activa con una clave |
| Demostración | Datos de ejemplo generados con el catálogo | 0 € | ✅ Funcionando |

**Decisión tomada: Tesseract.** Es gratuito, va dentro de la imagen Docker y no envía nada
fuera del servidor, así que no necesita contrato de encargado ni transferencia internacional.

Prueba del 03/11 con la aplicación en marcha, subiendo el mismo albarán por la API:

| | Gemini Flash | Tesseract |
| --- | --- | --- |
| Líneas detectadas | 5 de 5 | 4 de 5 |
| Suma de líneas = base imponible | Sí | No (error detectado y corregido: ver abajo) |
| Coste | céntimos/mes | 0 € |

El lector con IA sigue disponible **por si algún día se quiere comparar**: entra antes que
Tesseract y, si falla (sin cuota, sin red, respuesta rara), la aplicación **cae sola** al
lector local (`ExtractorHibrido`). Ponerlo o quitarlo es una variable de entorno, no un
cambio de código.

### Qué falta exactamente

1. **5 albaranes reales** de proveedores distintos para afinar las reglas de lectura. Es medio
   día de trabajo y no cuesta nada. Sin ellos, la precisión en fotos de móvil está sin medir.
2. **Nada más.** No hace falta clave ni contratar ningún servicio de OCR.

### Opciones (solo si algún día se quiere mejorar la precisión)

| Opción | Precisión en fotos | Coste con 20 cocinas | Trabajo para activarlo |
| --- | --- | --- | --- |
| **Tesseract** (la elegida) | Media | 0 € | Ninguno |
| **Gemini** (Google AI Studio) | Buena | 2,40 €/mes (Flash-Lite) · 16,74 €/mes (Gemini 3 Flash) | **Ninguno**: ya está integrado, solo poner la clave. La capa gratuita puede usar los datos para mejorar sus productos; la de pago no |
| **OpenAI** | Buena | 3,17 €/mes | Medio día para el adaptador + token |
| **AWS Textract** | Buena en facturas | 8,30 €/mes en modo texto · 55 €/mes en modo facturas | 2 días |

> ⚠️ **Ojo con un documento antiguo:** en `PLAN-FASE-2.md` (23/10) propuse Amazon Textract
> porque daba por hecho que había que ir a AWS de todas formas. El análisis de costes del
> 26/10 lo descarta por precio: **Textract cuesta 2,6 veces más que OpenAI** en modo texto y
> 17 veces más en modo facturas.

### Cómo se activa (para que no haya dudas de esfuerzo)

```bash
# Opcional: asi se añadiria la lectura con IA, sin tocar codigo.
# Sin estas lineas, la aplicacion usa Tesseract y no gasta nada.
CHEFSTOOL_OCR_VISION_CLAVE=<clave de Google AI Studio>
CHEFSTOOL_OCR_VISION_MODELO=gemini-3.5-flash
```

Con OpenAI haría falta un adaptador nuevo (`ExtractorOpenAi`) que implemente el mismo puerto
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

### Las cuatro opciones de AWS (precios verificados el 26/10, Irlanda)

| Opción | Qué incluye | Coste/mes |
| --- | --- | ---: |
| **A. Recomendada** | EC2 `t4g.medium` + RDS `db.t4g.micro` 20 GB + S3 | **54,6 $ (≈50 €)** |
| B. Económica | EC2 `t4g.small` + PostgreSQL en el contenedor + S3 | **27,1 $ (≈25 €)** |
| C. Alta disponibilidad | 2 × `t4g.medium` + balanceador + RDS Multi-AZ | **117 $** |
| D. Contenedores gestionados | ECS Fargate + balanceador + RDS | **77 $** |

> **Matiz importante:** la comparativa general dice que, **si la empresa no obliga a ir a AWS**,
> un **VPS de 6 €/mes** (Hetzner, centro de datos en Alemania o Finlandia) sobra para las 20
> cocinas: el mismo `docker-compose` que ya está probado. Los 50 €/mes son el precio de tener
> facturación, soporte y base gestionada de AWS, no de una necesidad técnica.

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
- **118 pruebas de backend** (con su propio PostgreSQL en Testcontainers) y **30 de frontend**,
  todas en verde.
- Documentación completa en `docs/` (ver índice en el `README.md`).

---

## 6. Lo que necesito de ti, por orden

1. **OK a la demo privada** y, si quieres, tres personas para probarla.
2. **5 albaranes reales** de proveedores distintos. Es lo único que falta para medir la
   precisión de la lectura en fotos de verdad.
3. **Cuenta de AWS** (o Render) + rol IAM + dominio + región. Si AWS se alarga, publicamos la
   demo en Render y seguimos.
4. **Correo**: ¿contratamos ya un proveedor (0–15 €/mes) o lo dejamos para el piloto?
5. **Una cocina piloto** con su jefe de cocina.

Con el punto 1 publico la demo privada; con los albaranes reales afino la lectura. **El OCR ya
no depende de vosotros: funciona y es gratis.**

---

## 7. Mensaje listo para enviar

> Hola Bernart:
>
> Estoy parado en una cosa que depende de vosotros; el resto está terminado.
>
> **1. Lectura de albaranes (OCR).** **Resuelto y a coste cero**: se lee con Tesseract, que va
> dentro de la imagen del servidor y no envía nada a terceros. Lo he probado con un albarán y
> funciona. Ya no necesito ninguna clave ni contratar nada. Solo te pido **5 albaranes reales**
> de proveedores distintos para afinar las reglas de lectura; es medio día de trabajo y no
> cuesta nada.
>
> **2. Servidor.** Necesito la **cuenta de AWS** y un usuario o rol para mí con permisos de EC2,
> RDS, S3, Route 53, CloudWatch y SSM; el **dominio** (uno nuevo tipo `app.chefstool.es` o un
> subdominio de la web corporativa) y la **región** (Irlanda por defecto; Aragón si queréis los
> datos en España). El coste es de **50 €/mes** para las 20 cocinas, o **27 €/mes** si empezamos
> por la opción económica. Si la política de la empresa no exige AWS, con un **VPS de 6 €/mes**
> sobra técnicamente: es el mismo despliegue que ya está probado. Si AWS tarda, publico la demo
> privada en Render en una tarde y no perdemos tiempo.
>
> Y cuando puedas: ¿contratamos ya el correo (0–15 €/mes) y qué cocina hacemos piloto?
