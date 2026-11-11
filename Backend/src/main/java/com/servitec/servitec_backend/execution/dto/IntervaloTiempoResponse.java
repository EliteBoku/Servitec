package com.servitec.servitec_backend.execution.dto;

import com.servitec.servitec_backend.shared.enums.TipoFinTiempo;
import com.servitec.servitec_backend.shared.enums.TipoInicioTiempo;

import java.time.LocalDateTime;

public record IntervaloTiempoResponse(
        Integer idRegistroTiempo,
        Integer idTecnico,
        String nombreTecnico,
        TipoInicioTiempo tipoInicio,
        LocalDateTime fechaHoraInicio,
        TipoFinTiempo tipoFin,
        LocalDateTime fechaHoraFin,
        Long duracionSegundos,
        boolean intervaloActivo
) {
}