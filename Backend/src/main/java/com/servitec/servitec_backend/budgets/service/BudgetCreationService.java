package com.servitec.servitec_backend.budgets.service;

import com.servitec.servitec_backend.budgets.dto.CrearPresupuestoOriginalRequest;
import com.servitec.servitec_backend.budgets.dto.DecisionPresupuestoResponse;
import com.servitec.servitec_backend.budgets.dto.DetallePresupuestoItemRequest;
import com.servitec.servitec_backend.budgets.dto.DetallePresupuestoResponse;
import com.servitec.servitec_backend.budgets.dto.PresupuestoResponse;
import com.servitec.servitec_backend.budgets.entity.DetallePresupuesto;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.DecisionPresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.DetallePresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.diagnostics.repository.DiagnosticoRepository;
import com.servitec.servitec_backend.inventory.entity.Repuesto;
import com.servitec.servitec_backend.inventory.service.CatalogService;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.orders.service.OrderStateTransitionService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import com.servitec.servitec_backend.shared.util.MoneyCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BudgetCreationService {

    private static final String CONDICION_MANO_OBRA_TEXTO =
            "El valor final de la mano de obra se determinará según el tiempo efectivo de ejecución registrado por el sistema, aplicando la tarifa por hora aprobada.";

    private final PresupuestoRepository presupuestoRepository;
    private final DetallePresupuestoRepository detallePresupuestoRepository;
    private final DecisionPresupuestoRepository decisionPresupuestoRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final CatalogService catalogService;
    private final OrderService orderService;
    private final OrderStateTransitionService orderStateTransitionService;

    public BudgetCreationService(
            PresupuestoRepository presupuestoRepository,
            DetallePresupuestoRepository detallePresupuestoRepository,
            DecisionPresupuestoRepository decisionPresupuestoRepository,
            DiagnosticoRepository diagnosticoRepository,
            CatalogService catalogService,
            OrderService orderService,
            OrderStateTransitionService orderStateTransitionService) {
        this.presupuestoRepository = presupuestoRepository;
        this.detallePresupuestoRepository = detallePresupuestoRepository;
        this.decisionPresupuestoRepository = decisionPresupuestoRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.orderStateTransitionService = orderStateTransitionService;
    }

    @Transactional
    public PresupuestoResponse crearPresupuestoOriginal(
            Integer idOrden,
            CrearPresupuestoOriginalRequest request,
            Integer idTecnicoAutenticado) {

        // Validar que el técnico autenticado sea el asignado a la orden
        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        // RN-01 y CV-05: Impedir la elaboración del presupuesto si la orden no tiene diagnóstico registrado
        if (!diagnosticoRepository.existsByOrdenServicio_IdOrden(idOrden)) {
            throw new BusinessRuleException(
                    "DIAGNOSTICO_PREVIO_REQUERIDO",
                    "No se puede elaborar el presupuesto: la orden no tiene un diagnóstico técnico registrado."
            );
        }

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.EN_DIAGNOSTICO) {
            throw new InvalidStateTransitionException(
                    "Solo se puede elaborar el presupuesto original cuando la orden se encuentra en estado EN_DIAGNOSTICO."
            );
        }

        if (presupuestoRepository.existsByOrdenServicio_IdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)) {
            throw new BusinessRuleException(
                    "PRESUPUESTO_ORIGINAL_YA_EXISTE",
                    "La orden de servicio ya cuenta con un presupuesto original registrado."
            );
        }

        List<DetallePresupuestoItemRequest> itemsSolicitados =
                request.detalles() != null ? request.detalles() : List.of();

        validarSinRepuestosDuplicados(itemsSolicitados);

        BigDecimal valorManoObraEstimado = MoneyCalculator.calcularManoObraEstimada(
                request.tarifaHora(), request.horasEstimadas()
        );

        LocalDateTime ahora = LocalDateTime.now();

        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setOrdenServicio(orden);
        presupuesto.setTecnicoCreador(orden.getTecnicoAsignado());
        presupuesto.setAutorizacionNuevaCotizacion(null);
        presupuesto.setTipoPresupuesto(TipoPresupuesto.ORIGINAL);
        presupuesto.setEstado(EstadoPresupuesto.PENDIENTE_APROBACION);
        presupuesto.setTarifaHoraAprobada(request.tarifaHora());
        presupuesto.setHorasEstimadas(request.horasEstimadas());
        presupuesto.setValorManoObraEstimado(valorManoObraEstimado);
        presupuesto.setSubtotalRepuestos(BigDecimal.ZERO);
        presupuesto.setTotalPresupuesto(valorManoObraEstimado);
        presupuesto.setFechaEmision(ahora);

        Presupuesto presupuestoGuardado = presupuestoRepository.save(presupuesto);

        BigDecimal subtotalRepuestos = BigDecimal.ZERO;
        List<DetallePresupuesto> detallesGuardados = new ArrayList<>();

        for (DetallePresupuestoItemRequest itemReq : itemsSolicitados) {
            // REQ-10 y REQ-11: Tomar repuesto activo del catálogo y congelar su precio unitario actual
            Repuesto repuesto = catalogService.obtenerEntidadActivaParaPresupuesto(itemReq.idRepuesto());
            BigDecimal precioCat = repuesto.getPrecioUnitarioActual();
            BigDecimal importeItem = MoneyCalculator.calcularImporteItem(precioCat, itemReq.cantidadAutorizada());

            subtotalRepuestos = subtotalRepuestos.add(importeItem);

            DetallePresupuesto detalle = new DetallePresupuesto();
            detalle.setPresupuesto(presupuestoGuardado);
            detalle.setRepuesto(repuesto);
            detalle.setCantidadAutorizada(itemReq.cantidadAutorizada());
            detalle.setPrecioUnitarioConservado(precioCat);
            detalle.setCantidadUtilizada(BigDecimal.ZERO);
            detalle.setJustificacion(
                    itemReq.justificacion() != null && !itemReq.justificacion().isBlank()
                            ? itemReq.justificacion().trim()
                            : null
            );

            detallesGuardados.add(detallePresupuestoRepository.save(detalle));
        }

        BigDecimal totalPresupuesto = subtotalRepuestos.add(valorManoObraEstimado);
        presupuestoGuardado.setSubtotalRepuestos(subtotalRepuestos);
        presupuestoGuardado.setTotalPresupuesto(totalPresupuesto);
        presupuestoGuardado = presupuestoRepository.save(presupuestoGuardado);

        // Cambiar estado operativo de la orden a PRESUPUESTADO y registrar en HISTORIAL_ESTADO_ORDEN
        OrdenServicio ordenActualizada = orderStateTransitionService.cambiarEstado(
                idOrden,
                EstadoOperativoOrden.EN_DIAGNOSTICO,
                EstadoOperativoOrden.PRESUPUESTADO,
                idTecnicoAutenticado,
                "Presupuesto original emitido y pendiente de aprobación del cliente.",
                null
        );
        presupuestoGuardado.setOrdenServicio(ordenActualizada);

        return construirResponse(presupuestoGuardado, detallesGuardados, null);
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listarPresupuestosPorOrden(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);

        return presupuestoRepository.findAllByIdOrdenOrderByFechaEmisionAsc(idOrden)
                .stream()
                .map(this::mapearPresupuestoCompleto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse consultarPresupuestoPorId(
            Integer idPresupuesto,
            Integer idUsuarioActual,
            String rolActual) {

        Presupuesto presupuesto = presupuestoRepository.findByIdWithDetails(idPresupuesto)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el presupuesto con ID: " + idPresupuesto));

        orderService.obtenerYValidarAccesoOrden(
                presupuesto.getOrdenServicio().getIdOrden(), idUsuarioActual, rolActual
        );

        return mapearPresupuestoCompleto(presupuesto);
    }

    public PresupuestoResponse mapearPresupuestoCompleto(Presupuesto presupuesto) {
        List<DetallePresupuesto> detalles = detallePresupuestoRepository
                .findByIdPresupuestoWithRepuesto(presupuesto.getIdPresupuesto());

        DecisionPresupuestoResponse decisionResponse = decisionPresupuestoRepository
                .findByIdPresupuestoWithCliente(presupuesto.getIdPresupuesto())
                .map(d -> new DecisionPresupuestoResponse(
                        d.getIdDecision(),
                        d.getDecision(),
                        d.getClienteDecisor().getIdUsuario(),
                        d.getClienteDecisor().getNombreCompleto(),
                        d.getFechaHoraDecision()
                ))
                .orElse(null);

        return construirResponse(presupuesto, detalles, decisionResponse);
    }

    private PresupuestoResponse construirResponse(
            Presupuesto p,
            List<DetallePresupuesto> detalles,
            DecisionPresupuestoResponse decisionResponse) {

        List<DetallePresupuestoResponse> detallesDto = detalles.stream()
                .map(d -> new DetallePresupuestoResponse(
                        d.getIdDetalle(),
                        d.getRepuesto().getIdRepuesto(),
                        d.getRepuesto().getCodigoRepuesto(),
                        d.getRepuesto().getNombre(),
                        d.getRepuesto().getUnidadMedida(),
                        d.getCantidadAutorizada(),
                        d.getPrecioUnitarioConservado(),
                        MoneyCalculator.calcularImporteItem(d.getPrecioUnitarioConservado(), d.getCantidadAutorizada()),
                        d.getCantidadUtilizada(),
                        d.getJustificacion()
                ))
                .toList();

        String prefijo = p.getTipoPresupuesto() == TipoPresupuesto.ORIGINAL ? "PO" : "PC";
        String codigoPresupuesto = String.format("%s-%d-%03d", prefijo, p.getFechaEmision().getYear(), p.getIdPresupuesto());

        Integer idAutorizacion = p.getAutorizacionNuevaCotizacion() != null
                ? p.getAutorizacionNuevaCotizacion().getIdAutorizacion()
                : null;

        return new PresupuestoResponse(
                p.getIdPresupuesto(),
                codigoPresupuesto,
                p.getOrdenServicio().getIdOrden(),
                p.getOrdenServicio().getCodigoOrden(),
                p.getOrdenServicio().getEstadoOperativo(),
                p.getTipoPresupuesto(),
                p.getEstado(),
                p.getTecnicoCreador().getIdUsuario(),
                p.getTecnicoCreador().getNombreCompleto(),
                idAutorizacion,
                p.getTarifaHoraAprobada(),
                p.getHorasEstimadas(),
                p.getValorManoObraEstimado(),
                p.getSubtotalRepuestos(),
                p.getTotalPresupuesto(),
                p.getTipoPresupuesto() == TipoPresupuesto.ORIGINAL ? CONDICION_MANO_OBRA_TEXTO : null,
                p.getFechaEmision(),
                detallesDto,
                decisionResponse
        );
    }

    public void validarSinRepuestosDuplicados(List<DetallePresupuestoItemRequest> items) {
        Set<Integer> idsVistos = new HashSet<>();
        for (DetallePresupuestoItemRequest item : items) {
            if (!idsVistos.add(item.idRepuesto())) {
                throw new BusinessRuleException(
                        "REPUESTO_DUPLICADO_EN_PRESUPUESTO",
                        "No debes repetir el mismo repuesto o insumo en varias líneas del mismo presupuesto; consolida su cantidad en una sola línea."
                );
            }
        }
    }
}