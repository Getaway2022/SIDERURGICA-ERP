package dsw.sigconbackend.service;

import dsw.sigconbackend.model.FacturaProveedor;
import dsw.sigconbackend.repository.FacturaProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FacturaProveedorService {

    @Autowired private FacturaProveedorRepository repo;

    public List<FacturaProveedor> listar() { return repo.findAll(); }
    public List<FacturaProveedor> listarPorProveedor(Long id) { return repo.findByProveedorId(id); }

    public FacturaProveedor registrar(FacturaProveedor f) {
        f.setEstado("Pendiente");
        return repo.save(f);
    }
}