package dsw.sigconbackend.controller;

import dsw.sigconbackend.dto.RegistrarPagoRequest;
import dsw.sigconbackend.model.*;
import dsw.sigconbackend.service.PlanillaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * HU24 — Calcular salarios automáticamente
 * HU25 — Validar asistencia para pago
 * HU26 — Registrar pago de planilla
 *
 * ─── Endpoints ────────────────────────────────────────────────────────────────
 *
 * GET    /planillas                                    → listar todas las planillas
 * GET    /planillas/{id}                               → buscar planilla por id
 * POST   /planillas                                    → HU24: generar planilla con cálculo automático
 * GET    /planillas/{id}/detalles                      → detalles (empleados y sus sueldos)
 * PATCH  /planillas/{id}/detalles/{detalleId}/bonificacion → ajustar bonificación manual
 *
 * PATCH  /planillas/{id}/detalles/{detalleId}/validar  → HU25: validar un empleado
 * POST   /planillas/{id}/validar-todo                  → HU25: aprobar toda la planilla a la vez
 *
 * POST   /planillas/{id}/detalles/{detalleId}/pagar    → HU26: registrar pago individual
 * GET    /planillas/{id}/pagos                         → historial de pagos de la planilla
 * GET    /planillas/empleado/{empleadoId}/pagos        → historial de pagos de un empleado
 */
@RestController
@RequestMapping("/planillas")
public class PlanillaController {

    @Autowired
    private PlanillaService planillaService;

    // ─── Listar / buscar ──────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Planilla>> listar() {
        return ResponseEntity.ok(planillaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id) {
        return planillaService.buscarPorId(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "mensaje", "Planilla no encontrada")));
    }

    @GetMapping("/{id}/detalles")
    public ResponseEntity<List<PlanillaDetalle>> detalles(@PathVariable Long id) {
        return ResponseEntity.ok(planillaService.listarDetalles(id));
    }

    // ─── HU24 — Generar planilla con cálculo automático ──────────────────────

    /**
     * POST /planillas
     * Body: { "periodo": "2025-07", "creadoPorUsuario": 1 }
     *
     * Genera la planilla del mes, lee los registros de asistencia y calcula:
     * descuento por faltas, por tardanzas, AFP (10%), EsSalud (9%) y sueldo neto.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> generar(@RequestBody Map<String, Object> body) {
        try {
            String periodo = (String) body.get("periodo");
            Long usuario   = body.get("creadoPorUsuario") != null
                    ? Long.valueOf(body.get("creadoPorUsuario").toString()) : null;

            if (periodo == null || periodo.isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "mensaje", "Debe indicar el período (YYYY-MM)"));

            Planilla planilla = planillaService.generarPlanilla(periodo, usuario);
            List<PlanillaDetalle> detalles = planillaService.listarDetalles(planilla.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "mensaje", "Planilla generada correctamente para el período " + periodo,
                    "planilla", planilla,
                    "detalles", detalles
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    /** Ajustar la bonificación de un detalle y recalcular el neto */
    @PatchMapping("/{id}/detalles/{detalleId}/bonificacion")
    public ResponseEntity<Map<String, Object>> actualizarBonificacion(
            @PathVariable Long id,
            @PathVariable Long detalleId,
            @RequestBody Map<String, Object> body) {
        try {
            BigDecimal bono = new BigDecimal(body.get("bonificacion").toString());
            PlanillaDetalle det = planillaService.actualizarBonificacion(detalleId, bono);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Bonificación actualizada",
                    "detalle", det
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    // ─── HU25 — Validar asistencia para pago ─────────────────────────────────

    /**
     * PATCH /planillas/{id}/detalles/{detalleId}/validar
     * Body: { "observacion": "Revisado y conforme" }
     *
     * Marca el detalle de un empleado como validado.
     * Cuando todos los detalles están validados, la planilla pasa a "Validado".
     */
    @PatchMapping("/{id}/detalles/{detalleId}/validar")
    public ResponseEntity<Map<String, Object>> validarDetalle(
            @PathVariable Long id,
            @PathVariable Long detalleId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String obs = body != null ? body.get("observacion") : null;
            PlanillaDetalle det = planillaService.validarDetalle(detalleId, obs);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Detalle validado correctamente",
                    "detalle", det
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    /**
     * POST /planillas/{id}/validar-todo
     * Body: { "validadoPorUsuario": 1 }
     *
     * Aprueba toda la planilla de una sola vez (valida todos los detalles).
     */
    @PostMapping("/{id}/validar-todo")
    public ResponseEntity<Map<String, Object>> validarTodo(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Long usuario = body != null && body.get("validadoPorUsuario") != null
                    ? Long.valueOf(body.get("validadoPorUsuario").toString()) : null;
            Planilla planilla = planillaService.validarTodaLaPlanilla(id, usuario);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Planilla aprobada correctamente",
                    "planilla", planilla
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    // ─── HU26 — Registrar pago de planilla ───────────────────────────────────

    /**
     * POST /planillas/{id}/detalles/{detalleId}/pagar
     * Body: { "metodoPago": "Transferencia", "numeroOperacion": "TRF-2025-0718" }
     *
     * Registra el pago de un empleado. Si se omite montoPagado se usa el sueldoNeto.
     * Cuando todos los empleados están pagados, la planilla pasa a "Pagado".
     */
    @PostMapping("/{id}/detalles/{detalleId}/pagar")
    public ResponseEntity<Map<String, Object>> registrarPago(
            @PathVariable Long id,
            @PathVariable Long detalleId,
            @RequestBody RegistrarPagoRequest req) {
        try {
            PagoPlanilla pago = planillaService.registrarPago(detalleId, req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "mensaje", "Pago registrado para " + pago.getEmpleado().getNombre(),
                    "pago", pago
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    /** Historial de pagos de una planilla */
    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<PagoPlanilla>> pagos(@PathVariable Long id) {
        return ResponseEntity.ok(planillaService.listarPagos(id));
    }

    /** Historial de pagos de un empleado (boleta histórica) */
    @GetMapping("/empleado/{empleadoId}/pagos")
    public ResponseEntity<List<PagoPlanilla>> pagosEmpleado(@PathVariable Long empleadoId) {
        return ResponseEntity.ok(planillaService.historialPagoEmpleado(empleadoId));
    }
}
