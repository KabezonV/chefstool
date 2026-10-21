# Chefstool — frontend

React 19 + Vite + Tailwind CSS 4 + React Router.

```bash
npm install
npm run dev     # http://localhost:5173
npm run build
npm run lint
```

## Estructura

```
src/
  components/layout/   AppShell (barra lateral + cabecera)
  components/ui/       Botones, tarjetas y badges reutilizables
  config/navigation.jsx  Menú lateral (una entrada por módulo)
  lib/                 Cliente HTTP y utilidades (cn)
  pages/               Pantallas por módulo
```

`VITE_API_URL` (por defecto `/api`) define la base de la API; en desarrollo el
proxy de Vite la redirige a `http://localhost:8080`.
