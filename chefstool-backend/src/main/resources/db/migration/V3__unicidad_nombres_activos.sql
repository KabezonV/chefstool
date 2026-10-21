-- La unicidad pasa a aplicarse solo sobre registros activos (deleted_at is null).
-- Antes, un nombre quedaba bloqueado para siempre aunque su fila estuviera borrada.
-- Tambien se compara sin distinguir mayusculas, igual que las comprobaciones del servicio.

-- Deduplicacion defensiva: conserva el registro mas antiguo de cada nombre activo.
UPDATE productos p
SET deleted_at = now()
WHERE p.deleted_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM productos otro
      WHERE otro.organizacion_id = p.organizacion_id
        AND lower(otro.nombre) = lower(p.nombre)
        AND otro.deleted_at IS NULL
        AND otro.created_at < p.created_at
  );

UPDATE proveedores p
SET deleted_at = now()
WHERE p.deleted_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM proveedores otro
      WHERE otro.organizacion_id = p.organizacion_id
        AND lower(otro.nombre) = lower(p.nombre)
        AND otro.deleted_at IS NULL
        AND otro.created_at < p.created_at
  );

UPDATE clientes c
SET deleted_at = now()
WHERE c.deleted_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM clientes otro
      WHERE otro.organizacion_id = c.organizacion_id
        AND lower(otro.nombre) = lower(c.nombre)
        AND otro.deleted_at IS NULL
        AND otro.created_at < c.created_at
  );

ALTER TABLE productos DROP CONSTRAINT IF EXISTS productos_unique;
CREATE UNIQUE INDEX productos_nombre_activo_unique
    ON productos (organizacion_id, lower(nombre))
    WHERE deleted_at IS NULL;

ALTER TABLE proveedores DROP CONSTRAINT IF EXISTS proveedores_unique;
CREATE UNIQUE INDEX proveedores_nombre_activo_unique
    ON proveedores (organizacion_id, lower(nombre))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX clientes_nombre_activo_unique
    ON clientes (organizacion_id, lower(nombre))
    WHERE deleted_at IS NULL;
