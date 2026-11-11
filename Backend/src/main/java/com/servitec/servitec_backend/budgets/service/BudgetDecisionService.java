package com.servitec.servitec_backend.budgets.service;

import com.servitec.servitec_backend.budgets.dto.PresupuestoResponse;
import com.servitec.servitec_backend.budgets.dto.RegistrarDecisionPresupuestoRequest;
import com.servitec.servitec_backend.budgets.entity.DecisionPresupuesto;
import com.servitec.servitec_backend.budgets.entity.DetallePresupuesto;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.DecisionPresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.DetallePresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.inventory.dto.SolicitudReservaItem;
import com.servitec.servitec_backend.inventory.service.InventoryReservationService;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderStateTransitionService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.TipoDecisionPresupuesto;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BudgetDecisionService {

    private final PresupuestoRepository presupuestoRepository;
    private final DetallePresupuestoRepository detallePresupuestoRepository;
    private final DecisionPresupuestoRepository decisionPresupuestoRepository;
    private final InventoryReservationService inventoryReservationService;
    private final OrderStateTransitionService orderStateTransitionService;
    private final BudgetCreationService budgetCreationService;

    public BudgetDecisionService(
            PresupuestoRepository presupuestoRepository,
            DetallePresupuestoRepository detallePresupuestoRepository,
            DecisionPresupuestoRepository decisionPresupuestoRepository,
            InventoryReservationService inventoryReservationService,
            OrderStateTransitionService orderStateTransitionService,
            BudgetCreationService budgetCreationService) {
        this.presupuestoRepository = presupuestoRepository;
        this.detallePresupuestoRepository = detallePresupuestoRepository;
        this.decisionPresupuestoRepository = decisionPresupuestoRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.orderStateTransitionService = orderStateTransitionService;
        this.budgetCreationService = budgetCreationService;
    }

    @Transactional
    public PresupuestoResponse decidirSobrePresupuesto(
            Integer idPresupuesto,
            RegistrarDecisionPresupuestoRequest request,
            Integer idClienteAutenticado) {

        Presupuesto presupuesto = presupuestoRepository.findByIdWithLock(idPresupuesto)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el presupuesto con ID: " + idPresupuesto));

        OrdenServicio orden = presupuesto.getOrdenServicio();

        // RN-05: Verificar que la orden pertenezca al Cliente autenticado
        if (!orden.getCliente().getIdUsuario().equals(idClienteAutenticado)) {
            throw new ForbiddenAccessException("No tienes permiso para aprobar o rechazar presupuestos de otros clientes.");
        }

        // REQ-13 y REQ-22: Impedir que una decisión ya registrada sea modificada mediante una segunda acción
        if (presupuesto.getEstado() != EstadoPresupuesto.PENDIENTE_APROBACION
                || decisionPresupuestoRepository.existsByPresupuesto_IdPresupuesto(idPresupuesto)) {
            throw new BusinessRuleException(
                    "DECISION_PRESUPUESTO_YA_REGISTRADA",
                    "Este presupuesto ya cuenta con una decisión registrada y no puede modificarse."
            );
        }

        if (presupuesto.getTipoPresupuesto() == TipoPresupuesto.ORIGINAL) {
            return procesarDecisionPresupuestoOriginal(presupuesto, orden, request.decision());
        } else {
            return procesarDecisionPresupuestoComplementario(presupuesto, orden, request.decision());
        }
    }

    private PresupuestoResponse procesarDecisionPresupuestoOriginal(
            Presupuesto presupuesto,
            OrdenServicio orden,
            TipoDecisionPresupuesto decision) {

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PRESUPUESTADO) {
            throw new InvalidStateTransitionException(
                    "Solo se puede decidir sobre el presupuesto original cuando la orden se encuentra en estado PRESUPUESTADO."
            );
        }

        List<DetallePresupuesto> detalles = detallePresupuestoRepository
                .findByIdPresupuestoWithRepuesto(presupuesto.getIdPresupuesto());

        LocalDateTime ahora = LocalDateTime.now();

        if (decision == TipoDecisionPresupuesto.APROBADO) {
            // REQ-13 y RN-14: Verificar disponibilidad total y crear reservas activas en la misma transacción
            List<SolicitudReservaItem> solicitudesReserva = detalles.stream()
                    .map(d -> new SolicitudReservaItem(
                            d.getIdDetalle(),
                            d.getRepuesto().getIdRepuesto(),
                            d.getCantidadAutorizada()
                    ))
                    .toList();

            inventoryReservationService.verificarYReservarDetalles(solicitudesReserva);

            presupuesto.setEstado(EstadoPresupuesto.APROBADO);
            Presupuesto presupuestoActualizado = presupuestoRepository.save(presupuesto);

            DecisionPresupuesto entidadDecision = new DecisionPresupuesto();
            entidadDecision.setPresupuesto(presupuestoActualizado);
            entidadDecision.setClienteDecisor(orden.getCliente());
            entidadDecision.setDecision(TipoDecisionPresupuesto.APROBADO);
            entidadDecision.setFechaHoraDecision(ahora);
            decisionPresupuestoRepository.save(entidadDecision);

            // La orden conserva su estado operativo PRESUPUESTADO hasta que el técnico inicie explícitamente la reparación
            return budgetCreationService.mapearPresupuestoCompleto(presupuestoActualizado);

        } else {
            // SRS 7.4, RN-02 y CV-06: Rechazo del presupuesto original y cancelación automática de la orden
            presupuesto.setEstado(EstadoPresupuesto.RECHAZADO);
            Presupuesto presupuestoActualizado = presupuestoRepository.save(presupuesto);

            DecisionPresupuesto entidadDecision = new DecisionPresupuesto();
            entidadDecision.setPresupuesto(presupuestoActualizado);
            entidadDecision.setClienteDecisor(orden.getCliente());
            entidadDecision.setDecision(TipoDecisionPresupuesto.RECHAZADO);
            entidadDecision.setFechaHoraDecision(ahora);
            DecisionPresupuesto decisionGuardada = decisionPresupuestoRepository.save(entidadDecision);

            OrdenServicio ordenCancelada = orderStateTransitionService.cambiarEstado(
                    orden.getIdOrden(),
                    EstadoOperativoOrden.PRESUPUESTADO,
                    EstadoOperativoOrden.CANCELADO,
                    orden.getCliente().getIdUsuario(),
                    "Cancelación automática por rechazo del presupuesto original.",
                    decisionGuardada.getIdDecision()
            );
            presupuestoActualizado.setOrdenServicio(ordenCancelada);

            return budgetCreationService.mapearPresupuestoCompleto(presupuestoActualizado);
        }
    }

    private PresupuestoResponse procesarDecisionPresupuestoComplementario(
            Presupuesto presupuesto,
            OrdenServicio orden,
            TipoDecisionPresupuesto decision) {

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "Solo se puede decidir sobre un presupuesto complementario cuando la orden se encuentra en estado PAUSADA."
            );
        }

        List<DetallePresupuesto> detalles = detallePresupuestoRepository
                .findByIdPresupuestoWithRepuesto(presupuesto.getIdPresupuesto());

        LocalDateTime ahora = LocalDateTime.now();

        if (decision == TipoDecisionPresupuesto.APROBADO) {
            // REQ-22 y RN-14: Verificar disponibilidad y reservar exclusivamente las cantidades de este presupuesto complementario
            List<SolicitudReservaItem> solicitudesReserva = detalles.stream()
                    .map(d -> new SolicitudReservaItem(
                            d.getIdDetalle(),
                            d.getRepuesto().getIdRepuesto(),
                            d.getCantidadAutorizada()
                    ))
                    .toList();

            inventoryReservationService.verificarYReservarDetalles(solicitudesReserva);
            presupuesto.setEstado(EstadoPresupuesto.APROBADO);
        } else {
            presupuesto.setEstado(EstadoPresupuesto.RECHAZADO);
        }

        Presupuesto presupuestoActualizado = presupuestoRepository.save(presupuesto);

        DecisionPresupuesto entidadDecision = new DecisionPresupuesto();
        entidadDecision.setPresupuesto(presupuestoActualizado);
        entidadDecision.setClienteDecisor(orden.getCliente());
        entidadDecision.setDecision(decision);
        entidadDecision.setFechaHoraDecision(ahora);
        decisionPresupuestoRepository.save(entidadDecision);

        // RN-12 y REQ-22: La orden permanece PAUSADA hasta acción explícita posterior
        return budgetCreationService.mapearPresupuestoCompleto(presupuestoActualizado);
    }
}