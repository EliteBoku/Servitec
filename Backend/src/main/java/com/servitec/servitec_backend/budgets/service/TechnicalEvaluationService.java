package com.servitec.servitec_backend.budgets.service;

import com.servitec.servitec_backend.budgets.dto.EvaluacionTecnicaResponse;
import com.servitec.servitec_backend.budgets.dto.RegistrarEvaluacionTecnicaRequest;
import com.servitec.servitec_backend.budgets.entity.EvaluacionTecnica;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.EvaluacionTecnicaRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TechnicalEvaluationService {

    private final EvaluacionTecnicaRepository evaluacionTecnicaRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final OrderService orderService;

    public TechnicalEvaluationService(
            EvaluacionTecnicaRepository evaluacionTecnicaRepository,
            PresupuestoRepository presupuestoRepository,
            OrderService orderService) {
        this.evaluacionTecnicaRepository = evaluacionTecnicaRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.orderService = orderService;
    }

    @Transactional
    public EvaluacionTecnicaResponse registrarEvaluacionTecnica(
            Integer idPresupuestoRechazado,
            RegistrarEvaluacionTecnicaRequest request,
            Integer idTecnicoAutenticado) {

        Presupuesto presupuesto = presupuestoRepository.findByIdWithLock(idPresupuestoRechazado)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el presupuesto con ID: " + idPresupuestoRechazado));

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                presupuesto.getOrdenServicio().getIdOrden(),
                idTecnicoAutenticado,
                RolNombre.TECNICO_ESPECIALISTA.name()
        );

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "La evaluación técnica solo puede registrarse cuando la orden se encuentra en estado PAUSADA."
            );
        }

        if (presupuesto.getTipoPresupuesto() != TipoPresupuesto.COMPLEMENTARIO
                || presupuesto.getEstado() != EstadoPresupuesto.RECHAZADO) {
            throw new BusinessRuleException(
                    "PRESUPUESTO_NO_EVALUABLE",
                    "Solo se puede realizar la evaluación técnica sobre un presupuesto complementario que haya sido rechazado por el Cliente."
            );
        }

        if (evaluacionTecnicaRepository.existsByPresupuestoRechazado_IdPresupuesto(idPresupuestoRechazado)) {
            throw new BusinessRuleException(
                    "EVALUACION_YA_REGISTRADA",
                    "Este presupuesto complementario rechazado ya cuenta con una evaluación técnica registrada."
            );
        }

        EvaluacionTecnica evaluacion = new EvaluacionTecnica();
        evaluacion.setPresupuestoRechazado(presupuesto);
        evaluacion.setTecnico(orden.getTecnicoAsignado());
        evaluacion.setElementosIndispensables(request.elementosIndispensables());
        evaluacion.setJustificacion(request.justificacion().trim());
        evaluacion.setFechaHora(LocalDateTime.now());

        EvaluacionTecnica guardada = evaluacionTecnicaRepository.save(evaluacion);
        return mapearAResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<EvaluacionTecnicaResponse> listarEvaluacionesPorOrden(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);

        return evaluacionTecnicaRepository.findAllByIdOrdenOrderByFechaHoraDesc(idOrden)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    private EvaluacionTecnicaResponse mapearAResponse(EvaluacionTecnica e) {
        OrdenServicio orden = e.getPresupuestoRechazado().getOrdenServicio();
        boolean permiteReanudar = !Boolean.TRUE.equals(e.getElementosIndispensables());
        return new EvaluacionTecnicaResponse(
                e.getIdEvaluacion(),
                e.getPresupuestoRechazado().getIdPresupuesto(),
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                e.getElementosIndispensables(),
                e.getJustificacion(),
                e.getTecnico().getIdUsuario(),
                e.getTecnico().getNombreCompleto(),
                e.getFechaHora(),
                permiteReanudar
        );
    }
}