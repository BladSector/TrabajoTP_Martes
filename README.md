# TrabajoTP_Martes - Sistema de cafeteria

Aplicacion web para organizar mesas, registrar pedidos, coordinar su preparacion en cocina, cobrar consumos y cerrar la caja del dia. Es una adaptacion a Java de un proyecto original en Python/Flask, conservando el menu, el logo y la idea de su interfaz.

Este README esta dirigido a los integrantes del trabajo practico. Describe lo que esta implementado actualmente, no funcionalidades futuras.

## 1. Tecnologias y herramientas

| Herramienta | Uso en el proyecto |
| --- | --- |
| Java 17 | Modelos, reglas de negocio y acceso a datos. |
| Spring Boot 3.5.6 | Inicio de la aplicacion y servidor web integrado. |
| Spring Web / MVC | Controladores y API HTTP. |
| Spring JDBC y JdbcTemplate | Ejecucion de consultas SQL desde los DAO. |
| Spring Transactions | Operaciones que se confirman o revierten juntas. |
| Jakarta Validation | Validacion de las solicitudes recibidas por la API. |
| MySQL y MySQL Connector/J | Base de datos principal y controlador de conexion. |
| H2 | Base alternativa para demostracion y pruebas. |
| HTML, CSS y JavaScript puro | Interfaz del navegador; no utiliza React. |
| Fetch y JSON | Comunicacion entre la interfaz y la API. |
| Lucide | Iconos de la interfaz, incluidos como archivo local. |
| Maven | Dependencias, compilacion, pruebas y generacion del JAR. |
| JUnit, Spring Boot Test y Mockito | Pruebas de reglas de negocio y API. |
| Playwright y Edge | Pruebas opcionales del flujo de navegador y capturas. |
| Git y GitHub | Control de versiones y repositorio compartido. |
| IntelliJ IDEA y MySQL Workbench | Herramientas sugeridas para editar y consultar la base. |

**Spring y DAO no son alternativas.** Spring Boot es el framework; DAO es el patron utilizado para separar el acceso a la base de datos. En este proyecto se usan juntos. No se utiliza JPA, Hibernate ni Lombok.

Los importes del modelo Java usan `double`, con ajustes a dos decimales mediante `Dinero`. Las columnas de importes en SQL usan `DECIMAL`.

## 2. Personas, pantallas y conceptos

| Concepto | Significado |
| --- | --- |
| Mozo | Persona que selecciona su perfil, toma pedidos, cobra y confirma entregas. |
| Cocina | Vista de trabajo para preparar pedidos y marcarlos como listos. |
| PerfilMozo | Identidad persistente de un mozo, recuperable desde cualquier sesion de esta aplicacion. |
| Mesa | Lugar de atencion. La carga inicial incluye seis mesas, dos barras y un sector Take Away. |
| Cuenta | Una atencion de una mesa: agrupa sus pedidos y pagos. No es una cuenta de usuario. |
| Tanda o comanda | Conjunto de productos que el mozo prepara en pantalla y confirma juntos. |
| Pedido | Un producto solicitado, con cantidad, responsable, fecha, precio y observaciones. |
| Cliente de Take Away | Nombre que permite separar pedidos y cobros dentro de ese sector. No tiene un perfil de acceso. |

Los perfiles de mozo no tienen contrasena. El selector Mozo/Cocina cambia de pantalla, pero no constituye un sistema de permisos o autenticacion. Tampoco hay un rol administrador separado: la vista de mozo permite gestionar platos, tickets y cierres.

Los perfiles de ejecucion de Spring (`mysql` y `demo`) eligen la base de datos; no son los perfiles de las personas.

## 3. Requerimientos funcionales

