package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Proveedor;
import dsw.sigconbackend.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProveedorService {

    @Autowired private ProveedorRepository repo;

    public List<Proveedor> listar() { return repo.findAll(); }

    public Proveedor buscarPorId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
    }

    public Proveedor crear(Proveedor p) {
        if (repo.existsByRuc(p.getRuc())) throw new RuntimeException("RUC ya registrado");
        return repo.save(p);
    }

    public Proveedor actualizar(Long id, Proveedor datos) {
        Proveedor p = buscarPorId(id);
        p.setNombre(datos.getNombre());
        p.setOrigen(datos.getOrigen());
        p.setContacto(datos.getContacto());
        p.setTelefono(datos.getTelefono());
        p.setCategoria(datos.getCategoria());
        p.setCalificacion(datos.getCalificacion());
        p.setEstado(datos.getEstado());
        return repo.save(p);
    }

    public void eliminar(Long id) { repo.deleteById(id); }
}