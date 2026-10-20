-- Ejecutar exclusivamente sobre una base NUEVA y local automation_migration_test.
\set ON_ERROR_STOP on
DO $$ BEGIN
    IF current_database() <> 'automation_migration_test' OR inet_server_addr() <> '127.0.0.1'::inet THEN
        RAISE EXCEPTION 'Se requiere automation_migration_test en localhost';
    END IF;
END $$;
CREATE SCHEMA almacen;
CREATE SCHEMA comercial;
CREATE SCHEMA compras;
CREATE TABLE almacen.inventario(id bigint PRIMARY KEY, stock integer, stock_minimo integer);
CREATE TABLE comercial.cotizacion(id bigint PRIMARY KEY);
CREATE TABLE comercial.pedido(id bigint PRIMARY KEY, codigo varchar(20), estado varchar(20));
CREATE TABLE comercial.venta(id bigint PRIMARY KEY, codigo varchar(20), pedido_id bigint);
CREATE TABLE comercial.despacho(id bigint PRIMARY KEY, codigo varchar(20), pedido_id bigint, venta_id bigint);
CREATE TABLE compras.orden_compra(id bigint PRIMARY KEY, codigo varchar(20));
CREATE TABLE compras.compra(id bigint PRIMARY KEY, orden_compra_id bigint);
INSERT INTO almacen.inventario VALUES (1, 42, 10);
INSERT INTO comercial.pedido VALUES (1, 'PED-0099', 'FACTURADO'), (2, 'PED-0100', 'PENDIENTE');
INSERT INTO comercial.venta VALUES (1, 'VEN-0200', 1);
INSERT INTO comercial.despacho VALUES (1, 'DES-0300', NULL, 1);
\ir ../../../migrations/001_automatizacion.sql
\ir ../../../migrations/001_automatizacion.sql
DO $$ BEGIN
    IF (SELECT stock FROM almacen.inventario WHERE id=1) <> 42 THEN RAISE EXCEPTION 'Se alteró stock físico'; END IF;
    IF (SELECT stock_objetivo FROM almacen.inventario WHERE id=1) <> 20 THEN RAISE EXCEPTION 'Objetivo incorrecto'; END IF;
    IF (SELECT stock_reservado FROM almacen.inventario WHERE id=1) <> 0 THEN RAISE EXCEPTION 'Reserva histórica inventada'; END IF;
    IF (SELECT estado_stock FROM comercial.pedido WHERE id=1) <> 'REVISION_LEGADO' THEN RAISE EXCEPTION 'Falta revisión histórica'; END IF;
    IF (SELECT estado_stock FROM comercial.pedido WHERE id=2) <> 'SIN_RESERVA' THEN RAISE EXCEPTION 'Pendiente incorrecto'; END IF;
    IF (SELECT pedido_id FROM comercial.despacho WHERE id=1) <> 1 THEN RAISE EXCEPTION 'Falta vínculo de despacho'; END IF;
    IF nextval('comercial.automatizacion_codigo_seq') <> 301 THEN RAISE EXCEPTION 'Secuencia incorrecta'; END IF;
    IF (SELECT count(*) FROM comercial.migracion_aplicada) <> 1 THEN RAISE EXCEPTION 'Migración no idempotente'; END IF;
END $$;
SELECT 'Migración repetible y existencias conservadas' AS resultado;
