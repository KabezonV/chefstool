-- Retira la tabla heredada creada por ddl-auto antes de Flyway, conservando los
-- nombres de ingrediente como productos de la organizacion por defecto.
DO $$
DECLARE
    org_id uuid;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'ingrediente'
    ) THEN
        SELECT id INTO org_id FROM organizaciones ORDER BY created_at LIMIT 1;

        INSERT INTO productos (organizacion_id, nombre, unidad_base)
        SELECT org_id,
               i.nombre,
               COALESCE(NULLIF(i.unidad_medida, ''), 'ud')
        FROM ingrediente i
        WHERE i.nombre IS NOT NULL AND org_id IS NOT NULL
        ON CONFLICT (organizacion_id, nombre) DO NOTHING;

        DROP TABLE ingrediente;
    END IF;
END $$;
