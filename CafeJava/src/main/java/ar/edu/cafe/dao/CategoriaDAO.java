package ar.edu.cafe.dao;

import ar.edu.cafe.model.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriaDAO extends JdbcDAO {
    public CategoriaDAO(JdbcTemplate jdbc) { super(jdbc); }
    private final RowMapper<Categoria> mapper = (rs, fila) -> {
        Categoria categoria = new Categoria();
        categoria.setId(rs.getLong("id")); categoria.setNombre(rs.getString("nombre"));
        return categoria;
    };
    public List<Categoria> listar() { return jdbc.query("SELECT * FROM categorias ORDER BY nombre", mapper); }
    public Optional<Categoria> buscarPorId(Long id) {
        return jdbc.query("SELECT * FROM categorias WHERE id=?", mapper, id).stream().findFirst();
    }
    public Optional<Categoria> buscarPorNombre(String nombre) {
        return jdbc.query("SELECT * FROM categorias WHERE nombre=?", mapper, nombre).stream().findFirst();
    }
    public Categoria guardar(Categoria categoria) {
        categoria.setId(insertar("INSERT INTO categorias(nombre) VALUES(?)", categoria.getNombre()));
        return categoria;
    }
}
