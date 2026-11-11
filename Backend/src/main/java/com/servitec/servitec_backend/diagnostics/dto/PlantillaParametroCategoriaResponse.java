package com.servitec.servitec_backend.diagnostics.dto;

public record PlantillaParametroCategoriaResponse(
        String codigoParametro,
        String etiqueta,
        String unidadMedidaPorDefecto,
        boolean obligatorio,
        boolean numerico
) {
}