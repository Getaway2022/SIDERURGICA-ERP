-- =====================================================================
-- 01_seed_demo.sql  |  Datos de demostración para analítica (12 meses)
--
-- SOLO PARA LA BRANCH DE ANALÍTICA DE NEON. BORRA los datos operativos
-- (cotizaciones, pedidos, ventas, despachos, compras, inventario).
--
-- Uso: pegar completo en el SQL Editor de Neon (o ejecutar con psql -f).
-- ANTES de ejecutar, verifica que arriba a la izquierda diga la branch 'analytics'.
-- No usa comandos de psql, funciona en el editor web.
--
-- Los datos respetan las transiciones de FlujoComercialService:
--   cotización APROBADA -> se convierte de inmediato en CONVERTIDA + pedido
--   pedido aprobado     -> RESERVADO (stock_reservado) + venta, pedido FACTURADO
--   despacho ENVIADO    -> único momento en que baja stock y reserva
--   anulación           -> LIBERADO
-- =====================================================================
BEGIN;
SELECT setseed(0.42);  -- datos reproducibles

TRUNCATE comercial.despacho, comercial.venta, comercial.pedido, comercial.cotizacion,
         compras.solicitud_reposicion, compras.compra, compras.orden_compra,
         almacen.inventario
RESTART IDENTITY CASCADE;

-- ---------- Proveedores demo (RUC ficticios) ----------
INSERT INTO compras.proveedor (nombre, origen, ruc, contacto, telefono, categoria, calificacion, estado)
VALUES
 ('Aceros del Norte S.A.C. (demo)', 'Perú',  '20999000011', 'Contacto Demo 1', '900000001', 'Acero largo', 4.5, 'Activo'),
 ('Laminados Andinos S.A. (demo)',  'Perú',  '20999000012', 'Contacto Demo 2', '900000002', 'Planchas',    4.2, 'Activo'),
 ('Importadora Pacífico (demo)',    'Chile', '20999000013', 'Contacto Demo 3', '900000003', 'Perfiles',    4.0, 'Activo'),
 ('Ferrocomercial Sur (demo)',      'Perú',  '20999000014', 'Contacto Demo 4', '900000004', 'Ferretería',  3.8, 'Activo')
ON CONFLICT (ruc) DO NOTHING;

-- ---------- Catálogo de productos ----------
-- peso = popularidad relativa (Pareto: 3 productos concentran ~2/3 de las ventas)
-- bajo = producto que debe quedar bajo el mínimo (PENDIENTE = sin atender, APROBADA = ya con orden de compra)
CREATE TEMP TABLE t_prod (id int, producto text, categoria text, unidad text,
    precio numeric, peso numeric, qmin int, qmax int, bajo text) ON COMMIT DROP;
INSERT INTO t_prod VALUES
 (1,  'Barra corrugada 1/2" x 9 m',    'Construcción', 'varilla', 32.50, 34,  20, 400, NULL),
 (2,  'Barra corrugada 3/8" x 9 m',    'Construcción', 'varilla', 19.80, 22,  20, 400, NULL),
 (3,  'Barra corrugada 5/8" x 9 m',    'Construcción', 'varilla', 50.90, 12,  10, 250, NULL),
 (4,  'Alambrón 5.5 mm',               'Alambres',     'kg',       3.60,  8, 200,2000, NULL),
 (5,  'Alambre negro N°16',            'Alambres',     'kg',       4.90,  5,  50, 600, NULL),
 (6,  'Plancha LAC 3 mm 1.22x2.44',    'Planchas',     'plancha', 210.00, 7,   5,  60, NULL),
 (7,  'Plancha LAF 6 mm 1.22x2.44',    'Planchas',     'plancha', 395.00, 4,   3,  40, 'PENDIENTE'),
 (8,  'Tubo cuadrado 40x40x2 mm',      'Tubería',      'tubo',     58.00, 4,  10, 120, NULL),
 (9,  'Ángulo 2" x 1/4" x 6 m',        'Perfiles',     'barra',    92.00, 3,   5,  80, 'PENDIENTE'),
 (10, 'Viga H 150 x 6 m',              'Perfiles',     'viga',    720.00, 2,   2,  20, 'APROBADA'),
 (11, 'Platina 1" x 1/8" x 6 m',       'Perfiles',     'barra',    24.00, 1.5,10, 150, NULL),
 (12, 'Clavos 3"',                     'Ferretería',   'kg',        5.20, 1.5,20, 300, NULL);

