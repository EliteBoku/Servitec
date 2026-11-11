package com.servitec.servitec_backend.closures.service;

import com.servitec.servitec_backend.budgets.entity.DetallePresupuesto;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.DetallePresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.closures.dto.CancelarOrdenRequest;
import com.servitec.servitec_backend.closures.dto.ItemUtilizadoLiquidacionResponse;
import com.servitec.servitec_backend.closures.dto.LiquidacionCancelacionResponse;
import com.servitec.servitec_backend.execution.service.ExecutionTimeService;
import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.inventory.dto.ConsumoReservaItem;
import com.servitec.servitec_backend.inventory.service.InventoryReservationService;
import com.servitec.servitec_backend.notifications.event.OrderCancelledEvent;
import com.servitec.servitec_backend.orders.entity.CancelacionOrden;
import com.servitec.servitec_backend.orders.entity.LiquidacionEconomica;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.repository.CancelacionOrdenRepository;
import com.servitec.servitec_backend.orders.repository.LiquidacionEconomicaRepository;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.orders.service.OrderStateTransitionService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.TipoCierreReserva;
import com.servitec.servitec_backend.shared.enums.TipoFinTiempo;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import com.servitec.servitec_backend.shared.util.MoneyCalculator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderCancellationService {

    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);
    private static final String NOTA_INFORMATIVA_LIQUIDACION =
            "Esta liquidación presenta valores económicos calculados de carácter informativo y no constituye constancia de pago recibido.";

    private final CancelacionOrdenRepository cancelacionOrdenRepository;
    private final LiquidacionEconomicaRepository liquidacionEconomicaRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final DetallePresupuestoRepository detallePresupuestoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExecutionTimeService executionTimeService;
    private final InventoryReservationService inventoryReservationService;
    private final ServiceClosureService serviceClosureService;
    private final OrderService orderService;
    private final OrderStateTransitionService orderStateTransitionService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderCancellationService(
            CancelacionOrdenRepository cancelacionOrdenRepository,
            LiquidacionEconomicaRepository liquidacionEconomicaRepository,
            PresupuestoRepository presupuestoRepository,
            DetallePresupuestoRepository detallePresupuestoRepository,
            UsuarioRepository usuarioRepository,
            ExecutionTimeService executionTimeService,
            InventoryReservationService inventoryReservationService,
            ServiceClosureService serviceClosureService,
            OrderService orderService,
            OrderStateTransitionService orderStateTransitionService,
            ApplicationEventPublisher eventPublisher) {
        this.cancelacionOrdenRepository = cancelacionOrdenRepository;
        this.liquidacionEconomicaRepository = liquidacionEconomicaRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.detallePresupuestoRepository = detallePresupuestoRepository;
        this.usuarioRepository = usuarioRepository;
        this.executionTimeService = executionTimeService;
        this.inventoryReservationService = inventoryReservationService;
        this.serviceClosureService = serviceClosureService;
        this.orderService = orderService;
        this.orderStateTransitionService = orderStateTransitionService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * REQ-25, REQ-26, RN-09, RN-10, RN-11, RN-14 y CV-16: Cancela una orden en estado PAUSADA,
     * descuenta físicamente solo los elementos utilizados, libera reservas no utilizadas y genera la liquidación.
     */
    @Transactional
    public LiquidacionCancelacionResponse cancelarOrdenPausada(
            Integer idOrden,
            CancelarOrdenRequest request,
            Integer idAdminAutenticado) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idAdminAutenticado, "ADMINISTRADOR"
        );

        if (cancelacionOrdenRepository.existsByOrdenServicio_IdOrden(idOrden)) {
            throw new BusinessRuleException(
                    "ORDEN_YA_CANCELADA",
                    "La orden de servicio ya cuenta con una cancelación administrativa registrada."
            );
        }

        // REQ-25 y CV-16: Solo se puede cancelar administrativamente una orden que se encuentre en estado PAUSADA
        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "Solo se puede cancelar administrativamente una orden de servicio que se encuentre en estado PAUSADA."
            );
        }

        Usuario administrador = usuarioRepository.findById(idAdminAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario administrador autenticado."));

        List<DetallePresupuesto> detallesAprobados = detallePresupuestoRepository
                .findAllByIdOrdenAndEstadoPresupuesto(idOrden, EstadoPresupuesto.APROBADO);

        if (request.cantidadesUtilizadas() != null && !request.cantidadesUtilizadas().isEmpty()) {
            serviceClosureService.aplicarYValidarCantidadesUtilizadas(detallesAprobados, request.cantidadesUtilizadas());
        }

        // 1. Detener y cerrar el registro de tiempo de ejecución (REQ-25 y REQ-26)
        long segundosEfectivos = executionTimeService.cerrarTiempoYCalcularSegundosEfectivos(
                idOrden, TipoFinTiempo.CANCELACION_ORDEN
        );

        // 2. Descontar de existencias físicas únicamente lo utilizado y liberar reservas no utilizadas (RN-14)
        List<ConsumoReservaItem> consumos = detallesAprobados.stream()
                .map(d -> new ConsumoReservaItem(
                        d.getIdDetalle(),
                        d.getRepuesto().getIdRepuesto(),
                        d.getCantidadUtilizada() != null ? d.getCantidadUtilizada() : BigDecimal.ZERO
                ))
                .toList();

        inventoryReservationService.consumirYLiberarReservas(
                consumos,
                TipoCierreReserva.CANCELACION_ORDEN,
                idAdminAutenticado,
                orden.getCodigoOrden()
        );

        // 3. Calcular mano de obra y repuestos utilizados según presupuestos aprobados (RN-10 y RN-11)
        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElseThrow(() -> new BusinessRuleException(
                        "PRESUPUESTO_ORIGINAL_NO_ENCONTRADO",
                        "No se encontró el presupuesto original de la orden para determinar la tarifa aprobada."
                ));

        BigDecimal tarifaHoraAplicada = presupuestoOriginal.getTarifaHoraAprobada();
        BigDecimal valorManoObra = MoneyCalculator.calcularManoObraEfectiva(tarifaHoraAplicada, segundosEfectivos);

        BigDecimal valorRepuestosUtilizados = BigDecimal.ZERO;
        for (DetallePresupuesto d : detallesAprobados) {
            BigDecimal cantUtilizada = d.getCantidadUtilizada() != null ? d.getCantidadUtilizada() : BigDecimal.ZERO;
            BigDecimal importe = MoneyCalculator.calcularImporteItem(d.getPrecioUnitarioConservado(), cantUtilizada);
            valorRepuestosUtilizados = valorRepuestosUtilizados.add(importe);
        }

        BigDecimal totalCalculado = valorManoObra.add(valorRepuestosUtilizados);
        LocalDateTime ahora = LocalDateTime.now();

        // 4. Guardar CANCELACION_ORDEN y LIQUIDACION_ECONOMICA
        CancelacionOrden cancelacion = new CancelacionOrden();
        cancelacion.setOrdenServicio(orden);
        cancelacion.setAdministrador(administrador);
        cancelacion.setJustificacion(request.justificacion().trim());
        cancelacion.setFechaHoraCancelacion(ahora);
        CancelacionOrden cancelacionGuardada = cancelacionOrdenRepository.save(cancelacion);

        LiquidacionEconomica liquidacion = new LiquidacionEconomica();
        liquidacion.setCancelacionOrden(cancelacionGuardada);
        liquidacion.setSegundosEfectivos(segundosEfectivos);
        liquidacion.setTarifaHoraAplicada(tarifaHoraAplicada);
        liquidacion.setValorManoObra(valorManoObra);
        liquidacion.setValorRepuestosInsumos(valorRepuestosUtilizados);
        liquidacion.setTotalCalculado(totalCalculado);
        liquidacion.setFechaGeneracion(ahora);
        LiquidacionEconomica liquidacionGuardada = liquidacionEconomicaRepository.save(liquidacion);

        // 5. Cambiar estado operativo de la orden a CANCELADO y registrar en HISTORIAL_ESTADO_ORDEN
        OrdenServicio ordenCancelada = orderStateTransitionService.cambiarEstado(
                idOrden,
                EstadoOperativoOrden.PAUSADA,
                EstadoOperativoOrden.CANCELADO,
                idAdminAutenticado,
                request.justificacion().trim(),
                null
        );
        cancelacionGuardada.setOrdenServicio(ordenCancelada);

        LiquidacionCancelacionResponse response = construirLiquidacionResponse(
                cancelacionGuardada, liquidacionGuardada, detallesAprobados, presupuestoOriginal
        );

        // 6. Notificar por correo al Cliente tras el commit (REQ-25)
        eventPublisher.publishEvent(new OrderCancelledEvent(
                ordenCancelada.getIdOrden(),
                ordenCancelada.getCodigoOrden(),
                ordenCancelada.getCliente().getNombreCompleto(),
                ordenCancelada.getCliente().getCorreo(),
                cancelacionGuardada.getJustificacion(),
                liquidacionGuardada.getTotalCalculado()
        ));

        return response;
    }

    @Transactional(readOnly = true)
    public LiquidacionCancelacionResponse consultarLiquidacionPorOrden(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);

        LiquidacionEconomica liquidacion = liquidacionEconomicaRepository.findByIdOrdenWithCancelacion(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La orden de servicio no cuenta con una liquidación económica por cancelación administrativa."
                ));

        List<DetallePresupuesto> detallesAprobados = detallePresupuestoRepository
                .findAllByIdOrdenAndEstadoPresupuesto(idOrden, EstadoPresupuesto.APROBADO);

        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElse(null);

        return construirLiquidacionResponse(
                liquidacion.getCancelacionOrden(),
                liquidacion,
                detallesAprobados,
                presupuestoOriginal
        );
    }

    private LiquidacionCancelacionResponse construirLiquidacionResponse(
            CancelacionOrden cancelacion,
            LiquidacionEconomica liquidacion,
            List<DetallePresupuesto> detallesAprobados,
            Presupuesto presupuestoOriginal) {

        OrdenServicio orden = cancelacion.getOrdenServicio();
        long segundos = liquidacion.getSegundosEfectivos() != null ? liquidacion.getSegundosEfectivos() : 0L;

        BigDecimal horasEstimadas = presupuestoOriginal != null && presupuestoOriginal.getHorasEstimadas() != null
                ? presupuestoOriginal.getHorasEstimadas()
                : BigDecimal.ZERO;

        BigDecimal horasEfectivas = BigDecimal.valueOf(segundos)
                .divide(SEGUNDOS_POR_HORA, 2, RoundingMode.HALF_UP);

        BigDecimal diferenciaHoras = horasEfectivas.subtract(horasEstimadas);

        List<ItemUtilizadoLiquidacionResponse> itemsDto = new ArrayList<>();
        for (DetallePresupuesto d : detallesAprobados) {
            itemsDto.add(serviceClosureService.mapearDetalleALiquidacionItem(d));
        }

        return new LiquidacionCancelacionResponse(
                cancelacion.getIdCancelacion(),
                liquidacion.getIdLiquidacion(),
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                orden.getEstadoOperativo(),
                cancelacion.getAdministrador().getIdUsuario(),
                cancelacion.getAdministrador().getNombreCompleto(),
                cancelacion.getJustificacion(),
                cancelacion.getFechaHoraCancelacion(),
                segundos,
                ExecutionTimeService.formatearSegundos(segundos),
                horasEstimadas,
                horasEfectivas,
                diferenciaHoras,
                liquidacion.getTarifaHoraAplicada(),
                liquidacion.getValorManoObra(),
                liquidacion.getValorRepuestosInsumos(),
                liquidacion.getTotalCalculado(),
                liquidacion.getFechaGeneracion(),
                NOTA_INFORMATIVA_LIQUIDACION,
                itemsDto
        );
    }
}