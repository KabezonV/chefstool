# Anexo técnico: comparativa de costes de IA y servidor

**Para:** Bernart · **Fecha:** 26/10/2026 · **Estado:** anexo del documento de decisión

> Resumen para decidir: **[COMPARATIVA-COSTES-IA.md](COMPARATIVA-COSTES-IA.md)**.
> Este anexo tiene el detalle: precios por producto, capas gratuitas, puntos de cruce por
> número de líneas, calidad medida, RGPD por proveedor y comparativa de servidores.

---

## 1. Resumen en un minuto

1. **Para una cocina (≈300 albaranes/mes) la lectura con IA cuesta entre 0 € y 3 € al mes en
   todas las opciones.** La diferencia de precio no es el criterio para decidir: lo son la
   calidad de lectura y el cumplimiento (RGPD).
2. **La opción más barata de las comerciales es un modelo de visión (Gemini Flash-Lite),
   ~0,0004 € por albarán.** Las APIs "clásicas" de documentos (AWS Textract, Azure, Google
   Document AI) cobran entre 3 y 250 veces más por el mismo trabajo.
3. **AWS y Azure solo salen a cuenta si cambia el tipo de documento**: a partir de unas
   **10 líneas por albarán** el OCR simple de Textract ya empata con Gemini Flash, y a partir
   de **~107 líneas** las facturas "prebuilt" de AWS/Azure salen más baratas que Google Gemini Flash. Un albarán de
   cocina típico tiene 15–25 líneas, así que estamos justo en la frontera con AWS pero
   claramente por debajo con Flash-Lite.
4. **Lo gratis también existe y ya funciona**: Tesseract en nuestro servidor (0 €) para PDF
   digitales y escaneos limpios. Su punto débil son las fotos torcidas o con poca luz.
5. **Irse fuera de AWS/OpenAI sí merece la pena**, no por el dinero (que es calderilla) sino
   porque Google y Mistral dan mejor lectura de albaranes en español a menor precio y con
   datos en la UE. La única razón para quedarse en AWS es tener ya contrato y facturación
   con ellos.

**Recomendación:** Gemini 2.5 Flash-Lite o Flash (según la calidad que midamos con albaranes
reales) para leer, Tesseract como red de seguridad cuando no haya conexión, y servidor propio
(Docker) en vez de AWS/Azure. Coste total estimado: **6–10 €/mes por cocina**, IA incluida.

---

## 2. Las dos decisiones que hay que tomar

| Decisión | Opciones | Coste/mes (1 cocina) | Quién decide |
| --- | --- | --- | --- |
| **Leer el albarán** | Tesseract local / Gemini / OpenAI / AWS Textract / Azure / Google Document AI / Mistral | 0 – 3 € | Bernart |
| **Dónde vive la app** | Servidor propio (Docker) / PaaS / AWS / Azure / Google Cloud | 4 – 60 € | Bernart |

El resto (base de datos, almacenamiento de fotos, copias de seguridad) va incluido en la
opción de servidor elegida.

---

## 3. Supuestos del cálculo

Para que los números sean comparables:

- **1 albarán = 1 página = 1 documento.** Una foto o un PDF de una hoja.
- **Volumen de referencia: 300 albaranes/mes** (≈14 al día en días laborables), y escenarios
  de 50, 1.000, 5.000 y 20.000 para ver cómo escala.
- **Albarán típico: 20 líneas de producto.**
- **Coste de los modelos de visión**: se cobra por *tokens*, no por página. Contamos
  560 tokens por imagen (dato publicado por Google), 400 de instrucciones y una respuesta de
  ~150 tokens + 35 por línea.
- **Tipo de cambio 1 USD = 0,92 EUR.** Precios **sin IVA** (en España, +21 %).
- Año de 12 meses, sin descuentos por compromiso ni por volumen.

> Los precios cambian. Todo lo marcado como *verificado* se ha consultado en la web del
> proveedor el **26/10/2026** (fuentes y enlaces en el apartado 14).

---

## 4. Precios verificados

### 4.1 APIs "clásicas" de documentos (cobran por página)

