# Chefstool — Informe de situación y propuesta

**Para:** Bernart · **De:** Victor · **Fecha:** 22 de octubre de 2026

---

## 1. Resumen en un minuto

Chefstool es la aplicación web para **controlar compras, precios y costes de las recetas** en cocina profesional. Está **terminada y funcionando** en su primera versión completa: se sube (o se fotografía) un albarán, la aplicación lee los datos, los revisa con nosotros y a partir de ahí calcula el coste real de cada plato, avisa cuando un proveedor sube precios y muestra el gasto por proveedor y por mes.

**Estado:** funciona de punta a punta en un ordenador, con base de datos real (Supabase), y está preparada para publicarse. **Todavía no está en internet**: falta decidir dónde alojarla y contratar dos servicios (correo y lectura de albaranes).

**Lo que te pido hoy:** tu visto bueno para **(A)** publicar una demo privada esta semana y **(B)** elegir cómo resolvemos la lectura de albaranes y el envío de correos.

---

## 2. El problema que resuelve

En una cocina profesional el coste de un plato cambia cada semana porque cambian los precios de compra. Hoy eso se controla con albaranes en papel, hojas de cálculo y memoria:

- **No se sabe el coste real de un plato** hasta que alguien se sienta a recalcular la escandallo a mano.
- **Las subidas de precio pasan desapercibidas** hasta que se mira la factura del mes.
- **No hay comparación entre proveedores**: no sabemos si el mismo producto nos cuesta más en un sitio que en otro.
- **Cada cambio de precio** obliga a rehacer números y se pierde el histórico.

Chefstool convierte el albarán en datos una sola vez y, desde ahí, todo se calcula solo.

---

## 3. Qué hace hoy

| Módulo | Qué resuelve | Estado |
| --- | --- | --- |
| **Albaranes** | Foto o PDF → lectura automática → pantalla de revisión → compra confirmada | Terminado |
| **Compras** | Histórico con filtros por proveedor y fechas, detalle línea a línea, IVA y totales | Terminado |
| **Productos** | Catálogo normalizado con histórico de precios y comparación entre proveedores | Terminado |
| **Proveedores, clientes y eventos** | Imputar compras y costes a cada servicio o cliente | Terminado |
| **Escandallos (fichas técnicas)** | Coste por ración calculado con el **mejor precio disponible** y margen sobre el precio de venta | Terminado |
| **Alertas de precio** | Aviso automático cuando un proveedor sube un producto ≥ 5 % | Terminado |
| **Panel e informes** | Gasto del mes, gasto por proveedor, variaciones y **ahorro potencial** | Terminado |
| **Informe por proveedor** | Compras, gasto total, ticket medio, peso sobre el gasto y variación de precios | Terminado |
| **Usuarios y permisos** | Organización, invitaciones por correo, tres roles (propietario, admin, miembro) | Terminado |
| **Tablet y móvil** | Instalable como aplicación (PWA), pensada para usarse en cocina | Terminado |
| **Exportar a Excel / CSV** | Cualquier listado, con los filtros aplicados, listo para abrir en Excel | Terminado |

**Un ejemplo real de uso** (así se lo explicaría a un jefe de cocina):

1. El repartidor deja el albarán → se hace una foto desde el móvil o la tablet.
2. La aplicación lee proveedor, fecha, número y las líneas con cantidades y precios.
3. En la pantalla de revisión se corrige lo que haga falta (2 clics) y se confirma.
4. Automáticamente: se actualizan los precios del catálogo, se recalculan los escandallos, salta una alerta si algo ha subido, y el gasto aparece en el panel.

---

## 4. Cómo está construido (en lenguaje llano)

```
   TABLET / MÓVIL / ORDENADOR            SERVIDOR                        DATOS
   ─────────────────────────             ────────                        ─────
   Chefstool (React)  ──────────►  API Chefstool (Java)  ──────►  Base de datos
   · pantallas                     · reglas del negocio           (PostgreSQL
   · instalable como app           · seguridad y permisos          en Supabase)
                                   · lectura de albaranes
                                          │
                                          └──►  Almacén de archivos
                                                (los albaranes originales)
```

- **Tecnología actual y con soporte a largo plazo**: Java 25 (última versión LTS), Spring Boot 4, React 19. No son tecnologías "de moda": son las que usan los bancos y las grandes empresas, y tienen soporte y programadores en el mercado.
- **Los datos son nuestros**: la base de datos es PostgreSQL alojada en Supabase (servidores en la UE), no una caja negra.
- **Seguridad**: cada usuario entra con su contraseña cifrada y un token; cada cocina solo puede ver sus propios datos; hay tres roles y límite de intentos de acceso; los albaranes se guardan en un almacén privado.
- **Estructura del código**: separada en capas (pantallas / lógica del negocio / acceso a datos). Esto significa que **cambiar de proveedor de OCR, de correo o de alojamiento no obliga a rehacer la aplicación**: se sustituye una pieza por otra.

