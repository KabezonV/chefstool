# Demostración a un cliente (en local)

Guion, preparación y plan B para enseñar Chefstool desde tu ordenador en unos
10 minutos.

## Antes de la reunión (5 minutos)

1. **Arranca los dos servidores**:

   ```bash
   cd chefstool-backend && ./mvnw spring-boot:run       # API en :8080
   cd chefstool-frontend && npm run dev -- --port 5180 --strictPort
   ```

   O todo junto con Docker: `cp .env.ejemplo .env` (rellenar), `docker compose up --build`
   y abrir <http://localhost:8081>.

2. **Calienta la aplicación**: inicia sesión una vez y entra en Panel e Informes. La
   primera petición tras un rato parado tarda unos segundos (la conexión con la base de
   datos se abre al vuelo). Si el cliente ve esa espera, parece que va lento.

3. **No depende de internet**: con `docker compose up` la base de datos es local, así
   que la demo funciona aunque el wifi del cliente falle. (Si arrancas el backend contra
   una base remota, entonces sí hace falta red.)

4. **Comprueba el OCR**: `tesseract --version`. Si no está instalado, evita el paso de
   "hacer una foto"; el resto de la demo funciona igual con el albarán de ejemplo.

5. **Credenciales de demostración**: `chef@demo.es` / `chefstool2026`
   (organización *Cocina Norte*, con 9 productos, 3 proveedores, 12 compras, 5 fichas
   técnicas, 4 alertas de precio y un documento pendiente de revisar).

## Guion de 10 minutos

| Minuto | Qué enseñar | Qué decir |
| --- | --- | --- |
| 0–1 | **Panel** | "Esto es lo que ve el jefe de cocina al entrar: gasto del mes, ahorro potencial detectado y avisos de subidas de precio." |
| 1–3 | **Documentos → Añadir documento → Probar con un ejemplo** | "Un albarán entra por foto o PDF, la aplicación lo lee y nos pide revisión. Nada se guarda sin confirmar." Confirmar y volver al panel. |
| 3–5 | **Compras** | "Aquí está la compra recién creada, con IVA y líneas. Se puede filtrar por proveedor o fechas, y descargar a Excel." |
| 5–7 | **Productos → un producto → histórico de precios** | "El mismo producto en tres proveedores, con su evolución. Esto es lo que antes estaba en una hoja de cálculo." |
| 7–9 | **Fichas técnicas → Pollo al ajillo** | "El coste por ración se calcula solo con el mejor precio de cada ingrediente, y el margen sobre el precio de venta." |
| 9–10 | **Informes** | "Coste por proveedor, peso de cada uno y cuánto han subido sus precios. Y el botón Excel." |

Cierre sugerido: "Lo que falta es ajustar la lectura a los albaranes de tus proveedores
y decidir si contratamos el servicio de correo y el de lectura avanzada".

## Lo que NO conviene enseñar (o hay que explicar antes)

| Situación | Por qué | Qué hacer |
| --- | --- | --- |
| Foto de un albarán real cualquiera | El lector es heurístico: con maquetas raras puede leer mal | Usar el albarán de ejemplo o un PDF ya probado |
| "¿Y el correo de invitación?" | No hay proveedor de correo contratado | "El enlace se genera; el envío por correo es una de las decisiones pendientes" |
| Recuperación de contraseña | El enlace aparece en pantalla (modo demostración) | Explicarlo como modo demo o no entrar en esa pantalla |
| Registro con el correo del cliente | Crea una organización real en la misma base de datos | Usar el usuario demo, o crear una cuenta nueva solo si quieres enseñar el registro |
| Consola del navegador | Hay avisos normales de desarrollo | No abrirla |

## Plan B si algo falla

1. **No carga o va lento** → refrescar. Si sigue, abrir <http://localhost:5180> en otra
   pestaña (a veces el proxy se atasca tras suspender el portátil). Si nada, reiniciar
   el backend y esperar 20 segundos.
2. **Error de conexión con la base de datos** → comprueba que `chefstool-db` está
   *healthy* (`docker compose ps`); si acabas de arrancar, espera 20 segundos.
3. **El albarán de ejemplo no lee nada** → usar el documento que ya está en
   *Documentos* con estado "pendiente de revisión" y continuar desde ahí.
4. **Todo se cae** → tener a mano el vídeo o capturas del flujo, y seguir con el
   [informe para dirección](INFORME-BERNART.md).

## Después de la reunión

