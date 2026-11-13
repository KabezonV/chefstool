# Guion de la presentación (videollamada, 11:00)

Chuleta para tener delante durante la llamada. Montaje: **navegador del Mac** y el albarán
de ejemplo subido como archivo. El móvil queda para el final y solo si está probado.

## Antes de entrar (15 minutos)

```bash
./scripts/preparar-demo.sh          # levanta, espera a la API y valida el login
open -a "Google Chrome" http://localhost:8081
```

1. **Entra** con `chef@demo.es` / `chefstool2026` (la sesión va por navegador).
2. Abre una **ventana nueva** de Chrome (`Cmd+N`) solo con la app y cierra la otra. En una
   pantalla compartida se ve absolutamente todo.
3. `Cmd+Shift+B` para ocultar marcadores y **activa No molestar**.
4. Comprueba que el archivo **`albaran-ejemplo.png` está en el Escritorio**.
5. Deja abierta esta chuleta en el móvil o en otra pantalla, no en la ventana que compartes.

## Guion (12 minutos)

| Min | Pantalla | Pulsas | Dices |
| --- | --- | --- | --- |
| 0–1 | Panel | — | "Esto es lo que ve el jefe de cocina al entrar: gasto del mes, ahorro potencial y avisos de subida de precio." |
| 1–4 | **Documentos → Añadir documento → Elegir archivo** | `albaran-ejemplo.png` | "Un albarán entra por foto o por PDF. La aplicación lo lee y **no guarda nada sin que yo lo revise**." |
| | Revisión | Señala confianza y líneas | "Aquí están las 6 líneas con cantidad, precio e importe, y el proveedor. Si algo no cuadra, se corrige aquí." |
| | | **Confirmar y guardar** | "Y ahora sí: se crea la compra y se actualizan los precios." |
| 4–6 | Compras | Abre la compra recién creada | "Con su IVA y sus líneas. Se filtra por proveedor o fechas y se descarga a Excel." |
| 6–8 | Productos → uno con varios proveedores | Histórico de precios | "El mismo producto en tres proveedores, con su evolución. **Y avisa cuando uno sube un 5 %.**" |
| 8–10 | Fichas técnicas | Una ficha | "El coste por ración se calcula solo con el mejor precio de cada ingrediente, y el margen sobre el precio de venta." |
| 10–11 | Informes | Panel + Excel | "Coste por proveedor, peso de cada uno y cuánto han subido. Esto es lo que antes era una hoja de cálculo." |
| 11–12 | Móvil (solo si está probado) | — | "Y todo esto desde el teléfono, en cocina." |

Cierre: *"Lo que falta es decidir el alojamiento y ajustar la lectura a vuestros proveedores."*

## Cómo hablar de los albaranes (importante)

No prometas precisión universal. Enseña el ejemplo y **pasa la pelota**:

> "La lectura ya funciona con albaranes reales: reconoce el proveedor, lo da de alta si no
> existe y entiende las filas con código, lote y % de IVA. Lo que necesito es que **lo probéis
> con 5 albaranes de vuestros proveedores** y me paséis los que fallen. Con esos afino las
> reglas y en una semana lo dejamos fino."

Si preguntan por qué no va al 100 % con una foto cualquiera:

> "Depende de la calidad de la foto. Con el PDF o con la foto original del móvil va mucho mejor
> que con una imagen reenviada por WhatsApp, que llega comprimida. La app avisa cuando la
> imagen no da para leerla."

## Preguntas previsibles

| Pregunta | Respuesta |
| --- | --- |
| ¿Cuánto cuesta tenerlo funcionando? | Servidor pequeño 6 €/mes; en AWS 27–50 €/mes. Está en `INFORME-AWS-DIMENSIONAMIENTO.md`. |
| ¿Quién puede ver qué? | Propietario, administrador y miembro. El miembro puede crear y editar productos, pero no borrar. |
| ¿Se puede importar lo que ya tengo en Excel? | Sí, importación de productos por CSV. Está hecho y probado. |
| ¿Y si se cae internet? | En local no depende de internet. En producción, el servidor no se cae por vuestra red; si se va la red del local, la app deja de responder hasta que vuelva. |
| ¿Los datos son nuestros? | Sí. Base de datos propia, sin servicios de terceros en el camino de ejecución. |
| ¿Cuándo estará en producción? | Depende de la decisión del servidor y el dominio. Con eso, 1–2 días. |
| ¿Y el correo de invitaciones? | Funciona la generación del enlace; el envío por correo requiere contratar un proveedor. |
| Pregunta que no sabes | "Te lo confirmo por escrito esta tarde." Nunca improvises cifras. |

## Qué no enseñar

- Fotos de albaranes reales sin probar antes: usa **solo** el archivo del Escritorio.
- La pantalla de recuperación de contraseña (el enlace aparece en pantalla, es modo demo).
- La consola del navegador.
- El panel con documentos pendientes: está limpio a propósito, no lo ensucies antes de entrar.

## Si algo falla

1. **No carga** → recarga. Si sigue, abre una ventana nueva.
2. **El albarán no lee** → no insistas delante del cliente: "esta imagen no es buena, con el PDF
   va perfecto", y sigue con Compras usando la compra que ya existe.
3. **Se cae Docker** → `docker compose up -d`, espera 30 s. Si no vuelve, sigue con capturas.

## Después

- Descartar el documento de la demo si se quedó a medias.
- Anotar las preguntas y los albaranes que prometió enviar.
