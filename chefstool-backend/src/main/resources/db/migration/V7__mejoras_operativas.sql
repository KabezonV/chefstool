-- Locales operativos, auditoria y sesiones renovables.

create table locales (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid not null references organizaciones (id) on delete cascade,
    nombre          text not null,
    activo          boolean not null default true,
    created_at      timestamptz not null default now(),
    deleted_at      timestamptz,
    constraint locales_nombre_unique unique (organizacion_id, nombre)
);

insert into locales (organizacion_id, nombre, activo)
select id, 'Principal', true
from organizaciones;

alter table documentos_compra
    add column local_id uuid references locales (id) on delete set null;
alter table compras
    add column local_id uuid references locales (id) on delete set null;
alter table fichas
    add column local_id uuid references locales (id) on delete set null;

update documentos_compra d
set local_id = l.id
from locales l
where l.organizacion_id = d.organizacion_id;

update compras c
set local_id = l.id
from locales l
where l.organizacion_id = c.organizacion_id;

update fichas f
set local_id = l.id
from locales l
where l.organizacion_id = f.organizacion_id;

create table auditoria_eventos (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid not null references organizaciones (id) on delete cascade,
    usuario_id      uuid references usuarios (id) on delete set null,
    accion          text not null,
    entidad         text not null,
    entidad_id      uuid,
    detalle         text,
    created_at      timestamptz not null default now()
);

create index auditoria_eventos_organizacion_idx
    on auditoria_eventos (organizacion_id, created_at desc);

create table sesiones_refresh (
    id              uuid primary key default gen_random_uuid(),
    usuario_id      uuid not null references usuarios (id) on delete cascade,
    organizacion_id uuid not null references organizaciones (id) on delete cascade,
    local_id        uuid references locales (id) on delete set null,
    token_hash      text not null,
    expira_at       timestamptz not null,
    revocado_at     timestamptz,
    created_at      timestamptz not null default now()
);

create unique index sesiones_refresh_token_unique
    on sesiones_refresh (token_hash);
create index sesiones_refresh_usuario_idx
    on sesiones_refresh (usuario_id, expira_at desc);
