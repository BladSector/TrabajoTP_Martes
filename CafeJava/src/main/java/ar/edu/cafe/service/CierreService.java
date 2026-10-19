package ar.edu.cafe.service;

import ar.edu.cafe.dao.*;
import ar.edu.cafe.model.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CierreService {
    private final TicketDAO ticketDAO;
    private final CierreDiarioDAO cierreDAO;
    private final CuentaDAO cuentaDAO;
    private final OperacionGuard guard;
    private final Clock clock;
    public CierreService(TicketDAO ticketDAO, CierreDiarioDAO cierreDAO, CuentaDAO cuentaDAO, OperacionGuard guard, Clock clock) {
        this.ticketDAO = ticketDAO; this.cierreDAO = cierreDAO; this.cuentaDAO = cuentaDAO; this.guard = guard; this.clock = clock;
    }
    @Transactional
    public CierreDiario generarCierre(LocalDate fecha) {
        guard.bloquear();
        if (fecha == null || fecha.isAfter(LocalDate.now(clock))) throw new IllegalArgumentException("Fecha de cierre invalida");
        var existente = cierreDAO.buscarPorFecha(fecha);
        if (existente.isPresent()) return existente.get();
        if (cuentaDAO.listarAbiertas().stream().anyMatch(c -> !c.getFechaApertura().toLocalDate().isAfter(fecha) && !c.estaSaldada())) {
            throw new IllegalArgumentException("Hay cuentas abiertas con consumos sin pagar");
        }
        CierreDiario cierre = new CierreDiario();
        cierre.setFecha(fecha); cierre.setFechaHoraCierre(LocalDateTime.now(clock));
        cierre.setNumeroInforme(cierreDAO.siguienteNumero()); cierre.calcularResumen(ticketDAO.listarPorFecha(fecha));
        return cierreDAO.guardar(cierre);
    }
    @Transactional(readOnly = true)
    public CierreDiario obtenerResumen(LocalDate fecha) {
        if (fecha == null) throw new IllegalArgumentException("Fecha obligatoria");
        return cierreDAO.buscarPorFecha(fecha).orElseGet(() -> {
            CierreDiario resumen = new CierreDiario(); resumen.setFecha(fecha);
            resumen.calcularResumen(ticketDAO.listarPorFecha(fecha)); return resumen;
        });
    }
    @Transactional(readOnly = true)
    public boolean existeCierre(LocalDate fecha) { return cierreDAO.buscarPorFecha(fecha).isPresent(); }
    @Transactional(readOnly = true)
    public List<CierreDiario> consultarHistorial() { return cierreDAO.listar(); }
}
