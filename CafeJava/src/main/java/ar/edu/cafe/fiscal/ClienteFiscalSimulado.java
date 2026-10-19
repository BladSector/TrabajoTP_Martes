package ar.edu.cafe.fiscal;

import ar.edu.cafe.model.*;
import org.springframework.stereotype.Component;

@Component
public class ClienteFiscalSimulado implements ClienteFiscal {
    @Override
    public void autorizar(TicketElectronico ticket) {
        if (!"SIMULACION".equals(ticket.getEntorno())) {
            throw new IllegalArgumentException("La autorizacion fiscal real no esta implementada; use SIMULACION");
        }
        ticket.setEstado(EstadoTicket.SIMULADO);
        // No se inventa un CAE ni se presenta el ticket como autorizado.
        ticket.setCae(null); ticket.setNumeroComprobante(null); ticket.setVencimientoCae(null);
    }
}
