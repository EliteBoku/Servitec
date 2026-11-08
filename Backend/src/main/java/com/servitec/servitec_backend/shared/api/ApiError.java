package com.servitec.servitec_backend.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        boolean exito,
        int codigoHttp,
        String codigoError,
        String mensaje,
        String ruta,
        LocalDateTime fechaHora,
        Map<String, String> erroresCampos,
        List<ItemSinDisponibilidad> elementosSinDisponibilidad
) {
    public record ItemSinDisponibilidad(
            Integer idRepuesto,
            String codigoRepuesto,
            String nombre,
            java.math.BigDecimal cantidadSolicitada,
            java.math.BigDecimal cantidadDisponible
    ) {}
}
