**Asunto:** Chefstool — costes de digitalización y servidor en AWS (documentos adjuntos)

Hola Bernart:

Te paso los dos documentos que me pediste, con los precios consultados hoy en las webs
oficiales de cada proveedor.

**1. Coste de digitalizar los albaranes (adjunto: "Chefstool - Costes de digitalizacion de
albaranes.pdf")**

- **OpenAI: 0,53 € por cada 1.000 albaranes.** Con 20 cocinas a 300 albaranes al mes son
  **3,17 € al mes en total**, no por cocina.
- La única alternativa más barata (Gemini Flash-Lite, 0,40 €) ahorra 13 céntimos por cada
  1.000 albaranes: con tu regla del medio céntimo, no compensa dar de alta un proveedor nuevo.
- **AWS Textract es más caro**: 2,6 veces en modo texto (1,38 €) y 17 veces en modo facturas
  (9,20 €). **Azure cobra exactamente lo mismo que AWS.** Google es un 20 % más barato, no el
  triple.
- He mirado también los más baratos del mercado (DeepSeek, Qwen, Gemma). El ahorro máximo es de
  0,044 céntimos por albarán, y son proveedores nuevos con los datos fuera de la UE: no
  compensa.
- **Mi propuesta: seguir con OpenAI** y dejar Gemini como plan B. Solo falta probar la calidad
  con 5 albaranes reales, que es medio día de trabajo.

**2. Servidor en AWS para 20 cocinas (adjunto: "Chefstool - Informe AWS 20 cocinas.pdf")**

Con 20 cocinas serían unos **6.000 albaranes al mes** (273 al día, 40 en la hora punta). Eso es
un **17 % de un núcleo de CPU**, así que no hace falta ni balanceador ni clúster.

Mi recomendación: **una instancia EC2 `t4g.medium` (2 vCPU, 4 GB) + PostgreSQL gestionado
`db.t4g.micro` + las fotos en S3**:

- **54,6 $/mes ≈ 50 €/mes** para las 20 cocinas, es decir **2,50 € por cocina**.
- Sumando la lectura con OpenAI, la plataforma completa sale a **unos 53 €/mes** (2,65 € por
  cocina).
- En el documento tienes las alternativas: la económica (27 $/mes, sin base gestionada), la de
  alta disponibilidad (117 $) y contenedores gestionados (77 $), con el desglose de precios.
- También está cuándo hay que crecer: a partir de ~50 cocinas se sube de instancia y a partir
  de ~100 se pone un segundo servidor con balanceador. El coste por cocina se mantiene en ~2 €.

**Sobre lo que me comentas en el audio:**

- **Albaranes en S3:** ya está hecho así. Las fotos y los PDF se guardan en un bucket privado
  (nunca en el disco del servidor) y se sirven con enlaces firmados que caducan en 5 minutos.
  El coste de S3 es de 4,14 $/mes al final del primer año.
- **Token de OpenAI:** en cuanto me lo pases activo la digitalización con OpenAI. El lector con
  IA ya está programado y probado: con OpenAI solo falta adaptar la llamada (medio día). Si
  prefieres verlo funcionando antes de decidir, con una clave **gratuita de Google AI Studio**
  (no pide tarjeta) la lectura con IA se activa el mismo día.
- **Cuenta de AWS:** cuando esté, me encargo yo del despliegue. He dejado el plan en 8 pasos
  (1–2 días) y se reutiliza lo que ya tenemos probado: el `docker-compose`, la imagen con OCR y
  las migraciones automáticas de base de datos.

**Lo que necesito para arrancar:**

1. El **token de OpenAI** (o que me confirmen que puedo generarlo con la cuenta de la empresa).
2. La **cuenta de AWS** con un rol para mí (EC2, RDS, S3, Route 53, CloudWatch y SSM).
3. **Dominio**: ¿usamos uno nuevo tipo `app.chefstool.es` o uno que ya tengáis?
4. **Región**: por defecto Irlanda (`eu-west-1`); si preferís que los datos estén en España,
   Aragón (`eu-south-2`).
5. **5 albaranes reales de proveedores** para medir la calidad antes de cerrar la decisión.
6. Confirmar si empezamos con la opción económica (27 $/mes) y pasamos a la recomendada cuando
   haya clientes, o vamos directamente a la recomendada.

El detalle de todo lo que falta para seguir, por orden de prioridad y con lo que cuesta cada
pieza, está en el documento **«Lo que falta para seguir»**
(`docs/PENDIENTES-BERNART.md` en el repositorio).

Cualquier duda de los números la vemos en cinco minutos: llevo el detalle de cada precio y la
fuente en los documentos.

Un saludo,
Víctor
