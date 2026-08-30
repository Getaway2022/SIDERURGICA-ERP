package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Empleado;
import dsw.sigconbackend.repository.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    public List<Empleado> listar() {
        return empleadoRepository.findAllActivos();
    }

    public List<Empleado> listarPorArea(String area) {
        return empleadoRepository.findByArea(area);
    }

    public Optional<Empleado> buscarPorId(Long id) {
        return empleadoRepository.findById(id);
    }

    public Empleado crear(Empleado empleado) {
        if (empleado.getNombre() == null || empleado.getNombre().trim().isEmpty())
            throw new IllegalArgumentException("El nombre del empleado es obligatorio.");
        if (empleado.getCodigo() == null || empleado.getCodigo().trim().isEmpty())
            empleado.setCodigo(generarCodigo());
        if (empleado.getHoraEntradaEsperada() == null)
            empleado.setHoraEntradaEsperada("08:00");
        empleado.setActivo(true);
        return empleadoRepository.save(empleado);
    }

    public Optional<Empleado> actualizar(Long id, Empleado datos) {
        return empleadoRepository.findById(id).map(e -> {
            e.setNombre(datos.getNombre());
            e.setCargo(datos.getCargo());
            e.setArea(datos.getArea());
            if (datos.getHoraEntradaEsperada() != null)
                e.setHoraEntradaEsperada(datos.getHoraEntradaEsperada());
            return empleadoRepository.save(e);
        });
    }

    public void eliminar(Long id) {
        empleadoRepository.findById(id).ifPresent(e -> {
            e.setActivo(false); // baja lógica, no se borra el historial de asistencia
            empleadoRepository.save(e);
        });
    }

    // Hora máxima de entrada general de la empresa, aplicada a todos los empleados activos.
    // El admin la puede cambiar día a día desde el panel de RRHH.
    public String obtenerHoraEntradaGeneral() {
        return empleadoRepository.findAllActivos().stream()
                .findFirst()
                .map(Empleado::getHoraEntradaEsperada)
                .orElse("08:00");
    }

    public void actualizarHoraEntradaGeneral(String nuevaHora) {
        if (nuevaHora == null || !nuevaHora.matches("^([01]\\d|2[0-3]):[0-5]\\d$"))
            throw new IllegalArgumentException("Hora inválida. Use formato HH:mm, ej: 08:00.");

        List<Empleado> activos = empleadoRepository.findAllActivos();
        activos.forEach(e -> e.setHoraEntradaEsperada(nuevaHora));
        empleadoRepository.saveAll(activos);
    }

    private String generarCodigo() {
        long siguiente = empleadoRepository.count() + 1;
        return String.format("EMP-%04d", siguiente);
    }
}
