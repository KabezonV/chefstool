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