CREATE TEMP TABLE t_prodw ON COMMIT DROP AS
SELECT *, sum(peso) OVER (ORDER BY id) AS acum FROM t_prod;

-- Con RESTART IDENTITY los ids generados son 1..12, iguales a t_prod.id
INSERT INTO almacen.inventario (producto, categoria, stock, stock_reservado, stock_minimo,
                                stock_objetivo, precio_unitario, unidad, fecha_actualizacion)
SELECT producto, categoria, 0, 0, 0, 0, precio, unidad, localtimestamp
FROM t_prod ORDER BY id;   -- stock real se calcula al final

-- ---------- Clientes ficticios ----------
CREATE TEMP TABLE t_cli (idx int, nombre text, ruc text) ON COMMIT DROP;
INSERT INTO t_cli
SELECT i::int, n, '20' || lpad((410000000 + i * 7919317)::text, 9, '0')
FROM unnest(ARRAY[
 'Constructora Andina (demo)', 'Inmobiliaria Pacífico (demo)', 'Ingeniería y Obras Lima (demo)',
 'Metalmecánica Rímac (demo)', 'Edificaciones del Sur (demo)', 'Ferretería Central (demo)',
 'Grupo Constructor Norte (demo)', 'Estructuras Metálicas Perú (demo)', 'Proyectos Viales SAC (demo)',
 'Contratistas Unidos (demo)', 'Cimentaciones Peruanas (demo)', 'Obras y Servicios Callao (demo)',
 'Maestranza Industrial (demo)', 'Techos y Coberturas (demo)', 'Vivienda Segura SAC (demo)',
 'Minera Santa Rosa (demo)', 'Agroindustrial Piura (demo)', 'Puentes y Calzadas (demo)',
 'Galpones del Perú (demo)', 'Constructora Horizonte (demo)'
]) WITH ORDINALITY AS u(n, i);

-- ---------- Calendario (desde el día 1 del mes inicial, así el primer mes es completo)
--            con estacionalidad, crecimiento y domingos casi sin actividad ----------
CREATE TEMP TABLE t_dias ON COMMIT DROP AS
SELECT dia, sum(w) OVER (ORDER BY dia) AS acum
FROM (
  SELECT d::date AS dia,
    (CASE extract(month FROM d)::int
       WHEN 1 THEN 0.8 WHEN 2 THEN 0.7 WHEN 3 THEN 1.0 WHEN 4 THEN 1.1 WHEN 5 THEN 1.2
       WHEN 6 THEN 1.1 WHEN 7 THEN 1.0 WHEN 8 THEN 1.1 WHEN 9 THEN 1.2 WHEN 10 THEN 1.3
       WHEN 11 THEN 1.3 ELSE 0.9 END)
    * (1 + 0.25 * (d::date - (current_date - 365)) / 365.0)
    * (CASE WHEN extract(dow FROM d) = 0 THEN 0.1 ELSE 1 END) AS w
  FROM generate_series(date_trunc('month', (current_date - 365)::timestamp), (current_date - 1)::timestamp, interval '1 day') d
) x;

