create table solicitudes_compra (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid not null references organizaciones (id) on delete cascade,
    local_id        uuid references locales (id) on delete set null,
    solicitante_id  uuid references usuarios (id) on delete set null,
    concepto        varchar(160) not null,
    descripcion     varchar(2000),
    proveedor       varchar(200),
    importe         numeric(12, 2) not null check (importe > 0),
    estado          varchar(16) not null default 'PENDIENTE'
                    check (estado in ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    revisada_por    uuid references usuarios (id) on delete set null,
    revisada_at     timestamptz,
    motivo_rechazo  varchar(1000),
    created_at      timestamptz not null default now()
);

create index solicitudes_compra_organizacion_local_fecha_idx
    on solicitudes_compra (organizacion_id, local_id, created_at desc);
