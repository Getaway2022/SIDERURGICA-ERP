-- =====================================================================
-- 03_reader_role.sql  |  Usuario de solo lectura para Power BI
-- La contraseña NO va en el archivo: se pasa por variable de psql.
--
--   psql "$ANALYTICS_URL" -v ON_ERROR_STOP=1 -v pwd='CLAVE_LARGA_Y_UNICA' -f 03_reader_role.sql
-- =====================================================================
SELECT format('CREATE ROLE powerbi_reader LOGIN PASSWORD %L', :'pwd')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'powerbi_reader') \gexec

-- Si el rol ya existía, actualiza la contraseña
SELECT format('ALTER ROLE powerbi_reader PASSWORD %L', :'pwd') \gexec

ALTER ROLE powerbi_reader SET default_transaction_read_only = on;
ALTER ROLE powerbi_reader SET statement_timeout = '30s';

-- Las vistas se ejecutan con los permisos de su dueño, así que el rol
-- no necesita acceso a comercial, almacen ni compras.
GRANT USAGE ON SCHEMA analytics TO powerbi_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO powerbi_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics GRANT SELECT ON TABLES TO powerbi_reader;

-- Comprobación: debe fallar con "permission denied"
--   SET ROLE powerbi_reader; SELECT * FROM comercial.venta LIMIT 1; RESET ROLE;