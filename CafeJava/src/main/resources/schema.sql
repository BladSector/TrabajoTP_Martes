CREATE TABLE IF NOT EXISTS mesas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    es_take_away BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_id BIGINT NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    precio DECIMAL(14,2) NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);
CREATE TABLE IF NOT EXISTS cuentas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mesa_id BIGINT NOT NULL,
    mesa_abierta_id BIGINT UNIQUE,
    fecha_apertura TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP NULL,
    estado VARCHAR(20) NOT NULL,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    FOREIGN KEY (mesa_abierta_id) REFERENCES mesas(id)
);
CREATE TABLE IF NOT EXISTS perfiles_mozo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    clave_activa VARCHAR(100) UNIQUE,
    fecha_creacion TIMESTAMP NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    nombre_producto VARCHAR(200) NOT NULL,
    cantidad INT NOT NULL,
    cantidad_pagada INT NOT NULL DEFAULT 0,
    precio_unitario DECIMAL(14,2) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    estado_cocina VARCHAR(30) NOT NULL,
    mozo VARCHAR(100) NOT NULL,
    perfil_mozo_id BIGINT NULL,
    nombre_cliente VARCHAR(100) NOT NULL,
    notas VARCHAR(1000) NOT NULL,
    cancelado BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (cuenta_id) REFERENCES cuentas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT fk_pedido_perfil FOREIGN KEY (perfil_mozo_id) REFERENCES perfiles_mozo(id)
);
CREATE TABLE IF NOT EXISTS pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    metodo VARCHAR(20) NOT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    descuento DECIMAL(14,2) NOT NULL,
    total DECIMAL(14,2) NOT NULL,
    FOREIGN KEY (cuenta_id) REFERENCES cuentas(id)
);
CREATE TABLE IF NOT EXISTS detalles_pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pago_id BIGINT NOT NULL,
    pedido_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(14,2) NOT NULL,
    UNIQUE (pago_id, pedido_id),
    FOREIGN KEY (pago_id) REFERENCES pagos(id),
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
);
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(100) NOT NULL UNIQUE,
    pago_id BIGINT NOT NULL UNIQUE,
    fecha_emision TIMESTAMP NOT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    descuento DECIMAL(14,2) NOT NULL,
    iva DECIMAL(14,2) NOT NULL,
    total DECIMAL(14,2) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    entorno VARCHAR(30) NOT NULL,
    tipo_documento_cliente VARCHAR(10) NOT NULL,
    numero_documento_cliente VARCHAR(30) NOT NULL,
    razon_social_cliente VARCHAR(200) NOT NULL,
    razon_social_emisor VARCHAR(200) NOT NULL,
    cuit_emisor VARCHAR(30) NOT NULL,
    domicilio_emisor VARCHAR(250) NOT NULL,
    condicion_iva_emisor VARCHAR(100) NOT NULL,
    punto_venta INT NOT NULL,
    numero_comprobante BIGINT NULL,
    cae VARCHAR(30) NULL,
    vencimiento_cae DATE NULL,
    FOREIGN KEY (pago_id) REFERENCES pagos(id)
);
CREATE TABLE IF NOT EXISTS items_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    codigo_producto VARCHAR(100) NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(14,2) NOT NULL,
    alicuota_iva DECIMAL(6,2) NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);
CREATE TABLE IF NOT EXISTS cierres_diarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL UNIQUE,
    fecha_hora_cierre TIMESTAMP NOT NULL,
    numero_informe INT NOT NULL,
    cantidad_tickets INT NOT NULL,
    total_efectivo DECIMAL(14,2) NOT NULL,
    total_tarjeta DECIMAL(14,2) NOT NULL,
    total_descuentos DECIMAL(14,2) NOT NULL,
    total_general DECIMAL(14,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS control_sistema (id INT PRIMARY KEY);
