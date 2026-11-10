package com.servitec.servitec_backend.orders.service;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.orders.entity.HistorialEstadoOrden;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.repository.HistorialEstadoOrdenRepository;
import com.servitec.servitec_backend.orders.repository.OrdenServicioRepository;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderStateTransitionService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final HistorialEstadoOrdenRepository historialEstadoOrdenRepository;
    private final UsuarioRepository usuarioRepository;

    public OrderStateTransitionService(
            OrdenServicioRepository ordenServicioRepository,
            HistorialEstadoOrdenRepository historialEstadoOrdenRepository,
            UsuarioRepository usuarioRepository) {
        this.ordenServicioRepository = ordenServicioRepository;
        this.historialEstadoOrdenRepository = historialEstadoOrdenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra en HISTORIAL_ESTADO_ORDEN la transición inicial (NULL -> SOLICITADO) al crearse la orden.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarCreacionInicial(OrdenServicio orden, Usuario cliente, LocalDateTime fechaHora) {
        HistorialEstadoOrden historial = new HistorialEstadoOrden();
        historial.setOrdenServicio(orden);
        historial.setUsuarioResponsable(cliente);
        historial.setIdDecisionPresupuesto(null);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(EstadoOperativoOrden.SOLICITADO);
        historial.setJustificacion("Solicitud de servicio registrada por el cliente.");
        historial.setFechaHoraCambio(fechaHora);
        historialEstadoOrdenRepository.save(historial);
    }

    /**
     * Ejecuta una transición de estado validando el estado origen esperado, la inmutabilidad de órdenes
     * finalizadas/canceladas (RN-04) y la obligatoriedad de justificación en pausas (REQ-15 / RN-12).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OrdenServicio cambiarEstado(
            Integer idOrden,
            EstadoOperativoOrden estadoEsperado,
            EstadoOperativoOrden estadoNuevo,
            Integer idUsuarioResponsable,
            String justificacion,
            Integer idDecisionPresupuesto) {

        OrdenServicio orden = ordenServicioRepository.findByIdWithLock(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de servicio con ID: " + idOrden));

        EstadoOperativoOrden estadoActual = orden.getEstadoOperativo();

        // RN-04 y REQ-25: Las órdenes finalizadas o canceladas son inmutables
        if (estadoActual == EstadoOperativoOrden.FINALIZADO || estadoActual == EstadoOperativoOrden.CANCELADO) {
            throw new InvalidStateTransitionException(
                    "La orden de servicio se encuentra en estado " + estadoActual + " (solo lectura) y no permite modificaciones."
            );
        }

        if (estadoEsperado != null && estadoActual != estadoEsperado) {
            throw new InvalidStateTransitionException(
                    String.format(
                            "Transición inválida: la orden se encuentra en estado %s y se requería el estado %s para pasar a %s.",
                            estadoActual, estadoEsperado, estadoNuevo
                    )
            );
        }

        // REQ-15, RN-12 y CV-07: La justificación es obligatoria cuando el estado nuevo es PAUSADA
        String justificacionLimpia = (justificacion != null && !justificacion.isBlank()) ? justificacion.trim() : null;
        if (estadoNuevo == EstadoOperativoOrden.PAUSADA && justificacionLimpia == null) {
            throw new BusinessRuleException(
                    "JUSTIFICACION_PAUSA_OBLIGATORIA",
                    "Debes ingresar una justificación obligatoria para pausar la reparación."
            );
        }

        Usuario responsable = usuarioRepository.findById(idUsuarioResponsable)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario responsable del cambio de estado."));

        LocalDateTime ahora = LocalDateTime.now();
        orden.setEstadoOperativo(estadoNuevo);
        OrdenServicio actualizada = ordenServicioRepository.save(orden);

        HistorialEstadoOrden historial = new HistorialEstadoOrden();
        historial.setOrdenServicio(actualizada);
        historial.setUsuarioResponsable(responsable);
        historial.setIdDecisionPresupuesto(idDecisionPresupuesto);
        historial.setEstadoAnterior(estadoActual);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setJustificacion(justificacionLimpia);
        historial.setFechaHoraCambio(ahora);
        historialEstadoOrdenRepository.save(historial);

        return actualizada;
    }
}
