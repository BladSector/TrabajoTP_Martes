package ar.edu.cafe.dao;

import ar.edu.cafe.model.CierreDiario;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CierreDiarioDAO extends JdbcDAO {
    public CierreDiarioDAO(JdbcTemplate jdbc) { super(jdbc); }
    private final RowMapper<CierreDiario> mapper = (rs, fila) -> {
        CierreDiario cierre = new CierreDiario();
        cierre.setId(rs.getInt("id")); cierre.setFecha(rs.getDate("fecha").toLocalDate());
        cierre.setFechaHoraCierre(rs.getTimestamp("fecha_hora_cierre").toLocalDateTime());
        cierre.setNumeroInforme(rs.getInt("numero_informe")); cierre.setCantidadTickets(rs.getInt("cantidad_tickets"));
        cierre.setTotalEfectivo(rs.getDouble("total_efectivo")); cierre.setTotalTarjeta(rs.getDouble("total_tarjeta"));
        cierre.setTotalDescuentos(rs.getDouble("total_descuentos")); cierre.setTotalGeneral(rs.getDouble("total_general"));
        return cierre;
    };
    public Optional<CierreDiario> buscarPorFecha(LocalDate fecha) {
        return jdbc.query("SELECT * FROM cierres_diarios WHERE fecha=?", mapper, fecha).stream().findFirst();
    }
    public List<CierreDiario> listar() { return jdbc.query("SELECT * FROM cierres_diarios ORDER BY fecha DESC", mapper); }
    public int siguienteNumero() {
        return jdbc.queryForObject("SELECT COALESCE(MAX(numero_informe),0)+1 FROM cierres_diarios", Integer.class);
    }
    public CierreDiario guardar(CierreDiario cierre) {
        cierre.setId(insertar("""
                INSERT INTO cierres_diarios(fecha,fecha_hora_cierre,numero_informe,cantidad_tickets,
                    total_efectivo,total_tarjeta,total_descuentos,total_general) VALUES(?,?,?,?,?,?,?,?)
                """, cierre.getFecha(), cierre.getFechaHoraCierre(), cierre.getNumeroInforme(), cierre.getCantidadTickets(),
                cierre.getTotalEfectivo(), cierre.getTotalTarjeta(), cierre.getTotalDescuentos(), cierre.getTotalGeneral()));
        return cierre;
    }
}
