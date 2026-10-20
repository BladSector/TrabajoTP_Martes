package ar.edu.cafe.dao;

import ar.edu.cafe.model.PerfilMozo;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PerfilMozoDAO extends JdbcDAO {
    public PerfilMozoDAO(JdbcTemplate jdbc) { super(jdbc); }
    private final RowMapper<PerfilMozo> mapper = (rs, fila) -> {
        PerfilMozo perfil = new PerfilMozo();
        perfil.setId(rs.getInt("id")); perfil.setNombre(rs.getString("nombre"));
        perfil.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
        perfil.setEliminado(rs.getBoolean("eliminado")); return perfil;
    };
    public List<PerfilMozo> listar(boolean incluirEliminados) {
        return jdbc.query("SELECT * FROM perfiles_mozo" + (incluirEliminados ? "" : " WHERE eliminado=FALSE")
                + " ORDER BY nombre,id", mapper);
    }
    public Optional<PerfilMozo> buscarPorId(Integer id) {
        return jdbc.query("SELECT * FROM perfiles_mozo WHERE id=?", mapper, id).stream().findFirst();
    }
    public Optional<PerfilMozo> buscarPorClave(String clave) {
        return jdbc.query("SELECT * FROM perfiles_mozo WHERE clave_activa=?", mapper, clave).stream().findFirst();
    }
    public PerfilMozo guardar(PerfilMozo perfil, String clave) {
        perfil.setId(insertar("INSERT INTO perfiles_mozo(nombre,clave_activa,fecha_creacion,eliminado) VALUES(?,?,?,FALSE)",
                perfil.getNombre(), clave, perfil.getFechaCreacion())); return perfil;
    }
    public boolean tienePedidosPendientes(Integer id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pedidos WHERE perfil_mozo_id=? AND cancelado=FALSE"
                + " AND estado_cocina<>'ENTREGADO'", Integer.class, id) > 0;
    }
    public void eliminar(Integer id) {
        jdbc.update("UPDATE perfiles_mozo SET eliminado=TRUE,clave_activa=NULL WHERE id=?", id);
    }
}
