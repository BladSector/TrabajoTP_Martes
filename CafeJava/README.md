# Cafe Java

Adaptacion del proyecto Python/Flask de Gabi.V2 a Java 17, Spring Boot y persistencia SQL. La interfaz funciona en el navegador. Se conservaron el logo y una copia del menu original; el proyecto Python no se modifica.

## Abrir el proyecto

Abrir esta carpeta en IntelliJ IDEA como proyecto Maven, seleccionando Java 17. El punto de entrada es `ar.edu.cafe.CafeApplication`.

La implementacion usa clases Java con atributos privados, getters/setters, metodos de negocio, servicios y DAO JDBC. No utiliza Lombok ni Hibernate, para que las relaciones y las consultas sean visibles al estudiar el codigo.

Los identificadores y el numero de comprobante usan `Integer`, la version objeto de `int`, para permitir `null` cuando todavia no fueron asignados. Cantidades y contadores usan `int`. Los DAO y la API tambien usan identificadores de 32 bits. Las tablas existentes conservan `BIGINT` por compatibilidad y no es necesario reiniciar ni migrar datos; los valores deben caber en un entero positivo de hasta 2147483647.

Los importes del modelo Java usan `double`, con redondeo a dos decimales en los calculos. El servidor calcula los totales: efectivo aplica automaticamente un 10 % de descuento y tarjeta no tiene descuento. No hay total manual ni rango de redondeo. Los DAO leen los importes con `getDouble()`; las columnas SQL conservan `DECIMAL`.

## Probar sin configurar MySQL

En PowerShell, desde esta carpeta:

```powershell
.\iniciar.cmd demo
```

O con Maven:

```powershell
mvn test
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Abrir http://localhost:8081. Para usar otro puerto, ejecutar `.\iniciar.cmd demo 8082`.

Para detener la aplicacion, ejecutar `.\detener.cmd`, o usar Ctrl+C en la terminal donde se inicio. El lanzador `iniciar.cmd` detiene las instancias anteriores de este proyecto antes de iniciar. Los archivos `.cmd` no requieren habilitar la ejecucion de scripts de PowerShell.

El lanzador usa el JAR compilado existente. Despues de modificar codigo Java, ejecutar `.\detener.cmd`, luego `mvn package` y finalmente `.\iniciar.cmd`. Los scripts `.ps1` siguen disponibles para equipos donde su ejecucion este habilitada.

El perfil `demo` usa H2 en modo de compatibilidad MySQL y guarda sus datos en `runtime/cafe-demo.mv.db`. La demostracion tiene su propia base; no usa ni modifica las bases MySQL existentes. Las pruebas usan otra base H2 temporal en memoria.

## Ejecutar con MySQL

El perfil predeterminado es `mysql`.

1. Ejecutar `database/crear-base.sql` en MySQL Workbench para crear `cafe_java`.
2. Usar un usuario con permisos para crear tablas y leer/escribir en esa base.
3. Iniciar con `.\iniciar.cmd`. La configuracion local se carga desde `application-local.properties`, excluido de Git. Si no existe ese archivo, configurar `CAFE_DB_USER` y `CAFE_DB_PASSWORD` antes del inicio.
4. Abrir http://localhost:8081.

La conexion puede configurarse con `CAFE_DB_URL`, `CAFE_DB_USER` y `CAFE_DB_PASSWORD`. La URL predeterminada es:

```text
jdbc:mysql://localhost:3306/cafe_java?connectionTimeZone=America/Argentina/Buenos_Aires
```

El esquema esta en `src/main/resources/schema.sql`. La aplicacion crea las tablas faltantes; no borra datos. En una base nueva, carga las nueve mesas y el menu de `menu-original.json`. La carga inicial solo ocurre si no hay productos; las ediciones posteriores se guardan en SQL.

Para importar otro menu con la misma estructura `{"menu":{"categoria":[productos]}}`, configurar `CAFE_MENU_JSON` con una ubicacion `file:C:/ruta/menu.json` antes del primer inicio de una base vacia.

## SQL para practicar

Abrir estos archivos en MySQL Workbench, en la conexion local del puerto 3306:

- `database/crear-tablas.sql`: crea la base y las doce tablas si no existen.
- `database/ver-tablas.sql`: muestra nombres, columnas, datos y ejemplos de relaciones.
- `database/reiniciar-datos.sql`: borra pedidos, cuentas, pagos, tickets y cierres, y libera las mesas. Conserva productos y categorias. Detener la app antes de ejecutarlo y volver a iniciarla al terminar.

Para reabrir el sistema despues de un cierre, se debe quitar el cierre de la jornada o reiniciar los datos de prueba. El sistema bloquea consumos y pagos mientras exista un cierre para la fecha actual.

## Flujo de atencion

1. Guardar un perfil de mozo con su nombre o elegir uno existente desde la cabecera.
2. Seleccionar una mesa y abrir su cuenta.
3. Agregar productos con cantidad, cliente y observaciones.
4. Cocina avanza de pendiente a en preparacion y listo. El mozo recibe el aviso y confirma la entrega desde Para entregar.
5. Elegir cantidades a cobrar y registrar el pago.
6. Se aplica automaticamente 10 % de descuento en efectivo. Tarjeta no tiene descuento.
7. Se genera el ticket sin mostrar el renglon de descuento. La cuenta se cierra y la mesa queda libre solo cuando no queden pagos ni entregas pendientes.
8. Consultar tickets y cerrar la jornada desde sus secciones.

Take away requiere nombre de cliente. Los pedidos pagados siguen visibles con la marca Pagado hasta su entrega; los entregados siguen visibles hasta su cobro. El historial permanece despues de liberar la mesa. Los cambios del menu no alteran los nombres ni los precios historicos de los consumos.

## Organizacion

- `model/`: Mesa, Cuenta, Categoria, Producto, Pedido, Pago, DetallePago, TicketElectronico, ItemTicket y CierreDiario, ademas de enums.
- `dao/`: acceso SQL mediante JdbcTemplate. PagoDAO guarda sus detalles y TicketDAO guarda sus items.
- `service/`: MesaService, MenuService, PedidoService, PagoService, TicketService, ImpresionService y CierreService.
- `fiscal/`: ConfiguracionFiscal, interfaz ClienteFiscal e implementacion simulada.
- `web/`: rutas HTTP, solicitudes validadas y manejo de errores.
- `config/`: carga inicial del menu y las mesas.
- `src/main/resources/static/`: interfaz HTML, CSS y JavaScript.
- `docs/UML.md`: diagrama de clases de esta implementacion.
- `docs/API.md`: rutas para probar desde Postman.

Los atributos principales de los servicios corresponden a la tabla acordada. Se agregaron dependencias para obtener productos, usar el reloj y coordinar las transacciones. `OperacionGuard` bloquea una fila de control durante cada operacion de mesa/pedido/pago/cierre, evitando cobros simultaneos duplicados o pagos durante el cierre.

El pago, sus detalles, las cantidades pagadas y el ticket se guardan en una misma transaccion. La mesa se libera en la transaccion que completa el ultimo requisito: cobro o entrega. Si falla la generacion del ticket, se revierte todo el pago. La impresion automatica se ejecuta despues del commit y no revierte un cobro registrado.

## Alcance de esta version

El circuito de mesas, menu, pedidos, pagos parciales, tickets e informe diario esta implementado. Para esta primera version de clase:

- La integracion fiscal es una simulacion, sin CAE ni validez fiscal. La autorizacion real de AFIP/ARCA y la firma con certificados del proyecto Python requieren una implementacion posterior de ClienteFiscal.
- Solo se admite IVA EXENTO. Cambiar la alicuota a un valor distinto de cero produce un error explicito.
- Se puede imprimir desde el navegador. La impresion directa usa Java Print Service y requiere una impresora que acepte texto UTF-8. Los comandos ESC/POS y ajustes especificos de Xprinter no estan trasladados.
- Los respaldos ZIP y la sincronizacion con GitHub del proyecto Python no se trasladaron. Para MySQL se debe implementar un respaldo de base de datos.
- Se copia el menu; no se importan mesas ocupadas, pagos, tickets ni cierres historicos de los JSON anteriores.
- Los perfiles de mozo se guardan en SQL, pero no tienen contrasena ni autenticacion. El servidor escucha solo en `127.0.0.1`.

## Verificacion

```powershell
mvn test
mvn package
```

Las pruebas cubren pago parcial, descuento, cierre de cuenta, historial de precio/nombre, validacion de cantidades, pedidos de otras cuentas, detalles duplicados, rollback ante fallo de ticket, cobros simultaneos, take away y cierre diario.

Referencia del acceso SQL utilizado: [documentacion oficial de Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html).

## Perfiles y pantallas

Los perfiles de mozo se guardan en `perfiles_mozo`. El selector permite recuperar cualquiera aunque se pierda la sesion del navegador; el ultimo elegido se recuerda en almacenamiento local. El boton de usuarios permite crearlos, seleccionarlos y eliminarlos. La baja es logica y se bloquea si hay pedidos sin entregar o cancelar. No depende de que se haya cerrado o cobrado la cuenta.

Cada pedido guarda el identificador del perfil y una copia del nombre al registrarlo. La pantalla Pedidos (`/historial-pedidos`) muestra autor, hora, mesa/cuenta, plato, cantidad, estado y pagos, con filtros por fecha y perfil, incluidos los eliminados. Al iniciar, `MigracionPerfiles` agrega la relacion a las bases anteriores y vincula sus pedidos por nombre sin borrar ni alterar el texto historico. Nombres iguales de pedidos anteriores se agrupan porque antes no habia otra identidad disponible.

- Mozo: http://localhost:8081/mozo. Mapa de mesas, gestion de platos, cobro, tickets y cierre diario.
- Cocina: http://localhost:8081/cocina. Cola con pendientes, en preparacion y listos para retirar. Cocina no marca entregado.
- Para entregar: http://localhost:8081/entregas. El mozo ve sus pedidos listos y confirma la entrega; tambien recibe avisos visuales cada tres segundos, incluso al cargar otra mesa.
- El selector Mozo/Cocina de la cabecera cambia la vista. Son vistas operativas; no hay autenticacion de usuarios en esta version.
- Ambas vistas se actualizan cada tres segundos cuando no hay un dialogo abierto.

El pedido de una mesa se arma en un dialogo con categorias, busqueda, opciones de cafe y cantidades editables. Se guarda toda la comanda al pulsar Confirmar tanda. Take Away pide el nombre del cliente y permite cobrar por cliente.

Gestion de platos permite preparar cambios y aplicarlos juntos. Tickets y cierre diario usan los mismos dialogos y tablas. Cobrar no libera una mesa con entregas pendientes. El mapa recupera tambien pedidos sin entregar de cuentas que versiones anteriores ya habian cerrado, sin modificar esos registros historicos.

La actualizacion de interfaz se empaqueta como version 1.1.0 para no reemplazar el JAR bloqueado por la instancia anterior. El puerto predeterminado ahora es 8081. La base MySQL y sus datos no se reinician.

