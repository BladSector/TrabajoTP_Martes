package ar.edu.cafe.config;

import ar.edu.cafe.dao.*;
import ar.edu.cafe.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.core.annotation.Order(1)
public class DatosIniciales implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final MesaDAO mesaDAO;
    private final CategoriaDAO categoriaDAO;
    private final ObjectMapper mapper;
    private final ResourceLoader recursos;
    private final String ubicacionMenu;
    public DatosIniciales(JdbcTemplate jdbc, MesaDAO mesaDAO, CategoriaDAO categoriaDAO, ObjectMapper mapper,
            ResourceLoader recursos, @Value("${cafe.menu-json}") String ubicacionMenu) {
        this.jdbc = jdbc; this.mesaDAO = mesaDAO; this.categoriaDAO = categoriaDAO;
        this.mapper = mapper; this.recursos = recursos; this.ubicacionMenu = ubicacionMenu;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        jdbc.update("INSERT INTO control_sistema(id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM control_sistema WHERE id=1)");
        if (mesaDAO.listar().isEmpty()) {
            for (int i = 1; i <= 9; i++) {
                Mesa mesa = new Mesa();
                mesa.setNombre(i <= 6 ? "Mesa " + i : i == 7 ? "Barra interior" : i == 8 ? "Barra exterior" : "Take Away Barra");
                mesa.setEsTakeAway(i == 9); mesaDAO.guardar(mesa);
            }
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM productos", Integer.class) != 0) return;
        var recurso = recursos.getResource(ubicacionMenu);
        if (!recurso.exists()) throw new IllegalStateException("No se encontro el JSON inicial del menu");
        JsonNode menu;
        try (var entrada = recurso.getInputStream()) { menu = mapper.readTree(entrada).path("menu"); }
        if (!menu.isObject()) throw new IllegalArgumentException("El archivo debe contener un objeto menu");
        Iterator<Map.Entry<String, JsonNode>> categorias = menu.fields();
        while (categorias.hasNext()) {
            var entrada = categorias.next();
            if (!entrada.getValue().isArray()) throw new IllegalArgumentException("Categoria de menu invalida");
            Categoria categoria = categoriaDAO.buscarPorNombre(entrada.getKey()).orElseGet(() -> {
                Categoria nueva = new Categoria(); nueva.setNombre(entrada.getKey()); return categoriaDAO.guardar(nueva);
            });
            for (JsonNode item : entrada.getValue()) {
                if (!item.has("id") || !item.has("precio") || item.path("nombre").asText().isBlank()) {
                    throw new IllegalArgumentException("Producto inicial incompleto");
                }
                JsonNode descripcion = item.path("descripcion");
                String texto = descripcion.isArray()
                        ? StreamSupport.stream(descripcion.spliterator(), false).map(JsonNode::asText).collect(Collectors.joining("\n"))
                        : descripcion.asText("");
                jdbc.update("INSERT INTO productos(id,categoria_id,nombre,descripcion,precio,disponible,eliminado) VALUES(?,?,?,?,?,?,FALSE)",
                        item.get("id").asLong(), categoria.getId(), item.get("nombre").asText(), texto,
                        Dinero.noNegativo(item.get("precio").doubleValue()), item.path("disponible").asBoolean(true));
            }
        }
        if (jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) conexion ->
                "H2".equals(conexion.getMetaData().getDatabaseProductName()))) {
            Long siguiente = jdbc.queryForObject("SELECT MAX(id)+1 FROM productos", Long.class);
            if (siguiente != null) jdbc.execute("ALTER TABLE productos ALTER COLUMN id RESTART WITH " + siguiente);
        }
    }
}
