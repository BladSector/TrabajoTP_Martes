package ar.edu.cafe;

import ar.edu.cafe.config.MigracionPerfiles;
import ar.edu.cafe.dao.PerfilMozoDAO;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class MigracionPerfilesTest {
    @Test
    void migraEsquemaAnteriorSinBorrarNombresYEsIdempotente() {
        // Una conexion permanente mantiene esta base aislada durante toda la prueba.
        JdbcTemplate jdbc = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(
                "jdbc:h2:mem:migracion-perfiles;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "", true));
        try {
            jdbc.execute("CREATE TABLE perfiles_mozo(id BIGINT AUTO_INCREMENT PRIMARY KEY,nombre VARCHAR(100) NOT NULL,clave_activa VARCHAR(100) UNIQUE,fecha_creacion TIMESTAMP NOT NULL,eliminado BOOLEAN DEFAULT FALSE NOT NULL)");
            jdbc.execute("CREATE TABLE pedidos(id BIGINT PRIMARY KEY,mozo VARCHAR(100) NOT NULL)");
            jdbc.update("INSERT INTO pedidos VALUES(1,'Gabi'),(2,' gabi '),(3,'Ana')");
            PerfilMozoDAO dao = new PerfilMozoDAO(jdbc);
            MigracionPerfiles migracion = new MigracionPerfiles(jdbc, dao, Clock.systemUTC());
            var args = new DefaultApplicationArguments(new String[0]); migracion.run(args); migracion.run(args);
            assertThat(dao.listar(false)).hasSize(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedidos WHERE perfil_mozo_id IS NOT NULL", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT mozo FROM pedidos WHERE id=2", String.class)).isEqualTo(" gabi ");
            Integer gabi = jdbc.queryForObject("SELECT perfil_mozo_id FROM pedidos WHERE id=1", Integer.class);
            assertThat(jdbc.queryForObject("SELECT perfil_mozo_id FROM pedidos WHERE id=2", Integer.class)).isEqualTo(gabi);
            dao.eliminar(gabi); migracion.run(args);
            assertThat(dao.listar(false)).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT perfil_mozo_id FROM pedidos WHERE id=1", Integer.class)).isEqualTo(gabi);
        } finally {
            ((org.springframework.jdbc.datasource.SingleConnectionDataSource) jdbc.getDataSource()).destroy();
        }
    }
}
