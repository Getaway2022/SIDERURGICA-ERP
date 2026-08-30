package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Presupuesto;
import dsw.sigconbackend.repository.PresupuestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PresupuestoService {

    @Autowired private PresupuestoRepository repo;

    private String periodoActual() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    public Presupuesto obtenerActual() {
        return repo.findByPeriodo(periodoActual())
            .orElseThrow(() -> new RuntimeException("No hay presupuesto configurado para " + periodoActual()));
    }

    public void validarCapital(BigDecimal monto) {
        Presupuesto p = obtenerActual();
        if (p.getMontoDisponible().compareTo(monto) < 0) {
            throw new RuntimeException("Capital insuficiente. Disponible: S/ " + p.getMontoDisponible());
        }
    }

    public void comprometer(BigDecimal monto) {
        validarCapital(monto);
        Presupuesto p = obtenerActual();
        p.setMontoDisponible(p.getMontoDisponible().subtract(monto));
        repo.save(p);
    }

    public void liberar(BigDecimal monto) {
        Presupuesto p = obtenerActual();
        p.setMontoDisponible(p.getMontoDisponible().add(monto));
        repo.save(p);
    }
}