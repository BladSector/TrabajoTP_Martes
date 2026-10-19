USE cafe_java;

-- Nombres de todas las tablas.
SHOW TABLES;

-- Columnas y tipos de datos.
DESCRIBE mesas;
DESCRIBE categorias;
DESCRIBE productos;
DESCRIBE cuentas;
DESCRIBE pedidos;
DESCRIBE perfiles_mozo;
DESCRIBE pagos;
DESCRIBE detalles_pago;
DESCRIBE tickets;
DESCRIBE items_ticket;
DESCRIBE cierres_diarios;
DESCRIBE control_sistema;

-- Datos de cada tabla. Seleccionar una consulta para ejecutarla en Workbench.
SELECT * FROM mesas ORDER BY id;
SELECT * FROM categorias ORDER BY id;
SELECT * FROM productos ORDER BY id;
SELECT * FROM cuentas ORDER BY id;
SELECT * FROM pedidos ORDER BY id;
SELECT * FROM perfiles_mozo ORDER BY id;
SELECT * FROM pagos ORDER BY id;
SELECT * FROM detalles_pago ORDER BY id;
SELECT * FROM tickets ORDER BY id;
SELECT * FROM items_ticket ORDER BY id;
SELECT * FROM cierres_diarios ORDER BY id;
SELECT * FROM control_sistema ORDER BY id;

-- Relaciones entre tablas.
SELECT p.id, c.nombre AS categoria, p.nombre, p.precio, p.disponible
FROM productos p
JOIN categorias c ON c.id = p.categoria_id
WHERE p.eliminado = FALSE
ORDER BY c.nombre, p.nombre;

SELECT cu.id AS cuenta, m.nombre AS mesa, cu.estado,
       pe.id AS pedido, pe.nombre_producto, pe.cantidad, pe.cantidad_pagada
FROM cuentas cu
JOIN mesas m ON m.id = cu.mesa_id
LEFT JOIN pedidos pe ON pe.cuenta_id = cu.id
ORDER BY cu.id, pe.id;

-- Autor de cada pedido, incluso cuando se elimino su perfil.
SELECT pe.id AS pedido, pe.fecha_hora, m.nombre AS mesa, pe.nombre_producto,
       pe.cantidad, pe.mozo AS nombre_al_registrar, pe.perfil_mozo_id,
       pm.eliminado AS perfil_eliminado, pe.estado_cocina, pe.cancelado
FROM pedidos pe
JOIN cuentas cu ON cu.id = pe.cuenta_id
JOIN mesas m ON m.id = cu.mesa_id
LEFT JOIN perfiles_mozo pm ON pm.id = pe.perfil_mozo_id
ORDER BY pe.fecha_hora DESC, pe.id DESC;
