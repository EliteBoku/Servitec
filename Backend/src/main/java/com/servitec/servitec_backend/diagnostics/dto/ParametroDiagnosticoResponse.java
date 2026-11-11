package com.servitec.servitec_backend.diagnostics.dto;

public record ParametroDiagnosticoResponse(
        Integer idParametro,
        String nombreParametro,
        String valor,
        String unidadMedida
) {
}