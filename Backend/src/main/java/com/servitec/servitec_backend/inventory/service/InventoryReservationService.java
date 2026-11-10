package com.servitec.servitec_backend.inventory.service;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.inventory.dto.ConsumoReservaItem;
import com.servitec.servitec_backend.inventory.dto.SolicitudReservaItem;
import com.servitec.servitec_backend.inventory.entity.MovimientoInventario;
import com.servitec.servitec_backend.inventory.entity.Repuesto;
import com.servitec.servitec_backend.inventory.entity.ReservaInventario;
import com.servitec.servitec_backend.inventory.repository.MovimientoInventarioRepository;
import com.servitec.servitec_backend.inventory.repository.RepuestoRepository;
import com.servitec.servitec_backend.inventory.repository.ReservaInventarioRepository;
import com.servitec.servitec_backend.shared.api.ApiError.ItemSinDisponibilidad;
import com.servitec.servitec_backend.shared.enums.EstadoReserva;
import com.servitec.servitec_backend.shared.enums.OrigenMovimientoInventario;
import com.servitec.servitec_backend.shared.enums.TipoCierreReserva;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InsufficientStockException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryReservationService {

    private final RepuestoRepository repuestoRepository;
    private final ReservaInventarioRepository reservaInventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;

    public InventoryReservationService(
            RepuestoRepository repuestoRepository,
            ReservaInventarioRepository reservaInventarioRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            UsuarioRepository usuarioRepository) {
        this.repuestoRepository = repuestoRepository;
        this.reservaInventarioRepository = reservaInventarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * REQ-13, REQ-22, RN-14: Verifica disponibilidad total y crea reservas activas en la misma transacción.
     * Si algún elemento no tiene disponibilidad suficiente, lanza InsufficientStockException con la lista completa
     * de faltantes, sin crear reservas parciales.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void verificarYReservarDetalles(List<SolicitudReservaItem> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }

        // Agrupar cantidades requeridas por idRepuesto en caso de múltiples detalles
        Map<Integer, BigDecimal> cantidadRequeridaPorRepuesto = new LinkedHashMap<>();
        for (SolicitudReservaItem item : detalles) {
            if (reservaInventarioRepository.existsByIdDetallePresupuesto(item.idDetallePresupuesto())) {
                throw new BusinessRuleException("RESERVA_DUPLICADA", "El detalle del presupuesto ya cuenta con una reserva registrada.");
            }
            cantidadRequeridaPorRepuesto.merge(item.idRepuesto(), item.cantidadAutorizada(), BigDecimal::add);
        }

        // Bloquear todos los repuestos en orden ascendente de ID para evitar deadlocks
        List<Integer> idsOrdenados = cantidadRequeridaPorRepuesto.keySet().stream().sorted().toList();
        Map<Integer, Repuesto> repuestosBloqueados = repuestoRepository.findAllByIdsWithLockOrderById(idsOrdenados)
                .stream()
                .collect(Collectors.toMap(Repuesto::getIdRepuesto, Function.identity()));

        List<ItemSinDisponibilidad> faltantes = new ArrayList<>();

        for (Integer idRepuesto : idsOrdenados) {
            Repuesto repuesto = repuestosBloqueados.get(idRepuesto);
            if (repuesto == null) {
                throw new ResourceNotFoundException("No se encontró el repuesto con ID: " + idRepuesto);
            }
            BigDecimal reservadasActivas = reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(idRepuesto);
            BigDecimal disponible = repuesto.getExistenciasActuales().subtract(reservadasActivas);
            BigDecimal solicitada = cantidadRequeridaPorRepuesto.get(idRepuesto);

            if (!Boolean.TRUE.equals(repuesto.getActivo()) || disponible.compareTo(solicitada) < 0) {
                faltantes.add(new ItemSinDisponibilidad(
                        repuesto.getIdRepuesto(),
                        repuesto.getCodigoRepuesto(),
                        repuesto.getNombre(),
                        solicitada,
                        disponible.max(BigDecimal.ZERO)
                ));
            }
        }

        if (!faltantes.isEmpty()) {
            throw new InsufficientStockException(
                    "No es posible aprobar el presupuesto porque uno o más elementos no cuentan con disponibilidad suficiente en inventario.",
                    faltantes
            );
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (SolicitudReservaItem item : detalles) {
            ReservaInventario reserva = new ReservaInventario();
            reserva.setIdDetallePresupuesto(item.idDetallePresupuesto());
            reserva.setCantidadReservada(item.cantidadAutorizada());
            reserva.setEstadoReserva(EstadoReserva.ACTIVA);
            reserva.setTipoCierre(null);
            reserva.setFechaHoraReserva(ahora);
            reserva.setFechaHoraCierre(null);
            reservaInventarioRepository.save(reserva);
        }
    }

    /**
     * REQ-14: Verifica que todos los detalles aprobados de la orden tengan su reserva ACTIVA antes de iniciar ejecución.
     */
    @Transactional(readOnly = true)
    public void validarReservasActivasParaEjecucion(List<Integer> idsDetallesPresupuestoOriginal) {
        if (idsDetallesPresupuestoOriginal == null || idsDetallesPresupuestoOriginal.isEmpty()) {
            return;
        }
        for (Integer idDetalle : idsDetallesPresupuestoOriginal) {
            ReservaInventario reserva = reservaInventarioRepository.findByIdDetallePresupuesto(idDetalle)
                    .orElseThrow(() -> new BusinessRuleException(
                            "RESERVA_FALTANTE",
                            "La orden no cuenta con la reserva activa requerida para iniciar la reparación."
                    ));
            if (reserva.getEstadoReserva() != EstadoReserva.ACTIVA) {
                throw new BusinessRuleException(
                        "RESERVA_NO_ACTIVA",
                        "La reserva asociada al detalle del presupuesto no se encuentra activa."
                );
            }
        }
    }

    /**
     * REQ-17, REQ-25, RN-14: Al cerrar o cancelar una orden, descuenta de las existencias físicas únicamente
     * las cantidades utilizadas, registra las salidas automáticas de origen RN14 y cierra las reservas liberando
     * automáticamente las cantidades no utilizadas.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumirYLiberarReservas(
            List<ConsumoReservaItem> consumos,
            TipoCierreReserva tipoCierre,
            Integer idUsuarioResponsable,
            String codigoOrden) {

        if (consumos == null || consumos.isEmpty()) {
            return;
        }

        Usuario responsable = usuarioRepository.findById(idUsuarioResponsable)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario responsable de la operación."));

        List<Integer> idsDetalle = consumos.stream().map(ConsumoReservaItem::idDetallePresupuesto).toList();
        Map<Integer, ReservaInventario> reservasPorDetalle = reservaInventarioRepository
                .findAllByIdDetallePresupuestoInWithLock(idsDetalle)
                .stream()
                .collect(Collectors.toMap(ReservaInventario::getIdDetallePresupuesto, Function.identity()));

        List<Integer> idsRepuestosOrdenados = consumos.stream()
                .map(ConsumoReservaItem::idRepuesto)
                .distinct()
                .sorted()
                .toList();

        Map<Integer, Repuesto> repuestosBloqueados = repuestoRepository
                .findAllByIdsWithLockOrderById(idsRepuestosOrdenados)
                .stream()
                .collect(Collectors.toMap(Repuesto::getIdRepuesto, Function.identity()));

        LocalDateTime ahora = LocalDateTime.now();

        for (ConsumoReservaItem consumo : consumos) {
            ReservaInventario reserva = reservasPorDetalle.get(consumo.idDetallePresupuesto());
            if (reserva == null) {
                throw new BusinessRuleException(
                        "RESERVA_INEXISTENTE",
                        "No existe una reserva asociada al detalle de presupuesto ID: " + consumo.idDetallePresupuesto()
                );
            }
            if (reserva.getEstadoReserva() != EstadoReserva.ACTIVA) {
                throw new BusinessRuleException(
                        "RESERVA_YA_PROCESADA",
                        "La reserva del detalle ID " + consumo.idDetallePresupuesto() + " ya fue cerrada o liberada previamente."
                );
            }

            BigDecimal utilizada = consumo.cantidadUtilizada();
            if (utilizada == null || utilizada.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("CANTIDAD_UTILIZADA_INVALIDA", "La cantidad utilizada no puede ser negativa.");
            }
            if (utilizada.compareTo(reserva.getCantidadReservada()) > 0) {
                throw new BusinessRuleException(
                        "CANTIDAD_SUPERA_RESERVA",
                        "La cantidad utilizada no puede superar la cantidad autorizada y reservada."
                );
            }

            // Si se utilizó una cantidad > 0, descontar físicamente y registrar salida automática RN14
            if (utilizada.compareTo(BigDecimal.ZERO) > 0) {
                Repuesto repuesto = repuestosBloqueados.get(consumo.idRepuesto());
                if (repuesto == null) {
                    throw new ResourceNotFoundException("No se encontró el repuesto ID: " + consumo.idRepuesto());
                }

                BigDecimal saldoAnterior = repuesto.getExistenciasActuales();
                BigDecimal saldoResultante = saldoAnterior.subtract(utilizada);
                if (saldoResultante.compareTo(BigDecimal.ZERO) < 0) {
                    throw new InsufficientStockException("Las existencias físicas son insuficientes para descontar el consumo.");
                }

                repuesto.setExistenciasActuales(saldoResultante);
                repuesto.setFechaActualizacion(ahora);
                repuestoRepository.save(repuesto);

                MovimientoInventario salidaAutomatica = new MovimientoInventario();
                salidaAutomatica.setRepuesto(repuesto);
                salidaAutomatica.setUsuarioRegistro(responsable);
                salidaAutomatica.setIdDetallePresupuesto(consumo.idDetallePresupuesto());
                salidaAutomatica.setTipoMovimiento(TipoMovimientoInventario.SALIDA);
                salidaAutomatica.setOrigenMovimiento(OrigenMovimientoInventario.RN14);
                salidaAutomatica.setCantidadMovimiento(utilizada);
                salidaAutomatica.setCantidadContada(null);
                salidaAutomatica.setExistenciasAnteriores(saldoAnterior);
                salidaAutomatica.setExistenciasResultantes(saldoResultante);
                salidaAutomatica.setMotivo(
                        (tipoCierre == TipoCierreReserva.CIERRE_SERVICIO
                                ? "Consumo efectivo por cierre técnico de orden "
                                : "Consumo efectivo por cancelación de orden ") + codigoOrden
                );
                salidaAutomatica.setFechaHoraMovimiento(ahora);
                movimientoInventarioRepository.save(salidaAutomatica);
            }

            // Cerrar la reserva (libera automáticamente cualquier remanente no utilizado sin crear entrada física)
            reserva.setEstadoReserva(EstadoReserva.CERRADA);
            reserva.setTipoCierre(tipoCierre);
            reserva.setFechaHoraCierre(ahora);
            reservaInventarioRepository.save(reserva);
        }
    }
}