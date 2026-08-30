package dsw.sigconbackend.service;

import dsw.sigconbackend.dto.LoginResponse;
import dsw.sigconbackend.repository.UsuarioRepository;
import dsw.sigconbackend.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    public AuthService(
        AuthenticationManager authenticationManager,
        UsuarioRepository usuarioRepository,
        JwtUtil jwtUtil
    ) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(String username, String password) {
        String normalizedUsername = username.trim();
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizedUsername, password)
        );

        List<Object[]> rows = usuarioRepository.findLoginDataByUsername(normalizedUsername);
        if (rows.isEmpty()) {
            return new LoginResponse(false, "Error al obtener datos del usuario", null, null, null);
        }

        Object[] row = rows.get(0);
        String rol = (String) row[2];

        LoginResponse.UsuarioDTO usuario = new LoginResponse.UsuarioDTO(
            ((Number) row[0]).longValue(),
            (String) row[1],
            rol,
            (String) row[3]
        );

        String accessToken  = jwtUtil.generateAccessToken(normalizedUsername, rol);
        String refreshToken = jwtUtil.generateRefreshToken(normalizedUsername);

        return new LoginResponse(true, "Login correcto", usuario, accessToken, refreshToken);
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            return new LoginResponse(false, "Refresh token inválido", null, null, null);
        }

        String type = jwtUtil.getClaimFromToken(refreshToken, "type");
        if (!"refresh".equals(type)) {
            return new LoginResponse(false, "Token no es de tipo refresh", null, null, null);
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        List<Object[]> rows = usuarioRepository.findLoginDataByUsername(username);
        if (rows.isEmpty()) {
            return new LoginResponse(false, "Usuario no encontrado", null, null, null);
        }

        Object[] row = rows.get(0);
        String rol = (String) row[2];
        String newAccessToken = jwtUtil.generateAccessToken(username, rol);

        return new LoginResponse(true, "Token renovado", null, newAccessToken, null);
    }
}
