# Coste de digitalizar albaranes: OpenAI y AWS frente al resto

**Para:** Bernart · **Fecha:** 26/10/2026 · **Estado:** documento de decisión (precios consultados hoy en cada proveedor)

---

## 1. Respuesta corta

1. **Con tu regla, nos quedamos con OpenAI.** De todas las opciones del mercado, la única más
   barata que OpenAI ahorra **1,3 céntimos por cada 100 albaranes**. Eso es 38 veces menos que
   tu umbral del medio céntimo por albarán: no compensa dar de alta un proveedor nuevo.
2. **AWS es más caro que OpenAI para esto**: el OCR de texto de Textract cuesta **2,6 veces
   más** y la lectura de facturas de Textract, **17 veces más**. Solo tiene sentido si por
   política interna hay que ir por AWS.
3. **Lo que sí importa no es el precio, son 0,5 céntimos de diferencia**: es la **calidad de
   lectura**. Hay que probar OpenAI con 5 albaranes reales antes de cerrar el modelo
   definitivo. Incluso en el peor caso (tener que usar el modelo grande de OpenAI, 20 veces
   más caro), con 500 albaranes al mes son **63 €/año**.

---

## 2. Lo que cuesta cada 1.000 albaranes

Precios verificados hoy, en euros y sin IVA. La columna de la derecha aplica tu regla:
*sí merece cambiar solo si la diferencia es grande* (medio céntimo por albarán o el triple de
precio).

| Opción | € / 1.000 albaranes | Diferencia con OpenAI | Veces el precio de OpenAI | ¿Merece cambiar? |
| --- | ---: | ---: | ---: | --- |
| Gemini 2.5 Flash-Lite | 0,40 € | **−0,13 €** (−0,013 cént./albarán) | 0,8× | ✅ **No**: ahorra 1,3 céntimos cada 100 |
| **OpenAI gpt-6-luna** *(proveedor ya integrado)* | **0,53 €** | — | 1,0× | **Base** |
| Mistral OCR | 0,92 € | +0,39 € | 1,7× | ✅ No: más caro |
| **AWS Textract** (solo texto) *(integrado)* | 1,38 € | +0,85 € | 2,6× | ✅ No: 2,6× |
| Azure Document Intelligence (texto) | 1,38 € | +0,85 € | 2,6× | ✅ No: 2,6× |
| Google Document AI (texto) | 1,38 € | +0,85 € | 2,6× | ✅ No: 2,6× |
| Gemini 2.5 Flash | 2,22 € | +1,69 € | 4,2× | 🔴 Descartar |
| **AWS Textract AnalyzeExpense** *(facturas)* | 9,20 € | +8,67 € | **17,4×** | 🔴 Descartar |
| OpenAI gpt-6.1-sol *(modelo grande)* | 10,58 € | +10,05 € | 20,0× | 🔴 Solo si la calidad obliga |
| Google Invoice Parser | 92,00 € | +91,47 € | **173,9×** | 🔴 Descartar |

**Cómo leerlo:** «€ / 1.000 albaranes» es literalmente lo que cuesta cada millar de fotos
procesadas. Con OpenAI, **mil albaranes cuestan 53 céntimos**; con el Invoice Parser de Google,
**92 euros**. La diferencia entre OpenAI y la opción más barata del mercado es de **13
céntimos por cada mil albaranes**.

### Nota sobre Azure

**Azure no es un ahorro: cobra exactamente lo mismo que AWS** por el mismo producto (1,38 €
por cada 1.000 albaranes en modo texto; 9,20 € en modo factura, igual que Textract). Frente a
OpenAI es **2,6 veces más caro** en el modo barato y **17 veces** en el de facturas.

La única ventaja real de Azure es su capa gratuita de **500 páginas al mes**, pensada para
probar: para una cocina de 500 albaranes al mes la lectura saldría a 0 €, es decir **3,17 €
menos al año** que con OpenAI — a cambio de dar de alta un proveedor nuevo, integrarlo y
quedarse con una cuota gratuita que no está pensada para producción. No compensa.

Resumen: **Azure solo tiene sentido si la empresa decide que su nube es Azure**, y en ese caso
el coste de lectura sería el mismo que con AWS (8,28 €/año con 500 albaranes al mes), no un
ahorro.

### Nota sobre Google

Google es **el único de los grandes que sale más barato que OpenAI**, y por eso es la única
alternativa que merece un comentario aparte:

- **Gemini 2.5 Flash-Lite: 0,40 € cada 1.000 albaranes** (frente a 0,53 € de OpenAI). Ahorra
  **13 céntimos por cada mil**, o sea 1,3 céntimos cada 100. Con la regla del medio céntimo,
  **no compensa cambiar**: es un 20 % más barato, no el triple.
