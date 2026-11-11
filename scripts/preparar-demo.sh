#!/usr/bin/env bash
# Deja la aplicacion lista para una demo. Uso: ./scripts/preparar-demo.sh
set -uo pipefail
cd "$(dirname "$0")/.."

ok()   { printf "  \033[32mOK\033[0m %s\n" "$1"; }
fail() { printf "  \033[31mFALLO\033[0m %s\n" "$1"; }

echo "1) Docker"
if ! docker info >/dev/null 2>&1; then
  fail "Docker no responde. Abre Docker Desktop y vuelve a lanzar el script."
  exit 1
fi
ok "Docker en marcha"

echo "2) Levantando servicios"
docker compose up -d >/dev/null 2>&1 || { fail "docker compose up"; exit 1; }

echo "3) Esperando a la API (maximo 3 minutos)"
for _ in $(seq 1 36); do
  estado=$(docker inspect -f '{{.State.Health.Status}}' chefstool-api 2>/dev/null || echo none)
  [ "$estado" = "healthy" ] && break
  sleep 5
done
if [ "$estado" = "healthy" ]; then ok "API sana"; else fail "API en estado: $estado"; exit 1; fi

echo "4) Calentando la aplicacion"
curl -s -o /dev/null -w "" http://localhost:8081/ || true
codigo=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' -d '{"email":"chef@demo.es","password":"chefstool2026"}')
if [ "$codigo" = "200" ]; then
  ok "Login demo funciona (chef@demo.es)"
else
  fail "Login demo devuelve $codigo: revisa la base de datos o crea una cuenta nueva"
fi

echo "5) Albaranes de ejemplo"
for f in docs/ejemplos/albaran-ejemplo-limpio.png docs/ejemplos/albaran-ejemplo-foto.jpg; do
  [ -f "$f" ] && ok "$(basename "$f")" || fail "falta $f"
done

echo
echo "Listo. Abre http://localhost:8081"
echo "  - Usuario demo: chef@demo.es (contrasena en docs/DEMO.md)"
echo "  - Movil: Ctrl/Cmd + Shift + M en Chrome y elige un iPhone o Pixel"
echo "  - Recuerda: ventana nueva de Chrome y No molestar activado"
