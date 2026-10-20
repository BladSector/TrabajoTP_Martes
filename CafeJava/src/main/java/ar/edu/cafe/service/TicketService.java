package ar.edu.cafe.service;

import ar.edu.cafe.dao.TicketDAO;
import ar.edu.cafe.fiscal.*;
import ar.edu.cafe.model.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    private final TicketDAO ticketDAO;
    private final ClienteFiscal clienteFiscal;
    private final ConfiguracionFiscal configuracion;
    private final Clock clock;
    public TicketService(TicketDAO ticketDAO, ClienteFiscal clienteFiscal, ConfiguracionFiscal configuracion, Clock clock) {
        this.ticketDAO = ticketDAO; this.clienteFiscal = clienteFiscal; this.configuracion = configuracion; this.clock = clock;
    }
    @Transactional
    public TicketElectronico generarTicket(Pago pago) {
        if (pago == null || pago.getId() == null || pago.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El ticket requiere un pago registrado");
        }
        if (configuracion.getAlicuotaIva() != 0.0) {
            throw new IllegalArgumentException("Esta version implementa solamente IVA EXENTO");
        }
        var existente = ticketDAO.buscarPorPagoId(pago.getId());
        if (existente.isPresent()) return existente.get();
        TicketElectronico ticket = new TicketElectronico();
        ticket.setCodigo("TICKET-" + UUID.randomUUID()); ticket.setPago(pago);
        ticket.setFechaEmision(LocalDateTime.now(clock)); ticket.setEntorno(configuracion.getEntorno());
        ticket.setDescuento(pago.getDescuento()); ticket.setRazonSocialEmisor(configuracion.getRazonSocial());
        ticket.setCuitEmisor(configuracion.getCuit()); ticket.setDomicilioEmisor(configuracion.getDomicilio());
        ticket.setCondicionIvaEmisor(configuracion.getCondicionIva()); ticket.setPuntoVenta(configuracion.getPuntoVenta());
        for (DetallePago detalle : pago.getDetalles()) {
            ItemTicket item = new ItemTicket();
            item.setCodigoProducto(detalle.getPedido().getProducto().getId().toString());
            item.setDescripcion(detalle.getPedido().getNombreProducto()); item.setCantidad(detalle.getCantidad());
            item.setPrecioUnitario(detalle.getPrecioUnitario()); item.setAlicuotaIva(configuracion.getAlicuotaIva());
            ticket.getItems().add(item);
        }
        ticket.calcularTotales(); clienteFiscal.autorizar(ticket);
        return ticketDAO.guardar(ticket);
    }
    @Transactional
    public void validarTicket(TicketElectronico ticket) {
        if (ticket == null || ticket.getId() == null) throw new IllegalArgumentException("Ticket sin identificador");
        TicketElectronico guardado = obtenerTicket(ticket.getId());
        clienteFiscal.autorizar(guardado); ticketDAO.actualizarValidacion(guardado);
    }
    @Transactional(readOnly = true)
    public TicketElectronico obtenerTicket(Integer id) {
        return ticketDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Ticket inexistente"));
    }
    @Transactional(readOnly = true)
    public TicketElectronico obtenerPorPago(Integer pagoId) {
        return ticketDAO.buscarPorPagoId(pagoId).orElseThrow(() -> new NoSuchElementException("Ticket inexistente"));
    }
    @Transactional(readOnly = true)
    public List<TicketElectronico> consultarHistorial() { return ticketDAO.listar(); }
}
