package ar.edu.cafe;

import ar.edu.cafe.dao.*;
import ar.edu.cafe.fiscal.ClienteFiscalSimulado;
import ar.edu.cafe.model.*;
import ar.edu.cafe.service.*;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:cafe-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "cafe.impresion.habilitada=false"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class FlujoCafeTest {
    @Autowired MesaService mesas;
    @Autowired MenuService menu;
    @Autowired PedidoService pedidos;
    @Autowired PagoService pagos;
    @Autowired TicketService tickets;
    @Autowired CierreService cierres;
    @Autowired PedidoDAO pedidoDAO;
    @Autowired PerfilMozoService perfiles;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoSpyBean ClienteFiscalSimulado clienteFiscal;
    private Producto producto;

    @TestConfiguration
    static class Tiempo {
        @Bean @Primary Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        }
    }

    @BeforeEach
    void preparar() {
        for (String tabla : List.of("items_ticket","tickets","detalles_pago","pagos","pedidos","cuentas",
                "cierres_diarios","productos","categorias","perfiles_mozo")) jdbc.update("DELETE FROM " + tabla);
        jdbc.update("UPDATE mesas SET estado='LIBRE'");
        Categoria categoria = new Categoria(); categoria.setNombre("Cafeteria");
        categoria = menu.crearCategoria(categoria);
        producto = new Producto(); producto.setNombre("Cafe grande"); producto.setPrecio(1000.00);
        producto.setCategoria(categoria); producto = menu.crearProducto(producto);
    }
    private Long mesaId(int indice) { return mesas.listarMesas().get(indice).getId(); }
    private Cuenta abrir(int indice) { return mesas.abrirCuenta(mesaId(indice)); }
    private Pedido agregar(Cuenta cuenta, int cantidad) {
        Pedido pedido = new Pedido(); pedido.setProducto(producto); pedido.setCantidad(cantidad); pedido.setMozo("Gabi");
        pedido = pedidos.agregarPedido(cuenta.getId(), pedido);
        pedidos.enviarACocina(List.of(pedido));
        return pedido;
    }
    private Pago pago(Pedido pedido, int cantidad, MetodoPago metodo) {
        DetallePago detalle = new DetallePago(); detalle.setPedido(pedido); detalle.setCantidad(cantidad);
        Pago pago = new Pago(); pago.setMetodo(metodo); pago.setDetalles(List.of(detalle)); return pago;
    }
    private int cantidad(String tabla) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class); }

    @Test
    void perfilesPersistentesSinDuplicadosYConValidacion() throws Exception {
        var creado = mvc.perform(post("/api/perfiles").contentType("application/json").content("{\"nombre\":\"  Gabi  Lopez  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Gabi Lopez")).andReturn();
        PerfilMozo mismo = perfiles.guardar("gabi lopez");
        assertThat(cantidad("perfiles_mozo")).isEqualTo(1);
        assertThat(creado.getResponse().getContentAsString()).contains("\"id\":" + mismo.getId());
        mvc.perform(get("/api/perfiles")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(mismo.getId()));
        mvc.perform(post("/api/perfiles").contentType("application/json").content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/perfiles/999999")).andExpect(status().isNotFound());
    }

    @Test
    void pedidoTomaResponsableDelPerfilNoDelNombreEnviado() throws Exception {
        PerfilMozo gabi = perfiles.guardar("Gabi");
        mvc.perform(post("/api/mesas/" + mesaId(0) + "/comandas").contentType("application/json")
                .content("{\"pedidos\":[{\"productoId\":" + producto.getId() + ",\"cantidad\":1,\"perfilMozoId\":" + gabi.getId() + ",\"mozo\":\"Otro\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedidos[0].mozo").value("Gabi"))
                .andExpect(jsonPath("$.pedidos[0].perfilMozoId").value(gabi.getId()));
        mvc.perform(get("/api/pedidos/historial").param("fecha", "2026-10-13").param("perfilId", gabi.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].mesa.nombre").exists())
                .andExpect(jsonPath("$[0].pedido.mozo").value("Gabi"));
        mvc.perform(get("/api/pedidos/historial").param("fecha", "2026-10-12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eliminarPerfilConservaHistorialYNoReutilizaIdentidad() throws Exception {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta, 1);
        Long anterior = pedido.getPerfilMozoId();
        mvc.perform(delete("/api/perfiles/" + anterior)).andExpect(status().isBadRequest());
        pedidos.cambiarEstado(pedido.getId(), EstadoCocina.EN_PREPARACION);
        pedidos.cambiarEstado(pedido.getId(), EstadoCocina.LISTO);
        mvc.perform(post("/api/pedidos/" + pedido.getId() + "/entregar").contentType("application/json")
                .content("{\"perfilMozoId\":" + perfiles.guardar("Otro").getId() + "}"))
                .andExpect(status().isBadRequest());
        pedidos.confirmarEntrega(pedido.getId(), anterior);
        perfiles.eliminar(anterior);
        assertThat(perfiles.listar(false)).noneMatch(p -> p.getId().equals(anterior));
        assertThat(perfiles.listar(true)).anyMatch(p -> p.getId().equals(anterior) && p.isEliminado());
        Pedido historico = pedidos.consultarHistorial(LocalDate.of(2026,10,13), anterior).get(0);
        assertThat(historico.getMozo()).isEqualTo("Gabi");
        assertThat(historico.getPerfilMozoId()).isEqualTo(anterior);
        assertThat(perfiles.guardar("Gabi").getId()).isNotEqualTo(anterior);
        Pedido nuevo = new Pedido(); nuevo.setProducto(producto); nuevo.setCantidad(1); nuevo.setPerfilMozoId(anterior);
        assertThatThrownBy(() -> pedidos.agregarPedido(cuenta.getId(), nuevo)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cantidad("pedidos")).isEqualTo(1);
    }

    @Test
    void interfazNoReutilizaArchivosDeVersionesAnteriores() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/app.js?v=")));
        mvc.perform(get("/app.js").param("v", "pedidos-directos-20261018")
                .header("If-Modified-Since", "Wed, 31 Dec 2099 23:59:59 GMT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void pagoParcialConDescuentoYSegundoPagoLiberaMesa() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,3);
        Pago parcial = pagos.registrarPago(cuenta.getId(),pago(pedido,2,MetodoPago.EFECTIVO));
        assertThat(parcial.getSubtotal()).isCloseTo(2000.00, within(0.001));
        assertThat(parcial.getDescuento()).isCloseTo(200.00, within(0.001));
        assertThat(parcial.getTotal()).isCloseTo(1800.00, within(0.001));
        assertThat(mesas.obtenerCuenta(cuenta.getId()).getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getCantidadPagada()).isEqualTo(2);
        pagos.registrarPago(cuenta.getId(),pago(pedido,1,MetodoPago.TARJETA));
        assertThat(mesas.obtenerCuenta(cuenta.getId()).getEstado()).isEqualTo(EstadoCuenta.CERRADA);
        assertThat(mesas.listarMesas().get(0).estaLibre()).isTrue();
        assertThat(tickets.consultarHistorial()).hasSize(2);
        assertThat(cierres.obtenerResumen(LocalDate.of(2026,10,13)).getTotalGeneral()).isCloseTo(2800.00, within(0.001));
    }

    @Test
    void cambioEnMenuNoModificaNombreNiPrecioHistorico() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        producto.setNombre("Cafe doble"); producto.setPrecio(9000.0); menu.actualizarProducto(producto);
        Pago pago = pagos.registrarPago(cuenta.getId(),pago(pedido,1,MetodoPago.TARJETA));
        TicketElectronico ticket = tickets.obtenerPorPago(pago.getId());
        assertThat(ticket.getItems().get(0).getDescripcion()).isEqualTo("Cafe grande");
        assertThat(ticket.getTotal()).isCloseTo(1000.00, within(0.001));
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.SIMULADO);
        assertThat(ticket.getCae()).isNull();
    }

    @Test
    void cantidadExcesivaNoGeneraPagoNiModificaPedido() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        assertThatThrownBy(() -> pagos.registrarPago(cuenta.getId(),pago(pedido,2,MetodoPago.EFECTIVO)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cantidad("pagos")).isZero(); assertThat(cantidad("tickets")).isZero();
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getCantidadPagada()).isZero();
    }

    @Test
    void noSePuedeCobrarPedidoDeOtraCuenta() {
        Cuenta primera = abrir(0), segunda = abrir(1); Pedido pedido = agregar(primera,1);
        assertThatThrownBy(() -> pagos.registrarPago(segunda.getId(),pago(pedido,1,MetodoPago.TARJETA)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("otra cuenta");
        assertThat(cantidad("pagos")).isZero();
    }

    @Test
    void noSeAdmitenDetallesDuplicados() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,2);
        Pago pago = pago(pedido,1,MetodoPago.TARJETA);
        pago.setDetalles(List.of(pago.getDetalles().get(0),pago.getDetalles().get(0)));
        assertThatThrownBy(() -> pagos.registrarPago(cuenta.getId(),pago)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cantidad("pagos")).isZero();
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getCantidadPagada()).isZero();
    }

    @Test
    void errorEnTicketRevierteTodoElPago() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        doThrow(new IllegalStateException("Fallo fiscal de prueba")).when(clienteFiscal).autorizar(any());
        assertThatThrownBy(() -> pagos.registrarPago(cuenta.getId(),pago(pedido,1,MetodoPago.EFECTIVO)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(cantidad("pagos")).isZero(); assertThat(cantidad("detalles_pago")).isZero();
        assertThat(cantidad("tickets")).isZero();
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getCantidadPagada()).isZero();
        assertThat(mesas.obtenerCuenta(cuenta.getId()).getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
    }

    @Test
    void dosCobrosSimultaneosNoDuplicanElPago() throws Exception {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Boolean> intento = () -> {
            inicio.await();
            try { pagos.registrarPago(cuenta.getId(),pago(pedido,1,MetodoPago.TARJETA)); return true; }
            catch (IllegalArgumentException esperado) { return false; }
        };
        try {
            Future<Boolean> primero = ejecutor.submit(intento), segundo = ejecutor.submit(intento);
            inicio.countDown();
            assertThat(List.of(primero.get(20,TimeUnit.SECONDS),segundo.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(cantidad("pagos")).isEqualTo(1); assertThat(cantidad("tickets")).isEqualTo(1);
        } finally { ejecutor.shutdownNow(); }
    }

    @Test
    void totalComodoDeEfectivoCalculaDescuento() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,2);
        Pago pago = pago(pedido,2,MetodoPago.EFECTIVO); pago.setTotal(1750.0);
        Pago registrado = pagos.registrarPago(cuenta.getId(),pago);
        assertThat(registrado.getDescuento()).isCloseTo(250.0, within(0.001));
        assertThat(registrado.getTotal()).isCloseTo(1750.0, within(0.001));
    }

    @Test
    void redondeoAceptaAmbosLimitesDelCincoPorCiento() {
        for (double total : new double[]{855.0,945.0}) {
            Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
            Pago pago = pago(pedido,1,MetodoPago.EFECTIVO); pago.setTotal(total);
            Pago registrado = pagos.registrarPago(cuenta.getId(),pago);
            assertThat(registrado.getTotal()).isCloseTo(total,within(0.000001));
            assertThat(registrado.getDescuento()).isCloseTo(1000.0-total,within(0.000001));
        }
    }

    @Test
    void redondeoFueraDeRangoNoRegistraPagosNiTickets() throws Exception {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        for (double total : new double[]{854.99,945.01,1000.0,1.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY}) {
            Pago pago = pago(pedido,1,MetodoPago.EFECTIVO); pago.setTotal(total);
            assertThatThrownBy(() -> pagos.registrarPago(cuenta.getId(),pago)).isInstanceOf(IllegalArgumentException.class);
        }
        mvc.perform(post("/api/cuentas/"+cuenta.getId()+"/pagos").contentType("application/json")
                .content("{\"metodo\":\"EFECTIVO\",\"detalles\":[{\"pedidoId\":"+pedido.getId()+",\"cantidad\":1}],\"totalEfectivo\":945.01}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("5%")));
        assertThat(cantidad("pagos")).isZero(); assertThat(cantidad("tickets")).isZero();
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getCantidadPagada()).isZero();
        assertThat(mesas.obtenerCuenta(cuenta.getId()).getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
    }

    @Test
    void redondeoDeCentavosNoAmpliaElCincoPorCiento() {
        producto.setPrecio(0.29); menu.actualizarProducto(producto);
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        for (double total : new double[]{0.24,0.28}) {
            Pago pago = pago(pedido,1,MetodoPago.EFECTIVO); pago.setTotal(total);
            assertThatThrownBy(() -> pagos.registrarPago(cuenta.getId(),pago)).isInstanceOf(IllegalArgumentException.class);
        }
        Pago pago = pago(pedido,1,MetodoPago.EFECTIVO); pago.setTotal(0.25);
        assertThat(pagos.registrarPago(cuenta.getId(),pago).getTotal()).isCloseTo(0.25,within(0.000001));
    }

    @Test
    void tarjetaIgnoraElRedondeoDeEfectivo() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        Pago pago = pago(pedido,1,MetodoPago.TARJETA); pago.setTotal(1.0);
        Pago registrado = pagos.registrarPago(cuenta.getId(),pago);
        assertThat(registrado.getTotal()).isCloseTo(1000.0,within(0.000001));
        assertThat(registrado.getDescuento()).isZero();
    }

    @Test
    void redondeoSeCalculaSoloSobreLaCantidadQueSePaga() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,3);
        Pago fuera = pago(pedido,1,MetodoPago.EFECTIVO); fuera.setTotal(1750.0);
        assertThatThrownBy(() -> pagos.registrarPago(cuenta.getId(),fuera)).isInstanceOf(IllegalArgumentException.class);
        Pago valido = pago(pedido,1,MetodoPago.EFECTIVO); valido.setTotal(855.0);
        assertThat(pagos.registrarPago(cuenta.getId(),valido).getTotal()).isCloseTo(855.0,within(0.000001));
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getCantidadPagada()).isEqualTo(1);
        assertThat(mesas.obtenerCuenta(cuenta.getId()).getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
    }

    @Test
    void cierreRechazaPendientesYBloqueaNuevasOperaciones() throws Exception {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1); LocalDate dia = LocalDate.of(2026,10,13);
        assertThatThrownBy(() -> cierres.generarCierre(dia)).isInstanceOf(IllegalArgumentException.class);
        pagos.registrarPago(cuenta.getId(),pago(pedido,1,MetodoPago.EFECTIVO));
        CierreDiario cierre = cierres.generarCierre(dia);
        assertThat(cierre.getTotalEfectivo()).isCloseTo(900.0, within(0.001));
        assertThat(cierres.generarCierre(dia).getId()).isEqualTo(cierre.getId());
        assertThatThrownBy(() -> abrir(1)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cerrada");
        mvc.perform(get("/api/cierres/texto").param("fecha", dia.toString()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("CIERRE DIARIO")));
        mvc.perform(get("/api/cierres/texto").param("fecha", "2026-10-12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void takeAwayRequiereClienteYProductoDebeEstarDisponible() {
        Cuenta cuenta = abrir(8);
        Pedido pedido = new Pedido(); pedido.setProducto(producto); pedido.setCantidad(1); pedido.setMozo("Gabi");
        assertThatThrownBy(() -> pedidos.agregarPedido(cuenta.getId(),pedido)).isInstanceOf(IllegalArgumentException.class);
        producto.setDisponible(false); menu.actualizarProducto(producto); pedido.setNombreCliente("Ana");
        assertThatThrownBy(() -> pedidos.agregarPedido(cuenta.getId(),pedido)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pedidoBorradorNoPuedeCobrarseYCancelacionConPagoSeRechaza() {
        Cuenta cuenta = abrir(0);
        Pedido pedido = new Pedido(); pedido.setProducto(producto); pedido.setCantidad(2); pedido.setMozo("Gabi");
        Pedido creado = pedidos.agregarPedido(cuenta.getId(),pedido);
        assertThatThrownBy(() -> pagos.registrarPago(cuenta.getId(),pago(creado,1,MetodoPago.TARJETA)))
                .isInstanceOf(IllegalArgumentException.class);
        pedidos.enviarACocina(List.of(creado)); pagos.registrarPago(cuenta.getId(),pago(creado,1,MetodoPago.TARJETA));
        assertThatThrownBy(() -> pedidos.cancelarPedido(creado.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importesDecimalesConDoubleSeRedondeanEnPagoYTicket() {
        producto.setPrecio(19.99); menu.actualizarProducto(producto);
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,3);
        Pago registrado = pagos.registrarPago(cuenta.getId(),pago(pedido,3,MetodoPago.EFECTIVO));
        assertThat(registrado.getSubtotal()).isCloseTo(59.97, within(0.000001));
        assertThat(registrado.getDescuento()).isCloseTo(6.00, within(0.000001));
        assertThat(registrado.getTotal()).isCloseTo(53.97, within(0.000001));
        TicketElectronico ticket = tickets.obtenerPorPago(registrado.getId());
        assertThat(ticket.getTotal()).isCloseTo(registrado.getTotal(), within(0.000001));
        assertThat(cierres.obtenerResumen(LocalDate.of(2026,10,13)).getTotalGeneral())
                .isCloseTo(53.97, within(0.000001));
    }

    @Test
    void importesNoFinitosSeRechazan() {
        assertThatThrownBy(() -> Dinero.noNegativo(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dinero.redondear(Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dinero.redondear(Double.MAX_VALUE)).isInstanceOf(IllegalArgumentException.class);
    }

 
    @Test
    void comandaCompletaSeRegistraYSeMuestraEnCocina() throws Exception {
        Pedido primero = new Pedido(); primero.setProducto(producto); primero.setCantidad(2);
        primero.setMozo("Gabi"); primero.setNotas("Tipo: Cortado");
        Pedido segundo = new Pedido(); segundo.setProducto(producto); segundo.setCantidad(1); segundo.setMozo("Gabi");
        Cuenta cuenta = pedidos.enviarComanda(mesaId(0), List.of(primero, segundo));
        assertThat(cuenta.getPedidos()).hasSize(2);
        assertThat(cuenta.getPedidos()).allMatch(p -> p.getEstadoCocina() == EstadoCocina.PENDIENTE);
        assertThat(pedidos.listarParaCocina()).hasSize(2);
        mvc.perform(get("/api/cocina")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mesa.nombre").value("Mesa 1"))
                .andExpect(jsonPath("$[0].pedido.notas").value("Tipo: Cortado"));
        mvc.perform(get("/api/mesas")).andExpect(jsonPath("$[0].pedidos.length()").value(2));
    }

    @Test
    void errorEnComandaNoDejaMesaOcupadaNiPedidosParciales() {
        Pedido valido = new Pedido(); valido.setProducto(producto); valido.setCantidad(1); valido.setMozo("Gabi");
        Producto inexistente = new Producto(); inexistente.setId(999999L);
        Pedido invalido = new Pedido(); invalido.setProducto(inexistente); invalido.setCantidad(1); invalido.setMozo("Gabi");
        assertThatThrownBy(() -> pedidos.enviarComanda(mesaId(0), List.of(valido, invalido)))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(cantidad("pedidos")).isZero(); assertThat(cantidad("cuentas")).isZero();
        assertThat(mesas.listarMesas().get(0).estaLibre()).isTrue();
    }

    @Test
    void cocinaPuedeTerminarPedidoPagadoInclusoDespuesDelCierre() {
        Cuenta cuenta = abrir(0); Pedido pedido = agregar(cuenta,1);
        pagos.registrarPago(cuenta.getId(),pago(pedido,1,MetodoPago.TARJETA));
        assertThat(mesas.obtenerCuenta(cuenta.getId()).getEstado()).isEqualTo(EstadoCuenta.CERRADA);
        assertThat(pedidos.listarParaCocina()).hasSize(1);
        assertThatThrownBy(() -> pedidos.cambiarEstado(pedido.getId(),EstadoCocina.LISTO))
                .isInstanceOf(IllegalArgumentException.class);
        cierres.generarCierre(LocalDate.of(2026,10,13));
        pedidos.cambiarEstado(pedido.getId(),EstadoCocina.EN_PREPARACION);
        pedidos.cambiarEstado(pedido.getId(),EstadoCocina.LISTO);
        pedidos.confirmarEntrega(pedido.getId(),"Gabi");
        assertThat(pedidos.listarParaCocina()).isEmpty();
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getEstadoCocina()).isEqualTo(EstadoCocina.ENTREGADO);
    }

    @Test
    void soloMozoResponsableConfirmaEntregaListaUnaVez() throws Exception {
        Pedido pedido = agregar(abrir(0),1);
        assertThatThrownBy(() -> pedidos.confirmarEntrega(pedido.getId(),"Gabi"))
                .isInstanceOf(IllegalArgumentException.class);
        pedidos.cambiarEstado(pedido.getId(),EstadoCocina.EN_PREPARACION);
        pedidos.cambiarEstado(pedido.getId(),EstadoCocina.LISTO);
        mvc.perform(put("/api/pedidos/" + pedido.getId() + "/estado").contentType("application/json")
                .content("{\"estado\":\"ENTREGADO\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos/" + pedido.getId() + "/entregar").contentType("application/json")
                .content("{\"mozo\":\"Otro\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos/" + pedido.getId() + "/entregar").contentType("application/json")
                .content("{\"mozo\":\" \"}")).andExpect(status().isBadRequest());
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().getEstadoCocina()).isEqualTo(EstadoCocina.LISTO);
        mvc.perform(post("/api/pedidos/" + pedido.getId() + "/entregar").contentType("application/json")
                .content("{\"mozo\":\" gabi \"}")).andExpect(status().isOk());
        assertThat(pedidos.listarParaCocina()).isEmpty();
        assertThatThrownBy(() -> pedidos.confirmarEntrega(pedido.getId(),"Gabi"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pedidoCanceladoNoSePuedeEntregar() {
        Pedido pedido = agregar(abrir(0),1);
        pedidos.cambiarEstado(pedido.getId(),EstadoCocina.EN_PREPARACION);
        pedidos.cambiarEstado(pedido.getId(),EstadoCocina.LISTO);
        pedidos.cancelarPedido(pedido.getId());
        assertThatThrownBy(() -> pedidos.confirmarEntrega(pedido.getId(),"Gabi"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(pedidoDAO.buscarPorId(pedido.getId()).orElseThrow().isCancelado()).isTrue();
    }

    @Test
    void loteDeMenuConErrorSeRevierteCompleto() {
        Producto modificado = new Producto(); modificado.setNombre("Cafe editado");
        modificado.setCategoria(producto.getCategoria()); modificado.setPrecio(1200.0);
        Producto invalido = new Producto(); invalido.setNombre("Invalido");
        invalido.setCategoria(producto.getCategoria()); invalido.setPrecio(-1.0);
        assertThatThrownBy(() -> menu.aplicarCambios(List.of(
                new MenuService.Cambio(MenuService.Operacion.ACTUALIZAR,producto.getId(),modificado),
                new MenuService.Cambio(MenuService.Operacion.CREAR,null,invalido))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(menu.listarProductos()).hasSize(1);
        assertThat(menu.listarProductos().get(0).getNombre()).isEqualTo("Cafe grande");
        assertThat(menu.listarProductos().get(0).getPrecio()).isCloseTo(1000.0,within(0.001));
    }

    @Test
    void apiEnviaComandaYValidaPedidosObligatorios() throws Exception {
        mvc.perform(post("/api/mesas/" + mesaId(0) + "/comandas").contentType("application/json").content(
                "{\"pedidos\":[{\"productoId\":" + producto.getId()
                + ",\"cantidad\":2,\"mozo\":\"Gabi\",\"nombreCliente\":\"\",\"notas\":\"Cortado\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedidos[0].estadoCocina").value("PENDIENTE"));
        mvc.perform(post("/api/mesas/" + mesaId(1) + "/comandas").contentType("application/json").content("{\"pedidos\":[]}"))
                .andExpect(status().isBadRequest());
        assertThat(cantidad("cuentas")).isEqualTo(1);
    }
    @Test
    void apiValidaSolicitudesYExponeErroresClaros() throws Exception {
        mvc.perform(get("/api/mesas")).andExpect(status().isOk()).andExpect(jsonPath("$[0].mesa.nombre").value("Mesa 1"));
        mvc.perform(get("/api/cuentas/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/api/menu").contentType("application/json").content("""
                {"nombre":"", "precio":-1, "categoriaId":1, "disponible":true}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }
}
