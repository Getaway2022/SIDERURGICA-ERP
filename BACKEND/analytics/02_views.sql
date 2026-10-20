-- =====================================================================
-- 02_views.sql  |  Indicadores para Power BI (esquema analytics)
-- Uso: psql "$ANALYTICS_URL" -v ON_ERROR_STOP=1 -f 02_views.sql
--
-- Reglas de negocio tomadas de FlujoComercialService:
--  * Una cotización aprobada pasa a CONVERTIDA al instante, por eso
--    "aprobada" = APROBADA + CONVERTIDA.
--  * El stock físico solo baja cuando el despacho pasa a ENVIADO.
--  * Disponible = stock - stock_reservado; bajo stock = disponible <= mínimo.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS analytics;

-- 1. Ventas por mes (excluye anuladas) con variación mensual
CREATE OR REPLACE VIEW analytics.v_ventas_mensual AS
WITH m AS (
  SELECT date_trunc('month', fecha_venta)::date                        AS mes,
         count(*) FILTER (WHERE estado <> 'ANULADO')                   AS num_ventas,
         count(*) FILTER (WHERE estado = 'ANULADO')                    AS num_anuladas,
         COALESCE(sum(cantidad) FILTER (WHERE estado <> 'ANULADO'), 0) AS unidades,
         COALESCE(sum(subtotal) FILTER (WHERE estado <> 'ANULADO'), 0) AS ventas_netas,
         COALESCE(sum(total)    FILTER (WHERE estado <> 'ANULADO'), 0) AS ventas_con_igv,
         round(avg(subtotal)    FILTER (WHERE estado <> 'ANULADO'), 2) AS ticket_promedio
  FROM comercial.venta
  GROUP BY 1
)
SELECT m.*,
       (m.mes < date_trunc('month', current_date)::date) AS mes_completo,
       round(100.0 * (m.ventas_netas / NULLIF(lag(m.ventas_netas) OVER (ORDER BY m.mes), 0) - 1), 1)
         AS variacion_pct
FROM m;

-- 2. Productos más vendidos, con participación y acumulado (Pareto)
CREATE OR REPLACE VIEW analytics.v_top_productos AS
WITH t AS (
  SELECT COALESCE(i.producto, v.producto) AS producto,
         i.categoria,
         sum(v.cantidad)  AS unidades,
         sum(v.subtotal)  AS ventas_netas,
         count(*)         AS num_ventas
  FROM comercial.venta v
  LEFT JOIN comercial.pedido    p ON p.id = v.pedido_id
  LEFT JOIN almacen.inventario  i ON i.id = p.inventario_id
  WHERE v.estado <> 'ANULADO'
  GROUP BY 1, 2
)
SELECT producto, categoria, unidades, ventas_netas, num_ventas,
       rank() OVER (ORDER BY ventas_netas DESC) AS ranking,
       round(100.0 * ventas_netas / sum(ventas_netas) OVER (), 1) AS participacion_pct,
       round(100.0 * sum(ventas_netas) OVER (ORDER BY ventas_netas DESC ROWS UNBOUNDED PRECEDING)
                   / sum(ventas_netas) OVER (), 1) AS acumulado_pct
FROM t;

-- 3. Cotizaciones aprobadas vs. rechazadas por mes
CREATE OR REPLACE VIEW analytics.v_cotizaciones_estado AS
WITH m AS (
  SELECT date_trunc('month', fecha_registro)::date AS mes,
         count(*)                                                        AS total,
         count(*) FILTER (WHERE estado IN ('APROBADA', 'CONVERTIDA'))    AS aprobadas,
         count(*) FILTER (WHERE estado = 'RECHAZADA')                    AS rechazadas,
         count(*) FILTER (WHERE estado = 'PENDIENTE')                    AS pendientes,
         COALESCE(sum(total) FILTER (WHERE estado IN ('APROBADA', 'CONVERTIDA')), 0) AS monto_aprobado,
         COALESCE(sum(total) FILTER (WHERE estado = 'RECHAZADA'), 0)     AS monto_rechazado
  FROM comercial.cotizacion
  GROUP BY 1
)
SELECT m.*,
       round(100.0 * aprobadas / NULLIF(aprobadas + rechazadas, 0), 1) AS tasa_aprobacion_pct
