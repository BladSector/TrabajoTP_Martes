package ar.edu.cafe.service;

import ar.edu.cafe.dao.*;
import ar.edu.cafe.model.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {
    public enum Operacion { CREAR, ACTUALIZAR, ELIMINAR }
    public record Cambio(Operacion operacion, Integer id, Producto producto) { }
    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    public MenuService(ProductoDAO productoDAO, CategoriaDAO categoriaDAO) {
        this.productoDAO = productoDAO; this.categoriaDAO = categoriaDAO;
    }
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() { return productoDAO.listar(); }
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() { return categoriaDAO.listar(); }
    @Transactional
    public Categoria crearCategoria(Categoria categoria) {
        if (categoria == null) throw new IllegalArgumentException("Categoria obligatoria");
        String nombre = texto(categoria.getNombre(), 100, "Nombre de categoria");
        categoria.setNombre(nombre);
        return categoriaDAO.buscarPorNombre(nombre).orElseGet(() -> categoriaDAO.guardar(categoria));
    }
    @Transactional
    public Producto crearProducto(Producto producto) {
        validarProducto(producto); producto.setId(null); producto.setEliminado(false);
        return productoDAO.guardar(producto);
    }
    @Transactional
    public void actualizarProducto(Producto producto) {
        if (producto == null || producto.getId() == null) throw new IllegalArgumentException("Identificador obligatorio");
        Producto actual = productoDAO.buscarPorId(producto.getId()).orElseThrow(() -> new NoSuchElementException("Producto inexistente"));
        if (actual.isEliminado()) throw new IllegalArgumentException("El producto fue eliminado");
        validarProducto(producto); productoDAO.actualizar(producto);
    }
    @Transactional
    public void eliminarProducto(Integer id) {
        productoDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Producto inexistente"));
        productoDAO.eliminar(id);
    }
    @Transactional
    public void aplicarCambios(List<Cambio> cambios) {
        if (cambios == null || cambios.isEmpty() || cambios.size() > 100) {
            throw new IllegalArgumentException("Seleccione entre 1 y 100 cambios del menu");
        }
        Set<Integer> ids = new HashSet<>();
        for (Cambio cambio : cambios) {
            if (cambio == null || cambio.operacion() == null) throw new IllegalArgumentException("Cambio invalido");
            if (cambio.operacion() != Operacion.CREAR && (cambio.id() == null || !ids.add(cambio.id()))) {
                throw new IllegalArgumentException("Identificador ausente o repetido en los cambios");
            }
            switch (cambio.operacion()) {
                case CREAR -> crearProducto(cambio.producto());
                case ACTUALIZAR -> {
                    if (cambio.producto() == null) throw new IllegalArgumentException("Producto obligatorio");
                    cambio.producto().setId(cambio.id()); actualizarProducto(cambio.producto());
                }
                case ELIMINAR -> eliminarProducto(cambio.id());
            }
        }
    }
    private void validarProducto(Producto producto) {
        if (producto == null) throw new IllegalArgumentException("Producto obligatorio");
        producto.setNombre(texto(producto.getNombre(), 200, "Nombre de producto"));
        if (producto.getCategoria() == null || producto.getCategoria().getId() == null) {
            throw new IllegalArgumentException("Seleccione una categoria");
        }
        producto.setCategoria(categoriaDAO.buscarPorId(producto.getCategoria().getId())
                .orElseThrow(() -> new NoSuchElementException("Categoria inexistente")));
        producto.actualizarPrecio(producto.getPrecio());
        if (producto.getDescripcion() == null) producto.setDescripcion("");
        if (producto.getDescripcion().length() > 10000) throw new IllegalArgumentException("Descripcion demasiado larga");
    }
    public static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {
            throw new IllegalArgumentException(campo + " obligatorio, hasta " + maximo + " caracteres");
        }
        return valor.strip();
    }
}
