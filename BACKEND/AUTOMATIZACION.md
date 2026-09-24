# Automatizaciones comerciales e inventario

## Flujo implementado

1. Aprobar una cotización crea un pedido pendiente, vinculado por `cotizacion_id`. Conserva el descuento de la cotización; el pedido agrega el IGV según la lógica comercial existente.
2. Aprobar el pedido reserva existencias y crea una venta pendiente. El pedido pasa a `FACTURADO` para mantener el estado utilizado por las pantallas actuales.
3. Aprobar la venta crea un despacho pendiente. La venta se completa al confirmar la entrega.
4. Preparar el despacho y validar su comprobante lo envía: descuenta stock físico y consume la reserva en una misma transacción.
5. Anular pedido, venta o despacho antes del envío anula sus documentos derivados y libera la reserva. Después del envío se rechaza la anulación; las devoluciones quedan fuera de esta entrega.
6. Cuando `stockDisponible = stock - stockReservado` llega al mínimo, se crea una solicitud con `cantidad = stockObjetivo - stockDisponible`. El objetivo inicial es el doble del mínimo y se edita desde Inventario. Si mínimo y objetivo son cero no se genera una solicitud de cantidad cero.
7. Solo ALMACEN o ADMIN pueden aprobar la reposición, seleccionando proveedor activo y precio. La aprobación crea una orden aprobada y compromete presupuesto; no registra una recepción.
8. Confirmar la recepción completa registra la compra, suma stock físico y cierra la solicitud. Si continúa la necesidad, se genera una nueva solicitud. Mientras hay una orden aprobada se conserva una única reposición activa por producto.

Las solicitudes son avisos persistentes visibles en Inventario y Abastecimiento; la bandeja se actualiza cada 30 segundos. La revisión del inventario se ejecuta cada 60 segundos y después de sus movimientos. El intervalo se configura con `automatizacion.reposicion.intervalo-ms`. No se envían correos.

La aprobación incluye la cantidad revisada por el responsable. Si cambió entretanto, se rechaza la operación sin comprometer presupuesto y se solicita actualizar la bandeja.

## Activación sobre una base existente

Esta entrega es local. No se aplicó SQL a Neon ni se desplegó el sistema.

1. Respaldar la base y detener las escrituras del backend anterior durante el cambio.
2. Conectarse a la base que se desea actualizar y ejecutar:

   ```powershell
   psql -v ON_ERROR_STOP=1 -f migrations/001_automatizacion.sql
   ```

   Configurar la conexión mediante los mecanismos habituales de PostgreSQL; no guardar credenciales en el repositorio. `ddl-auto` continúa en `none`: el backend necesita esta migración antes de arrancar con el nuevo código.

3. Arrancar el backend y frontend actualizados juntos. La primera revisión programada generará las solicitudes necesarias para el inventario existente.
4. Conciliar los pedidos y presupuestos históricos indicados abajo antes de continuar esos documentos.

La migración es transaccional e idempotente: no elimina documentos ni modifica `stock`. Crea columnas, secuencia y restricciones. Si existen ventas duplicadas por pedido, despachos duplicados por venta o recepciones duplicadas por orden, falla y revierte completa; revisar esos documentos antes de repetir. También valida que el stock físico no sea negativo.

## Documentos históricos

El código anterior descontaba stock al aprobar, pero también permitía aprobar productos inexistentes y repetir descuentos. Por eso no es seguro inventar una reserva ni devolver existencias automáticamente.

- Los pedidos antiguos `APROBADO` o `FACTURADO` se marcan `REVISION_LEGADO`. Cancelar, crear una venta o enviar un despacho asociado exige conciliación.
- Si se verifica que el pedido ya descontó exactamente su cantidad, asociar el `inventario_id` correcto y cambiar `estado_stock` a `DESCONTADO_LEGADO` mediante una corrección controlada. El nuevo envío no descontará de nuevo; cancelar devolverá esa cantidad una sola vez.
- Si no hubo descuento, corregir y reservar según las existencias verificadas. No cambiar únicamente el estado a `RESERVADO`: ese estado debe coincidir con `inventario.stock_reservado`.
- Se recupera `despacho.pedido_id` a partir de su venta cuando existe esa relación. Los despachos sin pedido requieren conciliación antes del envío.
- Las órdenes antiguas conservan `presupuesto_periodo` vacío. Antes de cancelarlas, determinar el período al que realmente se cargó su presupuesto. Las órdenes nuevas guardan el período y liberan capital allí, aunque se cancelen otro mes.
- No se crean vínculos de cotizaciones históricas convertidas mediante coincidencias de nombre: sus documentos se conservan.

Consulta de revisión:

```sql
SELECT id, codigo, producto, cantidad, estado, estado_stock, inventario_id
FROM comercial.pedido WHERE estado_stock = 'REVISION_LEGADO';
SELECT id, codigo, estado FROM compras.orden_compra WHERE presupuesto_periodo IS NULL;
```

## Consistencia y alcance

Cada flujo se ejecuta dentro de una transacción. Un bloqueo transaccional de PostgreSQL serializa las operaciones comerciales y de inventario entre instancias del backend. Es deliberadamente global para el volumen de este ERP académico; para mayor volumen puede sustituirse por bloqueos por producto y documento con un orden estable.

Una secuencia de PostgreSQL reemplaza los correlativos basados en `MAX + 1` y `COUNT + 1`. Se conserva el prefijo de cada documento y pueden existir saltos. Los índices únicos refuerzan la relación de un documento por origen y una solicitud activa por producto.

Repetir una aprobación, envío, cancelación o recepción vinculada devuelve el resultado ya registrado y no repite el movimiento. Las ventas directas reservan al aprobarse y obtienen un pedido asociado. Los despachos nuevos se generan desde una venta aprobada. Se admiten recepciones completas por orden; recepciones parciales no están incluidas.

Las órdenes conservan su contenido aprobado. Para cambiar producto, cantidad o importe, cancelar la orden antes de recibirla y crear una nueva. La acción de cancelar conserva el documento y libera el presupuesto una sola vez.

## Pruebas

```powershell
mvn test
# Integración: solo una base DESECHABLE llamada automation_test en 127.0.0.1.
# El test crea y elimina las tablas; nunca apuntar a datos de trabajo.
$env:AUTOMATION_TEST_DB_URL='jdbc:postgresql://127.0.0.1:55439/automation_test'
mvn test
```

El usuario de esa base de pruebas es `automation_test`, sin contraseña, en un servidor de pruebas limitado a localhost. Sin la variable se omiten las pruebas de integración. `AutomatizacionIntegrationTest` rechaza otras direcciones o nombres de base.

La prueba SQL de migración usa otra base local nueva, `automation_migration_test`:

```powershell
psql -h 127.0.0.1 -p 55439 -U automation_test -d automation_migration_test -f src/test/resources/migration-check.sql
```

Frontend, desde `FRONTEND`:

```powershell
npm run test:ci -- --include=src/app/features/dashboard/inventario/reposiciones.spec.ts
npm run build
```
