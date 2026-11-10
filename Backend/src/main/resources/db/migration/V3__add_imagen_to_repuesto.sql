-- =============================================================================
-- SERVITEC - MIGRACIÓN V3: ADICIÓN DE COLUMNA IMAGEN_URL EN TABLA REPUESTO
-- =============================================================================

ALTER TABLE repuesto
    ADD COLUMN imagen_url VARCHAR(500) NULL AFTER descripcion;