| ID | Requerimiento implementado |
| --- | --- |
| RF-01 | Crear perfiles de mozo por nombre y guardarlos en la base de datos. |
| RF-02 | Listar y seleccionar perfiles existentes aunque se pierda la sesion; recordar localmente el ultimo seleccionado. |
| RF-03 | Eliminar un perfil mediante baja logica, conservando sus pedidos. Bloquear la baja mientras tenga pedidos sin entregar ni cancelar. |
| RF-04 | Mostrar el mapa de mesas, su disponibilidad, la cuenta activa y los consumos pendientes de pago. |
| RF-05 | Abrir una cuenta por mesa y reutilizarla mientras siga abierta, sin crear dos cuentas activas para la misma mesa. |
| RF-06 | Consultar platos por categoria y busqueda, mostrando su precio y disponibilidad. |
| RF-07 | Armar una tanda temporal, agregar platos con un clic, editar cantidades, quitar productos y escribir comentarios opcionales desde un boton separado. |
| RF-08 | Elegir la variante en los cafes que la requieren: cafe solo, cortado, lagrima o cafe con leche. |
| RF-09 | Confirmar una tanda completa, registrar sus pedidos en la cuenta y enviarlos a cocina en una transaccion. |
| RF-10 | Registrar por pedido el perfil responsable, una copia de su nombre, fecha y hora, producto, cantidad, precio y observaciones. |
| RF-11 | Mostrar en cocina las colas de pendientes, en preparacion y listos para retirar, con busqueda y referencia al mozo y la mesa. |
| RF-12 | Avisar al mozo cuando sus pedidos esten listos y permitir que el perfil responsable confirme la entrega. Cocina no confirma entregas. |
| RF-13 | Cancelar pedidos de cuentas abiertas que todavia no tengan cantidades pagadas, sin borrar su registro. |
| RF-14 | Exigir nombre de cliente en Take Away y permitir separar sus consumos al cobrar. |
| RF-15 | Cobrar toda la cuenta o cantidades parciales de los pedidos, con efectivo o tarjeta como medio registrado. |
| RF-16 | Aplicar 10 % de descuento en efectivo y admitir un total manual dentro de un margen del 5 % respecto del total ya descontado. |
| RF-17 | Generar un ticket por pago y liberar la mesa cuando no queden consumos pendientes de pago. |
| RF-18 | Consultar e imprimir tickets; la impresion directa depende de que exista una impresora compatible configurada. |
| RF-19 | Crear categorias y crear, editar, cambiar disponibilidad o eliminar logicamente platos, con cambios de platos aplicados en lote. |
| RF-20 | Consultar el registro de pedidos por fecha y perfil, incluyendo los pedidos de perfiles eliminados. |
| RF-21 | Consultar ventas y totales diarios, registrar un cierre por fecha y conservar el historial de cierres. |
| RF-22 | Bloquear nuevos pedidos y pagos cuando la jornada actual este cerrada, manteniendo disponible la preparacion y entrega. |
| RF-23 | Dar feedback visual al agregar productos, confirmar tandas, pagar, entregar y realizar otras acciones; mostrar errores y estados de procesamiento. |

## 4. Casos de flujo

### CF-01. Crear o recuperar un perfil

**Actor:** mozo.

1. Abrir la vista Mozo y acceder al selector o al boton de usuarios.
2. Ingresar un nombre y pulsar Guardar y usar, o seleccionar un perfil ya guardado.
3. El sistema deja ese perfil activo para los nuevos pedidos.
4. Si se pierde la sesion, volver a seleccionarlo desde la lista de la base de datos.

**Resultado:** el perfil se reutiliza sin perder sus registros anteriores. Guardar un nombre activo equivalente, ignorando mayusculas y espacios normalizados, recupera el perfil existente.

**Alternativa:** al eliminarlo desaparece del selector activo, pero su historial permanece. Si tiene pedidos pendientes, la baja se rechaza. Crear luego otro perfil con el mismo nombre genera una identidad nueva, no hereda los pedidos anteriores.

### CF-02. Registrar una tanda para una mesa

**Actor:** mozo. **Condiciones:** perfil seleccionado y jornada abierta.

1. Pulsar Agregar pedidos en una mesa.
2. Buscar un plato o elegir su categoria.
3. Hacer clic en el plato para agregar una unidad a la tanda, sin abrir un formulario de cantidad u observaciones.
4. Si es un cafe con variantes, elegir primero el tipo de cafe.
5. Ajustar cantidades en la tanda y, si hace falta, abrir el boton de comentario.
6. Pulsar Confirmar tanda.
7. El servidor abre o recupera la cuenta de la mesa, guarda todos los pedidos y los deja pendientes en cocina.