| Proveedor | Producto | Precio | Capa gratuita | Coste por albarán |
| --- | --- | --- | --- | --- |
| **AWS Textract** | *Detect Document Text* (solo texto) | 1,50 $ / 1.000 pág (0,60 $ a partir de 1 M pág/mes) | Solo 3 meses y solo cuentas nuevas (1.000 pág/mes) | 0,0015 $ |
| **AWS Textract** | *AnalyzeExpense* (facturas con líneas) | 10,00 $ / 1.000 pág | 100 pág/mes, 3 meses | 0,0100 $ |
| **AWS Textract** | *Tables* (tablas) | 15,00 $ / 1.000 pág | — | 0,0150 $ |
| **Azure Document Intelligence** | *Read* (texto) | 1,50 $ / 1.000 pág (0,60 $ a partir de 1 M) | F0: 500 pág/mes gratis (cuota de la capa gratuita) | 0,0015 $ |
| **Azure Document Intelligence** | *Prebuilt Invoice* (facturas) | 10,00 $ / 1.000 pág | — | 0,0100 $ |
| **Azure Document Intelligence** | *Custom extraction* | 30,00 $ / 1.000 pág | — | 0,0300 $ |
| **Google Document AI** | *Document OCR* | 1,50 $ / 1.000 pág (0,60 $ a partir de 5 M) | **1.000 pág/mes gratis, sin caducidad** | 0,0015 $ |
| **Google Document AI** | *Invoice Parser* | 0,10 $ / documento | — | 0,1000 $ |
| **Google Document AI** | *Expense Parser* | 0,10 $ / documento | — | 0,1000 $ |
| **Google Document AI** | *Form Parser* | 30,00 $ / 1.000 pág | — | 0,0300 $ |
| **Mistral** | *OCR* | 1,00 $ / 1.000 pág (0,50 $ en lote) ⚠️ | — | 0,0010 $ |

⚠️ Mistral no publica sus precios en un formato que se pueda leer automáticamente (su web los
carga con JavaScript). Se toma el precio de lista conocido: **1 $/1.000 páginas**. Confirmar al
contratar.

### 4.2 Modelos de visión (cobran por tokens)

| Proveedor | Modelo | Entrada / Salida (por 1 M tokens) | Coste por albarán | Capa gratuita |
| --- | --- | --- | --- | --- |
| **Google Gemini** | 2.5 Flash-Lite | 0,10 $ / 0,40 $ | **0,0004 $** | Sí (con límites de peticiones/minuto) |
| **Google Gemini** | 2.5 Flash | 0,30 $ / 2,50 $ | 0,0024 $ | Sí (con límites) |
| **Google Gemini** | 3 Flash (preview) | 0,50 $ / 3,00 $ | 0,0030 $ | No |
| **OpenAI** | gpt-6-luna | 0,10 $ / 0,50 $ | 0,0006 $ | No |
| **OpenAI** | gpt-6.1-sol | 2,00 $ / 10,00 $ | 0,0115 $ | No |
| **Local (nuestro servidor)** | Tesseract 5 | — | **0 $** (solo CPU) | — |

Los modelos "grandes" (Gemini 3.1 Pro a 2 $/12 $, gpt-6-astra a 10 $/50 $) cuestan 5–50 veces
más y **no aportan nada** para leer un albarán: con los pequeños ya sacamos 98 % de confianza.

---

## 5. Coste según el volumen (€/mes, sin IVA)

Volumen = albaranes al mes. Se aplican las capas gratuitas.

| Opción | 50 | 300 | 1.000 | 5.000 | 20.000 |
| --- | ---: | ---: | ---: | ---: | ---: |
| **Tesseract local** | 0,00 | 0,00 | 0,00 | 0,00 | 0,00 |
| **Gemini 2.5 Flash-Lite** | 0,02 | 0,12 | 0,40 | 2,01 | 8,02 |
| **OpenAI gpt-6-luna** | 0,03 | 0,16 | 0,53 | 2,65 | 10,58 |
| **Mistral OCR** | 0,05 | 0,28 | 0,92 | 4,60 | 18,40 |
| **AWS Textract (texto)** | 0,07 | 0,41 | 1,38 | 6,90 | 27,60 |
| **Azure Read** | 0,00 | 0,00 | 0,69 | 6,21 | 26,91 |
| **Google Document OCR** | 0,00 | 0,00 | 0,00 | 5,52 | 26,22 |
| **Gemini 2.5 Flash** | 0,11 | 0,67 | 2,22 | 11,10 | 44,40 |
| **Gemini 3 Flash** | 0,14 | 0,84 | 2,79 | 13,94 | 55,75 |
| **AWS Textract AnalyzeExpense** | 0,46 | 2,76 | 9,20 | 46,00 | 184,00 |
| **Azure Prebuilt Invoice** | 0,46 | 2,76 | 9,20 | 46,00 | 184,00 |
| **OpenAI gpt-6.1-sol** | 0,53 | 3,17 | 10,58 | 52,90 | 211,60 |
| **Google Invoice Parser** | 4,60 | 27,60 | 92,00 | 460,00 | 1.840,00 |

