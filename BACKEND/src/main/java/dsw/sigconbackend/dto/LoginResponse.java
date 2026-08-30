package dsw.sigconbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginResponse {
    private boolean success;
    private String mensaje;
    private UsuarioDTO usuario;
    private String accessToken;
    private String refreshToken;

    public LoginResponse(boolean success, String mensaje, UsuarioDTO usuario) {
        this.success = success;
        this.mensaje = mensaje;
        this.usuario = usuario;
    }

    public LoginResponse(boolean success, String mensaje, UsuarioDTO usuario,
                         String accessToken, String refreshToken) {
        this.success = success;
        this.mensaje = mensaje;
        this.usuario = usuario;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    @Data
    @NoArgsConstructor
    public static class UsuarioDTO {
        private Long id;
        private String username;
        private String rol;
        private String estado;

        public UsuarioDTO(Long id, String username, String rol, String estado) {
            this.id = id;
            this.username = username;
            this.rol = rol;
            this.estado = estado;
        }
    }
}