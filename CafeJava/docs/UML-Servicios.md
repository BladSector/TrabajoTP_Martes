# UML de servicios - TrabajoTP_Martes

Vista resumida de la capa de servicios y sus dependencias principales. Esta separada del [UML completo del modelo](UML.md): los servicios coordinan los casos de uso y utilizan entidades y DAO, pero no son entidades del negocio.

No se detallan aqui todos los miembros de cada servicio. Se omiten constructores, accesores y dependencias transversales del dibujo para facilitar la lectura. Los nombres y operaciones mostrados existen en el codigo.

[Abrir el diagrama de servicios en SVG para ampliarlo](diagramas/servicios.svg).

```mermaid
classDiagram
    direction LR
    class ClienteFiscal {
        <<interface>>
    }
    class MesaService {
        -MesaDAO mesaDAO
        -CuentaDAO cuentaDAO
        -PedidoDAO pedidoDAO
        +listarMesas() List~Mesa~
        +listarPedidosMesa(Integer mesaId) List~Pedido~
        +abrirCuenta(Integer mesaId) Cuenta
        +cerrarCuenta(Integer cuentaId) void
        +cerrarSiCompletada(Integer cuentaId) boolean
        +obtenerCuenta(Integer cuentaId) Cuenta
        +obtenerCuentaAbierta(Integer mesaId) Optional~Cuenta~
    }
    class MenuService {
        -ProductoDAO productoDAO
        -CategoriaDAO categoriaDAO
        +listarProductos() List~Producto~
        +listarCategorias() List~Categoria~
        +crearCategoria(Categoria categoria) Categoria
        +crearProducto(Producto producto) Producto
        +actualizarProducto(Producto producto) void
        +eliminarProducto(Integer id) void
        +aplicarCambios(List~Cambio~ cambios) void
    }
    class PerfilMozoService {
        -PerfilMozoDAO perfilDAO
        +listar(boolean incluirEliminados) List~PerfilMozo~
        +guardar(String nombre) PerfilMozo
        +obtenerActivo(Integer id) PerfilMozo
        +eliminar(Integer id) void
    }
    class PedidoService {
        -PedidoDAO pedidoDAO
        -CuentaDAO cuentaDAO
        -ProductoDAO productoDAO
        -MesaService mesaService
        -PerfilMozoService perfiles
        +agregarPedido(Integer cuentaId, Pedido pedido) Pedido
        +enviarACocina(List~Pedido~ pedidos) void
        +cancelarPedido(Integer id) void
        +enviarComanda(Integer mesaId, List~Pedido~ nuevosPedidos) Cuenta
        +listarParaCocina() List~Pedido~
        +consultarHistorial(LocalDate fecha, Integer perfilId) List~Pedido~
        +cambiarEstado(Integer id, EstadoCocina siguiente) void
        +confirmarEntrega(Integer id, Integer perfilId) void
        +confirmarEntrega(Integer id, String mozo) void
    }
    class PagoService {
        -PagoDAO pagoDAO
        -PedidoDAO pedidoDAO
        -MesaService mesaService
        -TicketService ticketService
        -ImpresionService impresionService
        +calcularDescuento(double subtotal, MetodoPago metodo) double
        +registrarPago(Integer cuentaId, Pago pago) Pago
    }
    class TicketService {
        -TicketDAO ticketDAO
        -ClienteFiscal clienteFiscal
        -ConfiguracionFiscal configuracion
        +generarTicket(Pago pago) TicketElectronico
        +validarTicket(TicketElectronico ticket) void
        +obtenerTicket(Integer id) TicketElectronico
        +obtenerPorPago(Integer pagoId) TicketElectronico
        +consultarHistorial() List~TicketElectronico~
    }
    class ImpresionService {
        -String nombreImpresora
        -boolean habilitada
        -boolean impresionAutomatica
        +generarContenido(TicketElectronico ticket) String
        +generarContenidoCierre(CierreDiario cierre) String
        +imprimirTicket(TicketElectronico ticket) boolean
        +imprimirCierre(CierreDiario cierre) boolean
    }
    class CierreService {
        -TicketDAO ticketDAO
        -CierreDiarioDAO cierreDAO
        -CuentaDAO cuentaDAO
        +generarCierre(LocalDate fecha) CierreDiario
        +obtenerResumen(LocalDate fecha) CierreDiario
        +existeCierre(LocalDate fecha) boolean
        +consultarHistorial() List~CierreDiario~
    }
    MesaService --> MesaDAO
    MesaService --> CuentaDAO
    MesaService --> PedidoDAO
    MenuService --> ProductoDAO
    MenuService --> CategoriaDAO
    PerfilMozoService --> PerfilMozoDAO
    PedidoService --> PedidoDAO
    PedidoService --> CuentaDAO
    PedidoService --> ProductoDAO
    PedidoService --> MesaService
    PedidoService --> PerfilMozoService
    PagoService --> PagoDAO
    PagoService --> PedidoDAO
    PagoService --> MesaService
    PagoService --> TicketService
    PagoService --> ImpresionService
    TicketService --> TicketDAO
    TicketService --> ClienteFiscal
    TicketService --> ConfiguracionFiscal
    CierreService --> TicketDAO
    CierreService --> CierreDiarioDAO
    CierreService --> CuentaDAO
    ClienteFiscal <|.. ClienteFiscalSimulado
```

`Clock` permite obtener fechas y horas de forma controlada. `OperacionGuard` coordina operaciones transaccionales. Ambas son dependencias adicionales de varios servicios y se omiten del dibujo resumido.

Los DAO y el cliente fiscal aparecen como colaboradores, no como parte del modelo. `Cambio` es un record interno de `MenuService`, no una entidad persistida. La sobrecarga `confirmarEntrega(Integer, String)` mantiene compatibilidad con solicitudes antiguas; la interfaz actual utiliza el identificador del perfil.
