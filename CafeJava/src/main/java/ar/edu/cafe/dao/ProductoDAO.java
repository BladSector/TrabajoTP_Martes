package ar.edu.cafe.dao;

import ar.edu.cafe.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductoDAO extends JdbcDAO {
    private final CategoriaDAO categoriaDAO;
    public ProductoDAO(JdbcTemplate jdbc, CategoriaDAO categoriaDAO) {
        super(jdbc); this.categoriaDAO = categoriaDAO;
    }
    private RowMapper<Producto> mapper() {
        return (rs, fila) -> {
            Producto producto = new Producto();
            producto.setId(rs.getLong("id")); producto.setNombre(rs.getString("nombre"));
            producto.setDescripcion(rs.getString("descripcion")); producto.setPrecio(rs.getDouble("precio"));
            producto.setDisponible(rs.getBoolean("disponible")); producto.setEliminado(rs.getBoolean("eliminado"));
            producto.setCategoria(categoriaDAO.buscarPorId(rs.getLong("categoria_id")).orElseThrow());
            return producto;
        };
    }
    public List<Producto> listar() {
        return jdbc.query("SELECT * FROM productos WHERE eliminado=FALSE ORDER BY categoria_id,nombre", mapper());
    }
    public Optional<Producto> buscarPorId(Long id) {
        return jdbc.query("SELECT * FROM productos WHERE id=?", mapper(), id).stream().findFirst();
    }
    public Producto guardar(Producto producto) {
        producto.setId(insertar("INSERT INTO productos(categoria_id,nombre,descripcion,precio,disponible,eliminado) VALUES(?,?,?,?,?,FALSE)",
                producto.getCategoria().getId(), producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.isDisponible()));
        return producto;
    }
    public void actualizar(Producto producto) {
        jdbc.update("UPDATE productos SET categoria_id=?,nombre=?,descripcion=?,precio=?,disponible=? WHERE id=?",
                producto.getCategoria().getId(), producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.isDisponible(), producto.getId());
    }
    public void eliminar(Long id) {
        jdbc.update("UPDATE productos SET eliminado=TRUE,disponible=FALSE WHERE id=?", id);
    }
}
