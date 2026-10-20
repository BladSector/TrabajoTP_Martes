package ar.edu.cafe.model;

public class ItemTicket {
    private Integer id;
    private String codigoProducto;
    private String descripcion;
    private int cantidad;
    private double precioUnitario = Dinero.CERO;
    private double alicuotaIva = Dinero.CERO;

    public double calcularImporte() {
        return Dinero.redondear(precioUnitario * cantidad);
    }
    public double calcularIva() {
        return Dinero.redondear(calcularImporte() * alicuotaIva / 100.0);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public double getAlicuotaIva() { return alicuotaIva; }
    public void setAlicuotaIva(double alicuotaIva) { this.alicuotaIva = alicuotaIva; }
}