**Resultado:** la tanda queda registrada y la mesa ocupada. Cada pedido queda asociado al perfil que lo registro.

**Alternativas:** antes de confirmar, los productos solo existen en la tanda del navegador. Descartarla no agrega pedidos a la base. Si el servidor rechaza uno de sus productos, se revierte la operacion completa; no se guarda una tanda parcial.

### CF-03. Preparar y entregar

**Actores:** cocina y mozo responsable.

1. Cocina recibe el pedido en Pendientes.
2. Lo pasa a En preparacion y luego a Listo.
3. El mozo recibe un aviso visual y lo encuentra en Para entregar.
4. El perfil responsable pulsa Confirmar entrega.
5. El pedido pasa a Entregado y sale de la cola de cocina.

```text
PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO
              Cocina         Cocina      Mozo
```

El estado `BORRADOR` tambien existe en el modelo y la API, antes del envio a cocina. La confirmacion normal de una tanda registra y envia sus pedidos juntos, por lo que llegan a cocina como `PENDIENTE`.

**Reglas:** no se pueden saltear estados ni entregar pedidos cancelados, no listos o ya entregados. La confirmacion comprueba el identificador del perfil responsable, pero no es autenticacion segura porque los perfiles son seleccionables sin contrasena.

Los avisos se consultan aproximadamente cada tres segundos, incluso mientras el mozo carga otra tanda. Las otras actualizaciones de pantalla evitan interrumpir los dialogos abiertos.

### CF-04. Cobrar total o parcialmente

**Actor:** mozo. **Condiciones:** cuenta abierta, jornada abierta y pedidos enviados a cocina, no cancelados.

1. Abrir Pagar cuenta y seleccionar los productos y cantidades a cobrar.
2. Elegir efectivo o tarjeta.
3. El servidor calcula los importes usando los precios guardados en los pedidos.
4. En efectivo, aplica el descuento del 10 %; opcionalmente se puede indicar un total redondeado valido.
5. Confirmar el pago.
6. El sistema registra el pago, sus detalles, las cantidades pagadas y el ticket en una misma transaccion.
7. Si queda saldo, mantiene la cuenta abierta. Si no queda saldo, la cierra y libera la mesa.

**Ejemplo de efectivo:** para un subtotal de $10.000, el total con descuento es $9.000. El total manual admitido va de $8.550 a $9.450. Los limites se ajustan a centavos sin ampliar el margen. Tarjeta no tiene descuento y no utiliza ese redondeo.

**Alternativas:** no se permite pagar cantidades superiores a lo pendiente, pedidos cancelados o de otra cuenta. Si falla la emision del ticket, se revierte todo el pago. Una falla de impresion posterior no deshace un pago ya confirmado.

**Importante:** se puede cobrar antes de entregar. Un pedido pagado sigue en cocina hasta su entrega, aunque la cuenta ya este cerrada y la mesa haya quedado libre.

### CF-05. Atender Take Away

**Actor:** mozo.

1. Abrir Agregar pedidos en Take Away.
2. Ingresar obligatoriamente el nombre del cliente.
3. Armar y confirmar su tanda.
4. Repetir para otros clientes, que pueden compartir la cuenta activa del sector.
5. Usar Cobrar cliente para seleccionar los consumos de una persona sin cobrar los de las demas.

**Resultado:** los pedidos conservan el nombre del cliente y pueden cobrarse por separado.

### CF-06. Modificar el menu o cancelar un pedido

**Actor:** operador desde la vista Mozo.

1. Abrir Gestion de platos y preparar altas, modificaciones, cambios de disponibilidad o bajas.
2. Revisar la lista de cambios pendientes.
3. Pulsar Aplicar cambios para guardarlos juntos o descartarlos antes de aplicar.

**Resultado:** se actualiza el catalogo para pedidos futuros. Cambiar el nombre o precio de un producto no modifica los pedidos historicos, porque estos guardan una copia de esos datos.

