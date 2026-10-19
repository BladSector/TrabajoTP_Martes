package ar.edu.cafe.service;

import ar.edu.cafe.dao.*;
import ar.edu.cafe.model.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PagoService {
    private final PagoDAO pagoDAO;
    private final PedidoDAO pedidoDAO;
    private final MesaService mesaService;
    private final TicketService ticketService;
    private final ImpresionService impresionService;
    private final OperacionGuard guard;
    private final Clock clock;
    public PagoService(PagoDAO pagoDAO, PedidoDAO pedidoDAO, MesaService mesaService, TicketService ticketService,
            ImpresionService impresionService, OperacionGuard guard, Clock clock) {
        this.pagoDAO = pagoDAO; this.pedidoDAO = pedidoDAO; this.mesaService = mesaService; this.ticketService = ticketService;
        this.impresionService = impresionService; this.guard = guard; this.clock = clock;
    }
    public double calcularDescuento(double subtotal, MetodoPago metodo) {
        subtotal = Dinero.noNegativo(subtotal);
        if (metodo == null) throw new IllegalArgumentException("Seleccione el medio de pago");
        return metodo == MetodoPago.EFECTIVO ? Dinero.redondear(subtotal * 0.10) : Dinero.CERO;
    }
    @Transactional
    public Pago registrarPago(Long cuentaId, Pago pago) {
        guard.bloquearDiaAbierto();
        Cuenta cuenta = mesaService.obtenerCuenta(cuentaId);
        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) throw new IllegalArgumentException("La cuenta esta cerrada");
        if (pago == null || pago.getMetodo() == null || pago.getDetalles() == null || pago.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Seleccione consumos y medio de pago");
        }
        // Cero indica que no se solicito un total manual en efectivo.
        double totalSolicitado = pago.getTotal();
        Set<Long> ids = new HashSet<>();
        for (DetallePago detalle : pago.getDetalles()) {
            if (detalle == null || detalle.getPedido() == null || detalle.getPedido().getId() == null) {
                throw new IllegalArgumentException("Detalle de pago invalido");
            }
            Long id = detalle.getPedido().getId();
            if (!ids.add(id)) throw new IllegalArgumentException("Pedido repetido en el pago");
            Pedido pedido = pedidoDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Pedido inexistente"));
            if (!cuentaId.equals(pedido.getCuentaId())) throw new IllegalArgumentException("El pedido pertenece a otra cuenta");
            if (pedido.isCancelado() || pedido.getEstadoCocina() == EstadoCocina.BORRADOR) {
                throw new IllegalArgumentException("Solo se cobran pedidos enviados a cocina y no cancelados");
            }
            pedido.registrarCantidadPagada(detalle.getCantidad());
            detalle.setId(null); detalle.setPedido(pedido); detalle.setPrecioUnitario(pedido.getPrecioUnitario());
        }
        pago.setId(null); pago.setCuentaId(cuentaId); pago.setFechaHora(LocalDateTime.now(clock)); pago.calcularSubtotal();
        if (totalSolicitado != 0.0 && pago.getMetodo() == MetodoPago.EFECTIVO) {
            totalSolicitado = Dinero.redondear(totalSolicitado);
            double base = Dinero.redondear(pago.getSubtotal() - calcularDescuento(pago.getSubtotal(), MetodoPago.EFECTIVO));
            if (base <= 0.0) throw new IllegalArgumentException("No se puede redondear un total sin importe");
            // Un 5 % es 1/20; truncar a centavos mantiene ambos limites dentro del margen.
            double margen = (Math.round(base * 100.0) / 20) / 100.0;
            double minimo = Dinero.redondear(Math.max(0.01, base - margen));
            double maximo = Dinero.redondear(Math.min(pago.getSubtotal(), base + margen));
            if (totalSolicitado < minimo || totalSolicitado > maximo) {
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        "El redondeo debe estar entre %.2f y %.2f (5%% del total con descuento)", minimo, maximo));
            }
            pago.aplicarDescuento(pago.getSubtotal() - totalSolicitado);
        } else {
            pago.aplicarDescuento(calcularDescuento(pago.getSubtotal(), pago.getMetodo()));
        }
        pago.calcularTotal(); pagoDAO.guardar(pago);
        for (DetallePago detalle : pago.getDetalles()) pedidoDAO.actualizar(detalle.getPedido());
        TicketElectronico ticket = ticketService.generarTicket(pago);
        if (mesaService.obtenerCuenta(cuentaId).estaSaldada()) mesaService.cerrarCuenta(cuentaId);
        if (impresionService.isImpresionAutomatica()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { impresionService.imprimirTicket(ticket); }
            });
        }
        return pago;
    }
}
