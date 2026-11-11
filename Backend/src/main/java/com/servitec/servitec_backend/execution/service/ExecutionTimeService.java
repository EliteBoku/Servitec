package com.servitec.servitec_backend.execution.service;

import com.servitec.servitec_backend.budgets.entity.DetallePresupuesto;
import com.servitec.servitec_backend.budgets.entity.EvaluacionTecnica;
import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.budgets.repository.DetallePresupuestoRepository;
import com.servitec.servitec_backend.budgets.repository.EvaluacionTecnicaRepository;
import com.servitec.servitec_backend.budgets.repository.PresupuestoRepository;
import com.servitec.servitec_backend.execution.dto.IntervaloTiempoResponse;
import com.servitec.servitec_backend.execution.dto.PausarReparacionRequest;
import com.servitec.servitec_backend.execution.dto.ResumenTiempoEjecucionResponse;
import com.servitec.servitec_backend.execution.entity.RegistroTiempo;
import com.servitec.servitec_backend.execution.repository.RegistroTiempoRepository;
import com.servitec.servitec_backend.inventory.service.InventoryReservationService;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.orders.service.OrderStateTransitionService;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoFinTiempo;
import com.servitec.servitec_backend.shared.enums.TipoInicioTiempo;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import com.servitec.servitec_backend.shared.util.MoneyCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExecutionTimeService {

    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);

    private final RegistroTiempoRepository registroTiempoRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final DetallePresupuestoRepository detallePresupuestoRepository;
    private final EvaluacionTecnicaRepository evaluacionTecnicaRepository;
    private final InventoryReservationService inventoryReservationService;
    private final OrderService orderService;
    private final OrderStateTransitionService orderStateTransitionService;

    public ExecutionTimeService(
            RegistroTiempoRepository registroTiempoRepository,
            PresupuestoRepository presupuestoRepository,
            DetallePresupuestoRepository detallePresupuestoRepository,
            EvaluacionTecnicaRepository evaluacionTecnicaRepository,
            InventoryReservationService inventoryReservationService,
            OrderService orderService,
            OrderStateTransitionService orderStateTransitionService) {
        this.registroTiempoRepository = registroTiempoRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.detallePresupuestoRepository = detallePresupuestoRepository;
        this.evaluacionTecnicaRepository = evaluacionTecnicaRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.orderService = orderService;
        this.orderStateTransitionService = orderStateTransitionService;
    }

    /**
     * REQ-14, REQ-26, RN-02, CV-07 y CV-17: Inicia la reparación verificando aprobación del presupuesto original
     * y reservas activas, cambia la orden a EN_EJECUCION y abre el primer intervalo de tiempo.
     */
    @Transactional
    public ResumenTiempoEjecucionResponse iniciarReparacion(Integer idOrden, Integer idTecnicoAutenticado) {
        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        // Validar que el presupuesto original exista y esté APROBADO (REQ-14 y RN-02)
        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElseThrow(() -> new BusinessRuleException(
                        "PRESUPUESTO_ORIGINAL_REQUERIDO",
                        "La reparación no puede comenzar sin la aprobación digital del presupuesto original."
                ));

        if (presupuestoOriginal.getEstado() != EstadoPresupuesto.APROBADO) {
            throw new BusinessRuleException(
                    "PRESUPUESTO_NO_APROBADO",
                    "La reparación no puede comenzar sin la aprobación digital del presupuesto original."
            );
        }

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PRESUPUESTADO) {
            throw new InvalidStateTransitionException(
                    "Solo se puede iniciar la reparación de una orden que se encuentre en estado PRESUPUESTADO."
            );
        }

        if (registroTiempoRepository.existsByOrdenServicio_IdOrdenAndFechaHoraFinIsNull(idOrden)) {
            throw new BusinessRuleException(
                    "INTERVALO_TIEMPO_ABIERTO",
                    "La orden ya cuenta con un intervalo de ejecución activo."
            );
        }

        // REQ-14: Verificar que existan reservas activas para todos los detalles del presupuesto original
        List<Integer> idsDetallesOriginal = detallePresupuestoRepository
                .findByIdPresupuestoWithRepuesto(presupuestoOriginal.getIdPresupuesto())
                .stream()
                .map(DetallePresupuesto::getIdDetalle)
                .toList();

        inventoryReservationService.validarReservasActivasParaEjecucion(idsDetallesOriginal);

        // Cambiar estado operativo de la orden a EN_EJECUCION y dejar traza en HISTORIAL_ESTADO_ORDEN
        OrdenServicio ordenEnEjecucion = orderStateTransitionService.cambiarEstado(
                idOrden,
                EstadoOperativoOrden.PRESUPUESTADO,
                EstadoOperativoOrden.EN_EJECUCION,
                idTecnicoAutenticado,
                "Inicio formal de la reparación.",
                null
        );

        LocalDateTime ahora = LocalDateTime.now();
        RegistroTiempo nuevoIntervalo = new RegistroTiempo();
        nuevoIntervalo.setOrdenServicio(ordenEnEjecucion);
        nuevoIntervalo.setTecnico(ordenEnEjecucion.getTecnicoAsignado());
        nuevoIntervalo.setTipoInicio(TipoInicioTiempo.INICIO_REPARACION);
        nuevoIntervalo.setFechaHoraInicio(ahora);
        nuevoIntervalo.setTipoFin(null);
        nuevoIntervalo.setFechaHoraFin(null);
        nuevoIntervalo.setDuracionSegundos(null);
        registroTiempoRepository.save(nuevoIntervalo);

        return construirResumenTiempo(ordenEnEjecucion, presupuestoOriginal);
    }

    /**
     * REQ-15, REQ-26, RN-12, CV-07 y CV-17: Pausa una orden en ejecución exigiendo justificación,
     * cerrando el intervalo de tiempo activo y registrando el motivo en HISTORIAL_ESTADO_ORDEN.
     */
    @Transactional
    public ResumenTiempoEjecucionResponse pausarReparacion(
            Integer idOrden,
            PausarReparacionRequest request,
            Integer idTecnicoAutenticado) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.EN_EJECUCION) {
            throw new InvalidStateTransitionException(
                    "Solo se puede pausar una orden de servicio que se encuentre en estado EN_EJECUCION."
            );
        }

        RegistroTiempo intervaloActivo = registroTiempoRepository.findIntervaloAbiertoByOrdenWithLock(idOrden)
                .orElseThrow(() -> new BusinessRuleException(
                        "SIN_INTERVALO_ACTIVO",
                        "No se encontró un intervalo de tiempo activo para cerrar al pausar la orden."
                ));

        LocalDateTime ahora = LocalDateTime.now();
        long segundosTramo = Math.max(0L, Duration.between(intervaloActivo.getFechaHoraInicio(), ahora).getSeconds());

        intervaloActivo.setTipoFin(TipoFinTiempo.PAUSA);
        intervaloActivo.setFechaHoraFin(ahora);
        intervaloActivo.setDuracionSegundos(segundosTramo);
        registroTiempoRepository.save(intervaloActivo);

        // Cambiar estado a PAUSADA almacenando la justificación obligatoria en HISTORIAL_ESTADO_ORDEN
        OrdenServicio ordenPausada = orderStateTransitionService.cambiarEstado(
                idOrden,
                EstadoOperativoOrden.EN_EJECUCION,
                EstadoOperativoOrden.PAUSADA,
                idTecnicoAutenticado,
                request.justificacion(),
                null
        );

        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElse(null);

        return construirResumenTiempo(ordenPausada, presupuestoOriginal);
    }

    /**
     * REQ-15, REQ-26, RN-08, RN-09, RN-12, CV-07 y CV-17: Reanuda explícitamente una reparación pausada
     * verificando que no existan bloqueos por presupuestos complementarios pendientes o elementos indispensables rechazados.
     */
    @Transactional
    public ResumenTiempoEjecucionResponse reanudarReparacion(Integer idOrden, Integer idTecnicoAutenticado) {
        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idTecnicoAutenticado, RolNombre.TECNICO_ESPECIALISTA.name()
        );

        if (orden.getEstadoOperativo() != EstadoOperativoOrden.PAUSADA) {
            throw new InvalidStateTransitionException(
                    "Solo se puede reanudar una orden de servicio que se encuentre en estado PAUSADA."
            );
        }

        if (registroTiempoRepository.existsByOrdenServicio_IdOrdenAndFechaHoraFinIsNull(idOrden)) {
            throw new BusinessRuleException(
                    "INTERVALO_TIEMPO_ABIERTO",
                    "La orden ya cuenta con un intervalo de tiempo abierto."
            );
        }

        validarCondicionesPresupuestosComplementariosParaReanudar(idOrden);

        OrdenServicio ordenReanudada = orderStateTransitionService.cambiarEstado(
                idOrden,
                EstadoOperativoOrden.PAUSADA,
                EstadoOperativoOrden.EN_EJECUCION,
                idTecnicoAutenticado,
                "Reanudación explícita de la reparación por el técnico especialista.",
                null
        );

        LocalDateTime ahora = LocalDateTime.now();
        RegistroTiempo nuevoIntervalo = new RegistroTiempo();
        nuevoIntervalo.setOrdenServicio(ordenReanudada);
        nuevoIntervalo.setTecnico(ordenReanudada.getTecnicoAsignado());
        nuevoIntervalo.setTipoInicio(TipoInicioTiempo.REANUDACION);
        nuevoIntervalo.setFechaHoraInicio(ahora);
        nuevoIntervalo.setTipoFin(null);
        nuevoIntervalo.setFechaHoraFin(null);
        nuevoIntervalo.setDuracionSegundos(null);
        registroTiempoRepository.save(nuevoIntervalo);

        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElse(null);

        return construirResumenTiempo(ordenReanudada, presupuestoOriginal);
    }

    /**
     * REQ-17, REQ-25 y REQ-26: Método interno invocado por closures al finalizar o cancelar una orden.
     * Cierra cualquier intervalo abierto y devuelve el total acumulado de segundos efectivos.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long cerrarTiempoYCalcularSegundosEfectivos(Integer idOrden, TipoFinTiempo tipoFin) {
        Optional<RegistroTiempo> intervaloAbiertoOpt =
                registroTiempoRepository.findIntervaloAbiertoByOrdenWithLock(idOrden);

        if (intervaloAbiertoOpt.isPresent()) {
            RegistroTiempo activo = intervaloAbiertoOpt.get();
            LocalDateTime ahora = LocalDateTime.now();
            long segundosTramo = Math.max(0L, Duration.between(activo.getFechaHoraInicio(), ahora).getSeconds());
            activo.setTipoFin(tipoFin);
            activo.setFechaHoraFin(ahora);
            activo.setDuracionSegundos(segundosTramo);
            registroTiempoRepository.save(activo);
        }

        Long totalSegundos = registroTiempoRepository.sumSegundosEfectivosCerradosByOrden(idOrden);
        return totalSegundos != null ? totalSegundos : 0L;
    }

    @Transactional(readOnly = true)
    public ResumenTiempoEjecucionResponse consultarResumenTiempo(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);
        Presupuesto presupuestoOriginal = presupuestoRepository
                .findByIdOrdenAndTipoPresupuesto(idOrden, TipoPresupuesto.ORIGINAL)
                .orElse(null);

        return construirResumenTiempo(orden, presupuestoOriginal);
    }

    /**
     * RN-08 y RN-09: Verifica si existen presupuestos complementarios que impidan reanudar la orden.
     */
    private void validarCondicionesPresupuestosComplementariosParaReanudar(Integer idOrden) {
        if (presupuestoRepository.existsByOrdenServicio_IdOrdenAndTipoPresupuestoAndEstado(
                idOrden, TipoPresupuesto.COMPLEMENTARIO, EstadoPresupuesto.PENDIENTE_APROBACION)) {
            throw new BusinessRuleException(
                    "COMPLEMENTARIO_PENDIENTE_DECISION",
                    "No es posible reanudar la reparación mientras exista un presupuesto complementario pendiente de aprobación del Cliente."
            );
        }

        List<Presupuesto> complementarios = presupuestoRepository.findAllByIdOrdenOrderByFechaEmisionAsc(idOrden)
                .stream()
                .filter(p -> p.getTipoPresupuesto() == TipoPresupuesto.COMPLEMENTARIO)
                .toList();

        if (complementarios.isEmpty()) {
            return;
        }

        // Verificar que todo presupuesto complementario rechazado tenga su evaluación técnica registrada
        for (Presupuesto comp : complementarios) {
            if (comp.getEstado() == EstadoPresupuesto.RECHAZADO
                    && !evaluacionTecnicaRepository.existsByPresupuestoRechazado_IdPresupuesto(comp.getIdPresupuesto())) {
                throw new BusinessRuleException(
                        "EVALUACION_TECNICA_REQUERIDA",
                        "Debes registrar la evaluación técnica sobre el presupuesto complementario rechazado antes de reanudar la reparación."
                );
            }
        }

        // Si el último presupuesto complementario emitido fue RECHAZADO y sus elementos son indispensables, bloquear reanudación
        Presupuesto ultimoComplementario = complementarios.get(complementarios.size() - 1);
        if (ultimoComplementario.getEstado() == EstadoPresupuesto.RECHAZADO) {
            EvaluacionTecnica evaluacion = evaluacionTecnicaRepository
                    .findByIdPresupuestoRechazadoWithDetails(ultimoComplementario.getIdPresupuesto())
                    .orElseThrow(() -> new BusinessRuleException(
                            "EVALUACION_TECNICA_REQUERIDA",
                            "Debes registrar la evaluación técnica sobre el presupuesto complementario rechazado."
                    ));

            if (Boolean.TRUE.equals(evaluacion.getElementosIndispensables())) {
                throw new BusinessRuleException(
                        "REANUDACION_BLOQUEADA_ELEMENTOS_INDISPENSABLES",
                        "La reparación no puede reanudarse hasta que se determine cómo proceder: la evaluación técnica confirmó que los elementos rechazados son indispensables."
                );
            }
        }
    }

    private ResumenTiempoEjecucionResponse construirResumenTiempo(
            OrdenServicio orden,
            Presupuesto presupuestoOriginal) {

        List<RegistroTiempo> registros = registroTiempoRepository
                .findAllByIdOrdenWithTecnicoOrderByInicioAsc(orden.getIdOrden());

        LocalDateTime ahora = LocalDateTime.now();
        long segundosAcumulados = 0L;

        List<IntervaloTiempoResponse> intervalosDto = new java.util.ArrayList<>();
        for (RegistroTiempo r : registros) {
            boolean activo = (r.getFechaHoraFin() == null);
            long duracion = activo
                    ? Math.max(0L, Duration.between(r.getFechaHoraInicio(), ahora).getSeconds())
                    : (r.getDuracionSegundos() != null ? r.getDuracionSegundos() : 0L);

            segundosAcumulados += duracion;

            intervalosDto.add(new IntervaloTiempoResponse(
                    r.getIdRegistroTiempo(),
                    r.getTecnico().getIdUsuario(),
                    r.getTecnico().getNombreCompleto(),
                    r.getTipoInicio(),
                    r.getFechaHoraInicio(),
                    r.getTipoFin(),
                    r.getFechaHoraFin(),
                    duracion,
                    activo
            ));
        }

        BigDecimal horasEstimadas = presupuestoOriginal != null && presupuestoOriginal.getHorasEstimadas() != null
                ? presupuestoOriginal.getHorasEstimadas()
                : BigDecimal.ZERO;

        BigDecimal tarifaHora = presupuestoOriginal != null && presupuestoOriginal.getTarifaHoraAprobada() != null
                ? presupuestoOriginal.getTarifaHoraAprobada()
                : BigDecimal.ZERO;

        BigDecimal valorManoObraEstimado = presupuestoOriginal != null && presupuestoOriginal.getValorManoObraEstimado() != null
                ? presupuestoOriginal.getValorManoObraEstimado()
                : BigDecimal.ZERO;

        BigDecimal horasEfectivas = BigDecimal.valueOf(segundosAcumulados)
                .divide(SEGUNDOS_POR_HORA, 2, RoundingMode.HALF_UP);

        BigDecimal diferenciaHoras = horasEfectivas.subtract(horasEstimadas);
        BigDecimal valorManoObraCalculado = MoneyCalculator.calcularManoObraEfectiva(tarifaHora, segundosAcumulados);

        return new ResumenTiempoEjecucionResponse(
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                orden.getEstadoOperativo(),
                segundosAcumulados,
                formatearSegundos(segundosAcumulados),
                horasEstimadas,
                horasEfectivas,
                diferenciaHoras,
                tarifaHora,
                valorManoObraEstimado,
                valorManoObraCalculado,
                intervalosDto
        );
    }

    public static String formatearSegundos(long totalSegundos) {
        long horas = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        long segundos = totalSegundos % 60;
        return String.format("%02d:%02d:%02d", horas, minutos, segundos);
    }
}