package dsw.sigconbackend.controller;

import dsw.sigconbackend.dto.UsuarioListadoResponse;
import dsw.sigconbackend.model.Usuario;
import dsw.sigconbackend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public List<UsuarioListadoResponse> listar() {
        return usuarioRepository.findAllWithDetails().stream()
                .map(row -> new UsuarioListadoResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        new UsuarioListadoResponse.RolResumen(
                                ((Number) row[2]).longValue(),
                                (String) row[3]
                        ),
                        (String) row[3],
                        row[4] == null ? null : ((Number) row[4]).longValue(),
                        (String) row[5]
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false,
                            "mensaje", "El nombre de usuario '" + usuario.getUsername() + "' ya existe."));
        }
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", "El username es obligatorio."));
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", "La contraseña es obligatoria."));
        }
        try {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            @SuppressWarnings("DataFlowIssue")
            Usuario guardado = usuarioRepository.save(usuario);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "mensaje", "Usuario creado correctamente.", "usuario", guardado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "mensaje", "Error al crear el usuario."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable long id, @RequestBody Usuario datos) {
        Optional<Usuario> encontrado = usuarioRepository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Usuario no encontrado."));
        }

        Usuario usuario = encontrado.get();

        if (datos.getUsername() != null && !datos.getUsername().equals(usuario.getUsername())) {
            if (usuarioRepository.findByUsername(datos.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false,
                                "mensaje", "El nombre de usuario '" + datos.getUsername() + "' ya existe."));
            }
            usuario.setUsername(datos.getUsername());
        }
        if (datos.getPassword() != null && !datos.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(datos.getPassword()));
        }
        if (datos.getRol() != null) {
            usuario.setRol(datos.getRol());
        }

        @SuppressWarnings("DataFlowIssue")
        Usuario actualizado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(Map.of("success", true,
                "mensaje", "Usuario actualizado correctamente.", "usuario", actualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable long id) {
        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Usuario no encontrado."));
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Usuario eliminado correctamente."));
    }
}
