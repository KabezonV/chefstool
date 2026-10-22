-- Recuperacion de contrasena: se guarda el hash del token de un solo uso, nunca el token.

create table recuperaciones_password (
    id         uuid primary key default gen_random_uuid(),
    usuario_id uuid        not null references usuarios (id) on delete cascade,
    token_hash text        not null,
    expira_at  timestamptz not null,
    usado_at   timestamptz,
    created_at timestamptz not null default now()
);

create unique index recuperaciones_password_token_unique
    on recuperaciones_password (token_hash);

create index recuperaciones_password_usuario_idx
    on recuperaciones_password (usuario_id);
