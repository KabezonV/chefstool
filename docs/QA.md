# QA y pruebas

## Cómo se ejecutan

```bash
# Backend: 137 tests (unitarios, integración y seguridad)
cd chefstool-backend && ./mvnw clean test

# Frontend: 37 tests (Vitest + Testing Library)
cd chefstool-frontend && npm test        # vitest run
cd chefstool-frontend && npm run test:watch
```

Los tests de backend que arrancan la aplicación levantan **su propio PostgreSQL con
Testcontainers** (`postgres:17-alpine`), así que solo necesitan **Docker** en marcha: no
hacen falta ni la base de datos local ni el archivo `.env` (ni variables `DB_*`). El
contenedor se crea una vez por ejecución y los tests son transaccionales: cada uno hace
*rollback*. La primera ejecución tarda algo más porque descarga la imagen.

Los tests de frontend no salen a red (`fetch` y el cliente de API van mockeados).

> Si `npm install` falla con `EACCES` en `~/.npm/_cacache`, es un problema de
> permisos de la caché global, no del proyecto. Solución sin tocar permisos:
> `npm install --cache /tmp/npm-cache`.

## Qué cubren los tests

| Ámbito | Fichero | Qué valida |
| --- | --- | --- |
| Modelo de datos | `ChefstoolBackendApplicationTests` | Arranque, Flyway y validación del esquema |
| Productos | `ProductoServiceTest`, `ProductoControllerTest` | Alta, duplicados, borrado lógico, reciclado de nombres |
| Parser de albaranes | `ParserAlbaranTest` | Proveedor (incluido el caso en que el OCR le pega la fecha), fecha, número (también si llega cortado), líneas, filas con columnas de más (código, lote, %IVA), líneas de totales que no deben convertirse en artículos, cargos que sí son líneas (portes, envases retornables, recargo de combustible), importes con coma y miles |
| Extracción real | `DocumentoControllerTest` | Sube un PDF y comprueba los datos extraídos; imagen sin OCR no inventa datos; varias fotos se juntan en un solo documento |
| OCR con IA | `ExtractorVisionTest` | Interpretación del JSON del modelo, importes en formato español, bloques markdown, respuesta vacía, cuándo se reintenta con el modelo de corrección y elección de la mejor lectura |
| OCR híbrido | `ExtractorHibridoTest` | Usa la IA si está configurada y cae a Tesseract si falla o no hay clave |
| OCR de fotos | `LectorImagenTest` | La segmentación por bloques mantiene cada fila del albarán en una línea, puntúa mejor que las columnas partidas, amplía las imágenes pequeñas y reduce las enormes |
| PDF escaneado | `LectorPdfTest` | Detecta que no hay texto y rasteriza las páginas (máx. 3) para pasarlas por OCR; aviso claro si el OCR no está disponible |
| Compras y precios | `ComprasYPreciosTest`, `CompraServiceTest` | Filtros, detalle, mejor proveedor, alertas por umbral, alta del proveedor leído del albarán cuando no existe, reutilización del existente sin duplicar y descarte de nombres contaminados |
| Fichas | `FichaServiceTest`, `FichaControllerTest` | Coste con el mejor precio, productos sin precio, margen (incluido negativo) |
| Equipo | `EquipoServiceTest`, `EquipoControllerTest` | Invitaciones, duplicados, último propietario, permisos por rol |
| Recuperación de contraseña | `RecuperacionPasswordServiceTest` | Enlace de un solo uso, caducidad, enlace nuevo anula el anterior, correo desconocido |
| Permisos de catálogo | `PermisosCatalogoTest` | Un miembro crea y edita productos pero recibe 403 al borrar; el propietario sí borra |
| Fuerza bruta | `AuthControllerTest` | 5 intentos fallidos → 429 incluso con la contraseña correcta |
| Almacenamiento | `AlmacenamientoS3Test` | Subida al bucket con la clave y el tipo correctos, descarga, objeto inexistente, configuración obligatoria |
| Autenticación | `AuthControllerTest` | Registro, duplicados, login, 401 sin token |
| Panel | `InformePanelTest` | Agregados, ahorro potencial, documentos pendientes, coste por proveedor (gasto, ticket medio, variación media y orden del ranking) |
| Datos de ejemplo | `DatosEjemploControllerTest` | Siembra completa, no duplica, exige autenticación |
| Importación CSV | `ImportacionProductosServiceTest` | Cabecera opcional, separador `;` o `,`, comillas, BOM, duplicados sin cortar la importación |
| Productos (paginación) | `ProductoControllerTest` | Envoltorio `contenido/totalElementos`, búsqueda por `q`, orden asc/desc y orden no permitido |
| Ficha en PDF | `FichaPdfServiceTest` | Genera un PDF válido con el nombre y el coste de la ficha |
| RGPD / baja de cuenta | `BajaCuentaServiceTest` | Contraseña obligatoria, borrado de la organización si está solo, bloqueo si es el último propietario con equipo, exportación de datos personales |
| Frontend | `formato`, `importes`, `utils`, `barras`, `documentos`, `ResourcePage`, `Auth` | Formateo de números y fechas, avisos de importes que no cuadran (líneas frente a base imponible, base más IVA frente a total, tolerancia de céntimos, línea cuyo importe no sale de cantidad por precio y líneas del formulario que solo traen cantidad y precio), utilidades, gráfico de barras, traducción de estados de documento, tabla genérica con alta, paginación, búsqueda, orden por cabecera, exportación CSV y Excel (firma zip + contenido) y pantalla de acceso |

