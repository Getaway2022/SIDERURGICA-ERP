package dsw.sigconbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Serializa movimientos relacionados, incluso entre varias instancias del backend. */
@Service
@RequiredArgsConstructor
public class OperacionAutomatica {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquear() {
        jdbc.execute("SELECT pg_advisory_xact_lock(73190241)");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String codigo(String prefijo) {
        Long numero = jdbc.queryForObject("SELECT nextval('comercial.automatizacion_codigo_seq')", Long.class);
        return prefijo + "-" + String.format("%06d", numero);
    }
}
