package dsw.sigconbackend.service;

import dsw.sigconbackend.dto.AsistenciaRequest;
import dsw.sigconbackend.model.Asistencia;
import dsw.sigconbackend.model.Empleado;
import dsw.sigconbackend.repository.AsistenciaRepository;
import dsw.sigconbackend.repository.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * HU21 — Registrar asistencia del personal
 *
 * Reglas de negocio:
 *  - Si no se envía hora de entrada -> estado "Ausente".
 *  - Si la hora de entrada es posterior a la hora esperada del empleado -> "Tardanza"
 *    (se calculan los minutos de diferencia).
 *  - En cualquier otro caso -> "Presente".
 *  - Solo se permite un registro por empleado por día.
 */
@Service
public class AsistenciaService {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    private static final DateTimeFormatter HORA_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public List<Asistencia> listarPorFecha(LocalDate fecha) {
        return asistenciaRepository.findByFecha(fecha != null ? fecha : LocalDate.now());
    }

    public List<Asistencia> historialPorEmpleado(Long empleadoId) {
        return asistenciaRepository.findByEmpleadoId(empleadoId);
    }

    public Asistencia registrar(AsistenciaRequest req) {
        if (req.getEmpleadoId() == null)
            throw new IllegalArgumentException("Debe indicar el empleado.");

        Empleado empleado = empleadoRepository.findById(req.getEmpleadoId())
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado."));

        LocalDate hoy = LocalDate.now();

        Optional<Asistencia> existente = asistenciaRepository.findByEmpleadoIdAndFecha(empleado.getId(), hoy);
        if (existente.isPresent())
            throw new IllegalArgumentException("Ya se registró la asistencia de " + empleado.getNombre() + " hoy.");

        Asistencia asistencia = new Asistencia();
        asistencia.setEmpleado(empleado);
        asistencia.setFecha(hoy);
        asistencia.setObservacion(req.getObservacion());

        aplicarHoraYCalcularEstado(asistencia, empleado, req.getHoraEntrada());

        return asistenciaRepository.save(asistencia);
    }

    // Calcula Presente/Tardanza/Ausente comparando la hora de entrada contra la hora
    // esperada del empleado, y deja seteados estado, horaEntrada y minutosTardanza.
    // Reutilizado tanto al registrar por primera vez como al corregir un registro existente.
    private void aplicarHoraYCalcularEstado(Asistencia asistencia, Empleado empleado, String horaEntradaTexto) {
        if (horaEntradaTexto == null || horaEntradaTexto.trim().isEmpty()) {
            asistencia.setEstado("Ausente");
            asistencia.setHoraEntrada(null);
            asistencia.setMinutosTardanza(0);
            return;
        }

        LocalTime horaEntrada = LocalTime.parse(horaEntradaTexto, HORA_FORMAT);
        asistencia.setHoraEntrada(horaEntrada);

        LocalTime horaEsperada = LocalTime.parse(
                empleado.getHoraEntradaEsperada() != null ? empleado.getHoraEntradaEsperada() : "08:00",
                HORA_FORMAT
        );

        long minutosDiferencia = ChronoUnit.MINUTES.between(horaEsperada, horaEntrada);

        if (minutosDiferencia > 0) {
            asistencia.setEstado("Tardanza");
            asistencia.setMinutosTardanza((int) minutosDiferencia);
        } else {
            asistencia.setEstado("Presente");
            asistencia.setMinutosTardanza(0);
        }
    }

    // Corrige la hora de entrada de un registro YA existente y recalcula su estado real
    // (a diferencia de actualizarEstado, que solo cambia la etiqueta sin tocar hora/minutos).
    public Optional<Asistencia> corregirHoraEntrada(Long id, String horaEntrada, String observacion) {
        return asistenciaRepository.findById(id).map(a -> {
            aplicarHoraYCalcularEstado(a, a.getEmpleado(), horaEntrada);
            if (observacion != null) a.setObservacion(observacion);
            return asistenciaRepository.save(a);
        });
    }

    // Permite corregir manualmente el estado de un registro del día (ej. licencia médica)
    public Optional<Asistencia> actualizarEstado(Long id, String estado, String observacion) {
        if (!List.of("Presente", "Tardanza", "Ausente").contains(estado))
            throw new IllegalArgumentException("Estado inválido. Use Presente, Tardanza o Ausente.");

        return asistenciaRepository.findById(id).map(a -> {
            a.setEstado(estado);
            if (observacion != null) a.setObservacion(observacion);
            if ("Ausente".equals(estado)) {
                a.setHoraEntrada(null);
                a.setMinutosTardanza(0);
            }
            return asistenciaRepository.save(a);
        });
    }

    // ─────────────────────────────────────────
    // HU22 — Registrar tardanzas y faltas
    // ─────────────────────────────────────────

    // Listado de incidencias (Tardanza/Ausente) en un rango. Si no se indican fechas,
    // toma el mes actual completo (del día 1 a hoy).
    public List<Asistencia> listarIncidencias(LocalDate desde, LocalDate hasta) {
        LocalDate[] rango = normalizarRango(desde, hasta);
        return asistenciaRepository.findIncidenciasEntreFechas(rango[0], rango[1]);
    }

    public List<Asistencia> listarIncidenciasPorEmpleado(Long empleadoId, LocalDate desde, LocalDate hasta) {
        LocalDate[] rango = normalizarRango(desde, hasta);
        return asistenciaRepository.findIncidenciasPorEmpleadoEntreFechas(empleadoId, rango[0], rango[1]);
    }

    // Resumen agrupado por empleado: cuántas tardanzas, cuántas faltas y minutos acumulados
    public List<dsw.sigconbackend.dto.ResumenIncidenciasDTO> resumenIncidencias(LocalDate desde, LocalDate hasta) {
        LocalDate[] rango = normalizarRango(desde, hasta);
        List<Asistencia> registros = asistenciaRepository.findEntreFechas(rango[0], rango[1]);

        java.util.Map<Long, dsw.sigconbackend.dto.ResumenIncidenciasDTO> resumenPorEmpleado = new java.util.LinkedHashMap<>();

        for (Asistencia a : registros) {
            Empleado emp = a.getEmpleado();
            dsw.sigconbackend.dto.ResumenIncidenciasDTO dto = resumenPorEmpleado.computeIfAbsent(
                    emp.getId(),
                    id -> new dsw.sigconbackend.dto.ResumenIncidenciasDTO(
                            emp.getId(), emp.getCodigo(), emp.getNombre(), emp.getArea(), 0, 0, 0, 0)
            );

            dto.setDiasRegistrados(dto.getDiasRegistrados() + 1);

            if ("Tardanza".equals(a.getEstado())) {
                dto.setTotalTardanzas(dto.getTotalTardanzas() + 1);
                dto.setTotalMinutosTardanza(dto.getTotalMinutosTardanza() +
                        (a.getMinutosTardanza() != null ? a.getMinutosTardanza() : 0));
            } else if ("Ausente".equals(a.getEstado())) {
                dto.setTotalFaltas(dto.getTotalFaltas() + 1);
            }
        }

        // Solo se muestran empleados con al menos una incidencia
        return resumenPorEmpleado.values().stream()
                .filter(dto -> dto.getTotalTardanzas() > 0 || dto.getTotalFaltas() > 0)
                .sorted((a, b) -> Long.compare(
                        b.getTotalTardanzas() + b.getTotalFaltas(),
                        a.getTotalTardanzas() + a.getTotalFaltas()))
                .toList();
    }

    private LocalDate[] normalizarRango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : hoy;
        if (inicio.isAfter(fin))
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        return new LocalDate[]{inicio, fin};
    }
}