## Pasada de QA en navegador

Se condujo un Chrome headless por la interfaz (35 comprobaciones) con cuentas
nuevas para no contaminar datos existentes.

**Flujo principal (14/14)**

- Registro desde la interfaz y llegada al panel
- Estados vacíos correctos (0 €, sin compras, todo revisado)
- Alta de producto y proveedor desde formularios
- Albarán de ejemplo → revisión con líneas → confirmación → compra visible
- Alta de ficha técnica y apertura del editor
- Invitación de un compañero y aparición como pendiente
- Ruta inexistente con página 404 amable
- Cierre de sesión

**Casos límite (9/9)**

- Duplicado de producto: mensaje claro (*"Ya existe un producto llamado…"*)
- Nombre en blanco: error de validación visible
- Imagen sin OCR disponible: el documento avisa y no inventa datos
- Filtro de compras sin resultados: no rompe
- Vista tablet (768 px): menú de hamburguesa y barra lateral fuera de pantalla
- Token inválido: redirección automática al acceso

**Resto de pantallas (12/12)**

- Ficha de producto con comparativa y mejor precio
- Editor de ficha con coste y margen recalculados
- Detalle de compra con líneas y totales
- Informes con gasto total y variaciones
- Usuarios y configuración
- Panel con datos reales y variación mensual

## Observaciones (no son fallos)

1. **Confianza baja**: la pantalla de revisión ya explica que, por debajo del
   85 %, hay que revisar proveedor, importes y líneas antes de confirmar.
2. **Cuenta nueva sin datos**: resuelto con la tarjeta *Cargar datos de ejemplo*
   del dashboard (`POST /api/demo/datos`).
3. **Nombre del albarán de ejemplo**: siempre es `albaran-demo.pdf`.

## Prueba de volumen

`scripts/qa-volumen.sh` registra una organización nueva, crea 120 compras confirmadas
repartidas en 6 meses y mide los listados. Resultado del 22/10/2026:

| Comprobación | Resultado |
| --- | --- |
| 120 compras creadas vía API (documento + confirmar) | 111 s (≈0,9 s por compra) |
| `GET /compras?pagina=0&tamano=20` | 0,34 s · total 120 · 6 páginas · 20 filas |
| `GET /compras?pagina=5` (última) | 0,32 s · 20 filas |
| `GET /compras?orden=importeTotal&direccion=desc` | 0,32 s · orden correcto |
| `GET /compras?proveedorId=…` | 0,21 s · 40 filas (1 de 3 proveedores) |
| `GET /productos?q=aceite` | 0,14 s · 1 resultado |
| `GET /documentos?pagina=0&tamano=20` | 0,20 s · 6 páginas |
| `GET /informes/panel` | 0,48 s |
| `GET /informes/proveedores` | 0,18 s |
| Página fuera de rango (`pagina=99`) | 200 con `contenido` vacío y `totalElementos` correcto |

