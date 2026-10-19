package ar.edu.cafe.fiscal;

import ar.edu.cafe.model.TicketElectronico;

public interface ClienteFiscal {
    void autorizar(TicketElectronico ticket);
}