-- ---------- Cotizaciones (1800) ----------
CREATE TEMP TABLE t_cot ON COMMIT DROP AS
WITH g AS (
  SELECT n, random() r_dia, random() r_prod, random() r_cli, random() r_cant,
         random() r_desc, random() r_est, random() r_hora, random() r_prec
  FROM generate_series(1, 1800) n
), s AS (
  SELECT g.*, d.dia, p.id AS inv_id, p.producto, p.precio, p.qmin, p.qmax,
         c.nombre AS cliente, c.ruc
  FROM g
  JOIN LATERAL (SELECT dia FROM t_dias
                WHERE acum >= g.r_dia * (SELECT max(acum) FROM t_dias)
                ORDER BY acum LIMIT 1) d ON true
  JOIN LATERAL (SELECT id, producto, precio, qmin, qmax FROM t_prodw
                WHERE acum >= g.r_prod * (SELECT max(acum) FROM t_prodw)
                ORDER BY acum LIMIT 1) p ON true
  JOIN t_cli c ON c.idx = 1 + floor(g.r_cli * 20)::int
), q AS (
  SELECT s.*, (qmin + floor(r_cant * (qmax - qmin + 1)))::int AS cant,
         round((precio * (0.95 + 0.15 * r_prec))::numeric, 2) AS precio_u,
         (ARRAY[0,0,0,0,3,5,8,10])[1 + floor(r_desc * 8)::int] AS pct
  FROM s
), t AS (
  SELECT q.*, round(cant * precio_u, 2) AS subtotal FROM q
), u AS (
  SELECT t.*, round(subtotal * pct / 100.0, 2) AS desc_monto FROM t
)
SELECT inv_id, producto, cliente, ruc, cant, precio_u, subtotal, pct, desc_monto,
       subtotal - desc_monto AS total,
       dia::timestamp + interval '8 hours' + r_hora * interval '10 hours' AS fecha,
       CASE
         WHEN current_date - dia < 6 THEN
           CASE WHEN r_est < 0.15 THEN 'APROBADA' WHEN r_est < 0.25 THEN 'RECHAZADA' ELSE 'PENDIENTE' END
         WHEN current_date - dia < 20 THEN
           CASE WHEN r_est < 0.35 THEN 'APROBADA' WHEN r_est < 0.55 THEN 'RECHAZADA' ELSE 'PENDIENTE' END
         ELSE
           CASE WHEN r_est < 0.40 THEN 'APROBADA' WHEN r_est < 0.82 THEN 'RECHAZADA' ELSE 'PENDIENTE' END
       END AS estado
FROM u;

INSERT INTO comercial.cotizacion (codigo, cliente, ruc, producto, cantidad, inventario_id, precio_unitario,
    subtotal, descuento_porcentaje, descuento_monto, total, estado, fecha_registro)
SELECT 'COT-' || lpad(row_number() OVER (ORDER BY fecha)::text, 6, '0'),
       cliente, ruc, producto, cant, inv_id, precio_u, subtotal, pct, desc_monto, total, estado, fecha
FROM t_cot ORDER BY fecha;

