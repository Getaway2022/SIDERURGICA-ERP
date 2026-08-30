package dsw.sigconbackend.service;

import dsw.sigconbackend.dto.RegistrarPagoRequest;
import dsw.sigconbackend.model.*;
import dsw.sigconbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HU24 — Calcular salarios automáticamente según asistencia.
 * HU25 — Validar asistencia y montos antes del pago.
 * HU26 — Registrar pago de planilla con historial.
 *
 * ── Reglas de negocio ────────────────────────────────────────────────────────
 *
 *  HU24 — Cálculo del sueldo neto:
 *   1. Se leen los registros de asistencia del período (fecha_inicio – fecha_fin).
 *   2. Se calcula automáticamente:
 *      - descuento_faltas    = (salarioBase / diasLaborables) × diasAusente
 *      - descuento_tardanzas = (salarioBase / (diasLaborables × 8h × 60min)) × minutosTardanza
 *      - descuento_afp       = salarioBase × 10 %
 *      - descuento_essalud   = salarioBase × 9 % (informativo, lo paga el empleador)
 *      - total_descuentos    = faltas + tardanzas + AFP
 *      - sueldo_neto         = salarioBase − total_descuentos + bonificacion
 *
 *  HU25 — Validación:
 *   - Solo se puede validar si el estado de la planilla es "Borrador".
 *   - Se puede validar individualmente (por empleado) o validar toda la planilla.
 *   - Cuando todos los detalles están validados, la planilla pasa a "Validado".
 *
 *  HU26 — Pago:
 *   - Solo se puede pagar si el detalle está validado.
 *   - No se puede pagar dos veces el mismo detalle.
 *   - Cuando todos los detalles tienen pago registrado, la planilla pasa a "Pagado".
 */
@Service
public class PlanillaService {

    @Autowired private PlanillaRepository planillaRepository;
    @Autowired private PlanillaDetalleRepository detalleRepository;
    @Autowired private PagoPlanillaRepository pagoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private AsistenciaRepository asistenciaRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // Listar planillas
    // ═════════════════════════════════════════════════════════════════════════

    public List<Planilla> listar() {
        return planillaRepository.findAllByOrderByPeriodoDesc();
    }

    public Optional<Planilla> buscarPorId(Long id) {
        return planillaRepository.findById(id);
    }

    public List<PlanillaDetalle> listarDetalles(Long planillaId) {
        return detalleRepository.findByPlanillaId(planillaId);
    }

