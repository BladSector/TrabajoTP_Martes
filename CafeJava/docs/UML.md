# UML de la version Java

Los tipos corresponden a la implementacion. Se omiten getters/setters para facilitar la lectura. Cuenta representa una atencion de una mesa, no un usuario.

```mermaid
classDiagram
    class Mesa {
        -Long id
        -String nombre
        -EstadoMesa estado
        -boolean esTakeAway
    }
    class Categoria {
        -Long id
        -String nombre
    }
    class Producto {
        -Long id
        -String nombre
        -String descripcion
        -double precio
        -boolean disponible
        -boolean eliminado
        -Categoria categoria
    }
    class PerfilMozo {
        -Long id
        -String nombre
        -LocalDateTime fechaCreacion
        -boolean eliminado
    }
    PerfilMozo "1" <-- "0..*" Pedido : perfilMozoId
    class PerfilMozoDAO {
        +listar(boolean incluirEliminados) List~PerfilMozo~
        +buscarPorId(Long id) Optional~PerfilMozo~
        +eliminar(Long id) void
    }
    class PerfilMozoService {
        -PerfilMozoDAO perfilDAO
        +listar(boolean incluirEliminados) List~PerfilMozo~
        +guardar(String nombre) PerfilMozo
        +obtenerActivo(Long id) PerfilMozo
        +eliminar(Long id) void
    }
    PerfilMozoService --> PerfilMozoDAO
    PedidoService --> PerfilMozoService
    class Pedido {
        -Long id
        -Long cuentaId
        -Producto producto
        -String nombreProducto
        -int cantidad
        -double precioUnitario
        -LocalDateTime fechaHora
        -EstadoCocina estadoCocina
        -String mozo
        -Long perfilMozoId
        -String nombreCliente
        -String notas
        -int cantidadPagada
        -boolean cancelado
    }
    class Cuenta {
        -Long id
        -Mesa mesa
        -LocalDateTime fechaApertura
        -LocalDateTime fechaCierre
        -EstadoCuenta estado
        -List~Pedido~ pedidos
        -List~Pago~ pagos
    }
    class DetallePago {
        -Long id
        -Pedido pedido
        -int cantidad
        -double precioUnitario
    }
    class Pago {
        -Long id
        -Long cuentaId
        -LocalDateTime fechaHora
        -MetodoPago metodo
        -double subtotal
        -double descuento
        -double total
        -List~DetallePago~ detalles
    }
    class ItemTicket {
        -Long id
        -String codigoProducto
        -String descripcion
        -int cantidad
        -double precioUnitario
        -double alicuotaIva
    }
    class TicketElectronico {
        -Long id
        -String codigo
        -Pago pago
        -LocalDateTime fechaEmision
        -List~ItemTicket~ items
        -double subtotal
        -double descuento
        -double iva
        -double total
        -EstadoTicket estado
        -String entorno
        -String tipoDocumentoCliente
        -String numeroDocumentoCliente
        -String razonSocialCliente
        -String razonSocialEmisor
        -String cuitEmisor
        -String domicilioEmisor
        -String condicionIvaEmisor
        -int puntoVenta
        -Long numeroComprobante
        -String cae
        -LocalDate vencimientoCae
    }
    class CierreDiario {
        -Long id
        -LocalDate fecha
        -LocalDateTime fechaHoraCierre
        -int numeroInforme
        -int cantidadTickets
        -double totalEfectivo
        -double totalTarjeta
        -double totalDescuentos
        -double totalGeneral
    }
    Mesa "1" -- "0..*" Cuenta
    Cuenta "1" *-- "0..*" Pedido
    Categoria "1" -- "0..*" Producto
    Pedido "0..*" --> "1" Producto
    Cuenta "1" -- "0..*" Pago
    Pago "1" *-- "1..*" DetallePago
    DetallePago "0..*" --> "1" Pedido
    Pago "1" -- "0..1" TicketElectronico
    TicketElectronico "1" *-- "1..*" ItemTicket
```

## Servicios

```mermaid
classDiagram
    class MesaService {
        -MesaDAO mesaDAO
        -CuentaDAO cuentaDAO
        +listarMesas() List~Mesa~
        +abrirCuenta(Long mesaId) Cuenta
        +cerrarCuenta(Long cuentaId) void
        +obtenerCuenta(Long cuentaId) Cuenta
    }
    class MenuService {
        -ProductoDAO productoDAO
        -CategoriaDAO categoriaDAO
        +listarProductos() List~Producto~
        +crearProducto(Producto producto) Producto
        +actualizarProducto(Producto producto) void
        +eliminarProducto(Long id) void
    }
    class PedidoService {
        -PedidoDAO pedidoDAO
        -CuentaDAO cuentaDAO
        -ProductoDAO productoDAO
        -MesaService mesaService
        +agregarPedido(Long cuentaId, Pedido pedido) Pedido
        +enviarACocina(List~Pedido~ pedidos) void
        +cancelarPedido(Long id) void
        +enviarComanda(Long mesaId, List~Pedido~ pedidos) Cuenta
        +listarParaCocina() List~Pedido~
        +cambiarEstado(Long id, EstadoCocina estado) void
        +confirmarEntrega(Long id, String mozo) void
    }
    class PagoService {
        -PagoDAO pagoDAO
        -PedidoDAO pedidoDAO
        -MesaService mesaService
        -TicketService ticketService
        +calcularDescuento(double subtotal, MetodoPago metodo) double
        +registrarPago(Long cuentaId, Pago pago) Pago
    }
    class TicketService {
        -TicketDAO ticketDAO
        -ClienteFiscal clienteFiscal
        -ConfiguracionFiscal configuracion
        +generarTicket(Pago pago) TicketElectronico
        +validarTicket(TicketElectronico ticket) void
        +consultarHistorial() List~TicketElectronico~
    }
    class ImpresionService {
        -String nombreImpresora
        -boolean habilitada
        -boolean impresionAutomatica
        +generarContenido(TicketElectronico ticket) String
        +imprimirTicket(TicketElectronico ticket) boolean
        +imprimirCierre(CierreDiario cierre) boolean
    }
    class CierreService {
        -TicketDAO ticketDAO
        -CierreDiarioDAO cierreDAO
        +generarCierre(LocalDate fecha) CierreDiario
        +existeCierre(LocalDate fecha) boolean
        +consultarHistorial() List~CierreDiario~
    }
    MesaService --> MesaDAO
    MesaService --> CuentaDAO
    MenuService --> ProductoDAO
    MenuService --> CategoriaDAO
    PedidoService --> PedidoDAO
    PedidoService --> CuentaDAO
    PedidoService --> ProductoDAO
    PedidoService --> MesaService
    PagoService --> PagoDAO
    PagoService --> PedidoDAO
    PagoService --> MesaService
    PagoService --> TicketService
    TicketService --> TicketDAO
    TicketService --> ClienteFiscal
    CierreService --> TicketDAO
    CierreService --> CierreDiarioDAO
    ClienteFiscal <|.. ClienteFiscalSimulado
```

Clock y OperacionGuard son dependencias adicionales de los servicios que realizan operaciones transaccionales. PagoService tambien utiliza ImpresionService despues de guardar el pago.
