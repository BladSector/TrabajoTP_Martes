package ar.edu.cafe.service;

import ar.edu.cafe.dao.*;
import ar.edu.cafe.model.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MesaService {
    private final MesaDAO mesaDAO;
    private final CuentaDAO cuentaDAO;
    private final PedidoDAO pedidoDAO;
    private final OperacionGuard guard;
    private final Clock clock;

    public MesaService(MesaDAO mesaDAO, CuentaDAO cuentaDAO, PedidoDAO pedidoDAO, OperacionGuard guard, Clock clock) {
        this.mesaDAO = mesaDAO; this.cuentaDAO = cuentaDAO; this.guard = guard; this.clock = clock;
        this.pedidoDAO = pedidoDAO;
    }
    @Transactional(readOnly = true)
    public List<Mesa> listarMesas() { return mesaDAO.listar(); }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosMesa(Integer mesaId) { return pedidoDAO.listarPorMesaConPendientes(mesaId); }

    @Transactional
    public Cuenta abrirCuenta(Integer mesaId) {
        guard.bloquearDiaAbierto();
        Mesa mesa = mesaDAO.buscarPorId(mesaId).orElseThrow(() -> new NoSuchElementException("Mesa inexistente"));
        Optional<Cuenta> abierta = cuentaDAO.buscarAbiertaPorMesa(mesaId);
        if (abierta.isPresent()) return abierta.get();
        if (listarPedidosMesa(mesaId).stream().anyMatch(p -> !p.isCancelado() && p.getEstadoCocina() != EstadoCocina.ENTREGADO)) {
            throw new IllegalArgumentException("La mesa tiene pedidos anteriores sin entregar");
        }
        if (!mesa.estaLibre()) throw new IllegalArgumentException("La mesa ya esta ocupada");
        Cuenta cuenta = new Cuenta(); cuenta.setMesa(mesa); cuenta.setFechaApertura(LocalDateTime.now(clock));
        cuentaDAO.guardar(cuenta); mesa.ocupar(); mesaDAO.actualizar(mesa);
        return cuenta;
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerCuenta(Integer cuentaId) {
        return cuentaDAO.buscarPorId(cuentaId).orElseThrow(() -> new NoSuchElementException("Cuenta inexistente"));
    }

    @Transactional(readOnly = true)
    public Optional<Cuenta> obtenerCuentaAbierta(Integer mesaId) {
        mesaDAO.buscarPorId(mesaId).orElseThrow(() -> new NoSuchElementException("Mesa inexistente"));
        return cuentaDAO.buscarAbiertaPorMesa(mesaId);
    }

    @Transactional
    public void cerrarCuenta(Integer cuentaId) {
        guard.bloquear();
        Cuenta cuenta = obtenerCuenta(cuentaId);
        if (cuenta.getEstado() == EstadoCuenta.CERRADA) return;
        cuenta.cerrar(); cuenta.setFechaCierre(LocalDateTime.now(clock)); cuentaDAO.actualizar(cuenta);
        Mesa mesa = cuenta.getMesa(); mesa.liberar(); mesaDAO.actualizar(mesa);
    }

    @Transactional
    public boolean cerrarSiCompletada(Integer cuentaId) {
        guard.bloquear();
        Cuenta cuenta = obtenerCuenta(cuentaId);
        if (cuenta.getEstado() != EstadoCuenta.ABIERTA || !cuenta.puedeCerrarse()) return false;
        cerrarCuenta(cuentaId); return true;
    }
}
