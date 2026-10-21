const formatoFecha = new Intl.DateTimeFormat('es-ES')

function fecha(valor) {
  return valor ? formatoFecha.format(new Date(valor)) : '—'
}

export const productos = {
  endpoint: '/productos',
  titulo: 'Productos',
  singular: 'producto',
  descripcion: 'Catálogo normalizado de ingredientes y materias primas.',
  vacio: 'Todavía no hay productos en el catálogo.',
  columnas: [
    { key: 'nombre', label: 'Nombre' },
    { key: 'categoria', label: 'Categoría' },
    { key: 'unidadBase', label: 'Unidad base' },
  ],
  campos: [
    { name: 'nombre', label: 'Nombre', required: true, placeholder: 'Tomate pera' },
    { name: 'categoria', label: 'Categoría', placeholder: 'Verdura, Secos, Lácteos…' },
    { name: 'unidadBase', label: 'Unidad base', placeholder: 'kg, l, ud' },
  ],
}

export const proveedores = {
  endpoint: '/proveedores',
  titulo: 'Proveedores',
  singular: 'proveedor',
  descripcion: 'Quién te vende cada producto y a qué precio.',
  vacio: 'Todavía no hay proveedores.',
  columnas: [
    { key: 'nombre', label: 'Nombre' },
    { key: 'cif', label: 'CIF' },
    { key: 'email', label: 'Email' },
    { key: 'telefono', label: 'Teléfono' },
  ],
  campos: [
    { name: 'nombre', label: 'Nombre', required: true, placeholder: 'Frutas García' },
    { name: 'cif', label: 'CIF', placeholder: 'B12345678' },
    { name: 'email', label: 'Email', type: 'email', placeholder: 'pedidos@proveedor.es' },
    { name: 'telefono', label: 'Teléfono', placeholder: '600 000 000' },
    { name: 'notas', label: 'Notas', type: 'textarea' },
  ],
}

export const clientes = {
  endpoint: '/clientes',
  titulo: 'Clientes',
  singular: 'cliente',
  descripcion: 'A quién imputas las compras y los eventos.',
  vacio: 'Todavía no hay clientes.',
  columnas: [
    { key: 'nombre', label: 'Nombre' },
    { key: 'email', label: 'Email' },
    { key: 'telefono', label: 'Teléfono' },
  ],
  campos: [
    { name: 'nombre', label: 'Nombre', required: true, placeholder: 'Hotel Costa' },
    { name: 'email', label: 'Email', type: 'email' },
    { name: 'telefono', label: 'Teléfono' },
    { name: 'notas', label: 'Notas', type: 'textarea' },
  ],
}

export const eventos = {
  endpoint: '/eventos',
  titulo: 'Eventos',
  singular: 'evento',
  descripcion: 'Servicios y caterings para calcular su coste real.',
  vacio: 'Todavía no hay eventos.',
  columnas: [
    { key: 'nombre', label: 'Evento' },
    { key: 'fecha', label: 'Fecha', format: fecha },
    { key: 'comensales', label: 'Comensales' },
    { key: 'clienteNombre', label: 'Cliente' },
  ],
  campos: [
    { name: 'nombre', label: 'Nombre', required: true, placeholder: 'Boda García' },
    { name: 'fecha', label: 'Fecha', type: 'date' },
    { name: 'comensales', label: 'Comensales', type: 'number', placeholder: '120' },
    {
      name: 'clienteId',
      label: 'Cliente',
      type: 'select',
      fuente: { endpoint: '/clientes', valor: 'id', etiqueta: 'nombre' },
    },
    { name: 'notas', label: 'Notas', type: 'textarea' },
  ],
}
