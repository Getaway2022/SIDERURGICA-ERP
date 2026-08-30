package dsw.sigconbackend.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
        boolean success,
        String mensaje,
        Map<String, String> errores,
        LocalDateTime timestamp) {

    public ApiError(String mensaje, Map<String, String> errores) {
        this(false, mensaje, errores, LocalDateTime.now());
    }
}
