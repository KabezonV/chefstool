# API de Chefstool

Base: `http://localhost:8080/api`

## Convenciones

- Autenticación: `Authorization: Bearer <token>` en todas las rutas salvo
  `POST /auth/login` y `POST /auth/registro`.
- Los importes son `number` en euros; las fechas, `YYYY-MM-DD`.
- **Listados paginados**: `GET /productos`, `/proveedores`, `/clientes`, `/eventos`,
  `/documentos`, `/compras` y `/fichas` aceptan `pagina` (0-based, por defecto `0`) y
  `tamano` (1-100, por defecto `20`) y devuelven:

  ```json
  {
    "contenido": [ … ],
    "pagina": 0,
    "tamano": 20,
    "totalElementos": 42,
    "totalPaginas": 3
  }
  ```

  `/productos`, `/proveedores`, `/clientes` y `/eventos` aceptan además `q` para buscar
  por nombre (sin distinguir mayúsculas ni acentos de la consulta).
- **Orden**: cualquier listado acepta `orden=<campo>&direccion=asc|desc`. El campo se
  valida contra una lista blanca; si no es válido se aplica el orden por defecto (no da
  error). Campos admitidos:

  | Recurso | Campos de `orden` | Por defecto |
  | --- | --- | --- |
  | Productos | `nombre`, `categoria`, `unidadBase` | `nombre` asc |
  | Proveedores | `nombre`, `cif`, `email` | `nombre` asc |
  | Clientes | `nombre`, `email` | `nombre` asc |
  | Eventos | `nombre`, `fecha`, `comensales` | `fecha` desc |
  | Documentos | `createdAt`, `nombreOriginal`, `estado`, `confianzaExtraccion` | `createdAt` desc |
  | Compras | `fecha`, `importeTotal`, `numeroDocumento`, `createdAt` | `fecha` desc |
  | Fichas | `nombre`, `precioVenta`, `raciones` | `nombre` asc |
- Los errores devuelven `{"error": "mensaje"}` con el código HTTP adecuado:

| Código | Significado |
| --- | --- |
| 400 | Datos no válidos, duplicado o regla de negocio incumplida |
| 401 | Token ausente/caducado o credenciales incorrectas |
| 404 | El recurso no existe (o está borrado) |
| 409 | Conflicto de estado (documento ya confirmado/descartado) |

## Autenticación

### Registro

```bash
curl -X POST http://localhost:8080/api/auth/registro \
  -H 'Content-Type: application/json' \
  -d '{"email":"chef@cocina.es","password":"secreto123",
       "nombre":"Ana","organizacion":"Cocina Norte"}'
```

`201` con:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "usuarioId": "…",
  "email": "chef@cocina.es",
  "nombre": "Ana",
  "organizacionId": "…",
  "organizacion": "Cocina Norte",
  "rol": "propietario"
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"chef@cocina.es","password":"secreto123"}'
```

Devuelve el mismo objeto `SesionResponse`. Con contraseña incorrecta: `401`.

### Sesión actual

```bash
curl http://localhost:8080/api/auth/yo -H "Authorization: Bearer $TOKEN"
```

## Productos

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/productos?pagina=0&tamano=20&q=tomate&orden=nombre&direccion=asc` | Listado paginado, buscable y ordenable |
| POST | `/productos` | Alta (`nombre`, `categoria`, `unidadBase`) |
| PUT | `/productos/{id}` | Edición |
| DELETE | `/productos/{id}` | Borrado lógico (204) |
| GET | `/productos/{id}/precios` | Histórico y comparativa por proveedor |
| POST | `/productos/importar` | Alta masiva desde CSV (multipart `archivo`) |

```bash
curl -X POST http://localhost:8080/api/productos \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nombre":"Tomate pera","categoria":"Verdura","unidadBase":"kg"}'
```

`GET /productos/{id}/precios`:

```json
{
  "productoId": "…",
  "nombre": "Harina de trigo",
  "unidadBase": "kg",
  "mejorProveedor": "Verduras Lopez",
  "mejorPrecio": 8.90,
  "proveedores": [
    {"proveedorId":"…","proveedorNombre":"Verduras Lopez","precioActual":8.90,
     "precioMinimo":8.90,"precioMaximo":8.90,"ultimaCompra":"2026-10-12"},
    {"proveedorId":"…","proveedorNombre":"Frutas Garcia","precioActual":13.20,
     "precioMinimo":9.50,"precioMaximo":13.20,"ultimaCompra":"2026-10-05"}
  ],
  "evolucion": [
    {"fecha":"2026-08-15","precio":9.50,"proveedorNombre":"Frutas Garcia"}
  ]
}
```

