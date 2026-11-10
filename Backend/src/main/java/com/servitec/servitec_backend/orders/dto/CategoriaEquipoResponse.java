package com.servitec.servitec_backend.orders.dto;

public record CategoriaEquipoResponse(
        Integer idCategoria,
        String nombre,
        Boolean activa
) {
}