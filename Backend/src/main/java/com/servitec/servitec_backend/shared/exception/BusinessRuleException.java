package com.servitec.servitec_backend.shared.exception;
public class BusinessRuleException extends RuntimeException {

    private final String codigoError;

    public BusinessRuleException(String mensaje) {
        super(mensaje);
        this.codigoError = "REGLA_NEGOCIO_VIOLADA";
    }

    public BusinessRuleException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    public String getCodigoError() {
        return codigoError;
    }
}