---

## 5. Calidad: cómo sabemos que funciona

No es una maqueta. Tiene pruebas automáticas y se ha usado de forma realista:

| Comprobación | Resultado |
| --- | --- |
| Pruebas automáticas del servidor | **88 pruebas** en verde (reglas de negocio, seguridad, permisos, lectura de albaranes, informes…) |
| Pruebas automáticas de la interfaz | **25 pruebas** en verde (paginación, búsqueda, ordenación, exportación, acceso) |
| Prueba de uso completa en navegador | **35 comprobaciones** (alta de datos, subida de albarán, errores, vista tablet, caducidad de sesión) |
| Prueba con volumen | **120 compras** creadas: los listados responden en 0,2–0,3 s y el panel en 0,5 s |
| Albarán real | Se subió un PDF de albarán y se leyeron 3 líneas, 47,08 € y 65 % de confianza |
| Copia de seguridad del código | Repositorio privado en GitHub + comprobación automática en cada cambio |

**Lo que NO prometemos todavía:** la lectura automática de albaranes es buena con PDFs y con formatos habituales, pero cada proveedor tiene su maqueta. Antes de generalizar hace falta probarla con los albaranes reales de la cocina piloto y ajustar. Es un trabajo de días, no de meses.

---

## 6. Lo que falta y las decisiones que necesito

### Decisión 1 — Lectura de albaranes (OCR)

Hoy la aplicación lee **PDFs con texto** ella sola y, para **fotos**, usa un motor gratuito instalado en el servidor. Funciona, pero con fotos torcidas o albaranes escritos a mano la precisión baja.

| Opción | Ventaja | Inconveniente | Coste orientativo |
| --- | --- | --- | --- |
| **A. Motor propio en el servidor** (lo que hay) | Coste 0, datos en casa | Precisión media en fotos | 0 € |
| **B. Servicio externo especializado** (Mindee, Google Vision) | Muy buena precisión, aprende de documentos | Los albaranes salen de nuestra infraestructura | ~0,01–0,10 € por documento (a confirmar) |
| **C. Mixto** (empezar con A, activar B cuando falle) | Lo mejor de ambos | Un poco más de trabajo | Solo se paga lo que se usa |

**Mi recomendación: C.** Empezamos gratis y, si en la cocina piloto las fotos fallan más de lo razonable, se activa el servicio externo sin tocar el resto de la aplicación (ya está preparada para ello).

### Decisión 2 — Correo electrónico

Las invitaciones y la recuperación de contraseña **ya funcionan**, pero sin enviar correo: el enlace se genera y hoy aparece en pantalla (modo demostración) o en el registro del servidor. Para uso real hace falta un proveedor de correo transaccional.

- Opciones: Resend, SendGrid, Amazon SES. Todas tienen plan gratuito o muy barato al principio (cientos/miles de correos al mes).
- **Coste orientativo: 0–15 €/mes.** Hay que contratarlo con el dominio de la empresa para que los correos no caigan en spam.

**Mi recomendación:** contratarlo antes del piloto. Es barato y sin él la app parece inacabada al invitar a alguien.

### Decisión 3 — Dónde se aloja

| Opción | Ventaja | Coste orientativo |
| --- | --- | --- |
| **Render** (servidor + web) | Sencillo, se conecta a GitHub, plan gratuito para probar | 0 € demo · 7–25 €/mes en uso real |
| **Azure** (lo más probable si la empresa ya lo usa) | Facturación y soporte de siempre, cumplimiento | 30–60 €/mes (a confirmar) |
| **Fly.io** | Muy barato y rápido en Europa | 5–15 €/mes |
| Base de datos | Supabase | 0 € (plan gratuito) · 25 $/mes plan Pro con copias |

**Mi recomendación:** empezar en **Render** con Supabase (coste casi 0) para la demo y el piloto; si la empresa prefiere Azure por facturación, se migra sin tocar código (está en un contenedor estándar).

### Decisión 4 — Protección de datos (RGPD)

La aplicación guarda nombres de proveedores, precios y usuarios. Antes de usarla con datos reales de la empresa hay que:

