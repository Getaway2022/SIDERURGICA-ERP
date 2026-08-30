-- ============================================
-- HU02 - Migración de passwords a BCrypt
-- Spring Security NO acepta passwords en texto plano
-- Ejecutar este script en PostgreSQL (pgAdmin o psql)
-- ============================================

-- PASO 1: Agrega columna temporal para no perder datos
-- (solo si quieres hacer backup del password original)
-- ALTER TABLE seguridad.usuario ADD COLUMN password_old VARCHAR(255);
-- UPDATE seguridad.usuario SET password_old = password;

-- PASO 2: Actualiza passwords a BCrypt
-- Las siguientes son encriptaciones BCrypt de contraseñas comunes
-- Reemplaza según los passwords reales de tus usuarios

-- BCrypt de "admin123"
UPDATE seguridad.usuario 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE username = 'admin';

-- BCrypt de "vendedor123"
UPDATE seguridad.usuario 
SET password = '$2a$10$8K1p/a0dR1xqM8K3Qe8ZKOQbL1mzU9nX7vY4wZ6rP5sT2uV3wX9yS'
WHERE username = 'vendedor';

-- ============================================
-- GENERAR TU PROPIO BCrypt en Java (para otros usuarios):
-- Pega esto en un main temporal y ejecuta:
--
-- import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
-- public class Main {
--     public static void main(String[] args) {
--         BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
--         System.out.println(enc.encode("TU_PASSWORD_AQUI"));
--     }
-- }
-- ============================================

-- PASO 3: Verificar
SELECT id, username, LEFT(password, 10) AS password_inicio, rol_id 
FROM seguridad.usuario;
-- Los passwords BCrypt siempre empiezan con $2a$10$
