**Asunto:** Chefstool — versión móvil terminada (y por qué ha llevado más días)

Hola Bernart:

El trabajo de móvil ya está terminado y probado: la app se usa en el teléfono sin tocar el
diseño de escritorio. Pero ha llevado más días de lo previsto y te cuento el motivo, porque el
retraso no ha sido el responsive en sí.

**Qué ha pasado**

Al probarlo en un móvil de verdad, y no en el navegador del ordenador, aparecieron fallos que
desde el escritorio no se ven. Uno llegó a dejar la app rota:

- **La app se quedaba en negro.** Al desplegar una versión nueva, el móvil seguía usando el
  `index.html` guardado en caché, que apuntaba a ficheros que ya no existían. Se ha corregido:
  caché nueva y el `index.html` se pide siempre a la red. Ahora un despliegue se ve al momento,
  sin pedir a nadie que borre datos del navegador.
- **Nombres de fichero largos.** Las fotos del móvil traen nombres de 30 caracteres sin
  espacios y desbordaban las tarjetas, arrastrando toda la página en horizontal. Corregido en
  tablas, tarjetas y títulos.
- **En iPhone**, al tocar un campo, Safari ampliaba la página y se quedaba el zoom hecho.
  Corregido subiendo el tamaño de letra de los campos a 16 px solo en móvil.

**Cambios añadidos por el camino**

Aprovechando las pruebas en el móvil se han metido tres cosas pequeñas que viste al abrirlo:

- **Tema claro y oscuro**, con la preferencia guardada. Lo que se veía en negro era el modo
  oscuro forzado del navegador Opera, no la app; ahora hay un botón en el menú lateral y en la
  pantalla de acceso.
- **Botón para ver la contraseña** al entrar, al registrarse y al restablecerla.
- **Guardar contraseña**: los campos ahora llevan nombre y el navegador ofrece guardarla, cosa
  que antes no hacía.

**Estado**

- 32 pruebas automáticas en verde, revisión de código y compilación correctas.
- Revisado en pantallas de 320 px y 390 px (móvil pequeño y habitual), en tema claro y oscuro,
  y en escritorio para confirmar que no cambia nada.
- Desplegado en el entorno de pruebas: se puede abrir desde el móvil y usar con normalidad.

Por mi parte no queda nada pendiente; solo tu prueba en el móvil. Si ves algo raro, dímelo y lo
ajusto el mismo día.

Un saludo,
Víctor
