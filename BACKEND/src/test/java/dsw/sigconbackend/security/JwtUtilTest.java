package dsw.sigconbackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "clave-de-prueba-segura-con-mas-de-32-caracteres",
                60_000,
                120_000);
    }

    @Test
    void generaAccessTokenConUsuarioRolYTipo() {
        String token = jwtUtil.generateAccessToken("admin", "ADMIN");

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("admin", jwtUtil.getUsernameFromToken(token));
        assertEquals("ADMIN", jwtUtil.getClaimFromToken(token, "rol"));
        assertEquals("access", jwtUtil.getClaimFromToken(token, "type"));
    }

    @Test
    void generaRefreshTokenSinConvertirloEnAccessToken() {
        String token = jwtUtil.generateRefreshToken("admin");

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("refresh", jwtUtil.getClaimFromToken(token, "type"));
        assertNull(jwtUtil.getClaimFromToken(token, "rol"));
    }

    @Test
    void rechazaUnTokenManipulado() {
        String token = jwtUtil.generateAccessToken("admin", "ADMIN");
        int signatureStart = token.lastIndexOf('.') + 1;
        int position = signatureStart + 2;
        char replacement = token.charAt(position) == 'a' ? 'b' : 'a';
        String manipulated = token.substring(0, position)
                + replacement
                + token.substring(position + 1);

        assertFalse(jwtUtil.isTokenValid(manipulated));
    }
}