    public List<PagoPlanilla> listarPagos(Long planillaId) {
        return pagoRepository.findByPlanillaId(planillaId);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HU24 — Generar planilla con cálculo automático de salarios
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public Planilla generarPlanilla(String periodo, Long creadoPorUsuario) {

        // Validar formato del período
        YearMonth ym;
        try {
            ym = YearMonth.parse(periodo, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            throw new IllegalArgumentException("El período debe tener formato YYYY-MM, ej. 2025-07");
        }

        // No generar si ya existe
        if (planillaRepository.findByPeriodo(periodo).isPresent())
            throw new IllegalArgumentException("Ya existe una planilla para el período " + periodo);

        LocalDate inicio = ym.atDay(1);
        LocalDate fin    = ym.atEndOfMonth();

        // Crear cabecera
        Planilla planilla = new Planilla();
        planilla.setPeriodo(periodo);
        planilla.setFechaInicio(inicio);
        planilla.setFechaFin(fin);
        planilla.setEstado("Borrador");
        planilla.setCreadoPorUsuario(creadoPorUsuario);
        planilla = planillaRepository.save(planilla);

        // Generar detalle para cada empleado activo
        List<Empleado> empleados = empleadoRepository.findAll()
                .stream().filter(Empleado::getActivo).toList();

        for (Empleado emp : empleados) {
            PlanillaDetalle det = calcularDetalle(planilla, emp, inicio, fin);
            detalleRepository.save(det);
        }

        return planilla;
    }

    /**
     * HU24 — Núcleo del cálculo de salario.
     * Lee la asistencia real del período y aplica todas las reglas de negocio.
     */
    private PlanillaDetalle calcularDetalle(Planilla planilla, Empleado emp,
                                             LocalDate inicio, LocalDate fin) {

        // Leer registros de asistencia del período
        List<Asistencia> asistencias = asistenciaRepository.findEntreFechas(inicio, fin)
                .stream().filter(a -> a.getEmpleado().getId().equals(emp.getId())).toList();

        // Contar días laborables del mes (lunes a sábado, sin domingos)
        int diasLaborables = calcularDiasLaborables(inicio, fin);

        int diasPresentes  = (int) asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        int diasTardanza   = (int) asistencias.stream().filter(a -> "Tardanza".equals(a.getEstado())).count();
        int diasAusente    = (int) asistencias.stream().filter(a -> "Ausente".equals(a.getEstado())).count();
        int totalMinutos   = asistencias.stream().mapToInt(a -> a.getMinutosTardanza() != null ? a.getMinutosTardanza() : 0).sum();

        BigDecimal salarioBase = emp.getSalarioBase() != null ? emp.getSalarioBase() : BigDecimal.ZERO;

        // ── Cálculos HU24 ──────────────────────────────────────────────────
        BigDecimal dias = BigDecimal.valueOf(diasLaborables > 0 ? diasLaborables : 1);
        BigDecimal minutosLaboralesMes = dias.multiply(BigDecimal.valueOf(8 * 60)); // 8h × 60min × días

        BigDecimal descFaltas    = salarioBase
                .divide(dias, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(diasAusente))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal descTardanzas = minutosLaboralesMes.compareTo(BigDecimal.ZERO) > 0
                ? salarioBase.divide(minutosLaboralesMes, 10, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(totalMinutos))
                             .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal descAfp       = salarioBase.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descEssalud   = salarioBase.multiply(BigDecimal.valueOf(0.09)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonificacion  = BigDecimal.ZERO; // se puede editar manualmente después

        BigDecimal totalDescuentos = descFaltas.add(descTardanzas).add(descAfp);
        BigDecimal sueldoNeto      = salarioBase.subtract(totalDescuentos).add(bonificacion)
                                                .setScale(2, RoundingMode.HALF_UP);

        PlanillaDetalle det = new PlanillaDetalle();
        det.setPlanilla(planilla);
        det.setEmpleado(emp);
        det.setDiasLaborables(diasLaborables);
        det.setDiasPresentes(diasPresentes);
        det.setDiasTardanza(diasTardanza);
        det.setDiasAusente(diasAusente);
        det.setTotalMinutosTardanza(totalMinutos);
        det.setSalarioBase(salarioBase);
        det.setDescuentoFaltas(descFaltas);
        det.setDescuentoTardanzas(descTardanzas);
        det.setDescuentoAfp(descAfp);
        det.setDescuentoEssalud(descEssalud);
        det.setBonificacion(bonificacion);
        det.setTotalDescuentos(totalDescuentos);
        det.setSueldoNeto(sueldoNeto);
        det.setValidado(false);

        return det;
    }

    /** Cuenta días laborables (lunes a sábado) en el rango dado */
    private int calcularDiasLaborables(LocalDate inicio, LocalDate fin) {
        int count = 0;
        LocalDate d = inicio;
        while (!d.isAfter(fin)) {
            if (d.getDayOfWeek().getValue() < 7) count++; // 1=Lun … 6=Sáb
            d = d.plusDays(1);
        }
        return count;
    }

    /** Permite editar la bonificación de un detalle y recalcula el neto */
    @Transactional
    public PlanillaDetalle actualizarBonificacion(Long detalleId, BigDecimal bonificacion) {
        PlanillaDetalle det = detalleRepository.findById(detalleId)
                .orElseThrow(() -> new IllegalArgumentException("Detalle no encontrado"));
        if (!"Borrador".equals(det.getPlanilla().getEstado()))
            throw new IllegalArgumentException("Solo se puede editar detalles de planillas en estado Borrador");

        det.setBonificacion(bonificacion.setScale(2, RoundingMode.HALF_UP));
        det.setSueldoNeto(det.getSalarioBase()
                .subtract(det.getTotalDescuentos())
                .add(det.getBonificacion())
                .setScale(2, RoundingMode.HALF_UP));
        return detalleRepository.save(det);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HU25 — Validar asistencia y montos antes del pago
    // ═════════════════════════════════════════════════════════════════════════

    /** Valida un detalle individual */
    @Transactional
    public PlanillaDetalle validarDetalle(Long detalleId, String observacion) {
        PlanillaDetalle det = detalleRepository.findById(detalleId)
                .orElseThrow(() -> new IllegalArgumentException("Detalle no encontrado"));

        if (!"Borrador".equals(det.getPlanilla().getEstado()))
            throw new IllegalArgumentException("Solo se pueden validar planillas en estado Borrador");

        det.setValidado(true);
        det.setObservacionValidacion(observacion);
        detalleRepository.save(det);

        // Si todos los detalles de la planilla están validados → la planilla pasa a Validado
        verificarYValidarPlanilla(det.getPlanilla().getId());

        return det;
    }

    /** Valida TODOS los detalles de una planilla de una sola vez */
    @Transactional
    public Planilla validarTodaLaPlanilla(Long planillaId, Long validadoPorUsuario) {
        Planilla planilla = planillaRepository.findById(planillaId)
                .orElseThrow(() -> new IllegalArgumentException("Planilla no encontrada"));

        if (!"Borrador".equals(planilla.getEstado()))
            throw new IllegalArgumentException("La planilla ya fue validada o pagada");

        List<PlanillaDetalle> detalles = detalleRepository.findByPlanillaId(planillaId);
        if (detalles.isEmpty())
            throw new IllegalArgumentException("La planilla no tiene empleados registrados");

        detalles.forEach(d -> {
            d.setValidado(true);
            if (d.getObservacionValidacion() == null)
                d.setObservacionValidacion("Aprobado en revisión masiva");
        });
        detalleRepository.saveAll(detalles);

        planilla.setEstado("Validado");
        planilla.setFechaValidacion(LocalDateTime.now());
        planilla.setValidadoPorUsuario(validadoPorUsuario);
        return planillaRepository.save(planilla);
    }

    /** Verifica si todos los detalles están validados y actualiza la planilla */
    private void verificarYValidarPlanilla(Long planillaId) {
        if (detalleRepository.todosValidados(planillaId)) {
            Planilla p = planillaRepository.findById(planillaId).orElseThrow();
            p.setEstado("Validado");
            p.setFechaValidacion(LocalDateTime.now());
            planillaRepository.save(p);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HU26 — Registrar pago de planilla
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public PagoPlanilla registrarPago(Long detalleId, RegistrarPagoRequest req) {

        PlanillaDetalle det = detalleRepository.findById(detalleId)
                .orElseThrow(() -> new IllegalArgumentException("Detalle de planilla no encontrado"));

        // HU25 — solo se puede pagar si está validado
        if (!det.getValidado())
            throw new IllegalArgumentException(
                    "El detalle de " + det.getEmpleado().getNombre() + " no ha sido validado aún");

        // HU26 — no pagar dos veces
        if (pagoRepository.findByPlanillaDetalleId(detalleId).isPresent())
            throw new IllegalArgumentException(
                    "Ya existe un pago registrado para " + det.getEmpleado().getNombre() + " en este período");

        BigDecimal monto = req.getMontoPagado() != null ? req.getMontoPagado() : det.getSueldoNeto();

        PagoPlanilla pago = new PagoPlanilla();
        pago.setPlanillaDetalle(det);
        pago.setEmpleado(det.getEmpleado());
        pago.setMontoPagado(monto.setScale(2, RoundingMode.HALF_UP));
        pago.setFechaPago(LocalDateTime.now());
        pago.setMetodoPago(req.getMetodoPago() != null ? req.getMetodoPago() : "Transferencia");
        pago.setNumeroOperacion(req.getNumeroOperacion());
        pago.setRegistradoPorUsuario(req.getRegistradoPorUsuario());
        pago.setConfirmado(true);

        PagoPlanilla guardado = pagoRepository.save(pago);

        // Si todos los empleados de la planilla tienen pago → planilla = Pagado
        verificarYMarcarPlanillaPagada(det.getPlanilla());

        return guardado;
    }

    private void verificarYMarcarPlanillaPagada(Planilla planilla) {
        long totalDetalles = detalleRepository.findByPlanillaId(planilla.getId()).size();
        long totalPagos    = pagoRepository.countByPlanillaId(planilla.getId());
        if (totalPagos >= totalDetalles) {
            planilla.setEstado("Pagado");
            planillaRepository.save(planilla);
        }
    }

    // Historial de pagos de un empleado (para boleta)
    public List<PagoPlanilla> historialPagoEmpleado(Long empleadoId) {
        return pagoRepository.findByEmpleadoId(empleadoId);
    }
}
