-- Invitaciones a una organizacion. Se resuelven al registrarse el correo invitado,
-- sin necesidad de envio de emails: si el usuario ya existe se anade al momento.

create table invitaciones (
    id              uuid primary key default gen_random_uuid(),
    organizacion_id uuid        not null references organizaciones (id) on delete cascade,
    email           text        not null,
    rol             text        not null default 'miembro',
    invitado_por    uuid        references usuarios (id) on delete set null,
    created_at      timestamptz not null default now(),
    aceptada_at     timestamptz,
    constraint invitaciones_rol_check check (rol in ('propietario', 'admin', 'miembro'))
);

create unique index invitaciones_pendientes_unique
    on invitaciones (organizacion_id, lower(email))
    where aceptada_at is null;
