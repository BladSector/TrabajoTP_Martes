package ar.edu.cafe.service;

import ar.edu.cafe.dao.PerfilMozoDAO;
import ar.edu.cafe.model.PerfilMozo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilMozoService {
    private final PerfilMozoDAO perfilDAO;
    private final OperacionGuard guard;
    private final Clock clock;
    public PerfilMozoService(PerfilMozoDAO perfilDAO, OperacionGuard guard, Clock clock) {
        this.perfilDAO = perfilDAO; this.guard = guard; this.clock = clock;
    }
    public static String nombreValido(String nombre) {
        return MenuService.texto(nombre, 100, "Nombre del mozo").replaceAll("\\s+", " ");
    }
    public static String clave(String nombre) { return nombreValido(nombre).toLowerCase(Locale.ROOT); }
    @Transactional(readOnly = true)
    public List<PerfilMozo> listar(boolean incluirEliminados) { return perfilDAO.listar(incluirEliminados); }
    @Transactional(readOnly = true)
    public PerfilMozo obtenerActivo(Integer id) {
        PerfilMozo perfil = perfilDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Perfil inexistente"));
        if (perfil.isEliminado()) throw new IllegalArgumentException("El perfil fue eliminado. Seleccione otro perfil");
        return perfil;
    }
    @Transactional
    public PerfilMozo guardar(String nombre) {
        guard.bloquear();
        String valido = nombreValido(nombre);
        return perfilDAO.buscarPorClave(clave(valido)).orElseGet(() -> {
            PerfilMozo perfil = new PerfilMozo(); perfil.setNombre(valido);
            perfil.setFechaCreacion(LocalDateTime.now(clock)); return perfilDAO.guardar(perfil, clave(valido));
        });
    }
    @Transactional
    public void eliminar(Integer id) {
        guard.bloquear(); obtenerActivo(id);
        if (perfilDAO.tienePedidosPendientes(id)) {
            throw new IllegalArgumentException("El perfil tiene pedidos pendientes. Entreguelos o cancelelos antes de eliminarlo");
        }
        perfilDAO.eliminar(id);
    }
}
