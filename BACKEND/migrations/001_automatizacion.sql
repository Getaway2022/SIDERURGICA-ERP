-- Ejecutar con psql -v ON_ERROR_STOP=1 -f migrations/001_automatizacion.sql
-- Requiere detener las escrituras del backend anterior durante la migración.
-- Transaccional e idempotente. No modifica existencias físicas ni elimina datos.
BEGIN;
SELECT pg_advisory_xact_lock(73190241);
CREATE TABLE IF NOT EXISTS comercial.migracion_aplicada (
    nombre varchar(100) PRIMARY KEY, aplicada_en timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
DO $$
DECLARE maximo bigint;
BEGIN
IF NOT EXISTS (SELECT 1 FROM comercial.migracion_aplicada WHERE nombre = '001_automatizacion') THEN
    ALTER TABLE almacen.inventario ADD COLUMN stock_reservado integer NOT NULL DEFAULT 0;
    ALTER TABLE almacen.inventario ADD COLUMN stock_objetivo integer;
    UPDATE almacen.inventario SET stock_objetivo = COALESCE(stock_minimo, 10) * 2;
    ALTER TABLE almacen.inventario ADD CONSTRAINT inventario_reserva_valida
        CHECK (stock_reservado >= 0 AND stock_reservado <= stock);
    ALTER TABLE almacen.inventario ADD CONSTRAINT inventario_objetivo_valido
        CHECK (stock_objetivo >= COALESCE(stock_minimo, 0));

    ALTER TABLE comercial.cotizacion ADD COLUMN inventario_id bigint REFERENCES almacen.inventario(id);
    ALTER TABLE comercial.pedido ADD COLUMN cotizacion_id bigint REFERENCES comercial.cotizacion(id);
    ALTER TABLE comercial.pedido ADD COLUMN inventario_id bigint REFERENCES almacen.inventario(id);
    ALTER TABLE comercial.pedido ADD COLUMN estado_stock varchar(30) NOT NULL DEFAULT 'SIN_RESERVA';
    -- El código anterior podía aprobar sin inventario y descontar repetidamente.
    -- No inferir una reserva ni un descuento a partir de un nombre de producto.
    UPDATE comercial.pedido SET estado_stock = 'REVISION_LEGADO'
        WHERE upper(estado) IN ('APROBADO', 'FACTURADO');
    UPDATE comercial.pedido SET estado_stock = 'ENVIADO'
        WHERE upper(estado) IN ('ENVIADO', 'ENTREGADO');
    UPDATE comercial.despacho d SET pedido_id = v.pedido_id FROM comercial.venta v
        WHERE d.venta_id = v.id AND d.pedido_id IS NULL;

    CREATE UNIQUE INDEX pedido_cotizacion_unica ON comercial.pedido(cotizacion_id);
    -- Si existen documentos históricos duplicados, falla toda la migración para revisión.
    CREATE UNIQUE INDEX venta_pedido_unico ON comercial.venta(pedido_id);
    CREATE UNIQUE INDEX despacho_venta_unica ON comercial.despacho(venta_id);
    CREATE UNIQUE INDEX compra_orden_unica ON compras.compra(orden_compra_id);

    ALTER TABLE compras.orden_compra ADD COLUMN presupuesto_periodo varchar(7);
    -- Las órdenes históricas requieren conciliar el presupuesto antes de liberar capital.

    CREATE TABLE compras.solicitud_reposicion (
        id bigserial PRIMARY KEY,
        inventario_id bigint NOT NULL REFERENCES almacen.inventario(id),
        producto varchar(200) NOT NULL,
        cantidad integer NOT NULL CHECK (cantidad > 0),
        estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
        orden_compra_id bigint UNIQUE REFERENCES compras.orden_compra(id),
        creada_en timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        actualizada_en timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        mensaje varchar(500) NOT NULL
    );
    CREATE UNIQUE INDEX reposicion_activa_unica ON compras.solicitud_reposicion(inventario_id)
        WHERE estado IN ('PENDIENTE', 'APROBADA');

    CREATE SEQUENCE comercial.automatizacion_codigo_seq;
    SELECT COALESCE(MAX(substring(codigo FROM '[0-9]+$')::bigint), 0) INTO maximo
    FROM (
        SELECT codigo FROM comercial.pedido UNION ALL SELECT codigo FROM comercial.venta
        UNION ALL SELECT codigo FROM comercial.despacho UNION ALL SELECT codigo FROM compras.orden_compra
    ) codigos WHERE codigo ~ '^(PED|VEN|DES|OC)-[0-9]+$';
    PERFORM setval('comercial.automatizacion_codigo_seq', GREATEST(maximo + 1, 1), false);
    INSERT INTO comercial.migracion_aplicada(nombre) VALUES ('001_automatizacion');
END IF;
END $$;
COMMIT;
