package ar.edu.cafe.config;

import ar.edu.cafe.dao.PerfilMozoDAO;
import ar.edu.cafe.model.PerfilMozo;
import ar.edu.cafe.service.PerfilMozoService;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class MigracionPerfiles implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PerfilMozoDAO perfiles;
    private final Clock clock;
    public MigracionPerfiles(JdbcTemplate jdbc, PerfilMozoDAO perfiles, Clock clock) {
        this.jdbc = jdbc; this.perfiles = perfiles; this.clock = clock;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // CREATE TABLE IF NOT EXISTS no actualiza las instalaciones anteriores.
        boolean columna = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) conexion -> {
            try (var columnas = conexion.getMetaData().getColumns(conexion.getCatalog(), null, "pedidos", null)) {
                while (columnas.next()) if ("perfil_mozo_id".equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) return true;
            }
            return false;
        }));
        if (!columna) jdbc.execute("ALTER TABLE pedidos ADD COLUMN perfil_mozo_id BIGINT NULL");
        boolean referencia = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) conexion -> {
            try (var claves = conexion.getMetaData().getImportedKeys(conexion.getCatalog(), null, "pedidos")) {
                while (claves.next()) if ("perfil_mozo_id".equalsIgnoreCase(claves.getString("FKCOLUMN_NAME"))) return true;
            }
            return false;
        }));
        if (!referencia) jdbc.execute("ALTER TABLE pedidos ADD CONSTRAINT fk_pedido_perfil FOREIGN KEY (perfil_mozo_id) REFERENCES perfiles_mozo(id)");
        for (var pedido : jdbc.queryForList("SELECT id,mozo FROM pedidos WHERE perfil_mozo_id IS NULL")) {
            String nombre = (String) pedido.get("mozo");
            String clave = PerfilMozoService.clave(nombre);
            PerfilMozo perfil = perfiles.buscarPorClave(clave).orElseGet(() -> {
                PerfilMozo nuevo = new PerfilMozo(); nuevo.setNombre(PerfilMozoService.nombreValido(nombre));
                nuevo.setFechaCreacion(LocalDateTime.now(clock)); return perfiles.guardar(nuevo, clave);
            });
            jdbc.update("UPDATE pedidos SET perfil_mozo_id=? WHERE id=? AND perfil_mozo_id IS NULL", perfil.getId(), pedido.get("id"));
        }
    }
}
