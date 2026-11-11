package com.servitec.servitec_backend.closures.service;

import com.servitec.servitec_backend.budgets.entity.DetallePresupuesto;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.DetallePresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.closures.dto.ActualizarCantidadesUtilizadasRequest;
import com.servitec.servitec_backend.closures.dto.CantidadUtilizadaItemRequest;
import com.servitec.servitec_backend.closures.dto.CerrarServicioRequest;
import com.servitec.servitec_backend.closures.dto.CierreTecnicoResponse;
import com.servitec.servitec_backend.closures.dto.ItemUtilizadoLiquidacionResponse;
import com.servitec.servitec_backend.closures.entity.CierreTecnico;
import com.servitec.servitec_backend.closures.repository.CierreTecnicoRepository;
import com.servitec.servitec_backend.execution.repository.RegistroTiempoRepository;
import com.servitec.servitec_backend.execution.service.ExecutionTimeService;
import com.servitec.servitec_backend.inventory.dto.ConsumoReservaItem;
import com.servitec.servitec_backend.inventory.service.InventoryReservationService;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.orders.service.OrderStateTransitionService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoCierreReserva;
import com.servitec.servitec_backend.shared.enums.TipoFinTiempo;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import com.servitec.servitec_backend.shared.util.MoneyCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServiceClosureService {

    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);

    private final CierreTecnicoRepository cierreTecnicoRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final DetallePresupuestoRepository detallePresupuestoRepository;
    private final RegistroTiempoRepository registroTiempoRepository;
    private final ExecutionTimeService executionTimeService;
    private final InventoryReservationService inventoryReservationService;
    private final OrderService orderService;
    private final OrderStateTransitionService orderStateTransitionService;

    public ServiceClosureService(
            CierreTecnicoRepository cierreTecnicoRepository,
            PresupuestoRepository presupuestoRepository,
            DetallePresupuestoRepository detallePresupuestoRepository,
            RegistroTiempoRepository registroTiempoRepository,
            ExecutionTimeService executionTimeService,
            InventoryReservationService inventoryReservationService,
            OrderService orderService,
            OrderStateTransitionService orderStateTransitionService) {
        this.cierreTecnicoRepository = cierreTecnicoRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.detallePresupuestoRepository = detallePresupuestoRepository;
        this.registroTiempoRepository = registroTiempoRepository;
        this.executionTimeService = executionTimeService;
        this.inventoryReservationService = inventoryReservationService;
        this.orderService = orderService;
        this.orderStateTransitionService = orderStateTransitionService;
    }

    /**
     * Permite al técnico registrar o actualizar las cantidades efectivamente utilizadas de repuestos aprobados
     * durante la ejecución o pausa antes del cierre o cancelación.
     */
    @Transactional
    public List<ItemUtilizadoLiquidacionResponse> actualizarCantidadesUtilizadas(
            Integer idOrden,
            ActualizarCantidadesUtilizadasRequest request,
            Integer idTecnicoAutenticado) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.EN_EJECUCION
                && orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "Solo se pueden registrar cantidades utilizadas cuando la orden se encuentra EN_EJECUCION o PAUSADA."
            );
        }

        List<DetallePresupuesto> detallesAprobados = detallePresupuestoRepository
                .findAllByIdOrdenAndEstadoPresupuesto(idOrden, EstadoPresupuesto.APROBADO);

        aplicarYValidarCantidadesUtilizadas(detallesAprobados, request.cantidadesUtilizadas());

        return detallesAprobados.stream()
                .map(this::mapearDetalleALiquidacionItem)
                .toList();
    }

    /**
     * REQ-16, REQ-17, REQ-26, RN-04, RN-08, RN-14 y CV-08: Finaliza el servicio técnico en una única transacción.
     */
    @Transactional
    public CierreTecnicoResponse cerrarServicioTecnico(
            Integer idOrden,
            CerrarServicioRequest request,
            Integer idTecnicoAutenticado) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        if (cierreTecnicoRepository.existsByOrdenServicio_IdOrden(idOrden)) {
            throw new BusinessRuleException(
                    "CIERRE_YA_REGISTRADO",
                    "La orden de servicio ya cuenta con un cierre técnico registrado."
            );
        }

        // CV-08 Criterio 2: Solo se permite cerrar cuando la orden está EN_EJECUCION
        if (orden.getEstadoOperativo() != EstadoOperativoOrden.EN_EJECUCION) {
            throw new InvalidStateTransitionException(
                    "Solo se puede realizar el cierre técnico cuando la orden de servicio se encuentra en estado EN_EJECUCION."
            );
        }

        // REQ-17 y RN-08: Impedir el cierre si existen presupuestos complementarios pendientes de decisión
        if (presupuestoRepository.existsByOrdenServicio_IdOrdenAndTipoPresupuestoAndEstado(
                idOrden, TipoPresupuesto.COMPLEMENTARIO, EstadoPresupuesto.PENDIENTE_APROBACION)) {
            throw new BusinessRuleException(
                    "PRESUPUESTO_COMPLEMENTARIO_PENDIENTE",
                    "La orden no puede finalizar hasta que exista una decisión del Cliente sobre los presupuestos complementarios pendientes."
            );
        }

        List<DetallePresupuesto> detallesAprobados = detallePresupuestoRepository
                .findAllByIdOrdenAndEstadoPresupuesto(idOrden, EstadoPresupuesto.APROBADO);

        if (request.cantidadesUtilizadas() != null && !request.cantidadesUtilizadas().isEmpty()) {
            aplicarYValidarCantidadesUtilizadas(detallesAprobados, request.cantidadesUtilizadas());
        }

        // 1. Detener registro de tiempo y obtener segundos efectivos acumulados (REQ-26)
        long segundosEfectivos = executionTimeService.cerrarTiempoYCalcularSegundosEfectivos(
                idOrden, TipoFinTiempo.CIERRE_SERVICIO
        );

        // 2. Descontar físicamente solo las cantidades utilizadas y cerrar reservas liberando las no utilizadas (RN-14)
        List<ConsumoReservaItem> consumos = detallesAprobados.stream()
                .map(d -> new ConsumoReservaItem(
                        d.getIdDetalle(),
                        d.getRepuesto().getIdRepuesto(),
                        d.getCantidadUtilizada()
                ))
                .toList();

        inventoryReservationService.consumirYLiberarReservas(
                consumos,
                TipoCierreReserva.CIERRE_SERVICIO,
                idTecnicoAutenticado,
                orden.getCodigoOrden()
        );

        // 3. Guardar el formulario de cierre técnico con pruebas, recomendaciones y garantía (REQ-16)
        LocalDateTime ahora = LocalDateTime.now();
        CierreTecnico cierre = new CierreTecnico();
        cierre.setOrdenServicio(orden);
        cierre.setTecnico(orden.getTecnicoAsignado());
        cierre.setPruebasFuncionamiento(request.pruebasFuncionamiento().trim());
        cierre.setRecomendaciones(request.recomendaciones().trim());
        cierre.setGarantiaAlcance(request.garantiaAlcance().trim());
        cierre.setGarantiaVigencia(request.garantiaVigencia().trim());
        cierre.setFechaCierre(ahora);
        CierreTecnico cierreGuardado = cierreTecnicoRepository.save(cierre);

        // 4. Cambiar estado definitivo a FINALIZADO (modo solo lectura conforme a RN-04)
        OrdenServicio ordenFinalizada = orderStateTransitionService.cambiarEstado(
                idOrden,
                EstadoOperativoOrden.EN_EJECUCION,
                EstadoOperativoOrden.FINALIZADO,
                idTecnicoAutenticado,
                "Cierre técnico completado y servicio finalizado.",
                null
        );
        cierreGuardado.setOrdenServicio(ordenFinalizada);

        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElse(null);

        return construirCierreResponse(cierreGuardado, detallesAprobados, presupuestoOriginal, segundosEfectivos);
    }

    @Transactional(readOnly = true)
    public CierreTecnicoResponse consultarCierrePorOrden(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);

        CierreTecnico cierre = cierreTecnicoRepository.findByIdOrdenWithDetails(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La orden de servicio aún no cuenta con un cierre técnico registrado."
                ));

        List<DetallePresupuesto> detallesAprobados = detallePresupuestoRepository
                .findAllByIdOrdenAndEstadoPresupuesto(idOrden, EstadoPresupuesto.APROBADO);

        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElse(null);

        Long segundosCerrados = registroTiempoRepository.sumSegundosEfectivosCerradosByOrden(idOrden);
        long segundosEfectivos = segundosCerrados != null ? segundosCerrados : 0L;

        return construirCierreResponse(cierre, detallesAprobados, presupuestoOriginal, segundosEfectivos);
    }

    public void aplicarYValidarCantidadesUtilizadas(
            List<DetallePresupuesto> detallesAprobados,
            List<CantidadUtilizadaItemRequest> cantidadesEnviadas) {

        Map<Integer, DetallePresupuesto> mapaAprobados = detallesAprobados.stream()
                .collect(Collectors.toMap(DetallePresupuesto::getIdDetalle, Function.identity()));

        for (CantidadUtilizadaItemRequest req : cantidadesEnviadas) {
            DetallePresupuesto detalle = mapaAprobados.get(req.idDetallePresupuesto());
            if (detalle == null) {
                throw new BusinessRuleException(
                        "ELEMENTO_SIN_AUTORIZACION",
                        "No puedes registrar el uso de un elemento que no fue aprobado por el Cliente."
                );
            }
            if (req.cantidadUtilizada().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException(
                        "CANTIDAD_UTILIZADA_INVALIDA",
                        "La cantidad utilizada no puede ser negativa."
                );
            }
            if (req.cantidadUtilizada().compareTo(detalle.getCantidadAutorizada()) > 0) {
                throw new BusinessRuleException(
                        "CANTIDAD_SUPERA_RESERVA",
                        String.format(
                                "La cantidad utilizada (%s) del elemento '%s' no puede superar la cantidad autorizada y reservada (%s).",
                                req.cantidadUtilizada().stripTrailingZeros().toPlainString(),
                                detalle.getRepuesto().getNombre(),
                                detalle.getCantidadAutorizada().stripTrailingZeros().toPlainString()
                        )
                );
            }
            detalle.setCantidadUtilizada(req.cantidadUtilizada());
            detallePresupuestoRepository.save(detalle);
        }
    }

    public ItemUtilizadoLiquidacionResponse mapearDetalleALiquidacionItem(DetallePresupuesto d) {
        BigDecimal autorizada = d.getCantidadAutorizada();
        BigDecimal utilizada = d.getCantidadUtilizada() != null ? d.getCantidadUtilizada() : BigDecimal.ZERO;
        BigDecimal liberada = autorizada.subtract(utilizada);
        BigDecimal importeUtilizado = MoneyCalculator.calcularImporteItem(d.getPrecioUnitarioConservado(), utilizada);

        return new ItemUtilizadoLiquidacionResponse(
                d.getIdDetalle(),
                d.getPresupuesto().getIdPresupuesto(),
                d.getPresupuesto().getTipoPresupuesto(),
                d.getRepuesto().getIdRepuesto(),
                d.getRepuesto().getCodigoRepuesto(),
                d.getRepuesto().getNombre(),
                d.getRepuesto().getUnidadMedida(),
                autorizada,
                utilizada,
                liberada,
                d.getPrecioUnitarioConservado(),
                importeUtilizado
        );
    }

    private CierreTecnicoResponse construirCierreResponse(
            CierreTecnico cierre,
            List<DetallePresupuesto> detallesAprobados,
            Presupuesto presupuestoOriginal,
            long segundosEfectivos) {

        BigDecimal tarifaHora = presupuestoOriginal != null && presupuestoOriginal.getTarifaHoraAprobada() != null
                ? presupuestoOriginal.getTarifaHoraAprobada()
                : BigDecimal.ZERO;

        BigDecimal horasEstimadas = presupuestoOriginal != null && presupuestoOriginal.getHorasEstimadas() != null
                ? presupuestoOriginal.getHorasEstimadas()
                : BigDecimal.ZERO;

        BigDecimal horasEfectivas = BigDecimal.valueOf(segundosEfectivos)
                .divide(SEGUNDOS_POR_HORA, 2, RoundingMode.HALF_UP);

        BigDecimal diferenciaHoras = horasEfectivas.subtract(horasEstimadas);
        BigDecimal valorManoObra = MoneyCalculator.calcularManoObraEfectiva(tarifaHora, segundosEfectivos);

        List<ItemUtilizadoLiquidacionResponse> itemsDto = new ArrayList<>();
        BigDecimal valorRepuestosUtilizados = BigDecimal.ZERO;

        for (DetallePresupuesto d : detallesAprobados) {
            ItemUtilizadoLiquidacionResponse itemDto = mapearDetalleALiquidacionItem(d);
            itemsDto.add(itemDto);
            valorRepuestosUtilizados = valorRepuestosUtilizados.add(itemDto.importeUtilizado());
        }

        BigDecimal valorTotalCalculado = valorManoObra.add(valorRepuestosUtilizados);
        OrdenServicio orden = cierre.getOrdenServicio();

        return new CierreTecnicoResponse(
                cierre.getIdCierre(),
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                orden.getEstadoOperativo(),
                cierre.getTecnico().getIdUsuario(),
                cierre.getTecnico().getNombreCompleto(),
                cierre.getPruebasFuncionamiento(),
                cierre.getRecomendaciones(),
                cierre.getGarantiaAlcance(),
                cierre.getGarantiaVigencia(),
                cierre.getFechaCierre(),
                segundosEfectivos,
                ExecutionTimeService.formatearSegundos(segundosEfectivos),
                horasEstimadas,
                horasEfectivas,
                diferenciaHoras,
                tarifaHora,
                valorManoObra,
                valorRepuestosUtilizados,
                valorTotalCalculado,
                itemsDto
        );
    }
}