### Importar catálogo desde CSV

`POST /productos/importar` con `multipart/form-data` y el campo `archivo`. Formato:
`nombre;categoria;unidadBase` (la cabecera es opcional; acepta también coma como
separador, comillas y el BOM de Excel). Devuelve:

```json
{"creados": 4, "omitidos": 1, "errores": ["Pimentón dulce: Ya existe un producto llamado Pimentón dulce"]}
```

Cada fila se guarda por separado: un duplicado no invalida el resto de la importación.
Límite de 1.000 filas por archivo.

## Proveedores

`GET|POST /proveedores`, `PUT|DELETE /proveedores/{id}`.
Campos: `nombre` (obligatorio), `cif`, `email`, `telefono`, `notas`.

## Clientes

`GET|POST /clientes`, `PUT|DELETE /clientes/{id}`.
Campos: `nombre` (obligatorio), `email`, `telefono`, `notas`.

## Eventos

`GET|POST /eventos`, `PUT|DELETE /eventos/{id}`.
Campos: `nombre` (obligatorio), `fecha`, `comensales`, `clienteId`, `notas`.

## Documentos

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/documentos?pagina=0&tamano=20` | Bandeja paginada (todos los estados) |
| POST | `/documentos` | Multipart `archivo` + `origen` (`subida`/`camara`) |
| POST | `/documentos` | Solo `origen=demo` → albarán de ejemplo sin archivo |
| GET | `/documentos/{id}` | Detalle con la extracción |
| PUT | `/documentos/{id}` | Guarda el borrador revisado (mismo cuerpo que la extracción) |
| POST | `/documentos/{id}/confirmar` | Crea la compra |
| DELETE | `/documentos/{id}` | Descarta (204) |
| GET | `/documentos/{id}/archivo` | Devuelve el archivo original |

```bash
# Subir una foto
curl -X POST http://localhost:8080/api/documentos \
  -H "Authorization: Bearer $TOKEN" \
  -F "archivo=@albaran.jpg" -F "origen=camara"

# Generar uno de demostración
curl -X POST http://localhost:8080/api/documentos \
  -H "Authorization: Bearer $TOKEN" -F "origen=demo"
```

Cuerpo de revisión y confirmación (`ExtraccionDocumento`):

```json
{
  "proveedorId": "…",
  "proveedorNombre": "Frutas Garcia",
  "numeroDocumento": "ALB-1234",
  "fecha": "2026-10-05",
  "moneda": "EUR",
  "importeNeto": 100.00,
  "importeIva": 10.00,
  "importeTotal": 110.00,
  "lineas": [
    {"descripcion":"Tomate","cantidad":10,"unidad":"kg",
     "precioUnitario":2.50,"importe":25.00,
     "productoId":"…","clienteId":null,"eventoId":null}
  ]
}
```

Al confirmar se crea la compra, se actualiza `proveedor_productos` y se genera
una alerta si el precio sube ≥5 % respecto al último registrado. Si llega
`proveedorNombre` sin `proveedorId` y no hay ningún proveedor con ese nombre, se
crea uno nuevo con ese nombre (sin duplicar cuando ya existe, ignorando
mayúsculas y espacios).

Al subir el archivo, el backend intenta leerlo:

- PDF digital → texto embebido (sin OCR).
- Imagen o PDF escaneado → Tesseract, si está instalado en el servidor.

Si no se puede leer, el documento se crea igualmente con
`"errorExtraccion": "mensaje"` y `extraccion: null`; se puede completar a mano
desde la pantalla de revisión.

## Compras

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/compras` | Filtros opcionales `proveedorId`, `desde`, `hasta` |
| GET | `/compras/{id}` | Detalle con líneas |

## Alertas de precio

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/alertas` | Pendientes (`?todas=true` incluye reconocidas) |
| POST | `/alertas/{id}/reconocer` | Marca como revisada (204) |

## Fichas técnicas

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/fichas` | Listado con coste por ración y margen |
| GET | `/fichas/{id}` | Detalle con líneas y precios aplicados |
| POST | `/fichas` | Alta con líneas |
| PUT | `/fichas/{id}` | Edición (recalcula costes) |
| DELETE | `/fichas/{id}` | Borrado lógico |
| GET | `/fichas/{id}/pdf` | Escandallo en PDF A4 (`application/pdf`) |

```json
{
  "nombre": "Salsa de tomate",
  "descripcion": "Base para pizzas",
  "raciones": 4,
  "precioVenta": 12.00,
  "lineas": [
    {"productoId":"…","descripcion":"Tomate","cantidad":2,"unidad":"kg"}
  ]
}
```

