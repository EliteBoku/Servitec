package com.servitec.servitec_backend.shared.exception;

import com.servitec.servitec_backend.shared.api.ApiError.ItemSinDisponibilidad;
import java.util.Collections;
import java.util.List;

public class InsufficientStockException extends BusinessRuleException {
    private final List<ItemSinDisponibilidad> elementosSinDisponibilidad;

    public InsufficientStockException(String mensaje) {
        super("STOCK_INSUFICIENTE", mensaje);
        this.elementosSinDisponibilidad = Collections.emptyList();
    }

    public InsufficientStockException(String mensaje, List<ItemSinDisponibilidad> elementosSinDisponibilidad) {
        super("STOCK_INSUFICIENTE", mensaje);
        this.elementosSinDisponibilidad = elementosSinDisponibilidad;
    }

    public List<ItemSinDisponibilidad> getElementosSinDisponibilidad() {
        return elementosSinDisponibilidad;
    }
}