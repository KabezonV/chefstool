**Asunto:** Chefstool — entrega del 13/11: versión móvil y lectura de albaranes corregida

Hola Bernart:

Ya está la entrega. Además del trabajo de móvil, esta vez va incluida una corrección
importante de la lectura de albaranes, que es lo que ha alargado el plazo. Te dejo primero
los enlaces y luego el detalle.

**Enlaces**

- **Repositorio completo (GitHub):** <https://github.com/KabezonV/chefstool>
  Es público: no necesitas invitación ni cuenta, lo abres y ya está.
- **Descarga directa en ZIP (esta entrega, 13/11/2026):**
  <https://github.com/KabezonV/chefstool/archive/refs/tags/entrega-2026-11-13.zip>
- **Página de la versión concreta** (por si quieres ver los cambios antes de descargar):
  <https://github.com/KabezonV/chefstool/tree/entrega-2026-11-13>

**Cómo levantarlo (3 pasos)**

Lo único que hace falta es tener **Docker Desktop** instalado y abierto. No hay que instalar
Java, ni Node, ni base de datos:

```bash
git clone https://github.com/KabezonV/chefstool.git
cd chefstool
cp .env.ejemplo .env
docker compose up --build
```

Cuando termine de compilar, abre <http://localhost:8081>, pulsa **Crear cuenta** y, ya dentro
del panel, **Cargar datos de ejemplo** para tener productos, compras y fichas con los que
probar. La primera vez tarda unos minutos porque compila; las siguientes arranca en segundos.

**Qué incluye esta entrega**

*Uso en móvil*

- La app se maneja entera desde el teléfono sin romper nada del escritorio: las tablas se
  convierten en fichas legibles, el menú se abre deslizando y el formulario de albaranes se
  rellena cómodamente con el pulgar.
- **Tema claro y oscuro**, con la preferencia guardada. Lo que viste en negro era el modo
  oscuro forzado del navegador, no la app; ahora hay un botón para elegir.
- **Ver la contraseña** al entrar, al registrarse y al restablecerla.
- El navegador ya ofrece **guardar la contraseña**, que antes no hacía.

*Lectura de albaranes*

- **Reconoce al proveedor y lo da de alta solo** si no estaba en el catálogo, sin duplicarlo
  cuando ya existe. Si no lo reconoce, la pantalla de revisión te deja escribirlo a mano.
- **Entiende las filas reales de tus proveedores**, las que llevan código, lote, cantidad,
  precio, importe y % de IVA, no solo las de cuatro columnas.
- **No confunde los totales con artículos**: las líneas de base imponible, IVA o descuento ya
  no se cuelan como productos.
- **Avisa cuando la foto no da para leerla**, antes de procesarla.

**Por qué ha llevado más días de lo previsto**

El trabajo de móvil estaba planteado como un ajuste de estilos. Al probarlo en teléfonos de
verdad empezaron a salir cosas que desde el ordenador no se ven:

- **La app se quedaba en negro** tras desplegar: el móvil guardaba en caché un `index.html`
  antiguo que apuntaba a ficheros que ya no existían. Hubo que rehacer el mecanismo de caché
  y forzar a que ese fichero se pida siempre al servidor.
- **Nombres de fichero largos**: las fotos del móvil traen nombres de más de 30 caracteres sin
  espacios y desbordaban las tarjetas, arrastrando la página entera en horizontal.
- **Zoom automático en iPhone**: al tocar un campo, Safari ampliaba la pantalla y ya no volvía.
- **La plantilla de entorno del backend estaba desfasada**, justo lo que se nota cuando alguien
  intenta levantar el proyecto desde cero.
- **La lectura de albaranes fallaba con fotos reales.** Probándola con albaranes de proveedores
  de verdad salieron tres problemas de fondo: el lector partía las columnas y no reconocía las
  líneas; las fotos reenviadas por WhatsApp llegan comprimidas a 768 px, sin resolución para el
  texto pequeño; y las filas con muchas columnas no encajaban en el formato que esperaba.

La lectura no era "un ajuste": había que cambiar cómo se segmenta la imagen, ampliar las fotos
pequeñas antes de leerlas, rehacer el reconocimiento de filas y añadir controles para no
inventar importes. Cada arreglo obligaba a volver a probar todo lo anterior, y de ahí el
retraso: no era añadir trabajo nuevo, era que lo que ya funcionaba dejaba de hacerlo.

**Cómo está probado**

- Revisado en pantallas de 320 px y 390 px (móvil pequeño y habitual), en tema claro y oscuro,
  y en escritorio para confirmar que no cambia nada de lo que ya teníamos.
- **165 pruebas automáticas en verde** (133 del backend y 32 del frontend), con la compilación
  y la revisión de código correctas.
- He clonado el repositorio desde cero, como lo vas a hacer tú, y lo he levantado: base de
  datos, migraciones, API y web funcionando. No debería darte ninguna sorpresa.
- Para la lectura he dejado **dos albaranes de prueba en `docs/ejemplos/`**, con escrito lo que
  debe salir, para que compruebes tú mismo el acierto: uno limpio (saca las 6 líneas exactas) y
  otro con aspecto de foto torcida y con sombra (saca 4 de 6).

**Lo que sigue pendiente de tu decisión**

La aplicación está terminada y funciona, pero para meter datos reales falta **dónde alojarla**
(servidor, dominio y copias de seguridad). Está detallado en `docs/PENDIENTES-BERNART.md`.
Cuando me digas por dónde tirar, lo dejo funcionando.

Cualquier cosa que veas rara al probarlo, dímela y la corrijo el mismo día.

Un saludo,
Víctor
