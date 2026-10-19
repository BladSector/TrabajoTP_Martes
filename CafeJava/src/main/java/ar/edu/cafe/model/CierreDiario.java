package ar.edu.cafe.model;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CierreDiario {
    private Long id;
    private LocalDate fecha;
    private LocalDateTime fechaHoraCierre;
    private int numeroInforme;
    private int cantidadTickets;
    private double totalEfectivo = Dinero.CERO;
    private double totalTarjeta = Dinero.CERO;
    private double totalDescuentos = Dinero.CERO;
    private double totalGeneral = Dinero.CERO;

    public void calcularResumen(List<TicketElectronico> tickets) {
        cantidadTickets = 0;
        totalEfectivo = Dinero.CERO; totalTarjeta = Dinero.CERO;
        totalDescuentos = Dinero.CERO; totalGeneral = Dinero.CERO;
        for (TicketElectronico ticket : tickets) {
            if (ticket.getEstado() == EstadoTicket.RECHAZADO) continue;
            cantidadTickets++;
            totalGeneral = Dinero.redondear(totalGeneral + ticket.getTotal());
            totalDescuentos = Dinero.redondear(totalDescuentos + ticket.getDescuento());
            if (ticket.getPago().getMetodo() == MetodoPago.EFECTIVO) {
                totalEfectivo = Dinero.redondear(totalEfectivo + ticket.getTotal());
            } else {
                totalTarjeta = Dinero.redondear(totalTarjeta + ticket.getTotal());
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDateTime getFechaHoraCierre() { return fechaHoraCierre; }
    public void setFechaHoraCierre(LocalDateTime fechaHoraCierre) { this.fechaHoraCierre = fechaHoraCierre; }

    public int getNumeroInforme() { return numeroInforme; }
    public void setNumeroInforme(int numeroInforme) { this.numeroInforme = numeroInforme; }

    public int getCantidadTickets() { return cantidadTickets; }
    public void setCantidadTickets(int cantidadTickets) { this.cantidadTickets = cantidadTickets; }

    public double getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(double totalEfectivo) { this.totalEfectivo = totalEfectivo; }

    public double getTotalTarjeta() { return totalTarjeta; }
    public void setTotalTarjeta(double totalTarjeta) { this.totalTarjeta = totalTarjeta; }

    public double getTotalDescuentos() { return totalDescuentos; }
    public void setTotalDescuentos(double totalDescuentos) { this.totalDescuentos = totalDescuentos; }

    public double getTotalGeneral() { return totalGeneral; }
    public void setTotalGeneral(double totalGeneral) { this.totalGeneral = totalGeneral; }
}
