package com.servitec.servitec_backend.servicesummary.controller;

import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.servicesummary.dto.ResumenServicioResponse;
import com.servitec.servitec_backend.servicesummary.service.ServiceSummaryService;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{idOrden}/summary")
public class ServiceSummaryController {

    private final ServiceSummaryService serviceSummaryService;
    private final CurrentUserProvider currentUserProvider;

    public ServiceSummaryController(
            ServiceSummaryService serviceSummaryService,
            CurrentUserProvider currentUserProvider) {
        this.serviceSummaryService = serviceSummaryService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ResumenServicioResponse>> consultarResumenServicio(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();

        ResumenServicioResponse resumen =
                serviceSummaryService.consultarResumenServicioCliente(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(resumen));
    }
}