### Coste anual

| Opción | 1 cocina (300/mes) | 10 cocinas | 100 cocinas |
| --- | ---: | ---: | ---: |
| Tesseract local | 0 € | 0 € | 0 € |
| Gemini 2.5 Flash-Lite | **1,44 €** | 14,44 € | 144,40 € |
| OpenAI gpt-6-luna | 1,90 € | 19,04 € | 190,44 € |
| Mistral OCR | 3,31 € | 33,12 € | 331,20 € |
| AWS Textract (texto) | 4,97 € | 49,68 € | 496,80 € |
| Google Document OCR | **0 €** | 33,12 € | 480,24 € |
| Gemini 2.5 Flash | 7,99 € | 79,92 € | 799,19 € |
| AWS Textract AnalyzeExpense | 33,12 € | 331,20 € | 3.312,00 € |
| Azure Prebuilt Invoice | 33,12 € | 331,20 € | 3.312,00 € |
| Google Invoice Parser | 331,20 € | 3.312,00 € | 33.120,00 € |

**Lectura:** con una cocina, la opción más cara de todas (Google Invoice Parser) cuesta
27,60 €/mes; la más barata de las buenas, 0,12 €/mes. La diferencia anual entre la mejor y la
peor opción comercial son **~330 €**, y entre las opciones razonables **~7 €**. A 100 cocinas,
en cambio, elegir bien ahorra **más de 3.000 €/año**.

---

## 6. A partir de cuántos documentos cambia (la pregunta de Bernart)

Aquí están las **cuatro fronteras** que hay que conocer. Las tres primeras dependen del
volumen; la cuarta, del tamaño del documento.

### 6.1 Frontera 1 — Las capas gratuitas: 500 y 1.000 documentos/mes

| Volumen | Quién es gratis | Quién ya cobra |
| --- | --- | --- |
| **0 – 500 docs/mes** | Azure *Read* (F0), Google *Document OCR* | AWS (salvo 3 meses de prueba) |
| **500 – 1.000 docs/mes** | Google *Document OCR* | Azure, AWS, Gemini, OpenAI |
| **más de 1.000 docs/mes** | — | Todas |

Es decir: **por debajo de 1.000 albaranes al mes, Google Document AI es literalmente gratis**,
y por debajo de 500 también Azure. AWS nunca es gratis de verdad (sus 1.000 páginas/mes duran
3 meses y solo para cuentas nuevas).

### 6.2 Frontera 2 — Volumen: no hay ningún punto en que el modelo de visión deje de ser el más barato

El precio de los modelos de visión **no baja con el volumen** (no tienen tramos), mientras que
las APIs clásicas sí: a partir de **1 millón de páginas/mes** AWS y Azure bajan de 1,50 $ a
0,60 $ por 1.000 páginas, y Google a partir de 5 millones. Aun así no llegan a igualar a
Flash-Lite (0,0004 $/albarán), porque su suelo es 0,0006 $/página.

| Volumen | Gemini Flash-Lite | Lo más barato de las clásicas |
| --- | ---: | ---: |
| 300 docs/mes | **0,12 €** | Google Document OCR **0 €** |
| 1.000 docs/mes | **0,40 €** | Google Document OCR **0 €** |
| 20.000 docs/mes | **8,02 €** | Google Document OCR 26,22 € |
| 1 millón de docs/mes | **400,80 €** | AWS Textract 1.380,00 € |

