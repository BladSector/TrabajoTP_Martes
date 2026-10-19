-- Reinicia los datos de prueba de cafe_java.
-- Conserva las mesas, categorias, productos y la configuracion.
-- Detener la aplicacion antes de ejecutar este archivo.
USE cafe_java;

START TRANSACTION;
UPDATE control_sistema SET id = id WHERE id = 1;
DELETE FROM items_ticket;
DELETE FROM tickets;
DELETE FROM detalles_pago;
DELETE FROM pagos;
DELETE FROM pedidos;
DELETE FROM cuentas;
DELETE FROM cierres_diarios;
UPDATE mesas SET estado = 'LIBRE';
COMMIT;

ALTER TABLE items_ticket AUTO_INCREMENT = 1;
ALTER TABLE tickets AUTO_INCREMENT = 1;
ALTER TABLE detalles_pago AUTO_INCREMENT = 1;
ALTER TABLE pagos AUTO_INCREMENT = 1;
ALTER TABLE pedidos AUTO_INCREMENT = 1;
ALTER TABLE cuentas AUTO_INCREMENT = 1;
ALTER TABLE cierres_diarios AUTO_INCREMENT = 1;

SELECT 'mesas' AS tabla, COUNT(*) AS registros FROM mesas
UNION ALL SELECT 'productos', COUNT(*) FROM productos
UNION ALL SELECT 'cuentas', COUNT(*) FROM cuentas
UNION ALL SELECT 'pedidos', COUNT(*) FROM pedidos
UNION ALL SELECT 'pagos', COUNT(*) FROM pagos
UNION ALL SELECT 'tickets', COUNT(*) FROM tickets
UNION ALL SELECT 'cierres_diarios', COUNT(*) FROM cierres_diarios;
