-- Modelo de datos inicial de Chefstool.
-- Convenciones: tablas y columnas en espanol, claves primarias UUID,
-- importes en numeric(12,2) y precios unitarios en numeric(12,4).

create table organizaciones (
    id         uuid primary key default gen_random_uuid(),
    nombre     text        not null,
    created_at timestamptz not null default now()
);

create table miembros_organizacion (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid        not null references organizaciones (id) on delete cascade,
    usuario_id      uuid        not null,
    email           text,
    nombre          text,
    rol             text        not null default 'miembro',
    created_at      timestamptz not null default now(),
    constraint miembros_organizacion_rol_check check (rol in ('propietario', 'admin', 'miembro')),
    constraint miembros_organizacion_unique unique (organizacion_id, usuario_id)
);

create table proveedores (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid        not null references organizaciones (id) on delete cascade,
    nombre          text        not null,
    cif             text,
    email           text,
    telefono        text,
    notas           text,
    created_at      timestamptz not null default now(),
    deleted_at      timestamptz,
    constraint proveedores_unique unique (organizacion_id, nombre)
);

create table productos (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid        not null references organizaciones (id) on delete cascade,
    nombre          text        not null,
    categoria       text,
    unidad_base     text        not null default 'ud',
    created_at      timestamptz not null default now(),
    deleted_at      timestamptz,
    constraint productos_unique unique (organizacion_id, nombre)
);

create table proveedor_productos (
    id                   uuid primary key default gen_random_uuid(),
    organizacion_id      uuid        not null references organizaciones (id) on delete cascade,
    proveedor_id         uuid        not null references proveedores (id) on delete cascade,
    producto_id          uuid        not null references productos (id) on delete cascade,
    nombre_en_proveedor  text,
    unidad               text,
    precio_ultimo        numeric(12, 4),
    fecha_ultima_compra  date,
    created_at           timestamptz not null default now(),
    constraint proveedor_productos_unique unique (proveedor_id, producto_id)
);

create table clientes (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid        not null references organizaciones (id) on delete cascade,
    nombre          text        not null,
    email           text,
    telefono        text,
    notas           text,
    created_at      timestamptz not null default now(),
    deleted_at      timestamptz
);

create table eventos (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid        not null references organizaciones (id) on delete cascade,
    cliente_id      uuid        references clientes (id) on delete set null,
    nombre          text        not null,
    fecha           date,
    comensales      integer,
    notas           text,
    created_at      timestamptz not null default now(),
    deleted_at      timestamptz
);

create table documentos_compra (
    id                   uuid primary key default gen_random_uuid(),
    organizacion_id      uuid        not null references organizaciones (id) on delete cascade,
    nombre_original      text        not null,
    ruta_almacenamiento  text,
    tipo_mime            text,
    estado               text        not null default 'pendiente_revision',
    origen               text        not null default 'subida',
    confianza_extraccion numeric(4, 2),
    extraccion_json      jsonb,
    error_extraccion     text,
    subido_por           uuid,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now(),
    constraint documentos_compra_estado_check check (estado in ('pendiente_revision', 'confirmado', 'descartado')),
    constraint documentos_compra_origen_check check (origen in ('subida', 'camara', 'demo'))
);

create table compras (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid           not null references organizaciones (id) on delete cascade,
    documento_id    uuid           references documentos_compra (id) on delete set null,
    proveedor_id    uuid           references proveedores (id) on delete set null,
    cliente_id      uuid           references clientes (id) on delete set null,
    evento_id       uuid           references eventos (id) on delete set null,
    numero_documento text,
    fecha           date           not null default current_date,
    moneda          text           not null default 'EUR',
    importe_neto    numeric(12, 2) not null default 0,
    importe_iva     numeric(12, 2) not null default 0,
    importe_total   numeric(12, 2) not null default 0,
    created_at      timestamptz    not null default now()
);

create table lineas_compra (
    id                   uuid primary key default gen_random_uuid(),
    compra_id            uuid           not null references compras (id) on delete cascade,
    producto_id          uuid           references productos (id) on delete set null,
    cliente_id           uuid           references clientes (id) on delete set null,
    evento_id            uuid           references eventos (id) on delete set null,
    descripcion_original text,
    cantidad             numeric(12, 3) not null default 0,
    unidad               text,
    precio_unitario      numeric(12, 4) not null default 0,
    importe              numeric(12, 2) not null default 0,
    precio_normalizado   numeric(12, 4),
    created_at           timestamptz    not null default now()
);

create table fichas (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid           not null references organizaciones (id) on delete cascade,
    nombre          text           not null,
    descripcion     text,
    raciones        integer        not null default 1,
    margen_objetivo numeric(5, 2),
    precio_venta    numeric(12, 2),
    created_at      timestamptz    not null default now(),
    deleted_at      timestamptz
);

create table ficha_lineas (
    id          uuid primary key default gen_random_uuid(),
    ficha_id    uuid           not null references fichas (id) on delete cascade,
    producto_id uuid           references productos (id) on delete set null,
    descripcion text,
    cantidad    numeric(12, 3) not null default 0,
    unidad      text,
    created_at  timestamptz    not null default now()
);

create table alertas_precio (
    id                uuid primary key default gen_random_uuid(),
    organizacion_id   uuid           not null references organizaciones (id) on delete cascade,
    producto_id       uuid           references productos (id) on delete cascade,
    proveedor_id      uuid           references proveedores (id) on delete set null,
    mensaje           text           not null,
    severidad         text           not null default 'aviso',
    porcentaje_cambio numeric(6, 2),
    precio_anterior   numeric(12, 4),
    precio_nuevo      numeric(12, 4),
    reconocida        boolean        not null default false,
    created_at        timestamptz    not null default now(),
    constraint alertas_precio_severidad_check check (severidad in ('info', 'aviso', 'critico'))
);

create index idx_proveedores_organizacion on proveedores (organizacion_id);
create index idx_productos_organizacion on productos (organizacion_id);
create index idx_clientes_organizacion on clientes (organizacion_id);
create index idx_eventos_organizacion on eventos (organizacion_id);
create index idx_eventos_cliente on eventos (cliente_id);
create index idx_documentos_compra_organizacion on documentos_compra (organizacion_id, estado);
create index idx_compras_organizacion_fecha on compras (organizacion_id, fecha desc);
create index idx_compras_proveedor on compras (proveedor_id);
create index idx_lineas_compra_compra on lineas_compra (compra_id);
create index idx_lineas_compra_producto on lineas_compra (producto_id);
create index idx_fichas_organizacion on fichas (organizacion_id);
create index idx_ficha_lineas_ficha on ficha_lineas (ficha_id);
create index idx_alertas_precio_organizacion on alertas_precio (organizacion_id, reconocida);

-- Organizacion por defecto mientras no exista autenticacion (etapa 7).
insert into organizaciones (id, nombre)
values ('00000000-0000-0000-0000-000000000001', 'Cocina demo');