**Conclusión:** no hay vuelta al volumen. Flash-Lite arranca por debajo del **precio más bajo**
que alcanzan las APIs clásicas con todos sus descuentos (0,0006 $/página frente a 0,0004 $ por
albarán), así que escalar no cambia el ganador. Las clásicas solo ganan por otra vía: con
documentos **muy largos** (ver frontera 4).

### 6.3 Frontera 3 — Presupuesto: cuántos albaranes entran con 20 €/mes

Si lo que se quiere es fijar un tope de gasto, esta tabla dice cuánto aguanta cada opción:

| Opción | Albaranes/mes con 20 € |
| --- | ---: |
| Tesseract local | sin límite (coste 0) |
| Gemini 2.5 Flash-Lite | **49.860** |
| OpenAI gpt-6-luna | 37.807 |
| Mistral OCR | 21.739 |
| AWS Textract / Azure Read / Google OCR | 14.492 |
| Gemini 2.5 Flash | 9.009 |
| AWS Textract AnalyzeExpense / Azure Prebuilt | 2.173 |
| Google Invoice Parser | **217** |

De 217 a 49.860 documentos con el mismo presupuesto: **la elección importa más que el volumen**.

### 6.4 Frontera 4 — Tamaño del documento: ~10, ~96 y ~107 líneas

Los modelos cobran por token y las APIs por página. Eso hace que, con documentos **muy
largos**, el modelo de visión se encarezca línea a línea y el clásico no. Los puntos de cruce
calculados (albarán de 1 página):

| Comparación | Empate a | Quién gana antes de ese punto |
| --- | ---: | --- |
| Gemini 2.5 Flash **vs** AWS Textract (solo texto) | **~10 líneas** | Gemini Flash |
| OpenAI gpt-6-luna **vs** AWS Textract (solo texto) | ~76 líneas | gpt-6-luna |
| Gemini 2.5 Flash-Lite **vs** AWS Textract (solo texto) | **~96 líneas** | Flash-Lite |
| Gemini 2.5 Flash **vs** AWS Textract AnalyzeExpense / Azure Invoice | **~107 líneas** | Gemini Flash |
| Gemini 2.5 Flash-Lite **vs** AWS AnalyzeExpense / Azure Invoice | ~703 líneas | Flash-Lite |
| Cualquier modelo **vs** Google Invoice Parser (0,10 $/doc) | >1.100 líneas | el modelo |

Nuestros albaranes de cocina tienen **15–25 líneas**, así que:

- contra el OCR simple de AWS estamos **en la frontera** (por eso conviene Flash-Lite, que
  aguanta hasta 96 líneas);
- contra las facturas "prebuilt" de AWS y Azure **ganamos siempre**: con Gemini Flash harían
  falta 107 líneas y con Flash-Lite, 703;
- contra el Invoice Parser de Google **no hay color** (0,10 $/documento es 40 veces más).

Y hay un detalle importante: **Textract (texto) no devuelve las líneas estructuradas**, solo el
texto; necesitaríamos nuestro parser (que ya existe y ya nos ha dado problemas con fotos
torcidas). El modelo de visión devuelve producto, cantidad y precio ya montados de una vez.

---

## 7. Calidad: lo que ya hemos medido

Prueba real con una foto de albarán torcida 90° (sin metadatos de orientación) y con las
líneas pegadas:

| Motor | Confianza de la extracción | Resultado |
| --- | ---: | --- |
| Tesseract 5 (local, gratis) | **5 %** | No detecta la rotación, no saca líneas |
| Gemini visión (modelo Flash) | **98 %** | Proveedor, número, fecha, importe y línea completa, en 7–10 s |

Lo que sabemos de cada familia:

