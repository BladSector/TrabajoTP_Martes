package ar.edu.cafe;

import ar.edu.cafe.dao.JdbcDAO;
import ar.edu.cafe.model.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

class IdentificadoresEnterosTest {
    private static class PruebaDAO extends JdbcDAO {
        PruebaDAO(JdbcTemplate jdbc) { super(jdbc); }
        Integer crear() { return insertar("INSERT INTO identificadores(nombre) VALUES(?)", "prueba"); }
    }

    private SingleConnectionDataSource baseTemporal() {
        return new SingleConnectionDataSource("jdbc:h2:mem:ids-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "", true);
    }

    @Test
    void identificadoresIntegerPermitenNuloAntesDeAsignarlos() throws Exception {
        assertThat(new Mesa().getId()).isNull();
        assertThat(new Pedido().getCuentaId()).isNull();
        assertThat(new Pedido().getPerfilMozoId()).isNull();
        assertThat(new TicketElectronico().getNumeroComprobante()).isNull();
        for (Class<?> tipo : new Class<?>[]{Mesa.class, Categoria.class, Producto.class, Cuenta.class,
                Pedido.class, Pago.class, DetallePago.class, TicketElectronico.class, ItemTicket.class,
                CierreDiario.class, PerfilMozo.class}) {
            assertThat(tipo.getDeclaredField("id").getType()).isEqualTo(Integer.class);
        }
    }

    @Test
    void claveGeneradaUsaIntegerYRevierteSiSuperaSuLimite() {
        SingleConnectionDataSource fuente = baseTemporal();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(fuente);
            jdbc.execute("CREATE TABLE identificadores(id BIGINT AUTO_INCREMENT PRIMARY KEY,nombre VARCHAR(30))");
            jdbc.execute("ALTER TABLE identificadores ALTER COLUMN id RESTART WITH 2147483647");
            PruebaDAO dao = new PruebaDAO(jdbc);
            TransactionTemplate transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
            Integer id = transaccion.execute(estado -> dao.crear());
            assertThat(id).isEqualTo(Integer.MAX_VALUE);
            assertThatThrownBy(() -> transaccion.execute(estado -> dao.crear())).isInstanceOf(ArithmeticException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM identificadores", Integer.class)).isEqualTo(1);
        } finally { fuente.destroy(); }
    }

    @Test
    void lecturaDeBigintFueraDeRangoNoTruncaElIdentificador() {
        SingleConnectionDataSource fuente = baseTemporal();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(fuente);
            jdbc.execute("CREATE TABLE identificadores(id BIGINT PRIMARY KEY)");
            jdbc.update("INSERT INTO identificadores(id) VALUES(2147483648)");
            assertThatThrownBy(() -> jdbc.queryForObject("SELECT id FROM identificadores", Integer.class))
                    .isInstanceOf(DataAccessException.class);
        } finally { fuente.destroy(); }
    }
}
