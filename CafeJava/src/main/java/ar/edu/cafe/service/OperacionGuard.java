package ar.edu.cafe.service;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperacionGuard {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public OperacionGuard(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }

    public void bloquear() {
        // El bloqueo se mantiene hasta el commit y coordina pagos con el cierre diario.
        jdbc.update("UPDATE control_sistema SET id=id WHERE id=1");
    }

    public void bloquearDiaAbierto() {
        bloquear();
        Integer cierres = jdbc.queryForObject("SELECT COUNT(*) FROM cierres_diarios WHERE fecha=?",
                Integer.class, LocalDate.now(clock));
        if (cierres != null && cierres > 0) throw new IllegalArgumentException("La jornada de hoy esta cerrada");
    }
}
