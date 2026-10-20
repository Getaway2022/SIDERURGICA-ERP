-- =====================================================================
-- 03_reader_role_neon_editor.sql | Rol de solo lectura para Power BI
-- Para el SQL Editor de Neon (sin comandos de psql).
--
-- 1) Reemplaza CAMBIAR_CLAVE (2 veces) por una clave larga y única.
-- 2) Ejecuta SOLO en la branch 'analytics', después de 02_views.sql.
-- 3) No guardes esta consulta con la clave escrita y borra la entrada
--    del panel History del editor.
-- =====================================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'powerbi_reader') THEN
    CREATE ROLE powerbi_reader LOGIN PASSWORD 'CAMBIAR_CLAVE';
  ELSE
    ALTER ROLE powerbi_reader PASSWORD 'CAMBIAR_CLAVE';
  END IF;
END $$;

ALTER ROLE powerbi_reader SET default_transaction_read_only = on;
ALTER ROLE powerbi_reader SET statement_timeout = '30s';

-- Las vistas corren con los permisos de su dueño: el rol no necesita
-- acceso a los esquemas comercial, almacen ni compras.
GRANT USAGE ON SCHEMA analytics TO powerbi_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO powerbi_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics GRANT SELECT ON TABLES TO powerbi_reader;

-- Comprobación (debe dar "permission denied"):
--   SET ROLE powerbi_reader; SELECT * FROM comercial.venta LIMIT 1; RESET ROLE;