-- ---------- Pedidos: uno por cotización aprobada ----------
CREATE TEMP TABLE t_ped ON COMMIT DROP AS
WITH b AS (
  SELECT c.id AS cot_id, c.cliente, c.ruc, c.producto, c.cantidad, c.inventario_id, c.precio_unitario,
         c.total AS subtotal, round(c.total * 0.18, 2) AS igv,
         least(c.fecha_registro + random() * interval '2 days', localtimestamp - interval '2 hours') AS fecha_ped,
         random() r_est, random() r_ven, random() r_ret, random() r_vend, random() r_tr
  FROM comercial.cotizacion c
  WHERE c.estado = 'APROBADA'
), e AS (
  SELECT b.*, (current_date - b.fecha_ped::date) AS edad FROM b
), s1 AS (
  SELECT e.*,
    CASE
      WHEN r_est < 0.07 THEN 'ANULADO'
      WHEN edad >= 45 THEN CASE WHEN r_est < 0.085 THEN 'FACTURADO' ELSE 'ENTREGADO' END         -- ~1.5% atascados
      WHEN edad >= 10 THEN CASE WHEN r_est < 0.14 THEN 'FACTURADO' WHEN r_est < 0.30 THEN 'ENVIADO' ELSE 'ENTREGADO' END
      WHEN edad >= 3  THEN CASE WHEN r_est < 0.12 THEN 'PENDIENTE' WHEN r_est < 0.50 THEN 'FACTURADO' ELSE 'ENVIADO' END
      ELSE                 CASE WHEN r_est < 0.30 THEN 'PENDIENTE' ELSE 'FACTURADO' END
    END AS estado
  FROM e
), s2 AS (
  SELECT s1.*,
    CASE estado
      WHEN 'PENDIENTE' THEN 'SIN_RESERVA'
      WHEN 'FACTURADO' THEN 'RESERVADO'
      WHEN 'ANULADO'   THEN CASE WHEN r_ven < 0.3 THEN 'SIN_RESERVA' ELSE 'LIBERADO' END
      ELSE 'ENVIADO'   -- ENVIADO y ENTREGADO conservan estado_stock = ENVIADO
    END AS estado_stock,
    CASE estado
      WHEN 'PENDIENTE' THEN NULL
      WHEN 'FACTURADO' THEN CASE WHEN r_ven < 0.25 THEN 'PENDIENTE' ELSE 'APROBADO' END
      WHEN 'ENVIADO'   THEN 'APROBADO'
      WHEN 'ENTREGADO' THEN 'COMPLETADO'
      WHEN 'ANULADO'   THEN CASE WHEN r_ven < 0.3 THEN NULL ELSE 'ANULADO' END
    END AS venta_estado
  FROM s1
)
SELECT s2.*,
  CASE
    WHEN estado = 'ENVIADO'   THEN 'ENVIADO'
    WHEN estado = 'ENTREGADO' THEN 'ENTREGADO'
    WHEN estado = 'FACTURADO' AND venta_estado = 'APROBADO'
         THEN CASE WHEN r_ret < 0.5 THEN 'PENDIENTE' ELSE 'PREPARADO' END
    WHEN estado = 'ANULADO' AND venta_estado IS NOT NULL AND r_ret < 0.5 THEN 'ANULADO'
  END AS desp_estado,
  CASE WHEN estado IN ('ENVIADO', 'ENTREGADO')
       THEN least(fecha_ped + interval '1 day' + r_tr * interval '2 days', localtimestamp - interval '30 minutes')
  END AS fecha_desp,
  CASE WHEN estado = 'ENTREGADO'
       THEN least(fecha_ped + interval '2 days' + r_tr * interval '2 days' + r_ret * interval '3 days',
                  localtimestamp - interval '10 minutes')
  END AS fecha_ent
FROM s2;

INSERT INTO comercial.pedido (codigo, cliente, ruc, producto, cantidad, cotizacion_id, inventario_id,
    estado_stock, precio_unitario, subtotal, igv, total, estado, fecha_registro)
SELECT 'PED-' || lpad(row_number() OVER (ORDER BY fecha_ped)::text, 6, '0'),
       cliente, ruc, producto, cantidad, cot_id, inventario_id, estado_stock,
       precio_unitario, subtotal, igv, subtotal + igv, estado, fecha_ped
FROM t_ped ORDER BY fecha_ped;

UPDATE comercial.cotizacion SET estado = 'CONVERTIDA'
WHERE estado = 'APROBADA'
  AND EXISTS (SELECT 1 FROM comercial.pedido p WHERE p.cotizacion_id = comercial.cotizacion.id);

-- ---------- Ventas ----------
INSERT INTO comercial.venta (codigo, cliente, ruc, producto, cantidad, precio_unitario, subtotal, igv, total,
    vendedor, estado, fecha_venta, pedido_id, cotizacion_id)
