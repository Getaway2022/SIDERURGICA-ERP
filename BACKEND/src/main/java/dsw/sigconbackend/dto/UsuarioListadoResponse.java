package dsw.sigconbackend.dto;

public record UsuarioListadoResponse(
    Long id,
    String username,
    RolResumen rol,
    String rolNombre,
    Long estadoId,
    String estado
) {
    public record RolResumen(Long id, String nombre) {}
}
