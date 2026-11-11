package com.servitec.servitec_backend.budgets.service;

import com.servitec.servitec_backend.budgets.dto.AutorizacionNuevaCotizacionResponse;
import com.servitec.servitec_backend.budgets.entity.AutorizacionNuevaCotizacion;
import com.servitec.servitec_backend.budgets.entity.EvaluacionTecnica;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.AutorizacionNuevaCotizacionRepository;
import com.servitec.servitec_backend.budgets.repository.EvaluacionTecnicaRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequoteAuthorizationService {

    private final AutorizacionNuevaCotizacionRepository autorizacionRepository;
    private final EvaluacionTecnicaRepository evaluacionTecnicaRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrderService orderService;

    public RequoteAuthorizationService(
            AutorizacionNuevaCotizacionRepository autorizacionRepository,
            EvaluacionTecnicaRepository evaluacionTecnicaRepository,
            PresupuestoRepository presupuestoRepository,
            UsuarioRepository usuarioRepository,
            OrderService orderService) {
        this.autorizacionRepository = autorizacionRepository;
        this.evaluacionTecnicaRepository = evaluacionTecnicaRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.usuarioRepository = usuarioRepository;
        this.orderService = orderService;
    }

    @Transactional
    public AutorizacionNuevaCotizacionResponse autorizarNuevaCotizacion(
            Integer idPresupuestoRechazado,
            Integer idAdminAutenticado) {

        Presupuesto presupuestoRechazado = presupuestoRepository.findByIdWithLock(idPresupuestoRechazado)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el presupuesto con ID: " + idPresupuestoRechazado));

        OrdenServicio orden = presupuestoRechazado.getOrdenServicio();

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "Solo se puede autorizar una nueva cotización cuando la orden de servicio se encuentra en estado PAUSADA."
            );
        }

        if (presupuestoRechazado.getTipoPresupuesto() != TipoPresupuesto.COMPLEMENTARIO
                || presupuestoRechazado.getEstado() != EstadoPresupuesto.RECHAZADO) {
            throw new BusinessRuleException(
                    "PRESUPUESTO_NO_RECHAZADO",
                    "Solo se puede autorizar una nueva cotización a partir de un presupuesto complementario previamente rechazado."
            );
        }

        EvaluacionTecnica evaluacion = evaluacionTecnicaRepository
                .findByIdPresupuestoRechazadoWithDetails(idPresupuestoRechazado)
                .orElseThrow(() -> new BusinessRuleException(
                        "EVALUACION_TECNICA_FALTANTE",
                        "No existe una evaluación técnica registrada por el técnico especialista que sustente la necesidad de una nueva propuesta."
                ));

        if (!Boolean.TRUE.equals(evaluacion.getElementosIndispensables())) {
            throw new BusinessRuleException(
                    "ELEMENTOS_NO_INDISPENSABLES",
                    "La evaluación técnica determinó que los elementos rechazados no son indispensables; el técnico puede reanudar la reparación con el alcance original."
            );
        }

        if (autorizacionRepository.existsByPresupuestoRechazado_IdPresupuesto(idPresupuestoRechazado)) {
            throw new BusinessRuleException(
                    "AUTORIZACION_YA_EXISTE",
                    "Ya fue registrada una autorización administrativa para este presupuesto complementario rechazado."
            );
        }

        Usuario administrador = usuarioRepository.findById(idAdminAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario administrador autenticado."));

        AutorizacionNuevaCotizacion autorizacion = new AutorizacionNuevaCotizacion();
        autorizacion.setPresupuestoRechazado(presupuestoRechazado);
        autorizacion.setEvaluacionTecnica(evaluacion);
        autorizacion.setAdministrador(administrador);
        autorizacion.setFechaHoraAutorizacion(LocalDateTime.now());

        AutorizacionNuevaCotizacion guardada = autorizacionRepository.save(autorizacion);
        return mapearAResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<AutorizacionNuevaCotizacionResponse> listarAutorizacionesPorOrden(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);

        return autorizacionRepository.findAllByIdOrden(idOrden)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    private AutorizacionNuevaCotizacionResponse mapearAResponse(AutorizacionNuevaCotizacion a) {
        OrdenServicio orden = a.getPresupuestoRechazado().getOrdenServicio();
        boolean yaUtilizada = presupuestoRepository.existsByAutorizacionNuevaCotizacion_IdAutorizacion(a.getIdAutorizacion());
        return new AutorizacionNuevaCotizacionResponse(
                a.getIdAutorizacion(),
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                a.getPresupuestoRechazado().getIdPresupuesto(),
                a.getEvaluacionTecnica().getIdEvaluacion(),
                a.getEvaluacionTecnica().getJustificacion(),
                a.getAdministrador().getIdUsuario(),
                a.getAdministrador().getNombreCompleto(),
                a.getFechaHoraAutorizacion(),
                yaUtilizada
        );
    }
}