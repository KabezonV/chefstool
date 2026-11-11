# Manual de usuario de Chefstool

Guía para el día a día en cocina. Está pensada para imprimirse o consultarse en la
tablet; no hace falta saber nada de informática.

---

## 1. Entrar

1. Abre el enlace de tu cocina en el navegador (Safari, Chrome…).
2. Escribe tu **correo** y tu **contraseña**.
3. En la tablet o el móvil, pulsa **Compartir → Añadir a pantalla de inicio** para
   tenerlo como una app más.

¿No recuerdas la contraseña? En la pantalla de acceso, pulsa **¿Has olvidado la
contraseña?**, escribe tu correo y sigue el enlace que recibirás.

## 2. Quién puede hacer qué

| | Propietario | Administrador | Miembro |
| --- | --- | --- | --- |
| Ver y trabajar con compras, productos y fichas | Sí | Sí | Sí |
| Borrar productos, proveedores y fichas | **Sí** | Sí | No |
| Invitar a compañeros y cambiar roles | Sí | Sí | No |
| Cambiar el nombre de la organización | Sí | Sí | No |

Si intentas borrar algo y no te deja, es por este motivo: pídeselo a un administrador.

## 3. Meter un albarán (la tarea de cada día)

### Opción A — Foto con la tablet o el móvil

1. **Documentos → Añadir documento**.
2. Pulsa **Hacer foto**.
3. Encuadra el albarán entero, con buena luz, intentando que el papel quede plano.
4. Espera unos segundos: la aplicación lee el documento y te lleva a la pantalla de
   revisión.

### Opción B — Archivo (PDF o imagen)

1. **Documentos → Añadir documento → Elegir archivo**.
2. Selecciona el PDF que te haya mandado el proveedor.

### Opción C — Prueba rápida sin papel

**Documentos → Añadir documento → Probar con un ejemplo** genera un albarán de
demostración. Sirve para enseñar la aplicación sin tener un papel a mano.

## 4. Revisar antes de guardar

La pantalla de revisión es la más importante: **nada se guarda sin tu confirmación**.

- **Confianza**: el porcentaje indica cuánto ha reconocido la lectura automática.
  Por debajo del 85 % conviene mirar línea por línea.
- **Proveedor**: si el que leyó el albarán ya está en tu catálogo, sale elegido. Si no,
  aparece **"Dar de alta un proveedor nuevo…"** con el nombre detectado escrito debajo:
  corrígelo ahí y se dará de alta al confirmar. También puedes elegir **"Sin asignar"** o
  cualquier proveedor de la lista.
- **Aviso "Los importes no cuadran"**: si aparece, significa que las líneas no suman
  lo mismo que el total. Suele ser una línea que no se ha leído.
- **Aviso "No se han reconocido las líneas"**: la foto no era buena o el formato es
  raro. Puedes escribir las líneas a mano; el original queda guardado.

Comprueba sobre todo: **proveedor**, **fecha**, **cantidad y precio de cada línea** y
el **total**. Cuando esté bien, pulsa **Confirmar y guardar**.

Al confirmar, la aplicación:

- crea la compra con sus líneas e IVA,
- **da de alta el proveedor si todavía no está en tu catálogo** (con el nombre que
  leyó del albarán; si ya existe, reutiliza el que hay),
- actualiza el precio de cada producto en ese proveedor,
- recalcula las fichas técnicas que usan esos productos,
- avisa si algún proveedor ha subido un precio un 5 % o más.

## 5. Consultar compras y precios

**Compras** — histórico con filtros por proveedor y fechas. Pulsa **Ver** para el
detalle con líneas e IVA.

**Productos** — pulsa un producto para ver:

- los proveedores que lo sirven,
- el **mejor precio** de cada uno,
- la **evolución** del precio en el tiempo.

Desde aquí puedes exportar el catálogo a **Excel**.

## 6. Fichas técnicas (escandallos)

1. **Fichas técnicas → Nueva ficha**.
2. Pon el nombre, las raciones y el precio de venta.
3. Añade los ingredientes con su cantidad y unidad (se eligen del catálogo).
4. La aplicación calcula sola el **coste por ración** usando el mejor precio disponible
   y el **margen** sobre el precio de venta.

Si mañana sube el precio de un ingrediente, la ficha se actualiza sin tocar nada.

El botón **PDF** de la ficha descarga el escandallo en una hoja A4 (cabecera con el
restaurante, coste por ración, margen y tabla de ingredientes), lista para imprimir
o archivar.

## 7. Informes

**Informes** muestra:

- **Gasto total** y **gasto del mes** frente al mes anterior.
- **Ahorro potencial**: lo que te costarían las mismas compras si hubieras usado el
  mejor precio de cada producto.
- **Gasto por proveedor** y **por mes**.
- **Coste histórico por proveedor**: compras, ticket medio, peso sobre el gasto y
  cuánto han subido sus precios.
- **Mayores variaciones de precio**.

Cualquier tabla se puede descargar con **Excel** o **CSV**.

## 8. Alertas de precio

Cuando un proveedor sube un producto un 5 % o más, aparece un aviso en el **Panel** y en
**Compras**. Pulsa **Reconocer** cuando lo hayas visto (o llamado al proveedor).

## 9. Trabajar con el equipo

**Usuarios y configuración → Invitar**:

1. Escribe el correo del compañero y elige su rol.
2. Cuando esa persona se registre con ese correo, entrará directamente en tu cocina con
   el rol asignado.

Puedes cambiar el rol o quitar a alguien desde la misma pantalla.

## 10. Cargar el catálogo de golpe (CSV)

Si tienes los productos en Excel: **Productos → Importar CSV** con tres columnas:

```
nombre;categoria;unidadBase
Tomate pera;Verduras;kg
Aceite de oliva;Aceites;l
```

La cabecera es opcional; sirve la coma o el punto y coma. Al terminar verás cuántos se
crearon y cuáles se omitieron (por ejemplo, si ya existían).

## 11. Preguntas frecuentes

**¿Se puede borrar una compra mal metida?**
Sí, desde el detalle de la compra (o descartando el documento antes de confirmarlo).

**¿Qué pasa si subo el mismo albarán dos veces?**
Se crea otra compra. Bórrala desde el detalle si ha sido un error.

**¿Funciona sin internet?**
Necesita conexión: los datos viven en el servidor de la cocina. En la tablet puedes
tenerla abierta como app, pero las pantallas se cargan desde el servidor.

**¿Se ven las fotos de los albaranes después?**
Sí. En el detalle del documento hay un enlace **Abrir original**.

**¿Puedo cambiar la contraseña?**
En *¿Has olvidado la contraseña?* desde la pantalla de acceso. Ahora mismo dentro de la
aplicación no hay pantalla de "cambiar contraseña".

**¿Quién ve mis datos?**
Solo las personas de tu organización. Cada cocina está aislada: nadie de otra
organización puede ver tus compras ni tus precios.

**¿Puedo descargar mis datos o darme de baja?**
Sí. En **Configuración → Tus datos y privacidad** tienes *Descargar mis datos (JSON)* y
*Quiero darme de baja*. La baja pide tu contraseña y no se puede deshacer: elimina tu
cuenta y, si eras el único miembro, todos los datos de la organización. Si hay más
personas, antes hay que nombrar otro propietario. El aviso legal completo está en
`/privacidad`.

---

*Si algo no cuadra, apunta la pantalla y el mensaje exacto: con eso se puede reproducir
y arreglar en minutos.*
