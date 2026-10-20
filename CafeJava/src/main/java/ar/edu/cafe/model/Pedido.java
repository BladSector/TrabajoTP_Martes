package ar.edu.cafe.model;

import java.time.LocalDateTime;

public class Pedido {
    private Integer id;
    private Integer cuentaId;
    private Producto producto;
    private String nombreProducto;
    private int cantidad;
    private double precioUnitario = Dinero.CERO;
    private LocalDateTime fechaHora;
    private EstadoCocina estadoCocina = EstadoCocina.BORRADOR;
    private String mozo;
    private Integer perfilMozoId;
    private String nombreCliente = "";
    private String notas = "";
    private int cantidadPagada;
    private boolean cancelado;

    public double calcularSubtotal() {
        return Dinero.redondear(precioUnitario * cantidad);
    }
    public void aumentarCantidad(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva");
        this.cantidad = Math.addExact(this.cantidad, cantidad);
    }
    public void cambiarEstadoCocina(EstadoCocina estado) { this.estadoCocina = estado; }
    public int obtenerCantidadPendienteDePago() { return cancelado ? 0 : cantidad - cantidadPagada; }
    public void registrarCantidadPagada(int cantidad) {
        if (cantidad <= 0 || cantidad > obtenerCantidadPendienteDePago()) {
            throw new IllegalArgumentException("La cantidad a pagar supera lo pendiente");
        }
        cantidadPagada += cantidad;
    }
    public void cancelar() {
        if (cantidadPagada > 0) throw new IllegalArgumentException("No se puede cancelar un pedido con pagos");
        cancelado = true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public EstadoCocina getEstadoCocina() { return estadoCocina; }
    public void setEstadoCocina(EstadoCocina estadoCocina) { this.estadoCocina = estadoCocina; }

    public String getMozo() { return mozo; }
    public void setMozo(String mozo) { this.mozo = mozo; }

    public Integer getPerfilMozoId() { return perfilMozoId; }
    public void setPerfilMozoId(Integer perfilMozoId) { this.perfilMozoId = perfilMozoId; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public int getCantidadPagada() { return cantidadPagada; }
    public void setCantidadPagada(int cantidadPagada) { this.cantidadPagada = cantidadPagada; }

    public boolean isCancelado() { return cancelado; }
    public void setCancelado(boolean cancelado) { this.cancelado = cancelado; }
}
