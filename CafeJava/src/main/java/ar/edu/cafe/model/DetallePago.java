package ar.edu.cafe.model;

public class DetallePago {
    private Integer id;
    private Pedido pedido;
    private int cantidad;
    private double precioUnitario = Dinero.CERO;

    public double calcularImporte() {
        return Dinero.redondear(precioUnitario * cantidad);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
}
