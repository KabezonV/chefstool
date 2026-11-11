# Albaranes de ejemplo para probar la lectura

Dos documentos **generados para este proyecto** (sin datos de nadie y sin material de
terceros) con los que se puede comprobar si la lectura automática funciona como debe,
porque se sabe de antemano lo que tiene que salir.

| Fichero | Qué es | Para qué sirve |
| --- | --- | --- |
| `albaran-ejemplo-limpio.png` | Escaneado nítido, 2400 × 3600 | Comprobar que el lector y el parser hacen su trabajo con una imagen buena |
| `albaran-ejemplo-foto.jpg` | 4400 × 3200, girado 2,5°, con sombra, desenfoque y compresión JPEG | Comprobar cómo se comporta con una foto de móvil de verdad |

## Lo que debe leerse

Los dos ficheros contienen el mismo albarán, con siete columnas (descripción, código,
lote, cantidad, precio, importe y % de IVA):

- **Proveedor:** DISTRIBUCIONES HORTOFRUTICOLAS EJEMPLO S.L.
- **Número:** A-2026/0457
- **Fecha:** 08/11/2026
- **Base imponible:** 37,80 € · **IVA 10 %:** 3,78 € · **Total:** 41,58 €

| Artículo | Código | Lote | Cantidad | Precio | Importe |
| --- | --- | --- | --- | --- | --- |
| LECHUGA ROMANA | 010203 | 2026040282 | 2 | 2,50 | 5,00 |
| TOMATE PERA | 0300034 | 2026040882 | 4 | 1,85 | 7,40 |
| CEBOLLA BLANCA | 010012 | 20260440000001 | 3 | 1,20 | 3,60 |
| PIMIENTO VERDE | 0100063 | 20260409131 | 2 | 3,40 | 6,80 |
| PATATA | 0110101 | 20260409167 | 10 | 0,95 | 9,50 |
| ZANAHORIA | 0110104 | 20260407167 | 5 | 1,10 | 5,50 |

## Cómo usarlos

1. Entra en la aplicación y ve a **Documentos → Añadir documento → Elegir archivo**.
2. Sube uno de los dos ficheros.
3. En la pantalla de revisión comprueba proveedor, fecha, las seis líneas y el total
   (41,58 €).

## Resultado medido (11/11/2026)

| Fichero | Proveedor | Número y fecha | Totales | Líneas |
| --- | --- | --- | --- | --- |
| `albaran-ejemplo-limpio.png` | Correcto | Correctos | Correctos | 6 de 6 exactas |
| `albaran-ejemplo-foto.jpg` | Correcto | Correctos | Correctos | 4 de 6 |

La diferencia entre los dos es la calidad de la imagen: con una foto girada, con sombra y
comprimida se pierden líneas. Es la razón del aviso que aparece al subir imágenes de menos
de 1200 px de ancho.