- Borrar los datos de prueba que se hayan creado (documentos pendientes, cuentas).
- Anotar las preguntas del cliente: casi siempre van sobre precios, importación desde
  Excel y quién puede ver qué.

---

# Demostración por videollamada

El guion de arriba sirve igual; aquí está lo que cambia cuando no estás delante del cliente.

## La noche antes

1. `docker compose up -d` y comprobar que todo está arriba: `docker compose ps` debe dar
   `chefstool-api` en *healthy* y `chefstool-web` en *Up*.
2. **Grabar un vídeo de respaldo** del flujo completo (QuickTime → *Archivo* → *Nueva
   grabación de pantalla*, 3-4 minutos). Si mañana falla algo, se comparte ese vídeo y la
   reunión no se cae.
3. Descargar el ZIP del tag `entrega-2026-11-11` y comprobar que arranca, para poder decir con
   seguridad que "lo que has descargado funciona".

## 30 minutos antes de la llamada

1. Si el portátil ha estado suspendido, **reiniciar Docker Desktop**: es la causa número uno
   de que la app no cargue en el momento clave.
2. `docker compose up -d` y **calentar la app**: entrar, visitar Panel e Informes. La primera
   petición tras un rato parado tarda unos segundos y parece que va lenta.
3. **Abrir una ventana nueva de Chrome solo con la app** (`Cmd+N`) y cerrar la ventana donde
   estén las demás pestañas. En una pantalla compartida se ve todo, incluido lo que no quieres
   mostrar.
4. Ocultar la barra de marcadores (`Cmd+Shift+B`), zoom al 100 % y **activar No molestar**
   (notificaciones de Slack, correo o WhatsApp aparecen en pantalla y en la grabación).
5. Probar **compartir pantalla y micrófono** con tiempo, no en el primer minuto de la reunión.

## Durante la llamada

- Compartir **la ventana de Chrome**, no la pantalla entera: se ve más grande y no expones el
  escritorio.
- Si al cliente le llega borroso, bajar la resolución de pantalla (por ejemplo a 1440 × 900) y
  subir el zoom del navegador al 110 %.
- Hablar despacio y **dejar 2 segundos entre pantalla y explicación**: por videollamada el
  vídeo llega con retardo y el cliente ve el cambio después de que lo hayas contado.

## Enseñar el móvil sin depender del móvil

En Chrome: `Cmd+Shift+M` (modo dispositivo) → elegir *iPhone 14 Pro* o *Pixel 7* → 390 × 844.
Se ve exactamente la app como en el teléfono y no depende de cables ni de WiFi.

> La cámara no funciona en ese modo. Para la parte de "escanear un albarán", usar **Elegir
> archivo** con `docs/ejemplos/albaran-ejemplo-limpio.png`, que saca las 6 líneas exactas.

Si quieres enseñar el teléfono de verdad, graba antes un clip de 30 segundos con el móvil
haciendo la foto y reprodúcelo: es más fiable que apuntar la webcam a la pantalla.

## Guion recomendado (10-12 minutos)

| Minuto | Qué enseñar | Idea que tiene que quedar |
| --- | --- | --- |
| 0–1 | Panel | "Esto es lo que ve el jefe de cocina al entrar" |
| 1–4 | **Albarán**: subir el de ejemplo → revisión → confirmar | *Momento clave*: nada se guarda sin revisar, y el proveedor se da de alta solo |
| 4–6 | Compras | La compra ya está dentro, con IVA y líneas |
| 6–8 | Producto con histórico de precios | *Momento clave*: el mismo producto en tres proveedores y el aviso de subida |
| 8–10 | Ficha técnica | El coste por ración se calcula solo |
| 10–11 | Informes + Excel | Lo que antes era una hoja de cálculo |
| 11–12 | Móvil en modo dispositivo | "Y todo esto desde el teléfono, en cocina" |

Cierre: "Lo que falta es ajustar la lectura a los albaranes de tus proveedores y decidir el
alojamiento".

## Si algo falla en directo

1. **Un albarán no se lee** → decir la verdad ("esta foto viene comprimida de WhatsApp; con la
   original o el PDF va mucho mejor") y cambiar al de ejemplo. No insistir delante del cliente.
2. **La app no carga** → refrescar; si sigue, abrir la ventana nueva. Si tampoco, reiniciar el
   backend y esperar 20 segundos.
3. **Se cae Docker** → cambiar al vídeo grabado la noche anterior y seguir la conversación.
4. **Pregunta para la que no hay respuesta** → "te lo confirmo por escrito esta tarde". Nunca
   improvisar cifras.