- Gemini 2.5 Flash (el que ya probamos con 98 % de acierto): 2,22 €/1.000. Es el que usaríamos
  si la calidad de OpenAI no llegara, pero sin cambiar de proveedor si ya estamos en Google.
- **Document AI** (su producto de documentos): 1,38 €/1.000 en modo texto (con 1.000 páginas
  gratis al mes) y 92 €/1.000 en modo factura. Es decir, su producto "clásico" es más caro que
  su propio modelo de visión: **conviene usar Gemini, no Document AI**.
- La **capa gratuita** de Gemini sirve para probar, pero puede usar los datos para mejorar sus
  productos: no vale para producción con datos del restaurante. Para producción con datos en
  la UE está **Vertex AI** (mismo precio, región europea).

En resumen: **Google es el plan B natural**. Si OpenAI no convence en la prueba de calidad, el
cambio sería a Gemini Flash (0,53 → 2,22 €/1.000) o a Flash-Lite (0,40 €/1.000), con medio día
de trabajo porque ya hay un adaptador escrito y probado.


---

## 3. De tantos a tantos albaranes: coste mensual (€/mes, sin IVA)

| Opción | 100 | 500 | 1.000 | 5.000 | 10.000 | 25.000 | 50.000 | 100.000 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Gemini 2.5 Flash-Lite | 0,04 | 0,20 | 0,40 | 2,01 | 4,01 | 10,03 | 20,06 | 40,11 |
| **OpenAI gpt-6-luna** | **0,05** | **0,26** | **0,53** | **2,65** | **5,29** | **13,23** | **26,45** | **52,90** |
| Mistral OCR | 0,09 | 0,46 | 0,92 | 4,60 | 9,20 | 23,00 | 46,00 | 92,00 |
| **AWS Textract** (texto) | 0,14 | 0,69 | 1,38 | 6,90 | 13,80 | 34,50 | 69,00 | 138,00 |
| Azure (texto) | 0,00 | 0,00 | 0,69 | 6,21 | 13,11 | 33,81 | 68,31 | 137,31 |
| Google (texto) | 0,00 | 0,00 | 0,00 | 5,52 | 12,42 | 33,12 | 67,62 | 136,62 |
| Gemini 2.5 Flash | 0,22 | 1,11 | 2,22 | 11,10 | 22,20 | 55,50 | 111,00 | 222,00 |
| **AWS Textract AnalyzeExpense** | 0,92 | 4,60 | 9,20 | 46,00 | 92,00 | 230,00 | 460,00 | 920,00 |
| Google Invoice Parser | 9,20 | 46,00 | 92,00 | 460,00 | 920,00 | 2.300,00 | 4.600,00 | 9.200,00 |

**Coste anual:**

| Opción | 500 albaranes/mes | 10.000 albaranes/mes | 50.000 albaranes/mes |
| --- | ---: | ---: | ---: |
| Gemini 2.5 Flash-Lite | 2,41 € | 48,13 € | 240,67 € |
| **OpenAI gpt-6-luna** | **3,17 €** | **63,48 €** | **317,40 €** |
| Mistral OCR | 5,52 € | 110,40 € | 552,00 € |
| **AWS Textract** (texto) | 8,28 € | 165,60 € | 828,00 € |
| Azure / Google (texto) | 0,00 € / 0,00 € | 157,32 € / 149,04 € | 819,72 € / 811,44 € |
| AWS Textract AnalyzeExpense | 55,20 € | 1.104,00 € | 5.520,00 € |
| OpenAI gpt-6.1-sol | 63,48 € | 1.269,60 € | 6.348,00 € |
| Google Invoice Parser | 552,00 € | 11.040,00 € | 55.200,00 € |

Tu caso de referencia (500 albaranes al mes): **OpenAI son 3,17 € al año**. La opción más
barata del mercado ahorraría 76 céntimos al año. AWS Textract, por el contrario, costaría
8,28 € al año; su versión de facturas, 55,20 €.

---

## 4. ¿A partir de cuántos albaranes la diferencia deja de ser calderilla?

Poniendo el listón en **500 €/año de diferencia** (el punto en que merece la pena cambiar de
proveedor, aunque sea solo por dinero):

