package com.servitec.servitec_backend.orders.dto;

import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;

import java.time.LocalDateTime;

public record OrdenServicioResponse(
        Integer idOrden,
        String codigoOrden,
        Integer idCliente,
        String nombreCliente,
        String correoCliente,
        Integer idTecnicoAsignado,
        String nombreTecnicoAsignado,
        Integer idCategoria,
        String nombreCategoria,
        String descripcionProblema,
        String direccionServicio,
        EstadoOperativoOrden estadoOperativo,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaEstimada,
        LocalDateTime fechaAsignacion
) {
}