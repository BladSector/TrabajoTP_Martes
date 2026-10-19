package ar.edu.cafe.model;

public class DetallePago {
    private Long id;
    private Pedido pedido;
    private int cantidad;
    private double precioUnitario = Dinero.CERO;

    public double calcularImporte() {
        return Dinero.redondear(precioUnitario * cantidad);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
}
