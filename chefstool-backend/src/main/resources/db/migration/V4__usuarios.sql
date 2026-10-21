-- Usuarios de la aplicacion. Las contrasenas se guardan con BCrypt.

create table usuarios (
    id            uuid primary key default gen_random_uuid(),
    email         text        not null,
    password_hash text        not null,
    nombre        text,
    created_at    timestamptz not null default now()
);

create unique index usuarios_email_unique on usuarios (lower(email));

-- Las pertenencias previas (si las hubiera) no tienen usuario asociado.
delete from miembros_organizacion
where usuario_id not in (select id from usuarios);

alter table miembros_organizacion
    add constraint miembros_organizacion_usuario_fk
    foreign key (usuario_id) references usuarios (id) on delete cascade;