FROM m;

-- 4. Productos con stock disponible por debajo (o igual) al mínimo
CREATE OR REPLACE VIEW analytics.v_stock_bajo_minimo AS
SELECT i.id AS inventario_id, i.producto, i.categoria, i.unidad,
       i.stock, i.stock_reservado,
       (i.stock - i.stock_reservado) AS stock_disponible,
       i.stock_minimo, i.stock_objetivo,
       GREATEST(i.stock_objetivo - (i.stock - i.stock_reservado), 0) AS cantidad_a_reponer,
       round(100.0 * (i.stock - i.stock_reservado) / NULLIF(i.stock_minimo, 0), 0) AS pct_del_minimo,
       s.estado AS estado_reposicion,
       s.orden_compra_id
FROM almacen.inventario i
LEFT JOIN compras.solicitud_reposicion s
       ON s.inventario_id = i.id AND s.estado IN ('PENDIENTE', 'APROBADA')
WHERE i.stock_minimo IS NOT NULL
  AND (i.stock - i.stock_reservado) <= i.stock_minimo;

-- 5. Pedidos que aún no salieron, con la etapa donde están detenidos
CREATE OR REPLACE VIEW analytics.v_pedidos_sin_despachar AS
SELECT p.id AS pedido_id, p.codigo, p.cliente, p.producto, p.cantidad, p.total,
       p.estado AS estado_pedido, p.estado_stock,
       p.fecha_registro::date AS fecha_pedido,
       (current_date - p.fecha_registro::date) AS dias_espera,
       CASE
         WHEN (current_date - p.fecha_registro::date) <= 3  THEN '0-3 días'
         WHEN (current_date - p.fecha_registro::date) <= 7  THEN '4-7 días'
         WHEN (current_date - p.fecha_registro::date) <= 15 THEN '8-15 días'
         ELSE 'Más de 15 días'
       END AS rango_espera,
       CASE
         WHEN p.estado = 'PENDIENTE'  THEN '1. Pedido sin aprobar'
         WHEN v.id IS NULL            THEN '2. Aprobado sin venta'
         WHEN v.estado = 'PENDIENTE'  THEN '3. Venta sin aprobar'
         WHEN d.id IS NULL            THEN '4. Sin despacho'
         WHEN d.estado = 'PENDIENTE'  THEN '5. Despacho pendiente'
         WHEN d.estado = 'PREPARADO'  THEN '6. Preparado, sin enviar'
         ELSE '7. Otro'
       END AS etapa
FROM comercial.pedido p
LEFT JOIN comercial.venta    v ON v.pedido_id = p.id AND v.estado <> 'ANULADO'
LEFT JOIN comercial.despacho d ON d.pedido_id = p.id AND d.estado <> 'ANULADO'
WHERE p.estado IN ('PENDIENTE', 'APROBADO', 'FACTURADO');

-- 6. Entradas (compras recibidas) y salidas (despachos enviados) por mes y producto
-- No existe tabla de movimientos: se derivan de compra y despacho.
-- Los ajustes manuales (PATCH /inventario/:id/stock) no quedan registrados.
CREATE OR REPLACE VIEW analytics.v_movimientos_stock AS
SELECT m.mes, i.producto, i.categoria, i.unidad,
       sum(m.entrada) AS entradas,
       sum(m.salida)  AS salidas,
       sum(m.entrada) - sum(m.salida) AS neto
FROM (
  SELECT date_trunc('month', c.fecha_compra)::date AS mes, c.inventario_id,
         c.cantidad AS entrada, 0 AS salida
  FROM compras.compra c
  UNION ALL
  SELECT date_trunc('month', d.fecha_despacho)::date, p.inventario_id, 0, d.cantidad
  FROM comercial.despacho d
  JOIN comercial.pedido p ON p.id = d.pedido_id
  WHERE d.estado IN ('ENVIADO', 'ENTREGADO')
) m
JOIN almacen.inventario i ON i.id = m.inventario_id
GROUP BY m.mes, i.producto, i.categoria, i.unidad;