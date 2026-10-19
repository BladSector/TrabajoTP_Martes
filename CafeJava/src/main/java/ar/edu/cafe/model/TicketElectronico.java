package ar.edu.cafe.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class TicketElectronico {
    private Long id;
    private String codigo;
    private Pago pago;
    private LocalDateTime fechaEmision;
    private List<ItemTicket> items = new ArrayList<>();
    private double subtotal = Dinero.CERO;
    private double descuento = Dinero.CERO;
    private double iva = Dinero.CERO;
    private double total = Dinero.CERO;
    private EstadoTicket estado = EstadoTicket.PENDIENTE_VALIDACION;
    private String entorno;
    private String tipoDocumentoCliente = "99";
    private String numeroDocumentoCliente = "0";
    private String razonSocialCliente = "Consumidor Final";
    private String razonSocialEmisor;
    private String cuitEmisor = "";
    private String domicilioEmisor = "";
    private String condicionIvaEmisor;
    private int puntoVenta;
    private Long numeroComprobante;
    private String cae;
    private LocalDate vencimientoCae;

    public void calcularTotales() {
        subtotal = Dinero.redondear(items.stream().mapToDouble(ItemTicket::calcularImporte).sum());
        // Esta version usa IVA EXENTO; el total siempre coincide con el pago.
        iva = Dinero.CERO;
        total = Dinero.redondear(subtotal - descuento);
    }
    public void registrarAutorizacion(Long numero, String cae, LocalDate vencimiento) {
        numeroComprobante = numero; this.cae = cae; vencimientoCae = vencimiento;
        estado = EstadoTicket.VALIDADO;
    }
    public void registrarRechazo() { estado = EstadoTicket.RECHAZADO; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public List<ItemTicket> getItems() { return items; }
    public void setItems(List<ItemTicket> items) { this.items = items; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    public double getIva() { return iva; }
    public void setIva(double iva) { this.iva = iva; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public EstadoTicket getEstado() { return estado; }
    public void setEstado(EstadoTicket estado) { this.estado = estado; }

    public String getEntorno() { return entorno; }
    public void setEntorno(String entorno) { this.entorno = entorno; }

    public String getTipoDocumentoCliente() { return tipoDocumentoCliente; }
    public void setTipoDocumentoCliente(String tipoDocumentoCliente) { this.tipoDocumentoCliente = tipoDocumentoCliente; }

    public String getNumeroDocumentoCliente() { return numeroDocumentoCliente; }
    public void setNumeroDocumentoCliente(String numeroDocumentoCliente) { this.numeroDocumentoCliente = numeroDocumentoCliente; }

    public String getRazonSocialCliente() { return razonSocialCliente; }
    public void setRazonSocialCliente(String razonSocialCliente) { this.razonSocialCliente = razonSocialCliente; }

    public String getRazonSocialEmisor() { return razonSocialEmisor; }
    public void setRazonSocialEmisor(String razonSocialEmisor) { this.razonSocialEmisor = razonSocialEmisor; }

    public String getCuitEmisor() { return cuitEmisor; }
    public void setCuitEmisor(String cuitEmisor) { this.cuitEmisor = cuitEmisor; }

    public String getDomicilioEmisor() { return domicilioEmisor; }
    public void setDomicilioEmisor(String domicilioEmisor) { this.domicilioEmisor = domicilioEmisor; }

    public String getCondicionIvaEmisor() { return condicionIvaEmisor; }
    public void setCondicionIvaEmisor(String condicionIvaEmisor) { this.condicionIvaEmisor = condicionIvaEmisor; }

    public int getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(int puntoVenta) { this.puntoVenta = puntoVenta; }

    public Long getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(Long numeroComprobante) { this.numeroComprobante = numeroComprobante; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public LocalDate getVencimientoCae() { return vencimientoCae; }
    public void setVencimientoCae(LocalDate vencimientoCae) { this.vencimientoCae = vencimientoCae; }
}
