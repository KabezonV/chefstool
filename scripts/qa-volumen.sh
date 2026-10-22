#!/usr/bin/env bash
#
# Prueba de volumen sobre una instalacion local: registra una organizacion nueva,
# crea N compras confirmadas y mide los listados y los informes.
#
#   BASE=http://localhost:8080/api N=120 ./scripts/qa-volumen.sh
#
# No toca los datos de las organizaciones existentes.
set -euo pipefail
BASE=${BASE:-http://localhost:8080/api}
N=${N:-120}
SUFIJO=$(date +%s)
json() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }
EMAIL="qa-vol-${SUFIJO}@cocina.es"
TOKEN=$(curl -s -X POST "$BASE/auth/registro" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"secreto123\",\"nombre\":\"QA\",\"organizacion\":\"QA Volumen $SUFIJO\"}" | json '["token"]')
auth=(-H "Authorization: Bearer $TOKEN")
echo "organizacion de prueba: QA Volumen $SUFIJO"
PRODUCTOS=()
for nombre in "Tomate pera" "Cebolla" "Aceite de oliva" "Harina de trigo" "Solomillo" "Nata" "Arroz" "Sal"; do
  id=$(curl -s -X POST "$BASE/productos" "${auth[@]}" -H 'Content-Type: application/json' \
    -d "{\"nombre\":\"$nombre\",\"categoria\":\"QA\",\"unidadBase\":\"kg\"}" | json '["id"]')
  PRODUCTOS+=("$id")
done
PROVEEDORES=()
for nombre in "Frutas QA" "Carnicas QA" "Secos QA"; do
  id=$(curl -s -X POST "$BASE/proveedores" "${auth[@]}" -H 'Content-Type: application/json' \
    -d "{\"nombre\":\"$nombre\"}" | json '["id"]')
  PROVEEDORES+=("$id")
done
echo "catalogo listo"
inicio=$(date +%s)
for i in $(seq 1 "$N"); do
  p=${PRODUCTOS[$((i % ${#PRODUCTOS[@]}))]}
  pr=${PROVEEDORES[$((i % ${#PROVEEDORES[@]}))]}
  dias=$(( (i * 3) % 180 ))
  precio=$(python3 -c "print(round(2 + ($i % 40) * 0.35, 2))")
  cantidad=$(( (i % 12) + 1 ))
  importe=$(python3 -c "print(round($precio * $cantidad, 2))")
  fecha=$(date -v-"${dias}"d +%Y-%m-%d)
  doc=$(curl -s -X POST "$BASE/documentos" "${auth[@]}" -F "origen=demo" | json '["id"]')
  curl -s -o /dev/null -X POST "$BASE/documentos/$doc/confirmar" "${auth[@]}" \
    -H 'Content-Type: application/json' \
    -d "{\"proveedorNombre\":\"QA\",\"proveedorId\":\"$pr\",\"numeroDocumento\":\"QA-$i\",\"fecha\":\"$fecha\",\"moneda\":\"EUR\",\"importeNeto\":$importe,\"importeIva\":0,\"importeTotal\":$importe,\"lineas\":[{\"descripcion\":\"linea\",\"cantidad\":$cantidad,\"unidad\":\"kg\",\"precioUnitario\":$precio,\"importe\":$importe,\"productoId\":\"$p\"}]}"
done
echo "creadas $N compras en $(( $(date +%s) - inicio )) s"
medir() {
  local etiqueta=$1 url=$2 salida tiempo
  salida=$(curl -s -w '\n%{time_total}' "$url" "${auth[@]}")
  tiempo=$(echo "$salida" | tail -1)
  printf "%-42s %6s s  %s\n" "$etiqueta" "$tiempo" "$(echo "$salida" | head -1 | python3 -c 'import sys,json;d=json.load(sys.stdin);print("total",d["totalElementos"],"paginas",d["totalPaginas"],"items",len(d["contenido"]))' 2>/dev/null || echo '?')"
}
echo
echo "--- listados ---"
medir "compras pagina 1" "$BASE/compras?pagina=0&tamano=20"
medir "compras ultima pagina" "$BASE/compras?pagina=$(( (N - 1) / 20 ))&tamano=20"
medir "compras orden importe desc" "$BASE/compras?pagina=0&tamano=20&orden=importeTotal&direccion=desc"
medir "compras filtro proveedor" "$BASE/compras?pagina=0&tamano=20&proveedorId=${PROVEEDORES[0]}"
medir "productos q=aceite" "$BASE/productos?pagina=0&tamano=20&q=aceite"
medir "documentos" "$BASE/documentos?pagina=0&tamano=20"
echo
echo "--- informes ---"
printf "%-42s %6s s\n" "informes/panel" "$(curl -s -o /dev/null -w '%{time_total}' "$BASE/informes/panel" "${auth[@]}")"
printf "%-42s %6s s\n" "informes/proveedores" "$(curl -s -o /dev/null -w '%{time_total}' "$BASE/informes/proveedores" "${auth[@]}")"
