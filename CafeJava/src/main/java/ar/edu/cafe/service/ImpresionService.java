package ar.edu.cafe.service;

import ar.edu.cafe.model.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.print.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImpresionService {
    private static final Logger LOG = LoggerFactory.getLogger(ImpresionService.class);
    private final String nombreImpresora;
    private final boolean habilitada;
    private final boolean impresionAutomatica;
    public ImpresionService(@Value("${cafe.impresion.nombre:}") String nombreImpresora,
            @Value("${cafe.impresion.habilitada:false}") boolean habilitada,
            @Value("${cafe.impresion.automatica:false}") boolean impresionAutomatica) {
        this.nombreImpresora = nombreImpresora; this.habilitada = habilitada; this.impresionAutomatica = impresionAutomatica;
    }
    public boolean isImpresionAutomatica() { return habilitada && impresionAutomatica; }
    public String generarContenido(TicketElectronico ticket) {
        StringBuilder texto = new StringBuilder();
        texto.append(ticket.getRazonSocialEmisor()).append("\n");
        texto.append("TICKET DE PRUEBA - SIN VALIDEZ FISCAL\n");
        texto.append(ticket.getCodigo()).append("\n");
        texto.append(ticket.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        texto.append("Cuenta: ").append(ticket.getPago().getCuentaId()).append("\n");
        texto.append("----------------------------------------\n");
        for (ItemTicket item : ticket.getItems()) {
            texto.append(item.getCantidad()).append(" x ").append(item.getDescripcion()).append("\n");
            texto.append("  $ ").append(importe(item.getPrecioUnitario())).append(" = $ ").append(importe(item.calcularImporte())).append("\n");
        }
        texto.append("----------------------------------------\n");
        texto.append("Subtotal: $ ").append(importe(ticket.getSubtotal())).append("\n");
        texto.append("Descuento: $ ").append(importe(ticket.getDescuento())).append("\n");
        texto.append("TOTAL: $ ").append(importe(ticket.getTotal())).append("\n");
        texto.append("Pago: ").append(ticket.getPago().getMetodo()).append("\n");
        return texto.toString();
    }
    public boolean imprimirTicket(TicketElectronico ticket) { return imprimir(generarContenido(ticket)); }
    public boolean imprimirCierre(CierreDiario cierre) { return imprimir(generarContenidoCierre(cierre)); }
    public String generarContenidoCierre(CierreDiario cierre) {
        return "CIERRE DIARIO - " + cierre.getFecha() + "\n"
                + "Tickets: " + cierre.getCantidadTickets() + "\n"
                + "Efectivo: $ " + importe(cierre.getTotalEfectivo()) + "\n"
                + "Tarjeta: $ " + importe(cierre.getTotalTarjeta()) + "\n"
                + "Descuentos: $ " + importe(cierre.getTotalDescuentos()) + "\n"
                + "TOTAL: $ " + importe(cierre.getTotalGeneral()) + "\n";
    }
    private String importe(double valor) { return String.format(Locale.ROOT, "%.2f", valor); }
    private boolean imprimir(String contenido) {
        if (!habilitada || nombreImpresora.isBlank()) return false;
        try {
            for (PrintService impresora : PrintServiceLookup.lookupPrintServices(null, null)) {
                if (impresora.getName().equals(nombreImpresora)) {
                    DocFlavor formato = DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8;
                    if (!impresora.isDocFlavorSupported(formato)) return false;
                    Doc documento = new SimpleDoc(contenido.getBytes(StandardCharsets.UTF_8), formato, null);
                    impresora.createPrintJob().print(documento, null);
                    return true;
                }
            }
        } catch (Exception error) {
            LOG.warn("No se pudo imprimir: {}", error.getMessage());
        }
        return false;
    }
}