**Caso relacionado:** desde los consumos de una cuenta se puede cancelar un pedido sin pagos. La cancelacion conserva el registro y deja de considerarlo pendiente de cobro; no equivale a eliminar el producto del menu.

### CF-07. Consultar quien registro cada pedido

**Actor:** operador desde la vista Mozo.

1. Abrir Pedidos.
2. Seleccionar una fecha y, opcionalmente, un perfil.
3. Consultar numero de pedido, hora, mesa/cuenta, responsable, plato, cantidad, estado y cantidad pagada.
4. Incluir perfiles eliminados desde el filtro del historial cuando corresponda.

**Resultado:** se conserva la trazabilidad de pedidos entregados, cancelados y asociados a cuentas ya cerradas.

### CF-08. Cerrar la jornada

**Actor:** operador desde la vista Mozo.

1. Abrir Cierre diario y consultar la fecha y su resumen.
2. Verificar que no haya consumos impagos en cuentas abiertas de esa fecha o anteriores.
3. Confirmar el cierre.
4. El sistema guarda cantidad de tickets, totales por medio de pago, descuentos y total general.
5. Consultar o imprimir el cierre desde el historial.

**Resultado:** hay un unico cierre guardado por fecha. Si se cierra la fecha actual, no se admiten nuevos pedidos ni pagos para esa jornada. La preparacion y entrega de lo ya registrado siguen disponibles.

La jornada depende de la fecha del sistema en la zona horaria de Buenos Aires. No hace falta borrar la base de datos para pasar al dia siguiente. No existe un boton de reapertura en esta version.

## 5. Arquitectura y estructura

```text
Navegador: HTML + CSS + JavaScript
                |
          Fetch / HTTP / JSON
                |
        Controller (web)
                |
        Service (negocio)
                |
        DAO (consultas SQL)
                |
           MySQL / H2
```

| Capa | Responsabilidad | Ejemplos |
| --- | --- | --- |
| `model` | Representar entidades, estados y operaciones propias del dominio. | `Mesa`, `Cuenta`, `Pedido`, `Pago`, `PerfilMozo`. |
| `web` | Recibir solicitudes, validar datos y devolver respuestas o errores. | `CafeController`, `PaginaController`, `ErroresController`. |
| `service` | Aplicar reglas de negocio y coordinar transacciones y DAO. | `PedidoService`, `PagoService`, `CierreService`. |
| `dao` | Leer y escribir datos mediante SQL y `JdbcTemplate`. | `PedidoDAO`, `CuentaDAO`, `PerfilMozoDAO`. |
| `config` | Cargar datos iniciales y adaptar instalaciones anteriores. | `DatosIniciales`, `MigracionPerfiles`. |
| `fiscal` | Encapsular la configuracion y emision fiscal simulada. | `ConfiguracionFiscal`, `ClienteFiscalSimulado`. |
| `static` | Mostrar pantallas, manejar la tanda temporal y consumir la API. | `index.html`, `styles.css`, `app.js`. |

```text
TrabajoTP_Martes/
|-- README.md                          Guia general para el equipo
|-- .gitignore                         Exclusiones de datos y archivos privados
`-- CafeJava/
    |-- pom.xml                        Dependencias y configuracion Maven
    |-- iniciar.cmd / detener.cmd      Lanzadores para Windows
    |-- README.md                      Guia tecnica complementaria
    |-- database/                      SQL para crear y consultar tablas
    |-- docs/
    |   |-- UML.md                     Diagrama de clases
    |   |-- API.md                     Rutas y ejemplos JSON
    |   `-- capturas/                  Capturas de las pruebas de interfaz
    |-- scripts/browser-check.cjs       Pruebas opcionales de navegador
    `-- src/
        |-- main/java/ar/edu/cafe/
        |   |-- CafeApplication.java   Punto de entrada
        |   |-- model/
        |   |-- dao/
        |   |-- service/
        |   |-- web/
        |   |-- config/
        |   `-- fiscal/
        |-- main/resources/
        |   |-- application*.properties
        |   |-- schema.sql
        |   |-- menu-original.json
        |   `-- static/
        `-- test/java/ar/edu/cafe/      Pruebas automaticas Java
```

