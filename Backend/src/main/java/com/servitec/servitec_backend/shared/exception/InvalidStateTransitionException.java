package com.servitec.servitec_backend.shared.exception;

public class InvalidStateTransitionException extends BusinessRuleException {
    public InvalidStateTransitionException(String mensaje) {
        super("TRANSICION_ESTADO_INVALIDA", mensaje);
    }
}
