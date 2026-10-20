package ar.edu.cafe.dao;

import ar.edu.cafe.model.*;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CuentaDAO extends JdbcDAO {
    private final MesaDAO mesaDAO;
    private final PedidoDAO pedidoDAO;
    private final PagoDAO pagoDAO;
    public CuentaDAO(JdbcTemplate jdbc, MesaDAO mesaDAO, PedidoDAO pedidoDAO, PagoDAO pagoDAO) {
        super(jdbc); this.mesaDAO = mesaDAO; this.pedidoDAO = pedidoDAO; this.pagoDAO = pagoDAO;
    }
    private RowMapper<Cuenta> mapper() {
        return (rs, fila) -> {
            Cuenta cuenta = new Cuenta();
            cuenta.setId(rs.getInt("id")); cuenta.setMesa(mesaDAO.buscarPorId(rs.getInt("mesa_id")).orElseThrow());
            cuenta.setFechaApertura(rs.getTimestamp("fecha_apertura").toLocalDateTime());
            var cierre = rs.getTimestamp("fecha_cierre");
            cuenta.setFechaCierre(cierre == null ? null : cierre.toLocalDateTime());
            cuenta.setEstado(EstadoCuenta.valueOf(rs.getString("estado")));
            cuenta.setPedidos(pedidoDAO.listarPorCuenta(cuenta.getId()));
            cuenta.setPagos(pagoDAO.listarPorCuenta(cuenta.getId()));
            return cuenta;
        };
    }
    public Optional<Cuenta> buscarPorId(Integer id) {
        return jdbc.query("SELECT * FROM cuentas WHERE id=?", mapper(), id).stream().findFirst();
    }
    public Optional<Cuenta> buscarAbiertaPorMesa(Integer mesaId) {
        return jdbc.query("SELECT * FROM cuentas WHERE mesa_abierta_id=?", mapper(), mesaId).stream().findFirst();
    }
    public List<Cuenta> listarAbiertas() {
        return jdbc.query("SELECT * FROM cuentas WHERE estado='ABIERTA' ORDER BY id", mapper());
    }
    public Cuenta guardar(Cuenta cuenta) {
        cuenta.setId(insertar("INSERT INTO cuentas(mesa_id,mesa_abierta_id,fecha_apertura,estado) VALUES(?,?,?,?)",
                cuenta.getMesa().getId(), cuenta.getMesa().getId(), cuenta.getFechaApertura(), cuenta.getEstado().name()));
        return cuenta;
    }
    public void actualizar(Cuenta cuenta) {
        jdbc.update("UPDATE cuentas SET estado=?,fecha_cierre=?,mesa_abierta_id=? WHERE id=?",
                cuenta.getEstado().name(), cuenta.getFechaCierre(),
                cuenta.getEstado() == EstadoCuenta.ABIERTA ? cuenta.getMesa().getId() : null, cuenta.getId());
    }
}