- Publicar un **aviso de privacidad** y una política de conservación.
- Definir **quién es el responsable** del tratamiento (la empresa) y quién el encargado (nosotros).
- Poder **borrar una cuenta y sus datos** cuando se pida (hoy hay borrado lógico de datos de negocio; falta el borrado de cuenta).

**Coste:** interno si lo asume alguien de la empresa; si se quiere revisar con un asesor, es un gasto puntual.

---

## 7. Coste estimado de funcionamiento

Estimación para **una cocina con 2–5 usuarios y unos 300 albaranes al mes** (precios a confirmar con cada proveedor):

| Concepto | Mensual |
| --- | --- |
| Servidor de la aplicación | 7–25 € |
| Base de datos (Supabase) | 0–25 € |
| Correo transaccional | 0–15 € |
| Lectura externa de albaranes (si se activa, 300 docs) | 3–30 € |
| Dominio y certificados | ~1 € |
| **Total** | **≈ 11–96 €/mes** |

Para comparar: **una sola subida de precio no detectada del 5 % en una compra mensual de 3.000 € son 150 €/mes**. La aplicación se paga sola con detectar una.

---

## 8. Plan de puesta en marcha

| Fase | Qué incluye | Cuándo |
| --- | --- | --- |
| **1. Demo privada** | Publicada en internet con datos de ejemplo, acceso para 3–4 personas | Esta semana |
| **2. Piloto en una cocina** | Datos reales de un proveedor, ajustar la lectura a sus albaranes, correo contratado, copias de seguridad | 2–3 semanas después de la demo |
| **3. Puesta en producción** | Aviso de privacidad, borrado de cuenta, copias verificadas, seguimiento de errores, formación al equipo | Tras el piloto |

---

## 9. Riesgos y cómo los controlamos

| Riesgo | Cómo lo controlamos |
| --- | --- |
| La lectura de albaranes falla con algún proveedor | Pantalla de revisión manual siempre disponible: nunca se inventan datos; se puede cambiar de motor sin tocar la app |
| Se pierden los archivos subidos | Almacenamiento en Supabase (no en el disco del servidor); verificado |
| Alguien accede a datos de otra cocina | Cada consulta filtra por organización; probado con usuarios de distintas cocinas |
| Se cae el servidor | Contenedor estándar: se puede levantar en otro proveedor en horas |
| Subidas de precio no detectadas | Alerta automática ≥ 5 % y comparación entre proveedores |
| Dependencia de una sola persona | Código documentado, en repositorio con historial y con pruebas automáticas |

---

## 10. Mi recomendación

**Sí, publicar — pero en dos pasos.**

1. **Esta semana: demo privada.** Publicar la aplicación en internet con datos de ejemplo y acceso para ti y 2–3 personas de confianza. Coste: prácticamente 0 €. Sirve para que la veas funcionando, para enseñarla y para decidir el resto con algo delante, no con una presentación.
2. **Después: piloto real con una cocina** (2–3 semanas), con correo contratado y la lectura ajustada a sus albaranes. Es ahí donde se demuestra el ahorro.

**No recomiendo** meter datos reales de toda la empresa hasta cerrar los tres puntos pendientes (correo, protección de datos y copias de seguridad verificadas). No es por falta de la aplicación, es por prudencia: con datos de compras reales, un fallo de privacidad o una copia que no restaura cuestan más que la app entera.

---

## 11. Qué necesito de ti (concreto)

1. **OK para la demo privada** — y si quieres, una lista de 3 personas que la prueben.
2. **Decisión sobre OCR**: empezar gratis y activar el servicio externo solo si hace falta (mi recomendación).
3. **Correo**: ¿contratamos ya un proveedor (0–15 €/mes) o lo dejamos para el piloto?
4. **Alojamiento**: ¿Render para empezar, o Azure porque la empresa ya lo usa?
5. **Una cocina piloto** con su jefe de cocina, y **sus albaranes reales** (5–10 de cada proveedor) para ajustar la lectura.

---

## Anexo — Cómo verlo en 5 minutos

1. Abrir la aplicación y entrar con el usuario de demostración.
2. **Panel**: gasto del mes, ahorro potencial detectado y alertas.
3. **Documentos → Añadir documento → Probar con un ejemplo**: se ve la lectura, la revisión y la confirmación.
4. **Fichas técnicas**: abrir un plato y ver el coste por ración calculado con el mejor precio.
5. **Informes**: coste por proveedor y variación de precios; botón **Excel** para descargar.
6. **Usuarios**: invitar a alguien y ver los permisos por rol.

---

*Documento preparado el 22/10/2026. Los importes son estimaciones orientativas pendientes de confirmar con cada proveedor.*
