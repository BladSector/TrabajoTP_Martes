package ar.edu.cafe.service;

import ar.edu.cafe.dao.*;
import ar.edu.cafe.model.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {
    private final PedidoDAO pedidoDAO;
    private final CuentaDAO cuentaDAO;
    private final ProductoDAO productoDAO;
    private final MesaService mesaService;
    private final OperacionGuard guard;
    private final Clock clock;
    private final PerfilMozoService perfiles;
    public PedidoService(PedidoDAO pedidoDAO, CuentaDAO cuentaDAO, ProductoDAO productoDAO,
            MesaService mesaService, OperacionGuard guard, Clock clock, PerfilMozoService perfiles) {
        this.pedidoDAO = pedidoDAO; this.cuentaDAO = cuentaDAO; this.productoDAO = productoDAO;
        this.mesaService = mesaService; this.guard = guard; this.clock = clock;
        this.perfiles = perfiles;
    }
    @Transactional(readOnly = true)
    public List<Pedido> listarParaCocina() { return pedidoDAO.listarParaCocina(); }

    @Transactional(readOnly = true)
    public List<Pedido> consultarHistorial(java.time.LocalDate fecha, Integer perfilId) {
        return pedidoDAO.consultarHistorial(fecha == null ? java.time.LocalDate.now(clock) : fecha, perfilId);
    }

    @Transactional
    public Cuenta enviarComanda(Integer mesaId, List<Pedido> nuevosPedidos) {
        guard.bloquearDiaAbierto();
        if (nuevosPedidos == null || nuevosPedidos.isEmpty() || nuevosPedidos.size() > 100) {
            throw new IllegalArgumentException("La comanda debe contener entre 1 y 100 productos");
        }
        Cuenta cuenta = mesaService.abrirCuenta(mesaId);
        List<Pedido> registrados = new java.util.ArrayList<>();
        for (Pedido pedido : nuevosPedidos) registrados.add(agregarPedido(cuenta.getId(), pedido));
        enviarACocina(registrados);
        return mesaService.obtenerCuenta(cuenta.getId());
    }
    private Cuenta cuentaAbierta(Integer id) {
        Cuenta cuenta = cuentaDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Cuenta inexistente"));
        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) throw new IllegalArgumentException("La cuenta esta cerrada");
        return cuenta;
    }
    @Transactional
    public Pedido agregarPedido(Integer cuentaId, Pedido pedido) {
        guard.bloquearDiaAbierto();
        Cuenta cuenta = cuentaAbierta(cuentaId);
        if (pedido == null || pedido.getProducto() == null || pedido.getProducto().getId() == null) {
            throw new IllegalArgumentException("Seleccione un producto");
        }
        if (pedido.getCantidad() <= 0 || pedido.getCantidad() > 1000) {
            throw new IllegalArgumentException("Cantidad entre 1 y 1000");
        }
        Producto producto = productoDAO.buscarPorId(pedido.getProducto().getId())
                .orElseThrow(() -> new NoSuchElementException("Producto inexistente"));
        if (!producto.isDisponible() || producto.isEliminado()) throw new IllegalArgumentException("Producto no disponible");
        PerfilMozo perfil = pedido.getPerfilMozoId() == null ? perfiles.guardar(pedido.getMozo()) : perfiles.obtenerActivo(pedido.getPerfilMozoId());
        pedido.setPerfilMozoId(perfil.getId()); pedido.setMozo(perfil.getNombre());
        String cliente = pedido.getNombreCliente() == null ? "" : pedido.getNombreCliente().strip();
        if (cuenta.getMesa().isEsTakeAway()) cliente = MenuService.texto(cliente, 100, "Nombre del cliente");
        if (cliente.length() > 100) throw new IllegalArgumentException("Nombre del cliente demasiado largo");
        String notas = pedido.getNotas() == null ? "" : pedido.getNotas().strip();
        if (notas.length() > 1000) throw new IllegalArgumentException("Observaciones hasta 1000 caracteres");
        pedido.setId(null); pedido.setCuentaId(cuentaId); pedido.setProducto(producto);
        pedido.setNombreProducto(producto.getNombre()); pedido.setPrecioUnitario(producto.getPrecio());
        pedido.setNombreCliente(cliente); pedido.setNotas(notas); pedido.setCantidadPagada(0);
        pedido.setCancelado(false); pedido.setEstadoCocina(EstadoCocina.BORRADOR); pedido.setFechaHora(LocalDateTime.now(clock));
        return pedidoDAO.guardar(pedido);
    }
    @Transactional
    public void enviarACocina(List<Pedido> pedidos) {
        guard.bloquearDiaAbierto();
        if (pedidos == null || pedidos.isEmpty()) throw new IllegalArgumentException("Seleccione pedidos");
        for (Pedido referencia : pedidos) {
            if (referencia == null || referencia.getId() == null) throw new IllegalArgumentException("Pedido sin identificador");
            Pedido pedido = pedidoDAO.buscarPorId(referencia.getId()).orElseThrow(() -> new NoSuchElementException("Pedido inexistente"));
            cuentaAbierta(pedido.getCuentaId());
            if (pedido.isCancelado()) throw new IllegalArgumentException("Pedido cancelado");
            if (pedido.getEstadoCocina() == EstadoCocina.BORRADOR) {
                pedido.cambiarEstadoCocina(EstadoCocina.PENDIENTE); pedidoDAO.actualizar(pedido);
            }
        }
    }
    @Transactional
    public void cancelarPedido(Integer id) {
        guard.bloquearDiaAbierto();
        Pedido pedido = pedidoDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Pedido inexistente"));
        cuentaAbierta(pedido.getCuentaId()); pedido.cancelar(); pedidoDAO.actualizar(pedido);
        mesaService.cerrarSiCompletada(pedido.getCuentaId());
    }
    @Transactional
    public void cambiarEstado(Integer id, EstadoCocina siguiente) {
        guard.bloquear();
        Pedido pedido = pedidoDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Pedido inexistente"));
        if (siguiente != EstadoCocina.EN_PREPARACION && siguiente != EstadoCocina.LISTO) {
            throw new IllegalArgumentException("Cocina solo puede preparar o marcar listo. La entrega la confirma el mozo");
        }
        // Preparacion y entrega siguen disponibles aunque se haya cobrado o cerrado la caja.
        if (pedido.isCancelado() || pedido.getEstadoCocina() == EstadoCocina.BORRADOR
                || siguiente == null || siguiente.ordinal() != pedido.getEstadoCocina().ordinal() + 1) {
            throw new IllegalArgumentException("Transicion de cocina invalida");
        }
        pedido.cambiarEstadoCocina(siguiente); pedidoDAO.actualizar(pedido);
    }

    @Transactional
    public void confirmarEntrega(Integer id, String mozo) {
        confirmarEntrega(id, perfiles.guardar(mozo).getId());
    }

    @Transactional
    public void confirmarEntrega(Integer id, Integer perfilId) {
        guard.bloquear();
        Pedido pedido = pedidoDAO.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Pedido inexistente"));
        PerfilMozo perfil = perfiles.obtenerActivo(perfilId);
        if (!perfil.getId().equals(pedido.getPerfilMozoId())) {
            throw new IllegalArgumentException("La entrega debe confirmarla el mozo " + pedido.getMozo());
        }
        if (pedido.isCancelado() || pedido.getEstadoCocina() != EstadoCocina.LISTO) {
            throw new IllegalArgumentException("Solo se puede entregar un pedido listo y no cancelado");
        }
        pedido.cambiarEstadoCocina(EstadoCocina.ENTREGADO); pedidoDAO.actualizar(pedido);
        mesaService.cerrarSiCompletada(pedido.getCuentaId());
    }
}
