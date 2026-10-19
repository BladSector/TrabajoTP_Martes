package ar.edu.cafe.dao;

import ar.edu.cafe.model.*;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PagoDAO extends JdbcDAO {
    private final PedidoDAO pedidoDAO;
    public PagoDAO(JdbcTemplate jdbc, PedidoDAO pedidoDAO) { super(jdbc); this.pedidoDAO = pedidoDAO; }
    private RowMapper<Pago> mapper() {
        return (rs, fila) -> {
            Pago pago = new Pago();
            pago.setId(rs.getLong("id")); pago.setCuentaId(rs.getLong("cuenta_id"));
            pago.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
            pago.setMetodo(MetodoPago.valueOf(rs.getString("metodo")));
            pago.setSubtotal(rs.getDouble("subtotal")); pago.setDescuento(rs.getDouble("descuento"));
            pago.setTotal(rs.getDouble("total")); pago.setDetalles(listarDetalles(pago.getId()));
            return pago;
        };
    }
    private List<DetallePago> listarDetalles(Long pagoId) {
        return jdbc.query("SELECT * FROM detalles_pago WHERE pago_id=? ORDER BY id", (rs, fila) -> {
            DetallePago detalle = new DetallePago();
            detalle.setId(rs.getLong("id")); detalle.setPedido(pedidoDAO.buscarPorId(rs.getLong("pedido_id")).orElseThrow());
            detalle.setCantidad(rs.getInt("cantidad")); detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
            return detalle;
        }, pagoId);
    }
    public Optional<Pago> buscarPorId(Long id) {
        return jdbc.query("SELECT * FROM pagos WHERE id=?", mapper(), id).stream().findFirst();
    }
    public List<Pago> listarPorCuenta(Long cuentaId) {
        return jdbc.query("SELECT * FROM pagos WHERE cuenta_id=? ORDER BY id", mapper(), cuentaId);
    }
    public Pago guardar(Pago pago) {
        pago.setId(insertar("INSERT INTO pagos(cuenta_id,fecha_hora,metodo,subtotal,descuento,total) VALUES(?,?,?,?,?,?)",
                pago.getCuentaId(), pago.getFechaHora(), pago.getMetodo().name(), pago.getSubtotal(), pago.getDescuento(), pago.getTotal()));
        for (DetallePago detalle : pago.getDetalles()) {
            detalle.setId(insertar("INSERT INTO detalles_pago(pago_id,pedido_id,cantidad,precio_unitario) VALUES(?,?,?,?)",
                    pago.getId(), detalle.getPedido().getId(), detalle.getCantidad(), detalle.getPrecioUnitario()));
        }
        return pago;
    }
}
