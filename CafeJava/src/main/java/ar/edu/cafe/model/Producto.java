package ar.edu.cafe.model;

public class Producto {
    private Long id;
    private String nombre;
    private String descripcion = "";
    private double precio = Dinero.CERO;
    private boolean disponible = true;
    private boolean eliminado;
    private Categoria categoria;

    public void actualizarPrecio(double precio) { this.precio = Dinero.noNegativo(precio); }
    public void cambiarDisponibilidad(boolean disponible) { this.disponible = disponible; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}
