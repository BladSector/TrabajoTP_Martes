package ar.edu.cafe.dao;

import ar.edu.cafe.model.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TicketDAO extends JdbcDAO {
    private final PagoDAO pagoDAO;
    public TicketDAO(JdbcTemplate jdbc, PagoDAO pagoDAO) { super(jdbc); this.pagoDAO = pagoDAO; }
    private RowMapper<TicketElectronico> mapper() {
        return (rs, fila) -> {
            TicketElectronico ticket = new TicketElectronico();
            ticket.setId(rs.getLong("id")); ticket.setCodigo(rs.getString("codigo"));
            ticket.setPago(pagoDAO.buscarPorId(rs.getLong("pago_id")).orElseThrow());
            ticket.setFechaEmision(rs.getTimestamp("fecha_emision").toLocalDateTime());
            ticket.setSubtotal(rs.getDouble("subtotal")); ticket.setDescuento(rs.getDouble("descuento"));
            ticket.setIva(rs.getDouble("iva")); ticket.setTotal(rs.getDouble("total"));
            ticket.setEstado(EstadoTicket.valueOf(rs.getString("estado"))); ticket.setEntorno(rs.getString("entorno"));
            ticket.setTipoDocumentoCliente(rs.getString("tipo_documento_cliente"));
            ticket.setNumeroDocumentoCliente(rs.getString("numero_documento_cliente"));
            ticket.setRazonSocialCliente(rs.getString("razon_social_cliente"));
            ticket.setRazonSocialEmisor(rs.getString("razon_social_emisor")); ticket.setCuitEmisor(rs.getString("cuit_emisor"));
            ticket.setDomicilioEmisor(rs.getString("domicilio_emisor")); ticket.setCondicionIvaEmisor(rs.getString("condicion_iva_emisor"));
            ticket.setPuntoVenta(rs.getInt("punto_venta")); ticket.setNumeroComprobante(rs.getObject("numero_comprobante", Long.class));
            ticket.setCae(rs.getString("cae")); Date fecha = rs.getDate("vencimiento_cae");
            ticket.setVencimientoCae(fecha == null ? null : fecha.toLocalDate());
            ticket.setItems(listarItems(ticket.getId()));
            return ticket;
        };
    }
    private List<ItemTicket> listarItems(Long ticketId) {
        return jdbc.query("SELECT * FROM items_ticket WHERE ticket_id=? ORDER BY id", (rs, fila) -> {
            ItemTicket item = new ItemTicket();
            item.setId(rs.getLong("id")); item.setCodigoProducto(rs.getString("codigo_producto"));
            item.setDescripcion(rs.getString("descripcion")); item.setCantidad(rs.getInt("cantidad"));
            item.setPrecioUnitario(rs.getDouble("precio_unitario")); item.setAlicuotaIva(rs.getDouble("alicuota_iva"));
            return item;
        }, ticketId);
    }
    public List<TicketElectronico> listar() {
        return jdbc.query("SELECT * FROM tickets ORDER BY fecha_emision DESC,id DESC", mapper());
    }
    public List<TicketElectronico> listarPorFecha(LocalDate fecha) {
        return jdbc.query("SELECT * FROM tickets WHERE fecha_emision>=? AND fecha_emision<? ORDER BY id",
                mapper(), fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }
    public Optional<TicketElectronico> buscarPorId(Long id) {
        return jdbc.query("SELECT * FROM tickets WHERE id=?", mapper(), id).stream().findFirst();
    }
    public Optional<TicketElectronico> buscarPorPagoId(Long pagoId) {
        return jdbc.query("SELECT * FROM tickets WHERE pago_id=?", mapper(), pagoId).stream().findFirst();
    }
    public TicketElectronico guardar(TicketElectronico ticket) {
        ticket.setId(insertar("""
                INSERT INTO tickets(codigo,pago_id,fecha_emision,subtotal,descuento,iva,total,estado,entorno,
                    tipo_documento_cliente,numero_documento_cliente,razon_social_cliente,razon_social_emisor,
                    cuit_emisor,domicilio_emisor,condicion_iva_emisor,punto_venta)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, ticket.getCodigo(), ticket.getPago().getId(), ticket.getFechaEmision(),
                ticket.getSubtotal(), ticket.getDescuento(), ticket.getIva(), ticket.getTotal(),
                ticket.getEstado().name(), ticket.getEntorno(), ticket.getTipoDocumentoCliente(),
                ticket.getNumeroDocumentoCliente(), ticket.getRazonSocialCliente(), ticket.getRazonSocialEmisor(),
                ticket.getCuitEmisor(), ticket.getDomicilioEmisor(), ticket.getCondicionIvaEmisor(), ticket.getPuntoVenta()));
        for (ItemTicket item : ticket.getItems()) {
            item.setId(insertar("INSERT INTO items_ticket(ticket_id,codigo_producto,descripcion,cantidad,precio_unitario,alicuota_iva) VALUES(?,?,?,?,?,?)",
                    ticket.getId(), item.getCodigoProducto(), item.getDescripcion(), item.getCantidad(),
                    item.getPrecioUnitario(), item.getAlicuotaIva()));
        }
        return ticket;
    }
    public void actualizarValidacion(TicketElectronico ticket) {
        jdbc.update("UPDATE tickets SET estado=?,numero_comprobante=?,cae=?,vencimiento_cae=? WHERE id=?",
                ticket.getEstado().name(), ticket.getNumeroComprobante(), ticket.getCae(), ticket.getVencimientoCae(), ticket.getId());
    }
}
