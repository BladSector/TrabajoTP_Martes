package ar.edu.cafe.web;

import ar.edu.cafe.model.*;
import ar.edu.cafe.service.*;
import ar.edu.cafe.fiscal.ConfiguracionFiscal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CafeController {
    private final MesaService mesas;
    private final MenuService menu;
    private final PedidoService pedidos;
    private final PagoService pagos;
    private final TicketService tickets;
    private final CierreService cierres;
    private final ImpresionService impresion;
    private final ConfiguracionFiscal fiscal;
    private final Environment entorno;
    private final Clock clock;
    private final PerfilMozoService perfiles;
    public CafeController(MesaService mesas, MenuService menu, PedidoService pedidos, PagoService pagos,
            TicketService tickets, CierreService cierres, ImpresionService impresion,
            ConfiguracionFiscal fiscal, Environment entorno, Clock clock, PerfilMozoService perfiles) {
        this.mesas = mesas; this.menu = menu; this.pedidos = pedidos; this.pagos = pagos; this.tickets = tickets;
        this.cierres = cierres; this.impresion = impresion; this.fiscal = fiscal; this.entorno = entorno; this.clock = clock;
        this.perfiles = perfiles;
    }

    public record MesaVista(Mesa mesa, Integer cuentaId, double pendiente, List<Pedido> pedidos) { }
    public record ComandaVista(Pedido pedido, Mesa mesa) { }
    public record CuentaRespuesta(Cuenta cuenta) { }
    public record EstadoSistema(LocalDate fecha, boolean cerrado, String entornoFiscal, String baseDatos) { }
    public record ProductoSolicitud(@NotBlank @Size(max=200) String nombre, @Size(max=10000) String descripcion,
            @NotNull @PositiveOrZero Double precio, boolean disponible, @NotNull @Positive Integer categoriaId) { }
    public record CategoriaSolicitud(@NotBlank @Size(max=100) String nombre) { }
    public record PerfilSolicitud(@NotBlank @Size(max=100) String nombre) { }
    public record PedidoSolicitud(@NotNull @Positive Integer productoId, @Min(1) @Max(1000) int cantidad,
            @Size(max=100) String mozo, @Size(max=100) String nombreCliente, @Size(max=1000) String notas,
            @Positive Integer perfilMozoId) { }
    public record ComandaSolicitud(@NotEmpty @Size(max=100) List<@NotNull @Valid PedidoSolicitud> pedidos) { }
    public record CambioMenuSolicitud(@NotNull MenuService.Operacion tipo, @Positive Integer id,
            @Valid ProductoSolicitud producto) { }
    public record CambiosMenuSolicitud(@NotEmpty @Size(max=100) List<@NotNull @Valid CambioMenuSolicitud> cambios) { }
    public record CocinaSolicitud(@NotEmpty List<@NotNull @Positive Integer> pedidosIds) { }
    public record EstadoSolicitud(@NotNull EstadoCocina estado) { }
    public record EntregaSolicitud(@Size(max=100) String mozo, @Positive Integer perfilMozoId) { }
    public record ItemPagoSolicitud(@NotNull @Positive Integer pedidoId, @Min(1) int cantidad) { }
    public record PagoSolicitud(@NotNull MetodoPago metodo, @NotEmpty List<@Valid ItemPagoSolicitud> detalles,
            @Null(message="El total en efectivo es automatico; actualice la aplicacion") Double totalEfectivo) { }
    public record PagoRespuesta(Pago pago, TicketElectronico ticket, boolean cuentaCerrada, boolean cuentaSaldada) { }

    @GetMapping("/perfiles")
    public List<PerfilMozo> listarPerfiles(@RequestParam(defaultValue="false") boolean incluirEliminados) {
        return perfiles.listar(incluirEliminados);
    }
    @PostMapping("/perfiles")
    public PerfilMozo guardarPerfil(@Valid @RequestBody PerfilSolicitud solicitud) { return perfiles.guardar(solicitud.nombre()); }
    @DeleteMapping("/perfiles/{id}")
    public void eliminarPerfil(@PathVariable Integer id) { perfiles.eliminar(id); }
    @GetMapping("/pedidos/historial")
    public List<ComandaVista> historialPedidos(@RequestParam(required=false) LocalDate fecha, @RequestParam(required=false) @Positive Integer perfilId) {
        Map<Integer, Mesa> mesasPorCuenta = new HashMap<>();
        return pedidos.consultarHistorial(fecha, perfilId).stream().map(pedido -> new ComandaVista(pedido,
                mesasPorCuenta.computeIfAbsent(pedido.getCuentaId(), cuentaId -> mesas.obtenerCuenta(cuentaId).getMesa()))).toList();
    }

    @GetMapping("/estado")
    public EstadoSistema estado() {
        LocalDate fecha = LocalDate.now(clock);
        boolean demo = Arrays.asList(entorno.getActiveProfiles()).contains("demo");
        return new EstadoSistema(fecha, cierres.existeCierre(fecha), fiscal.getEntorno(), demo ? "DEMO" : "MYSQL");
    }
    @GetMapping("/mesas")
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public List<MesaVista> listarMesas() {
        return mesas.listarMesas().stream().map(mesa -> {
            Cuenta cuenta = mesas.obtenerCuentaAbierta(mesa.getId()).orElse(null);
            double pendiente = cuenta == null ? Dinero.CERO : Dinero.redondear(cuenta.obtenerPedidosPendientesDePago().stream()
                    .mapToDouble(p -> p.getPrecioUnitario() * p.obtenerCantidadPendienteDePago())
                    .sum());
            return new MesaVista(mesa, cuenta == null ? null : cuenta.getId(), pendiente,
                    mesas.listarPedidosMesa(mesa.getId()));
        }).toList();
    }
    @GetMapping("/mesas/{id}/cuenta")
    public CuentaRespuesta cuentaMesa(@PathVariable Integer id) { return new CuentaRespuesta(mesas.obtenerCuentaAbierta(id).orElse(null)); }
    @PostMapping("/mesas/{id}/cuenta")
    public Cuenta abrirCuenta(@PathVariable Integer id) { return mesas.abrirCuenta(id); }
    @GetMapping("/cuentas/{id}")
    public Cuenta cuenta(@PathVariable Integer id) { return mesas.obtenerCuenta(id); }
    @PostMapping("/cuentas/{id}/cerrar")
    public void cerrarCuenta(@PathVariable Integer id) { mesas.cerrarCuenta(id); }

    @GetMapping("/menu")
    public List<Producto> productos() { return menu.listarProductos(); }
    @GetMapping("/categorias")
    public List<Categoria> categorias() { return menu.listarCategorias(); }
    @PostMapping("/menu/cambios")
    public void aplicarCambiosMenu(@Valid @RequestBody CambiosMenuSolicitud solicitud) {
        menu.aplicarCambios(solicitud.cambios().stream().map(cambio -> new MenuService.Cambio(
                cambio.tipo(), cambio.id(), cambio.producto() == null ? null : producto(cambio.producto()))).toList());
    }
    @PostMapping("/categorias")
    public Categoria crearCategoria(@Valid @RequestBody CategoriaSolicitud solicitud) {
        Categoria categoria = new Categoria(); categoria.setNombre(solicitud.nombre()); return menu.crearCategoria(categoria);
    }
    private Producto producto(ProductoSolicitud solicitud) {
        Producto producto = new Producto(); producto.setNombre(solicitud.nombre()); producto.setDescripcion(solicitud.descripcion());
        producto.setPrecio(solicitud.precio()); producto.setDisponible(solicitud.disponible());
        Categoria categoria = new Categoria(); categoria.setId(solicitud.categoriaId()); producto.setCategoria(categoria);
        return producto;
    }
    @PostMapping("/menu")
    public Producto crearProducto(@Valid @RequestBody ProductoSolicitud solicitud) { return menu.crearProducto(producto(solicitud)); }
    @PutMapping("/menu/{id}")
    public void actualizarProducto(@PathVariable Integer id, @Valid @RequestBody ProductoSolicitud solicitud) {
        Producto producto = producto(solicitud); producto.setId(id); menu.actualizarProducto(producto);
    }
    @DeleteMapping("/menu/{id}")
    public void eliminarProducto(@PathVariable Integer id) { menu.eliminarProducto(id); }

    @PostMapping("/cuentas/{id}/pedidos")
    public Pedido agregarPedido(@PathVariable Integer id, @Valid @RequestBody PedidoSolicitud solicitud) {
        return pedidos.agregarPedido(id, pedido(solicitud));
    }
    private Pedido pedido(PedidoSolicitud solicitud) {
        Pedido pedido = new Pedido(); Producto producto = new Producto(); producto.setId(solicitud.productoId());
        pedido.setProducto(producto); pedido.setCantidad(solicitud.cantidad()); pedido.setMozo(solicitud.mozo());
        pedido.setPerfilMozoId(solicitud.perfilMozoId());
        pedido.setNombreCliente(solicitud.nombreCliente()); pedido.setNotas(solicitud.notas());
        return pedido;
    }
    @PostMapping("/mesas/{id}/comandas")
    public Cuenta enviarComanda(@PathVariable Integer id, @Valid @RequestBody ComandaSolicitud solicitud) {
        return pedidos.enviarComanda(id, solicitud.pedidos().stream().map(this::pedido).toList());
    }
    @GetMapping("/cocina")
    public List<ComandaVista> cocina() {
        Map<Integer, Mesa> mesasPorCuenta = new HashMap<>();
        return pedidos.listarParaCocina().stream().map(pedido -> new ComandaVista(pedido,
                mesasPorCuenta.computeIfAbsent(pedido.getCuentaId(), cuentaId -> mesas.obtenerCuenta(cuentaId).getMesa()))).toList();
    }
    @PostMapping("/cuentas/{id}/cocina")
    public void enviarCocina(@PathVariable Integer id, @Valid @RequestBody CocinaSolicitud solicitud) {
        Cuenta cuenta = mesas.obtenerCuenta(id);
        var propios = cuenta.getPedidos().stream().map(Pedido::getId).toList();
        if (!propios.containsAll(solicitud.pedidosIds())) throw new IllegalArgumentException("Hay pedidos de otra cuenta");
        List<Pedido> referencias = solicitud.pedidosIds().stream().map(pedidoId -> {
            Pedido pedido = new Pedido(); pedido.setId(pedidoId); return pedido;
        }).toList();
        pedidos.enviarACocina(referencias);
    }
    @PostMapping("/pedidos/{id}/cancelar")
    public void cancelarPedido(@PathVariable Integer id) { pedidos.cancelarPedido(id); }
    @PutMapping("/pedidos/{id}/estado")
    public void estadoPedido(@PathVariable Integer id, @Valid @RequestBody EstadoSolicitud solicitud) {
        pedidos.cambiarEstado(id, solicitud.estado());
    }
    @PostMapping("/pedidos/{id}/entregar")
    public void entregarPedido(@PathVariable Integer id, @Valid @RequestBody EntregaSolicitud solicitud) {
        if (solicitud.perfilMozoId() != null) pedidos.confirmarEntrega(id, solicitud.perfilMozoId());
        else pedidos.confirmarEntrega(id, solicitud.mozo());
    }
    @PostMapping("/cuentas/{id}/pagos")
    public PagoRespuesta pagar(@PathVariable Integer id, @Valid @RequestBody PagoSolicitud solicitud) {
        Pago pago = new Pago(); pago.setMetodo(solicitud.metodo());
        List<DetallePago> detalles = new ArrayList<>();
        for (ItemPagoSolicitud item : solicitud.detalles()) {
            Pedido pedido = new Pedido(); pedido.setId(item.pedidoId());
            DetallePago detalle = new DetallePago(); detalle.setPedido(pedido); detalle.setCantidad(item.cantidad()); detalles.add(detalle);
        }
        pago.setDetalles(detalles);
        Pago registrado = pagos.registrarPago(id, pago);
        Cuenta cuenta = mesas.obtenerCuenta(id);
        return new PagoRespuesta(registrado, tickets.obtenerPorPago(registrado.getId()),
                cuenta.getEstado() == EstadoCuenta.CERRADA, cuenta.estaSaldada());
    }
    @GetMapping("/tickets")
    public List<TicketElectronico> historialTickets() { return tickets.consultarHistorial(); }
    @GetMapping("/tickets/{id}")
    public TicketElectronico ticket(@PathVariable Integer id) { return tickets.obtenerTicket(id); }
    @GetMapping(value="/tickets/{id}/texto", produces="text/plain;charset=UTF-8")
    public String textoTicket(@PathVariable Integer id) { return impresion.generarContenido(tickets.obtenerTicket(id)); }
    @PostMapping("/tickets/{id}/imprimir")
    public void imprimirTicket(@PathVariable Integer id) {
        if (!impresion.imprimirTicket(tickets.obtenerTicket(id))) {
            throw new IllegalArgumentException("Impresora no configurada o formato no compatible; puede imprimir desde el navegador");
        }
    }
    @GetMapping("/cierres/resumen")
    public CierreDiario resumen(@RequestParam(required=false) LocalDate fecha) {
        return cierres.obtenerResumen(fecha == null ? LocalDate.now(clock) : fecha);
    }
    @PostMapping("/cierres")
    public CierreDiario cerrarDia(@RequestParam(required=false) LocalDate fecha) {
        return cierres.generarCierre(fecha == null ? LocalDate.now(clock) : fecha);
    }
    @GetMapping("/cierres")
    public List<CierreDiario> historialCierres() { return cierres.consultarHistorial(); }
    private CierreDiario cierreGuardado(LocalDate fecha) {
        if (!cierres.existeCierre(fecha)) throw new IllegalArgumentException("La jornada todavia no tiene un cierre registrado");
        return cierres.obtenerResumen(fecha);
    }
    @GetMapping(value="/cierres/texto", produces="text/plain;charset=UTF-8")
    public String textoCierre(@RequestParam LocalDate fecha) {
        return impresion.generarContenidoCierre(cierreGuardado(fecha));
    }
    @PostMapping("/cierres/imprimir")
    public void imprimirCierre(@RequestParam LocalDate fecha) {
        if (!impresion.imprimirCierre(cierreGuardado(fecha))) {
            throw new IllegalArgumentException("Impresora no configurada o formato no compatible; puede imprimir desde el navegador");
        }
    }
}
