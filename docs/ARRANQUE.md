# Arrancar Chefstool en tu ordenador

Guía para tener la aplicación funcionando en local en unos 5 minutos.

## Qué necesitas

- **Docker Desktop** ([Windows](https://docs.docker.com/desktop/install/windows-install/) ·
  [macOS](https://docs.docker.com/desktop/install/mac-install/)). En Windows, deja que
  active WSL 2 cuando lo pida.
- Git (ya lo tienes, has clonado el repositorio).
- Nada más: la base de datos **PostgreSQL va incluida** en el `docker-compose` y se crea
  sola la primera vez.

## Pasos

```bash
git clone https://github.com/KabezonV/chefstool.git
cd chefstool
cp .env.ejemplo .env
```

Edita `.env` y rellena:

| Variable | Qué poner |
| --- | --- |
| `DB_HOST` | `postgres` (el servicio de la propia composición) |
| `DB_PORT` | `5432` |
| `DB_NAME` | `chefstool` |
| `DB_USER` | `chefstool` |
| `DB_PASSWORD` | `chefstool` (cámbiala si el equipo es compartido) |
| `CHEFSTOOL_JWT_SECRET` | ya viene un valor de ejemplo; en un servidor, uno propio |

El resto puede quedarse como está. Después:

```bash
docker compose up --build
```

La primera vez tarda unos minutos (descarga Java, Node y Tesseract). Cuando termine:

- Aplicación: **http://localhost:8081**
- API: http://localhost:8080

## Entrar

- Usuario de demostración: `chef@demo.es` · contraseña `chefstool2026`
  (organización *Cocina Norte* con productos, compras, fichas técnicas y alertas de
  precio ya cargadas).
- O pulsa **Crear cuenta** y tendrás tu propia organización vacía, con un botón para
  cargar datos de ejemplo.

## Qué probar en 5 minutos

1. **Panel**: gasto del mes, ahorro potencial y alertas.
2. **Documentos → Añadir documento → Probar con un ejemplo**: así entra un albarán
   (foto o PDF), se revisa y se convierte en compra.
3. **Productos → cualquiera**: histórico de precios y comparación entre proveedores.
4. **Fichas técnicas**: coste por ración calculado con el mejor precio de cada
   ingrediente y margen sobre el precio de venta.
5. **Informes**: coste por proveedor, variaciones de precio y botón **Excel**.

## Si algo falla

| Síntoma | Solución |
| --- | --- |
| `docker: command not found` | Docker Desktop no está abierto o no está instalado |
| La API no arranca y dice `Connection refused` | Espera 20 s (la base tarda en aceptar conexiones) y comprueba con `docker compose ps` que `chefstool-db` está *healthy* |
| `chefstool.seguridad.jwt-secret debe tener al menos 32 caracteres` | Pon una cadena más larga en `CHEFSTOOL_JWT_SECRET` |
| El puerto 8080 u 8081 está ocupado | Cambia el número de la izquierda en `docker-compose.yml` (`'8090:8080'`) |
| La lectura de fotos no encuentra texto | Es normal con fotos muy movidas o escritas a mano: se corrige a mano en la pantalla de revisión |

## Dónde están los datos

- Base de datos: volumen `chefstool_datos` (sobrevive a reinicios).
- Albaranes: volumen `chefstool_albaranes`.
- Para empezar de cero: `docker compose down -v` (borra ambos volúmenes).

## Parar y limpiar

```bash
docker compose down          # parar
docker compose down -v       # parar y borrar los albaranes locales y el volumen
```

## Documentación

- Resumen del proyecto y decisiones pendientes: [INFORME-BERNART.md](INFORME-BERNART.md)
- Guion para enseñarlo a un cliente: [DEMO.md](DEMO.md)
- Despliegue en un servidor: [DESPLIEGUE.md](DESPLIEGUE.md)
- Referencia de la API: [API.md](API.md)
