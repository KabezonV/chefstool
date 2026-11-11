# Entrega del 11/11/2026 — mensajes y orden de trabajo

Material de apoyo de la entrega: el texto del WhatsApp, la orden de trabajo y los datos
para justificarla. El email va aparte, en [EMAIL-ENTREGA-MOVIL.md](EMAIL-ENTREGA-MOVIL.md).

- **Referencia entregada:** tag `entrega-2026-11-11` del repositorio
  <https://github.com/KabezonV/chefstool>
- **ZIP:** <https://github.com/KabezonV/chefstool/archive/refs/tags/entrega-2026-11-11.zip>
- **Alcance:** adaptación a móvil + corrección de la lectura de albaranes.

---

## Mensaje de WhatsApp

> Hola Bernart 👋 Ya está la entrega: versión móvil + arreglo de la lectura de albaranes (lo
> que se estaba resistiendo).
>
> ZIP: https://github.com/KabezonV/chefstool/archive/refs/tags/entrega-2026-11-11.zip
>
> Se levanta con Docker en 3 comandos, están en el README. Si puedes, escanea un par de
> albaranes con la cámara del móvil (foto original, no reenviada) y me dices qué tal va.
>
> El detalle completo te lo he mandado por email.

Versión de una sola línea:

> Hola Bernart, ya está la entrega (móvil + lectura de albaranes):
> https://github.com/KabezonV/chefstool/archive/refs/tags/entrega-2026-11-11.zip — se levanta
> con Docker en 3 comandos. Pruébalo y me dices. El detalle, por email.

---

## Orden de trabajo

### Cabecera

| Campo | Contenido |
| --- | --- |
| Cliente | Bernart |
| Proyecto | Chefstool — control de compras y costes para cocinas profesionales |
| Referencia | Tag `entrega-2026-11-11` · repo `KabezonV/chefstool` |
| Fecha de entrega | 11/11/2026 |
| Alcance | Adaptación a móvil + corrección de la lectura de albaranes |

### Trabajos realizados

| # | Concepto | Descripción | Evidencia |
| --- | --- | --- | --- |
| 1 | Adaptación a móvil | Tablas convertidas en fichas, menú deslizante, formularios usables con el pulgar | Revisado a 320 y 390 px |
| 2 | Tema claro / oscuro | Selector propio con preferencia guardada (antes lo forzaba el navegador) | Botón visible en la app |
| 3 | Contraseñas | Ver contraseña y ofrecer guardarla al navegador | Login, registro y restablecer |
| 4 | Caché del service worker | La app se quedaba en negro tras desplegar por un `index.html` antiguo | `sw.js` reescrito |
| 5 | Nombres de fichero largos | Las fotos del móvil desbordaban las tarjetas y rompían la página | Corregido en listados |
| 6 | Zoom automático iOS | Safari ampliaba al enfocar un campo y no volvía | Corregido |
| 7 | Plantilla de entorno | `.env.ejemplo` apuntaba a un servicio retirado | Arranque desde cero verificado |
| 8 | Lectura de albaranes | Segmentación por bloques, ampliado de imágenes pequeñas, filas con código/lote/%IVA, totales excluidos | Commits `df3914c`, `c53558a`, `e35e68a`, `fca750f` |
| 9 | Proveedor automático | Alta del proveedor leído si no existe, sin duplicar, con corrección manual en revisión | Commits `b18019a`, `b8f10d0` |
| 10 | Aviso de calidad de imagen | Aviso antes de procesar fotos de menos de 1200 px | Pantalla de subida |
| 11 | Pruebas | 165 automáticas (133 backend + 32 frontend) + 2 albaranes de ejemplo | `docs/ejemplos/`, `docs/QA.md` |
| 12 | Empaquetado y entrega | Tag, ZIP, repositorio público, email y manual actualizados | `entrega-2026-11-11` |

### Incidencias que justifican la desviación

| Incidencia | Impacto |
| --- | --- |
| Caché del service worker | La app no arrancaba en móvil; obligó a rehacer el mecanismo y reprobar todo |
| Lectura de albaranes con fotos reales | Tres causas simultáneas (columnas partidas, compresión de WhatsApp, filas con columnas de más). Requirió cambios en el lector y en el parser, y repetir pruebas con 31 albaranes reales |
| Cada corrección invalidaba pruebas anteriores | Reproceso completo tras cada arreglo |

### Horas

| Concepto | Horas |
| --- | --- |
| Adaptación a móvil y correcciones asociadas | *(a completar)* |
| Lectura de albaranes (análisis, cambios y pruebas) | *(a completar)* |
| Pruebas y empaquetado | *(a completar)* |
| **Total** | *(a completar)* |

### No incluido / pendiente de decisión del cliente

- Servidor, dominio y copias de seguridad (bloqueante para datos reales).
- Precisión del OCR con albaranes de cada proveedor: requiere 5 albaranes reales para afinar
  las reglas.
- Envío de correo transaccional (hoy el enlace de recuperación se muestra en pantalla).

### Dos consejos

- No poner horas que no se puedan justificar: el repositorio tiene la fecha de cada commit.
- Enlazar el **tag**, no la rama `main`: así lo facturado coincide con lo entregado.