| Opción frente a OpenAI | Haría falta llegar a… | Comentario |
| --- | ---: | --- |
| Gemini 2.5 Flash-Lite | **nunca** | Es más barata, pero la diferencia nunca llega a 500 €/año |
| Mistral OCR | 106.564 albaranes/mes | Fuera de escala |
| AWS / Azure / Google (texto) | 48.962 albaranes/mes | Equivale a ~160 cocinas |
| Gemini 2.5 Flash | 24.640 albaranes/mes | ~80 cocinas |
| AWS Textract AnalyzeExpense | 4.805 albaranes/mes | ~16 cocinas |
| OpenAI gpt-6.1-sol | 4.145 albaranes/mes | ~14 cocinas |
| Google Invoice Parser | **455 albaranes/mes** | Ojo: ya duele con una sola cocina |

Es decir: **con una cocina, ninguna decisión de proveedor de lectura mueve la aguja**. Solo a
partir de ~15–20 cocinas la elección empieza a notarse en la factura, y solo contra las
opciones caras (AnalyzeExpense, Invoice Parser).

---

## 5. Lo que no sale en la tabla de precios

| Aspecto | OpenAI | AWS Textract | Resto |
| --- | --- | --- | --- |
| **Proveedor ya integrado** | ✅ Sí | ✅ Sí | ❌ Alta nueva (lo que querías evitar) |
| **Calidad con nuestros albaranes** | 🟡 Sin medir todavía | 🟡 Sin medir | Gemini: 98 % medido |
| **Devuelve las líneas ya montadas** | ✅ Sí (JSON directo) | ❌ Solo texto: hay que mantener nuestro parser | Depende del producto |
| **Fotos torcidas** | ✅ Bien | ✅ Bien | ✅ Bien |
| **Datos en la UE** | 🟡 Residencia UE disponible (+10 % de precio) | ✅ Regiones UE | Google UE con Vertex; Mistral es europea |
| **Esfuerzo de integración** | 0,5 día (es REST, como lo ya hecho) | 1 día (SDK ya instalado) | 0,5–2 días + alta de proveedor |
| **Salir más adelante** | 0,5 día | 1 día | 0,5–2 días |

Aviso que conviene tener presente: la **capa gratuita** de Gemini puede usar los datos para
mejorar sus productos, así que solo vale para demos. En OpenAI (API de pago) y en AWS no se
entrena con nuestros datos.

---

## 6. Conclusión y propuesta

1. **Leer con OpenAI** (`gpt-6-luna`): es proveedor integrado, el más barato de los grandes y
   no exige alta nueva. **3,17 €/año** con 500 albaranes/mes.
2. **Descartar el cambio a Google, Azure o Mistral**: la única que ahorra algo lo hace en
   céntimos, y las demás cuestan entre 2,6 y 174 veces más.
3. **AWS queda como plan B**: si por política hay que ir por AWS, la opción razonable es
   Textract en modo texto (8,28 €/año) manteniendo nuestro parser, y **no** AnalyzeExpense
   (55,20 €/año) salvo que queramos dejar de mantener el parser.
4. **Prueba pendiente (1 día de trabajo):** leer 5 albaranes reales con OpenAI y comparar con
   lo que ya medimos con Gemini. Si la calidad de `gpt-6-luna` no llega, el siguiente escalón
   es el modelo grande de la misma casa (63 €/año con 500 albaranes), no cambiar de proveedor.
5. **Coste total de la plataforma** con esta decisión: servidor propio (5,99 €/mes) + lectura
   (0,26 €/mes) ≈ **6,25 €/mes por cocina**.

---

## 7. Metodología y fuentes

- **1 albarán = 1 página** (foto o PDF de una hoja) con ~20 líneas de producto.
- Los modelos de visión cobran por *tokens*: 560 tokens por imagen, 400 de instrucciones y
  ~850 de respuesta. Aunque la imagen costara el triple de tokens, seguiría por debajo de
  1 céntimo por albarán.
- Tipo de cambio **1 USD = 0,92 EUR**. Precios **sin IVA** (+21 % en España).
- Precios consultados el **26/10/2026**: [AWS Textract](https://aws.amazon.com/textract/pricing/),
  [Azure Document Intelligence](https://prices.azure.com/api/retail/prices) (API oficial de
  precios, West Europe), [Google Document AI](https://cloud.google.com/document-ai/pricing),
  [Gemini API](https://ai.google.dev/gemini-api/docs/pricing),
  [OpenAI](https://platform.openai.com/docs/pricing), [Mistral](https://mistral.ai/pricing),
  [Hetzner](https://www.hetzner.com/cloud/).
- Anexo con el análisis completo (calidad, RGPD por proveedor, capas gratuitas, puntos de
  cruce por número de líneas, comparativa de servidores): **[COMPARATIVA-COSTES-IA-ANEXO.md](COMPARATIVA-COSTES-IA-ANEXO.md)**.
