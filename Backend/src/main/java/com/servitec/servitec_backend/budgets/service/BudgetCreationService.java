package com.servitec.servitec_backend.budgets.service;

import com.servitec.servitec_backend.budgets.dto.CrearPresupuestoComplementarioRequest;
import com.servitec.servitec_backend.budgets.dto.CrearPresupuestoOriginalRequest;
import com.servitec.servitec_backend.budgets.dto.DecisionPresupuestoResponse;
import com.servitec.servitec_backend.budgets.dto.DetallePresupuestoItemRequest;
import com.servitec.servitec_backend.budgets.dto.DetallePresupuestoResponse;
import com.servitec.servitec_backend.budgets.dto.ItemComplementarioRequest;
import com.servitec.servitec_backend.budgets.dto.PresupuestoResponse;
import com.servitec.servitec_backend.budgets.entity.AutorizacionNuevaCotizacion;
import com.servitec.servitec_backend.budgets.entity.DetallePresupuesto;
import com.servitec.servitec_backend.budgets.entity.EvaluacionTecnica;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.AutorizacionNuevaCotizacionRepository;
import com.servitec.servitec_backend.budgets.repository.DecisionPresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.DetallePresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.EvaluacionTecnicaRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.diagnostics.repository.DiagnosticoRepository;
import com.servitec.servitec_backend.inventory.entity.Repuesto;
import com.servitec.servitec_backend.inventory.service.CatalogService;
import com.servitec.servitec_backend.notifications.event.ComplementaryBudgetCreatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BudgetCreationService {

    private static final String CONDICION_MANO_OBRA_TEXTO =
            "El valor final de la mano de obra se determinará según el tiempo efectivo de ejecución registrado por el sistema, aplicando la tarifa por hora aprobada.";

    private final PresupuestoRepository presupuestoRepository;
    private final DetallePresupuestoRepository detallePresupuestoRepository;
    private final DecisionPresupuestoRepository decisionPresupuestoRepository;
    private final EvaluacionTecnicaRepository evaluacionTecnicaRepository;
    private final AutorizacionNuevaCotizacionRepository autorizacionRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final CatalogService catalogService;
    private final OrderService orderService;
    private final OrderStateTransitionService orderStateTransitionService;
    private final ApplicationEventPublisher eventPublisher;

    public BudgetCreationService(
            PresupuestoRepository presupuestoRepository,
            DetallePresupuestoRepository detallePresupuestoRepository,
            DecisionPresupuestoRepository decisionPresupuestoRepository,
            EvaluacionTecnicaRepository evaluacionTecnicaRepository,
            AutorizacionNuevaCotizacionRepository autorizacionRepository,
            DiagnosticoRepository diagnosticoRepository,
            CatalogService catalogService,
            OrderService orderService,
            OrderStateTransitionService orderStateTransitionService,
            ApplicationEventPublisher eventPublisher) {
        this.presupuestoRepository = presupuestoRepository;
        this.detallePresupuestoRepository = detallePresupuestoRepository;
        this.decisionPresupuestoRepository = decisionPresupuestoRepository;
        this.evaluacionTecnicaRepository = evaluacionTecnicaRepository;
        this.autorizacionRepository = autorizacionRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.orderStateTransitionService = orderStateTransitionService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PresupuestoResponse crearPresupuestoOriginal(
            Integer idOrden,
            CrearPresupuestoOriginalRequest request,
            Integer idTecnicoAutenticado) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

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

        validarSinRepuestosDuplicados(itemsSolicitados.stream().map(DetallePresupuestoItemRequest::idRepuesto).toList());

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

    /**
     * REQ-21, RN-08 y CV-12: Registra repuestos e insumos adicionales cuando la orden se encuentra PAUSADA,
     * generando un presupuesto complementario en estado PENDIENTE_APROBACION y notificando al Cliente por correo.
     */
    @Transactional
    public PresupuestoResponse crearPresupuestoComplementario(
            Integer idOrden,
            CrearPresupuestoComplementarioRequest request,
            Integer idTecnicoAutenticado) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "El registro de repuestos e insumos adicionales solo está permitido cuando la orden se encuentra en estado PAUSADA."
            );
        }

        if (presupuestoRepository.existsByOrdenServicio_IdOrdenAndTipoPresupuestoAndEstado(
                idOrden, TipoPresupuesto.COMPLEMENTARIO, EstadoPresupuesto.PENDIENTE_APROBACION)) {
            throw new BusinessRuleException(
                    "COMPLEMENTARIO_PENDIENTE_EXISTENTE",
                    "La orden ya tiene un presupuesto complementario pendiente de aprobación por parte del Cliente."
            );
        }

        // RN-09 y REQ-24: Si el último complementario fue rechazado y sus elementos son indispensables, exige autorización admin
        verificarSiRequiereAutorizacionAdministrativa(idOrden);

        return guardarPresupuestoComplementarioInterno(orden, request.detalles(), null, false);
    }

    /**
     * REQ-24, RN-09 y CV-15: Elabora una nueva propuesta de presupuesto complementario vinculada
     * a una autorización administrativa previa sobre la orden pausada.
     */
    @Transactional
    public PresupuestoResponse crearNuevaCotizacionComplementaria(
            Integer idOrden,
            Integer idAutorizacion,
            CrearPresupuestoComplementarioRequest request,
            Integer idTecnicoAutenticado) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "Solo se puede elaborar una nueva cotización complementaria cuando la orden se encuentra en estado PAUSADA."
            );
        }

        AutorizacionNuevaCotizacion autorizacion = autorizacionRepository.findByIdWithDetails(idAutorizacion)
                .orElseThrow(() -> new BusinessRuleException(
                        "AUTORIZACION_NO_ENCONTRADA",
                        "No existe la autorización administrativa requerida para elaborar la nueva cotización complementaria."
                ));

        if (!autorizacion.getPresupuestoRechazado().getOrdenServicio().getIdOrden().equals(idOrden)) {
            throw new BusinessRuleException(
                    "AUTORIZACION_ORDEN_INCORRECTA",
                    "La autorización administrativa seleccionada no corresponde a esta orden de servicio."
            );
        }

        if (presupuestoRepository.existsByAutorizacionNuevaCotizacion_IdAutorizacion(idAutorizacion)) {
            throw new BusinessRuleException(
                    "AUTORIZACION_YA_UTILIZADA",
                    "Esta autorización administrativa ya fue utilizada para generar una propuesta de presupuesto complementario."
            );
        }

        if (presupuestoRepository.existsByOrdenServicio_IdOrdenAndTipoPresupuestoAndEstado(
                idOrden, TipoPresupuesto.COMPLEMENTARIO, EstadoPresupuesto.PENDIENTE_APROBACION)) {
            throw new BusinessRuleException(
                    "COMPLEMENTARIO_PENDIENTE_EXISTENTE",
                    "La orden ya cuenta con un presupuesto complementario pendiente de decisión."
            );
        }

        return guardarPresupuestoComplementarioInterno(orden, request.detalles(), autorizacion, true);
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

    private PresupuestoResponse guardarPresupuestoComplementarioInterno(
            OrdenServicio orden,
            List<ItemComplementarioRequest> itemsSolicitados,
            AutorizacionNuevaCotizacion autorizacion,
            boolean esNuevaPropuestaAutorizada) {

        validarSinRepuestosDuplicados(itemsSolicitados.stream().map(ItemComplementarioRequest::idRepuesto).toList());

        LocalDateTime ahora = LocalDateTime.now();

        Presupuesto complementario = new Presupuesto();
        complementario.setOrdenServicio(orden);
        complementario.setTecnicoCreador(orden.getTecnicoAsignado());
        complementario.setAutorizacionNuevaCotizacion(autorizacion);
        complementario.setTipoPresupuesto(TipoPresupuesto.COMPLEMENTARIO);
        complementario.setEstado(EstadoPresupuesto.PENDIENTE_APROBACION);
        complementario.setTarifaHoraAprobada(null);
        complementario.setHorasEstimadas(null);
        complementario.setValorManoObraEstimado(null);
        complementario.setSubtotalRepuestos(BigDecimal.ZERO);
        complementario.setTotalPresupuesto(BigDecimal.ZERO);
        complementario.setFechaEmision(ahora);

        Presupuesto guardado = presupuestoRepository.save(complementario);

        BigDecimal subtotalAdicional = BigDecimal.ZERO;
        List<DetallePresupuesto> detallesGuardados = new ArrayList<>();

        for (ItemComplementarioRequest itemReq : itemsSolicitados) {
            Repuesto repuesto = catalogService.obtenerEntidadActivaParaPresupuesto(itemReq.idRepuesto());
            BigDecimal precioCat = repuesto.getPrecioUnitarioActual();
            BigDecimal importeItem = MoneyCalculator.calcularImporteItem(precioCat, itemReq.cantidadAutorizada());

            subtotalAdicional = subtotalAdicional.add(importeItem);

            DetallePresupuesto detalle = new DetallePresupuesto();
            detalle.setPresupuesto(guardado);
            detalle.setRepuesto(repuesto);
            detalle.setCantidadAutorizada(itemReq.cantidadAutorizada());
            detalle.setPrecioUnitarioConservado(precioCat);
            detalle.setCantidadUtilizada(BigDecimal.ZERO);
            detalle.setJustificacion(itemReq.justificacion().trim());

            detallesGuardados.add(detallePresupuestoRepository.save(detalle));
        }

        guardado.setSubtotalRepuestos(subtotalAdicional);
        guardado.setTotalPresupuesto(subtotalAdicional);
        guardado = presupuestoRepository.save(guardado);

        PresupuestoResponse response = construirResponse(guardado, detallesGuardados, null);

        // Publicar evento post-commit para enviar notificación por correo con enlace seguro al Cliente
        eventPublisher.publishEvent(new ComplementaryBudgetCreatedEvent(
                response.idPresupuesto(),
                response.codigoPresupuesto(),
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                orden.getCliente().getNombreCompleto(),
                orden.getCliente().getCorreo(),
                response.totalPresupuesto(),
                esNuevaPropuestaAutorizada
        ));

        return response;
    }

    private void verificarSiRequiereAutorizacionAdministrativa(Integer idOrden) {
        List<Presupuesto> complementarios = presupuestoRepository.findAllByIdOrdenOrderByFechaEmisionAsc(idOrden)
                .stream()
                .filter(p -> p.getTipoPresupuesto() == TipoPresupuesto.COMPLEMENTARIO)
                .toList();

        if (complementarios.isEmpty()) {
            return;
        }

        Presupuesto ultimo = complementarios.get(complementarios.size() - 1);
        if (ultimo.getEstado() == EstadoPresupuesto.RECHAZADO) {
            Optional<EvaluacionTecnica> evalOpt = evaluacionTecnicaRepository
                    .findByIdPresupuestoRechazadoWithDetails(ultimo.getIdPresupuesto());

            if (evalOpt.isEmpty()) {
                throw new BusinessRuleException(
                        "EVALUACION_TECNICA_REQUERIDA",
                        "Existe un presupuesto complementario rechazado pendiente de evaluación técnica."
                );
            }

            if (Boolean.TRUE.equals(evalOpt.get().getElementosIndispensables())) {
                throw new BusinessRuleException(
                        "AUTORIZACION_ADMIN_REQUERIDA",
                        "El presupuesto complementario anterior fue rechazado con elementos indispensables; se requiere autorización previa del Administrador para elaborar una nueva propuesta."
                );
            }
        }
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

    public void validarSinRepuestosDuplicados(List<Integer> idsRepuestos) {
        Set<Integer> idsVistos = new HashSet<>();
        for (Integer idRepuesto : idsRepuestos) {
            if (!idsVistos.add(idRepuesto)) {
                throw new BusinessRuleException(
                        "REPUESTO_DUPLICADO_EN_PRESUPUESTO",
                        "No debes repetir el mismo repuesto o insumo en varias líneas del mismo presupuesto; consolida su cantidad en una sola línea."
                );
            }
        }
    }
}