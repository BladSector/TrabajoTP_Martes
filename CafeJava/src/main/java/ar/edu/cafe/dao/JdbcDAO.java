package ar.edu.cafe.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

public abstract class JdbcDAO {
    protected final JdbcTemplate jdbc;

    protected JdbcDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    protected Long insertar(String sql, Object... valores) {
        GeneratedKeyHolder claves = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < valores.length; i++) sentencia.setObject(i + 1, valores[i]);
            return sentencia;
        }, claves);
        Number clave = claves.getKey();
        if (clave == null) throw new IllegalStateException("La base no devolvio el identificador");
        return clave.longValue();
    }
}
