-- =============================================================================
-- SERVITEC - MIGRACIÓN V1: ESQUEMA INICIAL COMPLETO (22 ENTIDADES DEL ERD)
-- Motor: MySQL 8.0 (InnoDB) | Charset: utf8mb4 | Collation: utf8mb4_unicode_ci
-- =============================================================================

-- 1. ROL
CREATE TABLE rol (
    id_rol INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(40) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uk_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. USUARIO
CREATE TABLE usuario (
    id_usuario INT NOT NULL AUTO_INCREMENT,
    id_rol INT NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    contrasena_hash VARCHAR(255) NULL,
    estado_cuenta VARCHAR(30) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_activacion DATETIME NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_usuario_rol ON usuario (id_rol);
CREATE INDEX idx_usuario_estado_cuenta ON usuario (estado_cuenta);

-- 3. TOKEN_CUENTA
CREATE TABLE token_cuenta (
    id_token INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    tipo_token VARCHAR(30) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    fecha_uso DATETIME NULL,
    CONSTRAINT pk_token_cuenta PRIMARY KEY (id_token),
    CONSTRAINT uk_token_cuenta_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_cuenta_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_token_cuenta_usuario_tipo ON token_cuenta (id_usuario, tipo_token);

-- 4. SESION_USUARIO
CREATE TABLE sesion_usuario (
    id_sesion INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    refresh_token_hash VARCHAR(128) NOT NULL,
    fecha_hora_creacion DATETIME NOT NULL,
    fecha_hora_ultima_actividad DATETIME NOT NULL,
    fecha_hora_expiracion DATETIME NOT NULL,
    fecha_hora_revocacion DATETIME NULL,
    CONSTRAINT pk_sesion_usuario PRIMARY KEY (id_sesion),
    CONSTRAINT uk_sesion_usuario_refresh_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_sesion_usuario_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sesion_usuario_usuario ON sesion_usuario (id_usuario);

-- 5. CATEGORIA_EQUIPO
CREATE TABLE categoria_equipo (
    id_categoria INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_categoria_equipo PRIMARY KEY (id_categoria),
    CONSTRAINT uk_categoria_equipo_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. ORDEN_SERVICIO
CREATE TABLE orden_servicio (
    id_orden INT NOT NULL AUTO_INCREMENT,
    codigo_orden VARCHAR(30) NOT NULL,
    id_cliente INT NOT NULL,
    id_tecnico_asignado INT NULL,
    id_categoria INT NOT NULL,
    descripcion_problema TEXT NOT NULL,
    direccion_servicio VARCHAR(255) NOT NULL,
    estado_operativo VARCHAR(30) NOT NULL,
    fecha_solicitud DATETIME NOT NULL,
    fecha_estimada DATETIME NULL,
    fecha_asignacion DATETIME NULL,
    CONSTRAINT pk_orden_servicio PRIMARY KEY (id_orden),
    CONSTRAINT uk_orden_servicio_codigo UNIQUE (codigo_orden),
    CONSTRAINT fk_orden_servicio_cliente FOREIGN KEY (id_cliente) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_orden_servicio_tecnico FOREIGN KEY (id_tecnico_asignado) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_orden_servicio_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_equipo (id_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices explícitos exigidos por SRS Sección 11.1
CREATE INDEX idx_orden_cliente ON orden_servicio (id_cliente);
CREATE INDEX idx_orden_tecnico ON orden_servicio (id_tecnico_asignado);
CREATE INDEX idx_orden_estado ON orden_servicio (estado_operativo);
CREATE INDEX idx_orden_fecha_solicitud ON orden_servicio (fecha_solicitud);

-- 7. CONSENTIMIENTO_DATOS
CREATE TABLE consentimiento_datos (
    id_consentimiento INT NOT NULL AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    id_orden_primera_solicitud INT NOT NULL,
    autorizado BOOLEAN NOT NULL,
    fecha_hora DATETIME NOT NULL,
    version_politica VARCHAR(30) NOT NULL,
    CONSTRAINT pk_consentimiento_datos PRIMARY KEY (id_consentimiento),
    CONSTRAINT uk_consentimiento_cliente UNIQUE (id_cliente),
    CONSTRAINT uk_consentimiento_primera_orden UNIQUE (id_orden_primera_solicitud),
    CONSTRAINT fk_consentimiento_cliente FOREIGN KEY (id_cliente) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_consentimiento_orden FOREIGN KEY (id_orden_primera_solicitud) REFERENCES orden_servicio (id_orden)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. DIAGNOSTICO (Relación 1:0..1 con ORDEN_SERVICIO -> id_orden UNIQUE)
CREATE TABLE diagnostico (
    id_diagnostico INT NOT NULL AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_tecnico INT NOT NULL,
    observaciones TEXT NOT NULL,
    fecha_hora_registro DATETIME NOT NULL,
    CONSTRAINT pk_diagnostico PRIMARY KEY (id_diagnostico),
    CONSTRAINT uk_diagnostico_orden UNIQUE (id_orden),
    CONSTRAINT fk_diagnostico_orden FOREIGN KEY (id_orden) REFERENCES orden_servicio (id_orden),
    CONSTRAINT fk_diagnostico_tecnico FOREIGN KEY (id_tecnico) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. PARAMETRO_DIAGNOSTICO
CREATE TABLE parametro_diagnostico (
    id_parametro INT NOT NULL AUTO_INCREMENT,
    id_diagnostico INT NOT NULL,
    nombre_parametro VARCHAR(80) NOT NULL,
    valor VARCHAR(150) NOT NULL,
    unidad_medida VARCHAR(30) NOT NULL,
    CONSTRAINT pk_parametro_diagnostico PRIMARY KEY (id_parametro),
    CONSTRAINT fk_parametro_diagnostico FOREIGN KEY (id_diagnostico) REFERENCES diagnostico (id_diagnostico)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_parametro_diagnostico_id ON parametro_diagnostico (id_diagnostico);

-- 10. REPUESTO
CREATE TABLE repuesto (
    id_repuesto INT NOT NULL AUTO_INCREMENT,
    codigo_repuesto VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    tipo_item VARCHAR(30) NOT NULL,
    unidad_medida VARCHAR(30) NOT NULL,
    descripcion TEXT NOT NULL,
    precio_unitario_actual DECIMAL(15, 2) NOT NULL,
    existencias_actuales DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion DATETIME NOT NULL,
    CONSTRAINT pk_repuesto PRIMARY KEY (id_repuesto),
    CONSTRAINT uk_repuesto_codigo UNIQUE (codigo_repuesto),
    CONSTRAINT chk_repuesto_precio_no_negativo CHECK (precio_unitario_actual >= 0),
    CONSTRAINT chk_repuesto_existencias_no_negativas CHECK (existencias_actuales >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_repuesto_filtro ON repuesto (activo, tipo_item, nombre);

-- 11. EVALUACION_TECNICA (Se crea antes de AUTORIZACION_NUEVA_COTIZACION; la FK a PRESUPUESTO se agrega tras crear PRESUPUESTO)
-- Nota: Para respetar el orden de claves foráneas entre PRESUPUESTO, EVALUACION_TECNICA y AUTORIZACION_NUEVA_COTIZACION,
-- creamos primero PRESUPUESTO, luego EVALUACION_TECNICA, luego AUTORIZACION_NUEVA_COTIZACION y añadimos la FK en PRESUPUESTO.

-- 12. PRESUPUESTO
CREATE TABLE presupuesto (
    id_presupuesto INT NOT NULL AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_tecnico_creador INT NOT NULL,
    id_autorizacion INT NULL,
    tipo_presupuesto VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    tarifa_hora_aprobada DECIMAL(15, 2) NULL,
    horas_estimadas DECIMAL(10, 2) NULL,
    valor_mano_obra_estimado DECIMAL(15, 2) NULL,
    subtotal_repuestos DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_presupuesto DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    fecha_emision DATETIME NOT NULL,
    CONSTRAINT pk_presupuesto PRIMARY KEY (id_presupuesto),
    CONSTRAINT uk_presupuesto_autorizacion UNIQUE (id_autorizacion),
    CONSTRAINT fk_presupuesto_orden FOREIGN KEY (id_orden) REFERENCES orden_servicio (id_orden),
    CONSTRAINT fk_presupuesto_tecnico FOREIGN KEY (id_tecnico_creador) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_presupuesto_orden_estado ON presupuesto (id_orden, estado);

-- 13. EVALUACION_TECNICA
CREATE TABLE evaluacion_tecnica (
    id_evaluacion INT NOT NULL AUTO_INCREMENT,
    id_presupuesto_rechazado INT NOT NULL,
    id_tecnico INT NOT NULL,
    elementos_indispensables BOOLEAN NOT NULL,
    justificacion TEXT NOT NULL,
    fecha_hora DATETIME NOT NULL,
    CONSTRAINT pk_evaluacion_tecnica PRIMARY KEY (id_evaluacion),
    CONSTRAINT uk_evaluacion_presupuesto_rechazado UNIQUE (id_presupuesto_rechazado),
    CONSTRAINT fk_evaluacion_presupuesto FOREIGN KEY (id_presupuesto_rechazado) REFERENCES presupuesto (id_presupuesto),
    CONSTRAINT fk_evaluacion_tecnico FOREIGN KEY (id_tecnico) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. AUTORIZACION_NUEVA_COTIZACION
CREATE TABLE autorizacion_nueva_cotizacion (
    id_autorizacion INT NOT NULL AUTO_INCREMENT,
    id_presupuesto_rechazado INT NOT NULL,
    id_evaluacion INT NOT NULL,
    id_administrador INT NOT NULL,
    fecha_hora_autorizacion DATETIME NOT NULL,
    CONSTRAINT pk_autorizacion_nueva_cotizacion PRIMARY KEY (id_autorizacion),
    CONSTRAINT uk_autorizacion_presupuesto_rechazado UNIQUE (id_presupuesto_rechazado),
    CONSTRAINT uk_autorizacion_evaluacion UNIQUE (id_evaluacion),
    CONSTRAINT fk_autorizacion_presupuesto_rechazado FOREIGN KEY (id_presupuesto_rechazado) REFERENCES presupuesto (id_presupuesto),
    CONSTRAINT fk_autorizacion_evaluacion FOREIGN KEY (id_evaluacion) REFERENCES evaluacion_tecnica (id_evaluacion),
    CONSTRAINT fk_autorizacion_administrador FOREIGN KEY (id_administrador) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vinculación FK de PRESUPUESTO.id_autorizacion hacia AUTORIZACION_NUEVA_COTIZACION
ALTER TABLE presupuesto
    ADD CONSTRAINT fk_presupuesto_autorizacion
        FOREIGN KEY (id_autorizacion) REFERENCES autorizacion_nueva_cotizacion (id_autorizacion);

-- 15. DETALLE_PRESUPUESTO
CREATE TABLE detalle_presupuesto (
    id_detalle INT NOT NULL AUTO_INCREMENT,
    id_presupuesto INT NOT NULL,
    id_repuesto INT NOT NULL,
    cantidad_autorizada DECIMAL(12, 2) NOT NULL,
    precio_unitario_conservado DECIMAL(15, 2) NOT NULL,
    cantidad_utilizada DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    justificacion TEXT NULL,
    CONSTRAINT pk_detalle_presupuesto PRIMARY KEY (id_detalle),
    CONSTRAINT fk_detalle_presupuesto_presupuesto FOREIGN KEY (id_presupuesto) REFERENCES presupuesto (id_presupuesto),
    CONSTRAINT fk_detalle_presupuesto_repuesto FOREIGN KEY (id_repuesto) REFERENCES repuesto (id_repuesto),
    CONSTRAINT chk_detalle_cantidades CHECK (
                                         cantidad_autorizada > 0
                                             AND cantidad_utilizada >= 0
                                             AND cantidad_utilizada <= cantidad_autorizada
                                         )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_detalle_presupuesto_presupuesto ON detalle_presupuesto (id_presupuesto);
CREATE INDEX idx_detalle_presupuesto_repuesto ON detalle_presupuesto (id_repuesto);

-- 16. RESERVA_INVENTARIO (Relación 1:0..1 con DETALLE_PRESUPUESTO -> id_detalle_presupuesto UNIQUE)
CREATE TABLE reserva_inventario (
    id_reserva INT NOT NULL AUTO_INCREMENT,
    id_detalle_presupuesto INT NOT NULL,
    cantidad_reservada DECIMAL(12, 2) NOT NULL,
    estado_reserva VARCHAR(30) NOT NULL,
    tipo_cierre VARCHAR(30) NULL,
    fecha_hora_reserva DATETIME NOT NULL,
    fecha_hora_cierre DATETIME NULL,
    CONSTRAINT pk_reserva_inventario PRIMARY KEY (id_reserva),
    CONSTRAINT uk_reserva_detalle_presupuesto UNIQUE (id_detalle_presupuesto),
    CONSTRAINT fk_reserva_detalle_presupuesto FOREIGN KEY (id_detalle_presupuesto) REFERENCES detalle_presupuesto (id_detalle),
    CONSTRAINT chk_reserva_cantidad_positiva CHECK (cantidad_reservada > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reserva_estado ON reserva_inventario (estado_reserva);

-- 17. MOVIMIENTO_INVENTARIO
CREATE TABLE movimiento_inventario (
    id_movimiento INT NOT NULL AUTO_INCREMENT,
    id_repuesto INT NOT NULL,
    id_usuario_registro INT NOT NULL,
    id_detalle_presupuesto INT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    origen_movimiento VARCHAR(30) NOT NULL,
    cantidad_movimiento DECIMAL(12, 2) NOT NULL,
    cantidad_contada DECIMAL(12, 2) NULL,
    existencias_anteriores DECIMAL(12, 2) NOT NULL,
    existencias_resultantes DECIMAL(12, 2) NOT NULL,
    motivo TEXT NOT NULL,
    fecha_hora_movimiento DATETIME NOT NULL,
    CONSTRAINT pk_movimiento_inventario PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_movimiento_repuesto FOREIGN KEY (id_repuesto) REFERENCES repuesto (id_repuesto),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (id_usuario_registro) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_movimiento_detalle_presupuesto FOREIGN KEY (id_detalle_presupuesto) REFERENCES detalle_presupuesto (id_detalle),
    CONSTRAINT chk_movimiento_saldos_no_negativos CHECK (existencias_anteriores >= 0 AND existencias_resultantes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_movimiento_repuesto_fecha ON movimiento_inventario (id_repuesto, fecha_hora_movimiento);
CREATE INDEX idx_movimiento_tipo_fecha ON movimiento_inventario (tipo_movimiento, fecha_hora_movimiento);

-- 18. DECISION_PRESUPUESTO (Relación 1:0..1 con PRESUPUESTO -> id_presupuesto UNIQUE)
CREATE TABLE decision_presupuesto (
    id_decision INT NOT NULL AUTO_INCREMENT,
    id_presupuesto INT NOT NULL,
    id_cliente_decisor INT NOT NULL,
    decision VARCHAR(30) NOT NULL,
    fecha_hora_decision DATETIME NOT NULL,
    CONSTRAINT pk_decision_presupuesto PRIMARY KEY (id_decision),
    CONSTRAINT uk_decision_presupuesto UNIQUE (id_presupuesto),
    CONSTRAINT fk_decision_presupuesto FOREIGN KEY (id_presupuesto) REFERENCES presupuesto (id_presupuesto),
    CONSTRAINT fk_decision_cliente FOREIGN KEY (id_cliente_decisor) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. REGISTRO_TIEMPO
CREATE TABLE registro_tiempo (
    id_registro_tiempo INT NOT NULL AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_tecnico INT NOT NULL,
    tipo_inicio VARCHAR(30) NOT NULL,
    fecha_hora_inicio DATETIME NOT NULL,
    tipo_fin VARCHAR(30) NULL,
    fecha_hora_fin DATETIME NULL,
    duracion_segundos BIGINT NULL,
    CONSTRAINT pk_registro_tiempo PRIMARY KEY (id_registro_tiempo),
    CONSTRAINT fk_registro_tiempo_orden FOREIGN KEY (id_orden) REFERENCES orden_servicio (id_orden),
    CONSTRAINT fk_registro_tiempo_tecnico FOREIGN KEY (id_tecnico) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_registro_tiempo_duracion CHECK (duracion_segundos IS NULL OR duracion_segundos >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_registro_tiempo_orden ON registro_tiempo (id_orden, fecha_hora_inicio);

-- 20. HISTORIAL_ESTADO_ORDEN
CREATE TABLE historial_estado_orden (
    id_historial_estado INT NOT NULL AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_usuario_responsable INT NOT NULL,
    id_decision_presupuesto INT NULL,
    estado_anterior VARCHAR(30) NULL,
    estado_nuevo VARCHAR(30) NOT NULL,
    justificacion TEXT NULL,
    fecha_hora_cambio DATETIME NOT NULL,
    CONSTRAINT pk_historial_estado_orden PRIMARY KEY (id_historial_estado),
    CONSTRAINT uk_historial_decision_presupuesto UNIQUE (id_decision_presupuesto),
    CONSTRAINT fk_historial_orden FOREIGN KEY (id_orden) REFERENCES orden_servicio (id_orden),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario_responsable) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_historial_decision_presupuesto FOREIGN KEY (id_decision_presupuesto) REFERENCES decision_presupuesto (id_decision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_historial_estado_orden_fecha ON historial_estado_orden (id_orden, fecha_hora_cambio);

-- 21. CIERRE_TECNICO (Relación 1:0..1 con ORDEN_SERVICIO -> id_orden UNIQUE)
CREATE TABLE cierre_tecnico (
    id_cierre INT NOT NULL AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_tecnico INT NOT NULL,
    pruebas_funcionamiento TEXT NOT NULL,
    recomendaciones TEXT NOT NULL,
    garantia_alcance TEXT NOT NULL,
    garantia_vigencia VARCHAR(100) NOT NULL,
    fecha_cierre DATETIME NOT NULL,
    CONSTRAINT pk_cierre_tecnico PRIMARY KEY (id_cierre),
    CONSTRAINT uk_cierre_tecnico_orden UNIQUE (id_orden),
    CONSTRAINT fk_cierre_tecnico_orden FOREIGN KEY (id_orden) REFERENCES orden_servicio (id_orden),
    CONSTRAINT fk_cierre_tecnico_usuario FOREIGN KEY (id_tecnico) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. CANCELACION_ORDEN (Relación 1:0..1 con ORDEN_SERVICIO -> id_orden UNIQUE)
CREATE TABLE cancelacion_orden (
    id_cancelacion INT NOT NULL AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_administrador INT NOT NULL,
    justificacion TEXT NOT NULL,
    fecha_hora_cancelacion DATETIME NOT NULL,
    CONSTRAINT pk_cancelacion_orden PRIMARY KEY (id_cancelacion),
    CONSTRAINT uk_cancelacion_orden_orden UNIQUE (id_orden),
    CONSTRAINT fk_cancelacion_orden_orden FOREIGN KEY (id_orden) REFERENCES orden_servicio (id_orden),
    CONSTRAINT fk_cancelacion_orden_admin FOREIGN KEY (id_administrador) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 23 (Entidad 22 del ERD). LIQUIDACION_ECONOMICA (Relación 1:1 con CANCELACION_ORDEN -> id_cancelacion UNIQUE)
CREATE TABLE liquidacion_economica (
    id_liquidacion INT NOT NULL AUTO_INCREMENT,
    id_cancelacion INT NOT NULL,
    segundos_efectivos BIGINT NOT NULL,
    tarifa_hora_aplicada DECIMAL(15, 2) NOT NULL,
    valor_mano_obra DECIMAL(15, 2) NOT NULL,
    valor_repuestos_insumos DECIMAL(15, 2) NOT NULL,
    total_calculado DECIMAL(15, 2) NOT NULL,
    fecha_generacion DATETIME NOT NULL,
    CONSTRAINT pk_liquidacion_economica PRIMARY KEY (id_liquidacion),
    CONSTRAINT uk_liquidacion_cancelacion UNIQUE (id_cancelacion),
    CONSTRAINT fk_liquidacion_cancelacion FOREIGN KEY (id_cancelacion) REFERENCES cancelacion_orden (id_cancelacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;