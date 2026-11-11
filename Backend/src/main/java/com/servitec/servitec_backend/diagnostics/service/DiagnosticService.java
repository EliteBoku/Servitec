package com.servitec.servitec_backend.diagnostics.service;

import com.servitec.servitec_backend.diagnostics.dto.DiagnosticoResponse;
import com.servitec.servitec_backend.diagnostics.dto.ParametroDiagnosticoResponse;
import com.servitec.servitec_backend.diagnostics.dto.PlantillaParametroCategoriaResponse;
import com.servitec.servitec_backend.diagnostics.dto.RegistrarDiagnosticoRequest;
import com.servitec.servitec_backend.diagnostics.entity.Diagnostico;
import com.servitec.servitec_backend.diagnostics.entity.ParametroDiagnostico;
import com.servitec.servitec_backend.diagnostics.repository.DiagnosticoRepository;
import com.servitec.servitec_backend.diagnostics.repository.ParametroDiagnosticoRepository;
import com.servitec.servitec_backend.diagnostics.validation.DiagnosticCategoryValidator;
import com.servitec.servitec_backend.diagnostics.validation.DiagnosticCategoryValidator.ParametroNormalizado;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.orders.service.OrderStateTransitionService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiagnosticService {

    private final DiagnosticoRepository diagnosticoRepository;
    private final ParametroDiagnosticoRepository parametroDiagnosticoRepository;
    private final DiagnosticCategoryValidator categoryValidator;
    private final OrderService orderService;
    private final OrderStateTransitionService orderStateTransitionService;

    public DiagnosticService(
            DiagnosticoRepository diagnosticoRepository,
            ParametroDiagnosticoRepository parametroDiagnosticoRepository,
            DiagnosticCategoryValidator categoryValidator,
            OrderService orderService,
            OrderStateTransitionService orderStateTransitionService) {
        this.diagnosticoRepository = diagnosticoRepository;
        this.parametroDiagnosticoRepository = parametroDiagnosticoRepository;
        this.categoryValidator = categoryValidator;
        this.orderService = orderService;
        this.orderStateTransitionService = orderStateTransitionService;
    }

    @Transactional(readOnly = true)
    public List<PlantillaParametroCategoriaResponse> obtenerPlantillaParaOrden(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);
        return categoryValidator.obtenerPlantillaPorCategoria(orden.getCategoria().getNombre());
    }

    @Transactional
    public DiagnosticoResponse registrarDiagnostico(
            Integer idOrden,
            RegistrarDiagnosticoRequest request,
            Integer idTecnicoAutenticado) {

        // Validar que el técnico autenticado sea el asignado a la orden
        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        // Restricción 3 del proyecto: Cada orden de servicio puede tener un único diagnóstico
        if (diagnosticoRepository.existsByOrdenServicio_IdOrden(idOrden)) {
            throw new BusinessRuleException(
                    "DIAGNOSTICO_YA_REGISTRADO",
                    "La orden de servicio ya cuenta con un diagnóstico técnico registrado."
            );
        }

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.SOLICITADO) {
            throw new InvalidStateTransitionException(
                    "Solo se puede registrar el diagnóstico inicial cuando la orden se encuentra en estado SOLICITADO."
            );
        }

        // REQ-07: Validar parámetros obligatorios según la categoría del equipo
        String nombreCategoria = orden.getCategoria().getNombre();
        List<ParametroNormalizado> parametrosValidados = categoryValidator.validarYNormalizarParametros(
                nombreCategoria,
                request.parametros()
        );

        // REQ-08 y REQ-09: Guardar observaciones, fecha/hora exacta y técnico responsable
        LocalDateTime ahora = LocalDateTime.now();
        Diagnostico diagnostico = new Diagnostico();
        diagnostico.setOrdenServicio(orden);
        diagnostico.setTecnico(orden.getTecnicoAsignado());
        diagnostico.setObservaciones(request.observaciones().trim());
        diagnostico.setFechaHoraRegistro(ahora);

        Diagnostico diagnosticoGuardado = diagnosticoRepository.save(diagnostico);

        List<ParametroDiagnosticoResponse> parametrosResponse = new ArrayList<>();
        for (ParametroNormalizado param : parametrosValidados) {
            ParametroDiagnostico entidadParam = new ParametroDiagnostico();
            entidadParam.setDiagnostico(diagnosticoGuardado);
            entidadParam.setNombreParametro(param.nombreParametro());
            entidadParam.setValor(param.valor());
            entidadParam.setUnidadMedida(param.unidadMedida());

            ParametroDiagnostico guardado = parametroDiagnosticoRepository.save(entidadParam);
            parametrosResponse.add(new ParametroDiagnosticoResponse(
                    guardado.getIdParametro(),
                    guardado.getNombreParametro(),
                    guardado.getValor(),
                    guardado.getUnidadMedida()
            ));
        }

        // Cambiar automáticamente el estado de la orden a EN_DIAGNOSTICO y registrar en HISTORIAL_ESTADO_ORDEN
        OrdenServicio ordenActualizada = orderStateTransitionService.cambiarEstado(
                idOrden,
                EstadoOperativoOrden.SOLICITADO,
                EstadoOperativoOrden.EN_DIAGNOSTICO,
                idTecnicoAutenticado,
                "Diagnóstico técnico registrado en sitio.",
                null
        );

        return new DiagnosticoResponse(
                diagnosticoGuardado.getIdDiagnostico(),
                ordenActualizada.getIdOrden(),
                ordenActualizada.getCodigoOrden(),
                nombreCategoria,
                ordenActualizada.getEstadoOperativo(),
                diagnosticoGuardado.getTecnico().getIdUsuario(),
                diagnosticoGuardado.getTecnico().getNombreCompleto(),
                diagnosticoGuardado.getObservaciones(),
                diagnosticoGuardado.getFechaHoraRegistro(),
                parametrosResponse
        );
    }

    @Transactional(readOnly = true)
    public DiagnosticoResponse consultarDiagnosticoPorOrden(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);

        Diagnostico diagnostico = diagnosticoRepository.findByIdOrdenWithDetails(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La orden de servicio aún no cuenta con un diagnóstico técnico registrado."
                ));

        List<ParametroDiagnosticoResponse> parametros = parametroDiagnosticoRepository
                .findByDiagnostico_IdDiagnosticoOrderByIdParametroAsc(diagnostico.getIdDiagnostico())
                .stream()
                .map(p -> new ParametroDiagnosticoResponse(
                        p.getIdParametro(),
                        p.getNombreParametro(),
                        p.getValor(),
                        p.getUnidadMedida()
                ))
                .toList();

        OrdenServicio orden = diagnostico.getOrdenServicio();
        return new DiagnosticoResponse(
                diagnostico.getIdDiagnostico(),
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                orden.getCategoria().getNombre(),
                orden.getEstadoOperativo(),
                diagnostico.getTecnico().getIdUsuario(),
                diagnostico.getTecnico().getNombreCompleto(),
                diagnostico.getObservaciones(),
                diagnostico.getFechaHoraRegistro(),
                parametros
        );
    }
}