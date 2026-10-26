/** Columnas del informe de coste por proveedor (también se usan al exportar). */
export const COLUMNAS_PROVEEDORES = [
  { label: 'Proveedor', valor: (fila) => fila.nombre },
  { label: 'Compras', valor: (fila) => fila.compras },
  { label: 'Gasto total', valor: (fila) => fila.gastoTotal },
  { label: 'Ticket medio', valor: (fila) => fila.ticketMedio },
  { label: '% del gasto', valor: (fila) => fila.porcentajeGasto },
  { label: 'Variación media (%)', valor: (fila) => fila.variacionMediaPct },
  { label: 'Productos', valor: (fila) => fila.productos },
  { label: 'Última compra', valor: (fila) => fila.ultimaCompra },
]