### Clases principales

- `Mesa` representa el lugar fisico y su estado; puede tener varias cuentas a lo largo del tiempo, pero solo una abierta a la vez.
- `Cuenta` agrupa la atencion de una mesa, sus pedidos y sus pagos.
- `Producto` pertenece a una `Categoria` y representa una opcion del catalogo.
- `Pedido` referencia al producto y conserva `nombreProducto` y `precioUnitario` como datos historicos. `mozo` conserva el nombre del responsable y `perfilMozoId` su identidad.
- `PerfilMozo` permite recuperar al responsable aunque se pierda la sesion.
- `Pago` contiene `DetallePago`, que indica que cantidades de cada pedido se cobraron.
- `TicketElectronico` contiene `ItemTicket` y documenta un pago.
- `CierreDiario` resume los tickets emitidos para una fecha.

Los servicios principales son `MesaService`, `MenuService`, `PedidoService`, `PagoService`, `TicketService`, `ImpresionService`, `CierreService` y `PerfilMozoService`. `OperacionGuard` coordina operaciones mediante una fila de control SQL para evitar conflictos entre pedidos, pagos y cierres simultaneos.

## 6. Base de datos y persistencia

Las doce tablas son `mesas`, `categorias`, `productos`, `cuentas`, `perfiles_mozo`, `pedidos`, `pagos`, `detalles_pago`, `tickets`, `items_ticket`, `cierres_diarios` y `control_sistema`.

El esquema se encuentra en [schema.sql](CafeJava/src/main/resources/schema.sql). La aplicacion crea las tablas faltantes y carga las nueve posiciones de atencion y el menu cuando corresponde. `menu-original.json` se usa como carga inicial; los cambios posteriores se guardan en SQL, no en ese JSON.

`MigracionPerfiles` incorpora la relacion con perfiles en bases anteriores y vincula sus pedidos por nombre sin borrar el texto historico. Para pedidos anteriores no puede distinguir personas que usaron el mismo nombre, porque antes no existia otra identidad.

Archivos para trabajar desde MySQL Workbench:

- [crear-base.sql](CafeJava/database/crear-base.sql): crear la base de datos.
- [crear-tablas.sql](CafeJava/database/crear-tablas.sql): consultar o crear la estructura inicial de tablas.
- [ver-tablas.sql](CafeJava/database/ver-tablas.sql): ver columnas, datos y relaciones, incluido quien registro cada pedido.
- [reiniciar-datos.sql](CafeJava/database/reiniciar-datos.sql): reinicio destructivo de datos operativos para pruebas. No se necesita para el uso normal y no debe ejecutarse sobre datos que se quieran conservar.

## 7. Como ejecutar el proyecto

### Requisitos

- JDK 17 y Maven disponibles en `PATH`.
- Git para clonar el repositorio.
- Navegador actualizado.
- MySQL en ejecucion solo si se elige el modo `mysql`. Workbench es opcional.

Comprobar las herramientas:

```powershell
java -version
mvn -version
```

Clonar y entrar al proyecto:

```powershell
git clone https://github.com/BladSector/TrabajoTP_Martes.git
cd TrabajoTP_Martes\CafeJava
```

No hace falta instalar Node ni ejecutar `npm install` para usar la aplicacion: el frontend se sirve desde Spring Boot.

### Opcion A: demostracion sin MySQL

```powershell
.\iniciar.cmd demo
```

H2 guarda sus datos de demostracion en `CafeJava/runtime/cafe-demo.mv.db`, separado de MySQL. En el primer inicio, el lanzador compila si no existe el JAR.

### Opcion B: MySQL

1. Ejecutar `database/crear-base.sql` desde Workbench o el cliente MySQL.
2. Elegir un usuario con permisos para crear tablas y leer/escribir en `cafe_java`.
3. Configurar las credenciales propias en la misma terminal. Los siguientes valores son ejemplos, no credenciales del proyecto:

```powershell
$env:CAFE_DB_USER = "tu_usuario_mysql"
$env:CAFE_DB_PASSWORD = "tu_contrasena_mysql"
.\iniciar.cmd mysql
```

