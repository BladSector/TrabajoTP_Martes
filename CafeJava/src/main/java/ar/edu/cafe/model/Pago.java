package ar.edu.cafe.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Pago {
    private Long id;
    private Long cuentaId;
    private LocalDateTime fechaHora;
    private MetodoPago metodo;
    private double subtotal = Dinero.CERO;
    private double descuento = Dinero.CERO;
    private double total;
    private List<DetallePago> detalles = new ArrayList<>();

    public double calcularSubtotal() {
        subtotal = Dinero.redondear(detalles.stream().mapToDouble(DetallePago::calcularImporte).sum());
        return subtotal;
    }
    public void aplicarDescuento(double importe) {
        importe = Dinero.noNegativo(importe);
        if (importe > subtotal) throw new IllegalArgumentException("Descuento mayor al subtotal");
        descuento = importe;
    }
    public double calcularTotal() { total = Dinero.redondear(subtotal - descuento); return total; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public MetodoPago getMetodo() { return metodo; }
    public void setMetodo(MetodoPago metodo) { this.metodo = metodo; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public List<DetallePago> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePago> detalles) { this.detalles = detalles; }
}
