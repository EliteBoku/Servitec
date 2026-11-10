package com.servitec.servitec_backend.orders.dto;

import java.time.LocalDateTime;

public record EstadoConsentimientoResponse(
        boolean requiereConsentimientoPrimeraSolicitud,
        boolean consentimientoOtorgado,
        LocalDateTime fechaHoraConsentimiento,
        String versionPolitica
) {
}