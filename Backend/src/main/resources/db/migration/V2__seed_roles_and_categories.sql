-- 1. Roles oficiales del sistema (Sección 5 del SRS)
INSERT INTO rol (nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Propietario y encargado de la gestión general de usuarios, catálogo, inventario y supervisión de órdenes.'),
    ('TECNICO_ESPECIALISTA', 'Personal operativo de campo encargado de diagnósticos, presupuestos, reparaciones y cierres técnicos.'),
    ('CLIENTE', 'Usuario final que solicita servicios técnicos, aprueba o rechaza presupuestos y consulta resúmenes de servicio.');

-- 2. Categorías oficiales de equipos (REQ-04 y CV-02)
INSERT INTO categoria_equipo (nombre, activa) VALUES
    ('Refrigeración', TRUE),
    ('Climatización', TRUE),
    ('Línea blanca', TRUE),
    ('Electricidad', TRUE);