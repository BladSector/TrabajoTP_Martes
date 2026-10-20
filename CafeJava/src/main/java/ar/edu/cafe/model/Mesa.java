package ar.edu.cafe.model;



public class Mesa {
    private Integer id;
    private String nombre;
    private EstadoMesa estado = EstadoMesa.LIBRE;
    private boolean esTakeAway;

    public void ocupar() { estado = EstadoMesa.OCUPADA; }
    public void liberar() { estado = EstadoMesa.LIBRE; }
    public boolean estaLibre() { return estado == EstadoMesa.LIBRE; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public EstadoMesa getEstado() { return estado; }
    public void setEstado(EstadoMesa estado) { this.estado = estado; }

    public boolean isEsTakeAway() { return esTakeAway; }
    public void setEsTakeAway(boolean esTakeAway) { this.esTakeAway = esTakeAway; }
}