Los tiempos de esa medición incluían la latencia hasta una base alojada externa; con el
PostgreSQL local del `docker-compose` son bastante menores.

## Robustez con fotos malas (16/11/2026)

Cuatro fotos generadas a propósito con los mismos defectos de un móvil de gama baja o de
una foto hecha con prisa (la misma tabla de 6 artículos, total 41,58 €). Todas con la
lectura por IA configurada:

| Foto | Defecto | Líneas | Proveedor, fecha y total |
| --- | --- | --- | --- |
| Torcida 12° | hoja inclinada sobre la mesa | 6 de 6 | Correctos |
| Perspectiva fuerte | foto en ángulo, hoja en trapecio | 6 de 6 | Correctos |
| Gama baja | 2 MP, ruido, desenfoque y JPEG calidad 35 | 6 de 6 | Correctos |
| Contraluz | sombra lateral dura y 8° de inclinación | 6 de 6 | Correctos |

Un albarán de bebidas con **portes, envases retornables y recargo de combustible** se
lee con las seis líneas (los tres cargos incluidos) y la suma coincide con la base
imponible, así que no salta ningún aviso. Si el cargo no llega como línea, la revisión
avisa de que falta y ofrece añadirlo con un botón.

Las descripciones se leen bien; el punto débil son las columnas de cantidad y precio, que
a veces se cruzan (por ejemplo 2 × 1,20 € donde el papel dice 3 × 1,20 €). Como el importe
que devuelve la lectura sí es coherente con esos dos números, la comprobación por línea no
lo detecta: lo detecta el aviso global, porque la suma de las líneas ya no coincide con la
base imponible del documento. Ese aviso es el que se ve en la pantalla de revisión.

## Albaranes reales de móvil (17/11/2026)

24 fotos reales de albaranes y tickets de proveedor, tal y como llegan por mensajería
(768 px de ancho). Se pasaron tres veces por la aplicación completa:

| Versión | Líneas leídas | Base + IVA = total |
| --- | --- | --- |
| Imagen tal cual | 216 | 14 de 24 |
| Imagen ampliada a 1600 px y JPEG de calidad 95 | 205 | 17 de 24 |
| Lo anterior + autocomprobación en el prompt | 201 | 19 de 24 |
| Lo anterior + segunda pasada con `gpt-4o` si no cuadra | 215 | 23 de 24 |

Con la segunda pasada, además, el número de documentos cuya suma de líneas coincide con la
base imponible sube de 7 a 11 de 24 (por ejemplo, un ticket de 31 líneas pasó de no tener
base imponible a leerla, y otro pasó de 0 € en las líneas a cuadrar al céntimo).

Lo que sí sale bien: **nunca** hubo un documento sin leer, el proveedor y la fecha se
reconocen en la mayoría, y la coherencia de los totales mejora con cada corrección.

Lo que no: en los tickets densos (20-30 líneas) la lectura se deja líneas por el camino,
así que la suma no coincide con la base imponible. Esos documentos son justo los que
enseñan el aviso de la pantalla de revisión. En 2 de las 24 fotos no se reconoció la base
imponible (quedó a 0), y en varias la fecha salió con el año cambiado.

Conclusión práctica: para un ticket largo y borroso hay que repasar líneas a mano; para un
albarán normal de una hoja la lectura es buena. La segunda pasada con un modelo mayor solo se
paga en los documentos que no cuadran (13 de 24 en esta muestra), no en todos.

## Pendiente de verificar a mano

- Albaranes reales de proveedores concretos (formatos y descuentos).
- iPad físico en vertical y horizontal, y captura con la cámara.
- Volumen de miles de compras: probado con 120 (ver tabla); con miles convendría
  revisar el informe del panel, que hoy recorre todas las compras de la organización.
