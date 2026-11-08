package com.servitec.servitec_backend.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean exito,
        String mensaje,
        T datos,
        LocalDateTime fechaHora
) {
    public static <T> ApiResponse<T> ok(String mensaje, T datos) {
        return new ApiResponse<>(true, mensaje, datos, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T datos) {
        return new ApiResponse<>(true, "Operación realizada correctamente", datos, LocalDateTime.now());
    }
}