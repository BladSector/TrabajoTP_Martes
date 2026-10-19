package ar.edu.cafe.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Cuenta {
    private Long id;
    private Mesa mesa;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private EstadoCuenta estado = EstadoCuenta.ABIERTA;
    private List<Pedido> pedidos = new ArrayList<>();
    private List<Pago> pagos = new ArrayList<>();

    public void agregarPedido(Pedido pedido) {
        if (estado != EstadoCuenta.ABIERTA) throw new IllegalArgumentException("La cuenta esta cerrada");
        pedidos.add(pedido);
    }
    public double calcularSubtotal() {
        return Dinero.redondear(pedidos.stream().filter(p -> !p.isCancelado())
                .mapToDouble(Pedido::calcularSubtotal).sum());
    }
    public List<Pedido> obtenerPedidosPendientesDePago() {
        return pedidos.stream().filter(p -> p.obtenerCantidadPendienteDePago() > 0).toList();
    }
    public boolean estaSaldada() { return obtenerPedidosPendientesDePago().isEmpty(); }
    public void cerrar() {
        if (!estaSaldada()) throw new IllegalArgumentException("La cuenta tiene consumos sin pagar");
        estado = EstadoCuenta.CERRADA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }

    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public EstadoCuenta getEstado() { return estado; }
    public void setEstado(EstadoCuenta estado) { this.estado = estado; }

    public List<Pedido> getPedidos() { return pedidos; }
    public void setPedidos(List<Pedido> pedidos) { this.pedidos = pedidos; }

    public List<Pago> getPagos() { return pagos; }
    public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
}