La conexion predeterminada apunta a `localhost:3306`, base `cafe_java`. Para otro servidor o puerto, configurar `CAFE_DB_URL`. Tambien se admite `application-local.properties`, pero ese archivo es privado y no se incluye en GitHub.

### Abrir, detener y recompilar

Abrir [http://localhost:8081](http://localhost:8081). Para otro puerto, por ejemplo, usar `.\iniciar.cmd demo 8082`.

```powershell
.\detener.cmd
```

Tambien se puede usar Ctrl+C en la terminal de inicio. Si Windows pregunta si desea terminar el trabajo por lotes, responder `S` para finalizar el lanzador.

El lanzador reutiliza el JAR existente. Despues de modificar codigo o archivos de la interfaz, ejecutar:

```powershell
.\detener.cmd
mvn package
.\iniciar.cmd mysql
```

Cambiar `mysql` por `demo` cuando corresponda. Los archivos `.cmd` no requieren habilitar la ejecucion de scripts de PowerShell.

### Pantallas

| Ruta | Contenido |
| --- | --- |
| `/mozo` | Mapa de mesas y operacion del salon. |
| `/cocina` | Preparacion y pedidos listos para retirar. |
| `/entregas` | Avisos y confirmacion de entrega por el mozo. |
| `/gestion-menu` | Categorias y gestion de platos. |
| `/historial-pedidos` | Registro de pedidos y responsables. |
| `/historial-tickets` | Consulta de comprobantes. |
| `/resumen-diario` | Ventas, cierre de jornada e historial. |

## 8. Pruebas y recorrido sugerido para clase

Desde `CafeJava`, ejecutar:

```powershell
mvn test
```

Las pruebas Java usan H2 temporal en memoria, no la base MySQL del equipo. Cubren pagos parciales, descuentos y redondeo, validaciones, rollback de operaciones, cobros simultaneos, Take Away, cocina y entregas, perfiles, historial, migracion y cierre diario.

`scripts/browser-check.cjs` recorre la interfaz con Playwright y Edge en escritorio y movil. Es una prueba adicional que registra operaciones y cierra la jornada de prueba: debe usarse con una instancia de demostracion temporal preparada para ese fin, nunca con datos reales.

Para presentar el programa manualmente, usar una base de demostracion con jornada abierta:

1. Crear dos perfiles de mozo y elegir uno.
2. Armar una tanda en una mesa con un plato, un cafe y un comentario opcional.
3. Confirmar y comprobar que aparece el responsable en Pedidos.
4. Abrir Cocina en otra pestana, preparar y marcar listo.
5. Volver al mozo y confirmar la entrega desde Para entregar.
6. Cobrar una parte en efectivo y el resto con tarjeta.
7. Comprobar que se libera la mesa y quedan los tickets.
8. Revisar el historial y cerrar la jornada cuando no queden consumos impagos.

## 9. Alcance y limitaciones actuales

- Es un proyecto educativo con integracion fiscal simulada. Los tickets no tienen CAE ni validez fiscal real.
- Registrar tarjeta solo indica el medio de pago; no existe integracion con un banco, terminal o pasarela de cobro.
- Los perfiles no tienen contrasenas ni control de permisos por usuario.
- El servidor escucha en `127.0.0.1`: por defecto, se usa desde el mismo equipo, no desde otros dispositivos de la red.
- La impresion desde el navegador esta disponible. La impresion directa requiere configurar una impresora compatible; no se trasladaron todos los comandos especificos del programa Python.
- Los datos de operacion se guardan en SQL. GitHub guarda codigo y documentacion, no funciona como respaldo de la base.
- Las credenciales locales, `runtime/`, `target/` y la configuracion del IDE quedan excluidas por `.gitignore`.

## 10. Documentacion complementaria

- [Guia tecnica de CafeJava](CafeJava/README.md).
- [Diagrama UML](CafeJava/docs/UML.md).
- [API y ejemplos de solicitudes](CafeJava/docs/API.md).
- [Capturas de las pantallas](CafeJava/docs/capturas/).