SELECT 'VEN-' || lpad(row_number() OVER (ORDER BY t.fecha_ped)::text, 6, '0'),
       p.cliente, p.ruc, p.producto, p.cantidad, p.precio_unitario, p.subtotal, p.igv, p.total,
       (ARRAY['Carla Rojas', 'Luis Mendoza', 'Marta Quispe', 'Jorge Salazar'])[1 + floor(t.r_vend * 4)::int],
       t.venta_estado, t.fecha_ped, p.id, t.cot_id
FROM t_ped t
JOIN comercial.pedido p ON p.cotizacion_id = t.cot_id
WHERE t.venta_estado IS NOT NULL
ORDER BY t.fecha_ped;

-- ---------- Despachos ----------
INSERT INTO comercial.despacho (codigo, cliente, ruc, producto, cantidad, direccion, transportista, estado,
    venta_id, pedido_id, fecha_despacho, fecha_entrega, comprobante, comprobante_validado,
    fecha_validacion_comprobante)
SELECT 'DES-' || lpad(row_number() OVER (ORDER BY t.fecha_ped)::text, 6, '0'),
       v.cliente, v.ruc, v.producto, v.cantidad, 'Lima (dirección demo)',
       (ARRAY['Transportes Rímac (demo)', 'Logística Andes (demo)', 'Fletes Lima Sur (demo)'])[1 + floor(t.r_tr * 3)::int],
       t.desp_estado, v.id, p.id,
       COALESCE(t.fecha_desp, t.fecha_ped + interval '1 hour'),
       t.fecha_ent,
       CASE WHEN t.desp_estado IN ('ENVIADO', 'ENTREGADO') THEN 'GR-DEMO-' || p.id END,
       t.desp_estado IN ('ENVIADO', 'ENTREGADO'),
       t.fecha_desp
FROM t_ped t
JOIN comercial.pedido p ON p.cotizacion_id = t.cot_id
JOIN comercial.venta  v ON v.pedido_id = p.id
WHERE t.desp_estado IS NOT NULL
ORDER BY t.fecha_ped;

-- ---------- Compras: reponen lo que se despacha, mes a mes ----------
CREATE TEMP TABLE t_res ON COMMIT DROP AS
SELECT inventario_id, sum(cantidad)::int AS q
FROM comercial.pedido WHERE estado_stock = 'RESERVADO' GROUP BY 1;

CREATE TEMP TABLE t_sal ON COMMIT DROP AS
SELECT p.inventario_id, date_trunc('month', d.fecha_despacho) AS mes, sum(d.cantidad)::int AS q
FROM comercial.despacho d JOIN comercial.pedido p ON p.id = d.pedido_id
WHERE d.estado IN ('ENVIADO', 'ENTREGADO')
GROUP BY 1, 2;

CREATE TEMP TABLE t_oc ON COMMIT DROP AS
WITH prov AS (
  SELECT array_agg(id ORDER BY id) AS ids FROM compras.proveedor
  WHERE ruc BETWEEN '20999000011' AND '20999000014'
), s AS (
  SELECT p.id AS inv_id, p.producto, p.precio, p.qmin, x.mes, x.q,
         random() r_q, random() r_f, random() r_p
  FROM t_sal x JOIN t_prodw p ON p.id = x.inventario_id
), c AS (
  SELECT s.*,
         (ceil(greatest(q * (0.9 + 0.5 * r_q), qmin * 2) / 10.0) * 10)::int AS cantidad,
         least(mes + r_f * 24 * interval '1 day', localtimestamp - interval '5 days') AS fecha_oc,
         round((precio * 0.72 * (0.97 + 0.06 * r_p))::numeric, 2) AS costo
  FROM s
)
SELECT c.inv_id, c.producto, c.cantidad, c.costo, c.fecha_oc, c.r_p,
       round(c.cantidad * c.costo, 2) AS total,
       prov.ids[1 + (c.inv_id % 4)] AS proveedor_id,
       row_number() OVER (ORDER BY c.fecha_oc) AS seq,
       CASE WHEN c.fecha_oc >= localtimestamp - interval '8 days'
            THEN CASE WHEN c.r_p < 0.34 THEN 'Pendiente'
                      WHEN c.r_p < 0.67 THEN 'Aprobada'
                      ELSE 'En tránsito' END
            ELSE 'Recibida'
       END AS estado
