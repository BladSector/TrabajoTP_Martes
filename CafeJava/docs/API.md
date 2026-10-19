# API

Todas las rutas usan JSON, excepto el texto de ticket. Los identificadores son numericos. Los errores devuelven `{"error":"mensaje"}`.

| Metodo | Ruta | Operacion |
| --- | --- | --- |
| GET | /api/estado | Fecha, jornada abierta/cerrada y perfil |
| GET | /api/mesas | Mesas, cuenta activa y pendiente |
| GET | /api/perfiles | Perfiles activos; incluirEliminados=true agrega las bajas |
| POST | /api/perfiles | Guardar o recuperar por nombre: {"nombre":"Gabi"} |
| DELETE | /api/perfiles/{id} | Baja logica si no tiene pedidos pendientes de entrega |
| GET | /api/pedidos/historial?fecha=2026-10-18&perfilId=1 | Registro por fecha (hoy por defecto); perfilId es opcional |
| GET | /api/cocina | Pedidos pendientes, en preparacion y listos, incluidos los ya pagados |
| POST | /api/mesas/{id}/comandas | Guardar una comanda completa y enviarla a cocina en una transaccion |
| GET | /api/mesas/{id}/cuenta | Cuenta activa, o {"cuenta":null} |
| POST | /api/mesas/{id}/cuenta | Abrir cuenta, o devolver la que ya esta abierta |
| GET | /api/cuentas/{id} | Cuenta completa e historial |
| POST | /api/cuentas/{id}/cerrar | Liberar mesa si no hay pendientes |
| GET | /api/menu | Productos no eliminados |
| POST | /api/menu | Crear producto |
| PUT | /api/menu/{id} | Modificar producto |
| DELETE | /api/menu/{id} | Baja logica, conserva historial |
| POST | /api/menu/cambios | Aplicar juntos cambios CREAR, ACTUALIZAR y ELIMINAR |
| GET | /api/categorias | Listar categorias |
| POST | /api/categorias | Crear categoria |
| POST | /api/cuentas/{id}/pedidos | Agregar borrador |
| POST | /api/cuentas/{id}/cocina | Enviar pedidos |
| PUT | /api/pedidos/{id}/estado | Cocina avanza a EN_PREPARACION o LISTO, nunca ENTREGADO |
| POST | /api/pedidos/{id}/entregar | El mozo responsable confirma un pedido LISTO como ENTREGADO |
| POST | /api/pedidos/{id}/cancelar | Cancelar un pedido sin pagos |
| POST | /api/cuentas/{id}/pagos | Registrar pago y emitir ticket |
| GET | /api/tickets | Historial |
| GET | /api/tickets/{id} | Ticket completo |
| GET | /api/tickets/{id}/texto | Comprobante text/plain |
| POST | /api/tickets/{id}/imprimir | Impresion directa configurada |
| GET | /api/cierres/resumen?fecha=2026-10-13 | Consultar resumen sin cerrar |
| POST | /api/cierres?fecha=2026-10-13 | Cerrar jornada |
| GET | /api/cierres | Historial de cierres |
| GET | /api/cierres/texto?fecha=2026-10-13 | Texto de un cierre guardado |
| POST | /api/cierres/imprimir?fecha=2026-10-13 | Impresion directa de un cierre guardado |

## Pedido

```json
{"productoId":3,"cantidad":2,"perfilMozoId":1,"nombreCliente":"","notas":"Cortado"}
```

## Envio a cocina

```json
{"pedidosIds":[1,2]}
```

## Entrega del mozo

```json
{"perfilMozoId":1}
```

Se envia a `POST /api/pedidos/{id}/entregar`. El identificador debe coincidir con el perfil responsable y seguir activo. No se permiten entregas anticipadas, canceladas ni repetidas. Preparacion y entrega siguen disponibles si el pedido ya fue pagado o la caja fue cerrada.

La vista `/entregas` consulta `/api/cocina` cada tres segundos y muestra los pedidos LISTO, con filtros por perfil y aviso visual de nuevos pedidos. Los perfiles son operativos, sin contrasena; no reemplazan autenticacion de usuarios.

El pedido conserva `mozo` como nombre historico y `perfilMozoId` como identidad. El servidor toma el nombre del perfil, no del texto enviado. Por compatibilidad, las solicitudes antiguas sin identificador pueden enviar `mozo`: se guarda o recupera su perfil por nombre. El cliente actual siempre envia el identificador. Eliminar un perfil no elimina pedidos ni permite reutilizar su identidad al crear otro con el mismo nombre.

## Pago parcial

```json
{"metodo":"EFECTIVO","detalles":[{"pedidoId":1,"cantidad":1}],"totalEfectivo":null}
```

Los precios y subtotales se calculan en el servidor. El campo opcional `totalEfectivo` permite redondear hasta un 5 % por debajo o por encima del total con el descuento automatico del 10 %. Ambos extremos se incluyen y se limitan a centavos sin ampliar el margen. Por ejemplo, un subtotal de 10000 deja un total con descuento de 9000 y admite de 8550 a 9450. Fuera del rango se devuelve HTTP 400 sin registrar pagos, tickets ni cantidades pagadas. En tarjeta se ignora ese campo. Si se omite, se conserva el descuento automatico.

## Producto

```json
{"nombre":"Cafe","descripcion":"Taza","precio":5500,"disponible":true,"categoriaId":1}
```