| Aspecto | Tesseract local | Modelos de visión (Gemini/OpenAI) | APIs clásicas (AWS/Azure/Google) |
| --- | --- | --- | --- |
| PDF digital | Excelente | Excelente | Excelente |
| Escaneo limpio | Bueno | Excelente | Excelente |
| Foto de móvil torcida | Malo | Muy bueno | Bueno (AWS/Azure corrigen orientación) |
| Manuscrito | Malo | Bueno | Regular-bueno (mejor en "prebuilt") |
| Devuelve líneas estructuradas | No (hay que parsear) | **Sí, JSON directo** | Sí en "prebuilt"; no en *Read*/*DetectText* |
| Latencia por albarán | 2–5 s (CPU) | 7–10 s | 1–3 s |
| Coste servidor | CPU del propio servidor | ninguno extra | ninguno extra |

Ninguna opción es perfecta con manuscrito. Para eso hace falta enseñar ejemplos reales al
sistema (2–3 albaranes por proveedor) y medir.

---

## 8. RGPD y datos (importante para decidir)

| Proveedor | Región en la UE | ¿Entrena con nuestros datos? | Contrato de encargado (DPA) |
| --- | --- | --- | --- |
| **Gemini API (Google AI Studio)** | Endpoint global | **La capa gratuita sí puede usar los datos para mejorar sus productos**; la de pago no | Sí |
| **Vertex AI (Google Cloud)** | `europe-west` (residencia garantizada) | No | Sí |
| **OpenAI API** | Residencia UE con *endpoints* regionales (+10 % de precio) | No (API) | Sí |
| **AWS Textract** | eu-west-1, eu-central-1, eu-west-3 | No | Sí |
| **Azure Document Intelligence** | West Europe / Spain Central | No | Sí |
| **Mistral** | UE (empresa francesa) | No | Sí |
| **Tesseract local** | Nuestro servidor | No sale nada del servidor | No aplica |

**Consecuencia:** la capa gratuita de Gemini vale para probar y para la demo, **no para
producción con datos del restaurante**. En producción, o se pasa a la capa de pago (que además
es baratísima) o se usa Vertex AI con residencia en la UE.

---

## 9. Esfuerzo de integración y dependencia

Lo que ya está hecho en la aplicación (esto abarata mucho las opciones del medio):

| Proveedor | Estado | Trabajo pendiente |
| --- | --- | --- |
| **Tesseract local** | ✅ En producción (dentro de la imagen Docker, con idioma español) | Ajustes finos del parser |
| **Gemini** | ✅ Adaptador escrito y probado (98 % en la foto de prueba) | Cambiar de modelo/clave = 5 minutos |
| **AWS Textract** | 🟡 SDK ya instalado en el proyecto | Adaptador (~1 día) |
| **Azure Document Intelligence** | ⚪ Nada | SDK + adaptador (~1–2 días) |
| **Mistral OCR** | ⚪ Nada | Es REST, como Gemini (~medio día) |
| **OpenAI** | ⚪ Nada | Es REST, como Gemini (~medio día) |

**Salida (lock-in):** cambiar de proveedor de lectura cuesta 0,5–2 días de trabajo, no meses:
todos van detrás de la misma interfaz (`ExtractorDocumento`). El código de la aplicación no
depende de ninguno.

---

## 10. Servidor: comparativa rápida

Todo lo que necesitamos (Java + PostgreSQL + almacenamiento de fotos) cabe en una máquina
pequeña. Precios verificados hoy, sin IVA:

| Opción | Coste/mes | Notas |
| --- | ---: | --- |
| **VPS Hetzner** (plan pequeño, desde) | **5,99 €** | El más barato de los serios; centro de datos en Alemania/Finlandia |
| **AWS Lightsail** (1 GB) | 3,50 $ ≈ 3,22 € | Sencillo, pero se queda corto de memoria con la base de datos |
| **Render** (512 MB) | 7 $ | Gestión incluida; poco margen para IA/OCR local |
| **AWS EC2 + RDS** | 30–60 $ | Lo que se usa normalmente en empresa; para una cocina es sobredimensionado |
| **Azure VM + PostgreSQL flexible** | 25–50 € | Igual: pensado para otro tamaño |
| **Google Cloud Run + Cloud SQL** | 10–25 € | Pago por uso, escala a cero |

**Recomendación:** VPS de 5,99 €/mes con Docker Compose (ya está preparado y probado). Si el
jefe prefiere que facturación y soporte sean de AWS o Azure por política de empresa, el
sobrecoste es de **~25–50 €/mes** por cocina.

---

## 11. Recomendación por escenario

| Escenario | Lectura recomendada | Servidor | Coste total/mes |
| --- | --- | --- | ---: |
| **Probar y enseñar (ahora)** | Tesseract local + Gemini capa gratuita para demos | VPS 5,99 € | **5,99 €** |
| **Una cocina en producción** | Gemini 2.5 Flash-Lite (0,12 €/mes); Flash si la calidad lo pide (0,67 €) | VPS 5,99 € | **6–7 €** |
| **10 cocinas** | Gemini 2.5 Flash (79,92 €/año) | VPS 5,99 € o PaaS | **7–12 €** |
| **100 cocinas** | Gemini 2.5 Flash + Vertex AI para residencia UE | 2 VPS + copias | **20–40 €** |
| **Si la empresa exige AWS** | Textract *Detect Document Text* + nuestro parser, o *AnalyzeExpense* si se quiere cero mantenimiento | EC2 + RDS | **35–65 €** |

**Por qué no recomiendo AWS/Azure para leer albaranes de cocina:** cuestan 3–25 veces más por
el mismo resultado, obligan a mantener un parser propio (ya nos ha costado varios fallos) y su
ventaja (corrección de orientación, formularios, tablas complejas) apenas se usa en un albarán
de proveedor de 20 líneas. Su sitio natural sería facturas de proveedores grandes o documentos
de varias páginas, que hoy no tenemos.

**Por qué «irse fuera de AWS/OpenAI» sí compensa:** no por el coste (que es calderilla) sino
porque Google y Mistral dan **mejor lectura en español, más barata y con datos en la UE**. La
única razón de peso para quedarse en AWS es que la empresa ya tenga contrato, facturación y
soporte ahí.

---

## 12. Riesgos y cómo los controlamos

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Cambio de precios o retirada de modelos | Medio | Ya nos pasó: un modelo que salía en la documentación respondía 404 con una clave nueva. Todo va detrás de una interfaz, cambiar de modelo es 5 minutos |
| Subida de precio sin avisar | Bajo | El gasto máximo está acotado por el número de albaranes; hay alerta de presupuesto en todos los proveedores |
| Capa gratuita con los datos usados para entrenar | **Alto (RGPD)** | Nunca usar la capa gratuita con datos reales en producción; pasar a pago o Vertex AI |
| Foto ilegible que el sistema "inventa" | Alto | Ya hay aviso de confianza baja en pantalla; el usuario revisa antes de guardar y el importe debe cuadrar |
| Dependencia de un proveedor | Bajo | Interfaz propia `ExtractorDocumento`; salir cuesta 0,5–2 días |
| Caída del proveedor | Medio | Tesseract local sigue disponible como red de seguridad |

---

## 13. Qué necesito que decidas

1. **Lectura de albaranes:** ¿Gemini (pago, ~1–8 €/año por cocina) o AWS/Azure (30–330 €/año)?
   Mi recomendación: **Gemini**, con Tesseract como respaldo.
2. **Servidor:** ¿VPS propio de 5,99 €/mes (mi recomendación) o AWS/Azure por política de
   empresa (25–50 €/mes más)?
3. **Datos personales:** ¿se firma el contrato de encargado del tratamiento con Google (o con
   Mistral, si se prefiere proveedor europeo) y se acepta que las fotos salgan del servidor?
   Si la respuesta es no, nos quedamos en Tesseract local (0 €, peor calidad en fotos).
4. **Albaranes reales:** necesito 2–3 fotos reales de proveedores para medir la calidad antes
   de firmar nada.

---

## 14. Fuentes (consultadas el 26/10/2026)

- AWS Textract, precios y capa gratuita: <https://aws.amazon.com/textract/pricing/>
- AWS Lightsail: <https://aws.amazon.com/lightsail/pricing/>
- Azure Document Intelligence (tarifa S0, West Europe, consultada por la API oficial de precios
  de Azure): <https://prices.azure.com/api/retail/prices>
- Google Document AI (OCR, parsers y capas gratuitas): <https://cloud.google.com/document-ai/pricing>
- Gemini API (precios por modelo y capa gratuita): <https://ai.google.dev/gemini-api/docs/pricing>
- OpenAI (precios por modelo, incluido el recargo del 10 % por residencia de datos):
  <https://platform.openai.com/docs/pricing>
- Mistral: <https://mistral.ai/pricing> (precio de OCR publicado: 1 $/1.000 páginas)
- Hetzner Cloud: <https://www.hetzner.com/cloud/>
- Render: <https://render.com/pricing>

> Este documento es una estimación de costes, no una oferta. Antes de contratar hay que
> confirmar precios, región e IVA en la web del proveedor.
