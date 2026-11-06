**Asunto:** Chefstool — versión móvil entregada (enlaces para descargar y probar)

Hola Bernart:

Ya está lista la versión para móvil. Te dejo los enlaces y, al final, por qué ha llevado más
días de lo que te dije.

**Enlaces**

- **Repositorio completo (GitHub):** <https://github.com/KabezonV/chefstool>
  Es público, así que no necesitas invitación ni cuenta: lo abres y ya está.
- **Descarga directa en ZIP (versión de esta entrega):**
  <https://github.com/KabezonV/chefstool/archive/refs/tags/entrega-2026-11-06.zip>
- **Página de la versión concreta** (por si prefieres revisar los cambios antes de descargar):
  <https://github.com/KabezonV/chefstool/tree/entrega-2026-11-06>

**Cómo levantarlo (3 pasos)**

Lo único que hace falta es tener **Docker Desktop** instalado y abierto. No hay que instalar
Java, ni Node, ni base de datos:

```bash
git clone https://github.com/KabezonV/chefstool.git
cd chefstool
cp .env.ejemplo .env
docker compose up --build
```

Cuando acabe de compilar, abre <http://localhost:8081>, pulsa **Crear cuenta** y, dentro del
panel, **Cargar datos de ejemplo** para tener productos, compras y fichas con los que probar.
La primera vez tarda unos minutos porque compila el backend y el frontend; las siguientes
arranca en segundos. Si prefieres el ZIP, se descomprime, se copia igual el `.env.ejemplo` a
`.env` y se lanza el mismo `docker compose up --build`.

**Qué incluye**

- **Uso en móvil completo**: la app se maneja desde el teléfono sin romper el diseño de
  escritorio. Las tablas se convierten en fichas legibles, el menú lateral se abre deslizando y
  el formulario de albaranes se rellena cómodamente con el pulgar.
- **Tema claro y oscuro**, con la preferencia guardada. Lo que viste en negro era el modo
  oscuro forzado del navegador, no la app; ahora hay un botón para elegir.
- **Ver la contraseña** al entrar, al registrarse y al restablecerla.
- **Guardar contraseña**: el navegador ya ofrece recordarla, que antes no lo hacía.

**Por qué ha llevado más días de lo previsto**

El trabajo de móvil estaba planteado como un ajuste de estilos, pero al probarlo en teléfonos de
verdad empezaron a salir problemas que desde el ordenador no se ven:

- **La app se quedaba en negro** tras desplegar: el móvil conservaba en caché un `index.html`
  antiguo que apuntaba a ficheros que ya no existían. Hubo que rehacer el mecanismo de caché
  del service worker y forzar a que el index se pida siempre al servidor.
- **Nombres de fichero largos**: las fotos del móvil traen nombres de más de 30 caracteres sin
  espacios y desbordaban las tarjetas, arrastrando toda la página en horizontal.
- **Zoom automático en iPhone**: al tocar un campo, Safari ampliaba la pantalla y ya no volvía.
- **La plantilla de entorno del backend estaba desfasada** (apuntaba a un servicio que ya no se
  usa), justo lo que se nota cuando alguien intenta levantar el proyecto desde cero.

Cada uno de esos arreglos obligaba a volver a probar todo lo anterior, y de ahí el retraso: no
era añadir trabajo nuevo, era que lo que ya estaba hecho dejaba de funcionar.

**Cómo está probado**

- Revisado en pantallas de 320 px y 390 px (móvil pequeño y habitual), en tema claro y oscuro,
  y en escritorio para confirmar que no cambia nada de lo que ya teníamos.
- 32 pruebas automáticas en verde, revisión de código y compilación correctas.
- He clonado el repositorio desde cero, como lo vas a hacer tú, y lo he levantado: base de datos,
  migraciones, API y web funcionando. No debería darte ninguna sorpresa.

Cualquier cosa que veas rara al probarlo, dímela y la corrijo el mismo día.

Un saludo,
Víctor
