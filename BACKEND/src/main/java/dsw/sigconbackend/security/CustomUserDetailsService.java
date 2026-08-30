package dsw.sigconbackend.security;

import dsw.sigconbackend.model.Usuario;
import dsw.sigconbackend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * HU02 — Spring Security usa esta clase para verificar credenciales
 * 
 * Cuando Angular hace POST /login con { username, password },
 * Spring Security llama a este servicio para cargar el usuario
 * y verificar la contraseña automáticamente.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        // El rol viene de tu tabla seguridad.rol
        // Spring Security requiere el prefijo "ROLE_"
        // Ej: si rol es "VENDEDOR" → "ROLE_VENDEDOR"
        String rolConPrefijo = "ROLE_" + usuario.getRolNombre().toUpperCase();

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())  // debe ser BCrypt en BD
                .authorities(new SimpleGrantedAuthority(rolConPrefijo))
                .build();
    }
}