FROM c CROSS JOIN prov;

INSERT INTO compras.orden_compra (codigo, proveedor_id, producto, cantidad, total, presupuesto_periodo,
    fecha_entrega, estado, created_at, updated_at)
SELECT 'OC-' || lpad(seq::text, 6, '0'), proveedor_id, producto, cantidad::text, total,
       to_char(fecha_oc, 'YYYY-MM'), (fecha_oc + interval '5 days')::date, estado, fecha_oc, fecha_oc
FROM t_oc ORDER BY fecha_oc;

INSERT INTO compras.compra (orden_compra_id, proveedor_id, inventario_id, producto, cantidad,
    precio_unitario, total, fecha_compra)
SELECT oc.id, t.proveedor_id, t.inv_id, t.producto, t.cantidad, t.costo, t.total,
       least(t.fecha_oc + interval '4 days' + t.r_p * interval '3 days', localtimestamp - interval '1 hour')
FROM t_oc t
JOIN compras.orden_compra oc ON oc.codigo = 'OC-' || lpad(t.seq::text, 6, '0')
WHERE t.estado = 'Recibida';

-- ---------- Stock final coherente con las reservas ----------
-- stock = reservado + disponible; los productos 'bajo' quedan bajo el mínimo
CREATE TEMP TABLE t_fin ON COMMIT DROP AS
SELECT p.id, COALESCE(r.q, 0) AS reservado, d.disp,
       CASE WHEN p.bajo IS NOT NULL
            THEN d.disp + greatest(5, ceil(p.qmax * 0.1)::int)
            ELSE greatest(round(d.disp * 0.2)::int, 5)
       END AS minimo
FROM t_prodw p
LEFT JOIN t_res r ON r.inventario_id = p.id
CROSS JOIN LATERAL (
  SELECT CASE WHEN p.bajo IS NOT NULL THEN floor(p.qmax * 0.25)::int
              ELSE round(p.qmax * (2 + 6 * random()))::int END AS disp
) d;

UPDATE almacen.inventario i
SET stock = f.reservado + f.disp,
    stock_reservado = f.reservado,
    stock_minimo = f.minimo,
    stock_objetivo = f.minimo * 2,
    fecha_actualizacion = localtimestamp - random() * interval '10 days'
FROM t_fin f WHERE i.id = f.id;

-- ---------- Solicitudes de reposición ----------
INSERT INTO compras.solicitud_reposicion (inventario_id, producto, cantidad, estado, mensaje, creada_en, actualizada_en)
SELECT i.id, i.producto, i.stock_objetivo - (i.stock - i.stock_reservado), 'PENDIENTE',
       'Stock disponible: ' || (i.stock - i.stock_reservado) || '. Reponer '
         || (i.stock_objetivo - (i.stock - i.stock_reservado)) || ' para alcanzar el objetivo '
         || i.stock_objetivo || '. Requiere aprobación de almacén.',
       localtimestamp - interval '2 days', localtimestamp - interval '2 days'
FROM almacen.inventario i JOIN t_prodw p ON p.id = i.id
WHERE p.bajo = 'PENDIENTE';

-- Reposición ya aprobada: tiene orden de compra vigente
INSERT INTO compras.orden_compra (codigo, proveedor_id, producto, cantidad, total, presupuesto_periodo,
    fecha_entrega, estado, created_at, updated_at)
