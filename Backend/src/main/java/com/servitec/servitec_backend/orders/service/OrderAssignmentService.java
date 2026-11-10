package com.servitec.servitec_backend.orders.service;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.orders.dto.AsignarTecnicoRequest;
import com.servitec.servitec_backend.orders.dto.OrdenServicioResponse;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.repository.OrdenServicioRepository;
import com.servitec.servitec_backend.shared.enums.EstadoCuenta;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderAssignmentService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrderService orderService;

    public OrderAssignmentService(
            OrdenServicioRepository ordenServicioRepository,
            UsuarioRepository usuarioRepository,
            OrderService orderService) {
        this.ordenServicioRepository = ordenServicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.orderService = orderService;
    }

    @Transactional
    public OrdenServicioResponse asignarTecnicoYFecha(Integer idOrden, AsignarTecnicoRequest request) {
        OrdenServicio orden = ordenServicioRepository.findByIdWithLock(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de servicio con ID: " + idOrden));

        // REQ-06 y CV-03: Solo se asigna a una orden en estado SOLICITADO
        if (orden.getEstadoOperativo() != EstadoOperativoOrden.SOLICITADO) {
            throw new InvalidStateTransitionException(
                    "Solo es posible asignar un técnico a órdenes de servicio que se encuentren en estado SOLICITADO."
            );
        }

        Usuario tecnico = usuarioRepository.findById(request.idTecnicoAsignado())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el técnico especialista con ID: " + request.idTecnicoAsignado()
                ));

        String rolTecnico = String.valueOf(tecnico.getRol().getNombre());
        if (!RolNombre.TECNICO_ESPECIALISTA.name().equals(rolTecnico)) {
            throw new BusinessRuleException(
                    "ROL_TECNICO_INVALIDO",
                    "El usuario seleccionado no posee el rol de Técnico especialista."
            );
        }

        String estadoCuentaTecnico = String.valueOf(tecnico.getEstadoCuenta());
        if (!EstadoCuenta.ACTIVA.name().equals(estadoCuentaTecnico)) {
            throw new BusinessRuleException(
                    "TECNICO_NO_DISPONIBLE",
                    "El técnico especialista seleccionado no tiene una cuenta activa disponible."
            );
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (request.fechaEstimada().isBefore(ahora.toLocalDate().atStartOfDay())) {
            throw new BusinessRuleException(
                    "FECHA_ESTIMADA_INVALIDA",
                    "La fecha estimada de atención no puede ser anterior a la fecha actual."
            );
        }

        // CV-03 Observaciones: La asignación no modifica automáticamente el estado operativo de la orden (sigue SOLICITADO)
        orden.setTecnicoAsignado(tecnico);
        orden.setFechaEstimada(request.fechaEstimada());
        orden.setFechaAsignacion(ahora);

        OrdenServicio actualizada = ordenServicioRepository.save(orden);
        return orderService.mapearAResponse(actualizada);
    }
}
