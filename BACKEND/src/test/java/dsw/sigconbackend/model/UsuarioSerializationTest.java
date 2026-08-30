package dsw.sigconbackend.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void noExponeElHashDePasswordEnLaRespuesta() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("$2a$10$hash-privado");

        String json = objectMapper.writeValueAsString(usuario);

        assertTrue(json.contains("admin"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("hash-privado"));
    }
}
