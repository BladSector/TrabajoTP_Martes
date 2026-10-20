package ar.edu.cafe.dao;

import ar.edu.cafe.model.*;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MesaDAO extends JdbcDAO {
    public MesaDAO(JdbcTemplate jdbc) { super(jdbc); }

    private final RowMapper<Mesa> mapper = (rs, fila) -> {
        Mesa mesa = new Mesa();
        mesa.setId(rs.getInt("id"));
        mesa.setNombre(rs.getString("nombre"));
        mesa.setEstado(EstadoMesa.valueOf(rs.getString("estado")));
        mesa.setEsTakeAway(rs.getBoolean("es_take_away"));
        return mesa;
    };

    public List<Mesa> listar() { return jdbc.query("SELECT * FROM mesas ORDER BY id", mapper); }
    public Optional<Mesa> buscarPorId(Integer id) {
        return jdbc.query("SELECT * FROM mesas WHERE id=?", mapper, id).stream().findFirst();
    }
    public Mesa guardar(Mesa mesa) {
        mesa.setId(insertar("INSERT INTO mesas(nombre,estado,es_take_away) VALUES(?,?,?)",
                mesa.getNombre(), mesa.getEstado().name(), mesa.isEsTakeAway()));
        return mesa;
    }
    public void actualizar(Mesa mesa) {
        jdbc.update("UPDATE mesas SET nombre=?,estado=?,es_take_away=? WHERE id=?",
                mesa.getNombre(), mesa.getEstado().name(), mesa.isEsTakeAway(), mesa.getId());
    }
}
