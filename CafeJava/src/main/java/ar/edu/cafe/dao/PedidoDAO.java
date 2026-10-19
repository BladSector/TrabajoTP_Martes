package ar.edu.cafe.dao;

import ar.edu.cafe.model.*;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoDAO extends JdbcDAO {
    private final ProductoDAO productoDAO;
    public PedidoDAO(JdbcTemplate jdbc, ProductoDAO productoDAO) { super(jdbc); this.productoDAO = productoDAO; }
    private RowMapper<Pedido> mapper() {
        return (rs, fila) -> {
            Pedido pedido = new Pedido();
            pedido.setId(rs.getLong("id")); pedido.setCuentaId(rs.getLong("cuenta_id"));
            pedido.setProducto(productoDAO.buscarPorId(rs.getLong("producto_id")).orElseThrow());
            pedido.setNombreProducto(rs.getString("nombre_producto"));
            pedido.setCantidad(rs.getInt("cantidad")); pedido.setCantidadPagada(rs.getInt("cantidad_pagada"));
            pedido.setPrecioUnitario(rs.getDouble("precio_unitario"));
            pedido.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
            pedido.setEstadoCocina(EstadoCocina.valueOf(rs.getString("estado_cocina")));
            pedido.setMozo(rs.getString("mozo")); pedido.setNombreCliente(rs.getString("nombre_cliente"));
            pedido.setPerfilMozoId(rs.getObject("perfil_mozo_id", Long.class));
            pedido.setNotas(rs.getString("notas")); pedido.setCancelado(rs.getBoolean("cancelado"));
            return pedido;
        };
    }
    public Optional<Pedido> buscarPorId(Long id) {
        return jdbc.query("SELECT * FROM pedidos WHERE id=?", mapper(), id).stream().findFirst();
    }
    public List<Pedido> listarPorCuenta(Long cuentaId) {
        return jdbc.query("SELECT * FROM pedidos WHERE cuenta_id=? ORDER BY id", mapper(), cuentaId);
    }
    public List<Pedido> listarParaCocina() {
        return jdbc.query("""
                SELECT * FROM pedidos WHERE cancelado=FALSE
                  AND estado_cocina IN ('PENDIENTE','EN_PREPARACION','LISTO')
                ORDER BY fecha_hora,id
                """, mapper());
    }
    public List<Pedido> consultarHistorial(java.time.LocalDate fecha, Long perfilId) {
        String sql = "SELECT * FROM pedidos WHERE fecha_hora>=? AND fecha_hora<?";
        if (perfilId == null) return jdbc.query(sql + " ORDER BY fecha_hora DESC,id DESC", mapper(), fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        return jdbc.query(sql + " AND perfil_mozo_id=? ORDER BY fecha_hora DESC,id DESC", mapper(), fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay(), perfilId);
    }
    public Pedido guardar(Pedido pedido) {
        pedido.setId(insertar("""
                INSERT INTO pedidos(cuenta_id,producto_id,nombre_producto,cantidad,cantidad_pagada,
                  precio_unitario,fecha_hora,estado_cocina,mozo,nombre_cliente,notas,cancelado,perfil_mozo_id)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, pedido.getCuentaId(), pedido.getProducto().getId(), pedido.getNombreProducto(),
                pedido.getCantidad(), pedido.getCantidadPagada(), pedido.getPrecioUnitario(), pedido.getFechaHora(),
                pedido.getEstadoCocina().name(), pedido.getMozo(), pedido.getNombreCliente(), pedido.getNotas(), pedido.isCancelado(), pedido.getPerfilMozoId()));
        return pedido;
    }
    public void actualizar(Pedido pedido) {
        jdbc.update("UPDATE pedidos SET cantidad=?,cantidad_pagada=?,estado_cocina=?,cancelado=? WHERE id=?",
                pedido.getCantidad(), pedido.getCantidadPagada(), pedido.getEstadoCocina().name(), pedido.isCancelado(), pedido.getId());
    }
}