SELECT 'OC-' || lpad(((SELECT count(*) FROM t_oc) + 1)::text, 6, '0'),
       (SELECT min(id) FROM compras.proveedor WHERE ruc = '20999000011'),
       i.producto,
       (i.stock_objetivo - (i.stock - i.stock_reservado))::text,
       round((i.stock_objetivo - (i.stock - i.stock_reservado)) * i.precio_unitario * 0.72, 2),
       to_char(localtimestamp, 'YYYY-MM'), (localtimestamp + interval '7 days')::date,
       'Aprobada', localtimestamp - interval '3 days', localtimestamp - interval '3 days'
FROM almacen.inventario i JOIN t_prodw p ON p.id = i.id
WHERE p.bajo = 'APROBADA';

INSERT INTO compras.solicitud_reposicion (inventario_id, producto, cantidad, estado, orden_compra_id, mensaje,
    creada_en, actualizada_en)
SELECT i.id, i.producto, i.stock_objetivo - (i.stock - i.stock_reservado), 'APROBADA', oc.id,
       'Reposición aprobada. Orden ' || oc.codigo || ' pendiente de recepción.',
       localtimestamp - interval '4 days', localtimestamp - interval '3 days'
FROM almacen.inventario i
JOIN t_prodw p ON p.id = i.id AND p.bajo = 'APROBADA'
JOIN compras.orden_compra oc ON oc.codigo = 'OC-' || lpad(((SELECT count(*) FROM t_oc) + 1)::text, 6, '0');

-- Historial de solicitudes ya recibidas (estado y mensaje que escribe CompraService)
INSERT INTO compras.solicitud_reposicion (inventario_id, producto, cantidad, estado, mensaje, creada_en, actualizada_en)
SELECT i.id, i.producto, 60, 'RECIBIDA',
       'Compra recibida e inventario actualizado.',
       localtimestamp - interval '40 days', localtimestamp - interval '35 days'
FROM almacen.inventario i WHERE i.id IN (1, 4);

-- ---------- Secuencia de códigos (igual que la migración 001) ----------
SELECT setval('comercial.automatizacion_codigo_seq',
  GREATEST((SELECT COALESCE(max(substring(codigo FROM '[0-9]+$')::bigint), 0) + 1 FROM (
      SELECT codigo FROM comercial.pedido UNION ALL SELECT codigo FROM comercial.venta
      UNION ALL SELECT codigo FROM comercial.despacho UNION ALL SELECT codigo FROM compras.orden_compra
  ) c WHERE codigo ~ '^(PED|VEN|DES|OC)-[0-9]+$'), 1), false);

-- ---------- Verificaciones de coherencia ----------
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM almacen.inventario i
             WHERE i.stock_reservado <> COALESCE((SELECT sum(p.cantidad) FROM comercial.pedido p
                   WHERE p.inventario_id = i.id AND p.estado_stock = 'RESERVADO'), 0)) THEN
    RAISE EXCEPTION 'stock_reservado no coincide con los pedidos RESERVADO';
  END IF;
  IF EXISTS (SELECT 1 FROM comercial.pedido p WHERE p.estado IN ('ENVIADO', 'ENTREGADO')
             AND NOT EXISTS (SELECT 1 FROM comercial.despacho d WHERE d.pedido_id = p.id
                             AND d.estado IN ('ENVIADO', 'ENTREGADO'))) THEN
    RAISE EXCEPTION 'Hay pedidos enviados sin despacho enviado';
  END IF;
END $$;

COMMIT;

-- Resumen rápido
SELECT 'cotizacion' AS tabla, estado, count(*) FROM comercial.cotizacion GROUP BY estado
UNION ALL SELECT 'pedido', estado, count(*) FROM comercial.pedido GROUP BY estado
UNION ALL SELECT 'venta', estado, count(*) FROM comercial.venta GROUP BY estado
UNION ALL SELECT 'despacho', estado, count(*) FROM comercial.despacho GROUP BY estado
UNION ALL SELECT 'orden_compra', estado, count(*) FROM compras.orden_compra GROUP BY estado
ORDER BY 1, 2;