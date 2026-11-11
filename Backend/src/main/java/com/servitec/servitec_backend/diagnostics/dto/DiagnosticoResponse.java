package com.servitec.servitec_backend.diagnostics.dto;

import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosticoResponse(
        Integer idDiagnostico,
        Integer idOrden,
        String codigoOrden,
        String nombreCategoria,
        EstadoOperativoOrden estadoOrdenActualizado,
        Integer idTecnico,
        String nombreTecnico,
        String observaciones,
        LocalDateTime fechaHoraRegistro,
        List<ParametroDiagnosticoResponse> parametros
) {
}