El coste usa el **mejor precio vigente** del producto entre todos los proveedores.
La respuesta incluye `costeTotal`, `costePorRacion`, `margenPct` y
`productosSinPrecio`.

## Panel e informes

```bash
curl http://localhost:8080/api/informes/panel -H "Authorization: Bearer $TOKEN"
```

```json
{
  "kpis": {
    "gastoMes": 364.54, "gastoMesAnterior": 140.25, "variacionPct": 159.92,
    "comprasMes": 3, "documentosPendientes": 0, "alertasPendientes": 2
  },
  "ahorroPotencial": 72.00,
  "lineasMejorables": 5,
  "comprasAnalizadas": 6,
  "gastoTotal": 609.29,
  "gastoPorProveedor": [{"etiqueta":"Frutas Garcia","importe":145.20}],
  "gastoPorMes": [{"etiqueta":"2026-10","importe":364.54}],
  "mayoresVariaciones": [
    {"producto":"Harina de trigo","proveedor":"Frutas Garcia",
     "porcentaje":26.92,"precioAnterior":10.40,"precioNuevo":13.20}
  ],
  "alertas": [{"id":"…","mensaje":"…","severidad":"critico"}],
  "documentosPorRevisar": [{"id":"…","nombreOriginal":"albaran.pdf"}]
}
```

### Coste histórico por proveedor

`GET /informes/proveedores` devuelve el ranking de proveedores ordenado por gasto:

```json
[
  {
    "proveedorId": "…", "nombre": "Distribuciones Norte",
    "compras": 4, "gastoTotal": 1290.08, "ticketMedio": 322.52,
    "porcentajeGasto": 82.0, "ultimaCompra": "2026-10-18",
    "productos": 3, "variacionMediaPct": 17.4
  }
]
```

`variacionMediaPct` compara el primer y el último precio de cada producto servido por
ese proveedor (media de las variaciones); es `null` si no hay dos precios comparables.

## Usuarios y organización

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/usuarios` | Miembros de la organización |
| POST | `/usuarios/invitar` | Invita por correo (`email`, `rol`) |
| GET | `/usuarios/invitaciones` | Invitaciones pendientes |
| DELETE | `/usuarios/invitaciones/{id}` | Cancela una invitación |
| PUT | `/usuarios/{id}/rol` | Cambia el rol (`propietario`, `admin`, `miembro`) |
| GET | `/organizacion` | Datos de la organización |
| PUT | `/organizacion` | Renombra la organización |

### Invitaciones

```bash
curl -X POST http://localhost:8080/api/usuarios/invitar \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"email":"chef@cocina.es","rol":"miembro"}'
```

Devuelve `estado: "anadido"` si la cuenta ya existía (se añade al momento) o
`estado: "pendiente"` si hay que esperar a que se registre con ese correo. En ese
caso, al registrarse entra en la organización que le invitó en lugar de crear una
propia.

### Permisos

| Acción | miembro | admin | propietario |
| --- | --- | --- | --- |
| Datos operativos (productos, albaranes, compras, fichas) | ✅ | ✅ | ✅ |
| Invitaciones y cambio de roles | ❌ | ✅ | ✅ |
| Renombrar la organización | ❌ | ✅ | ✅ |

Un rol sin permiso recibe `403`.

## Datos de ejemplo

`POST /demo/datos` (solo `propietario` o `admin`) siembra la organización actual con un juego
de prueba: 8 productos, 3 proveedores, 2 clientes, 2 eventos, 3 albaranes ya confirmados (uno
de ellos genera una alerta de subida de precio) y 2 fichas técnicas.

```bash
curl -X POST http://localhost:8080/api/demo/datos -H "Authorization: Bearer $TOKEN"
```

Respuesta: `{"productos":8,"proveedores":3,"clientes":2,"eventos":2,"compras":3,"fichas":2}`.
Devuelve `409` si la organización ya tiene productos, para no duplicar datos.

## Flujo completo de ejemplo

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/registro -H 'Content-Type: application/json' \
  -d '{"email":"demo@chef.es","password":"secreto123"}' | jq -r .token)

DOC=$(curl -s -X POST localhost:8080/api/documentos -H "Authorization: Bearer $TOKEN" \
  -F "origen=demo" | jq -r .id)

curl -s -X POST localhost:8080/api/documentos/$DOC/confirmar \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d @extraccion.json

curl -s localhost:8080/api/informes/panel -H "Authorization: Bearer $TOKEN" | jq .kpis
```
