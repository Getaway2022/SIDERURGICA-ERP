package dsw.sigconbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ReposicionProgramada {
    private final ReposicionService reposicion;
    @Scheduled(initialDelayString = "${automatizacion.reposicion.intervalo-ms:60000}",
               fixedDelayString = "${automatizacion.reposicion.intervalo-ms:60000}")
    public void revisar() {
        try { reposicion.revisarInventario(); }
        catch (RuntimeException e) { log.error("No se pudo revisar reposición; se reintentará en el siguiente ciclo", e); }
    }
}
