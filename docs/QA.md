# QA y pruebas

## Cómo se ejecutan

```bash
# Backend: 76 tests (unitarios, integración y seguridad)
cd chefstool-backend && ./mvnw clean test

# Frontend: 24 tests (Vitest + Testing Library)
cd chefstool-frontend && npm test        # vitest run
cd chefstool-frontend && npm run test:watch
```

Los tests del backend usan la base de datos configurada en `.env`, pero son
transaccionales: cada test hace *rollback*. Los de frontend no salen a red
(`fetch` y el cliente de API van mockeados).

> Si `npm install` falla con `EACCES` en `~/.npm/_cacache`, es un problema de
> permisos de la caché global, no del proyecto. Solución sin tocar permisos:
> `npm install --cache /tmp/npm-cache`.

## Qué cubren los tests

| Ámbito | Fichero | Qué valida |
| --- | --- | --- |
| Modelo de datos | `ChefstoolBackendApplicationTests` | Arranque, Flyway y validación del esquema |
| Productos | `ProductoServiceTest`, `ProductoControllerTest` | Alta, duplicados, borrado lógico, reciclado de nombres |
| Parser de albaranes | `ParserAlbaranTest` | Proveedor, fecha, número, líneas, importes con coma y miles, líneas que no cuadran |
| Extracción real | `DocumentoControllerTest` | Sube un PDF y comprueba los datos extraídos; imagen sin OCR no inventa datos |
| Compras y precios | `ComprasYPreciosTest`, `CompraServiceTest` | Filtros, detalle, mejor proveedor, alertas por umbral |
| Fichas | `FichaServiceTest`, `FichaControllerTest` | Coste con el mejor precio, productos sin precio, margen (incluido negativo) |
| Equipo | `EquipoServiceTest`, `EquipoControllerTest` | Invitaciones, duplicados, último propietario, permisos por rol |
| Autenticación | `AuthControllerTest` | Registro, duplicados, login, 401 sin token |
| Panel | `InformePanelTest` | Agregados, ahorro potencial, documentos pendientes |
| Datos de ejemplo | `DatosEjemploControllerTest` | Siembra completa, no duplica, exige autenticación |
| Productos (paginación) | `ProductoControllerTest` | Envoltorio `contenido/totalElementos`, búsqueda por `q`, orden asc/desc y orden no permitido |
| Frontend | `formato`, `utils`, `barras`, `ResourcePage`, `Auth` | Formateo, tabla genérica con alta, paginación, búsqueda, orden por cabecera y exportación CSV, pantalla de acceso |

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

## Pendiente de verificar a mano

- Albaranes reales de proveedores concretos (formatos y descuentos).
- iPad físico en vertical y horizontal, y captura con la cámara.
- Comportamiento con muchos registros: la paginación está cubierta por tests,
  falta probarla con volúmenes reales (miles de compras).
