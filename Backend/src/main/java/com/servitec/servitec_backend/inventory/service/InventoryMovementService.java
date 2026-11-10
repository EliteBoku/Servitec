package com.servitec.servitec_backend.inventory.service;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.inventory.dto.MovimientoInventarioResponse;
import com.servitec.servitec_backend.inventory.dto.RegistrarMovimientoManualRequest;
import com.servitec.servitec_backend.inventory.entity.MovimientoInventario;
import com.servitec.servitec_backend.inventory.entity.Repuesto;
import com.servitec.servitec_backend.inventory.repository.MovimientoInventarioRepository;
import com.servitec.servitec_backend.inventory.repository.RepuestoRepository;
import com.servitec.servitec_backend.inventory.repository.ReservaInventarioRepository;
import com.servitec.servitec_backend.shared.enums.OrigenMovimientoInventario;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InsufficientStockException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryMovementService {

    private final RepuestoRepository repuestoRepository;
    private final ReservaInventarioRepository reservaInventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;

    public InventoryMovementService(
            RepuestoRepository repuestoRepository,
            ReservaInventarioRepository reservaInventarioRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            UsuarioRepository usuarioRepository) {
        this.repuestoRepository = repuestoRepository;
        this.reservaInventarioRepository = reservaInventarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public MovimientoInventarioResponse registrarMovimientoManual(
            RegistrarMovimientoManualRequest request,
            Integer idAdminResponsable) {

        Usuario admin = usuarioRepository.findById(idAdminResponsable)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario administrador responsable."));

        // Bloqueo pesimista sobre el repuesto para garantizar consistencia ante concurrencia (REQ-28 / RN-15)
        Repuesto repuesto = repuestoRepository.findByIdWithLock(request.idRepuesto())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el repuesto o insumo con ID: " + request.idRepuesto()));

        BigDecimal existenciasAnteriores = repuesto.getExistenciasActuales();
        BigDecimal reservasActivas = reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(repuesto.getIdRepuesto());
        BigDecimal disponibles = existenciasAnteriores.subtract(reservasActivas);

        BigDecimal valorIngresado = request.cantidad();
        BigDecimal cantidadMovimiento;
        BigDecimal cantidadContada = null;
        BigDecimal existenciasResultantes;

        switch (request.tipoMovimiento()) {
            case ENTRADA -> {
                if (valorIngresado.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("CANTIDAD_INVALIDA", "La cantidad de una entrada debe ser mayor que cero.");
                }
                cantidadMovimiento = valorIngresado;
                existenciasResultantes = existenciasAnteriores.add(valorIngresado);
            }
            case SALIDA -> {
                if (valorIngresado.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("CANTIDAD_INVALIDA", "La cantidad de una salida debe ser mayor que cero.");
                }
                if (valorIngresado.compareTo(disponibles) > 0) {
                    throw new InsufficientStockException(
                            String.format(
                                    "La salida solicitada (%s) supera las existencias disponibles (%s). Existencias físicas: %s, Reservas activas comprometidas: %s.",
                                    valorIngresado.stripTrailingZeros().toPlainString(),
                                    disponibles.stripTrailingZeros().toPlainString(),
                                    existenciasAnteriores.stripTrailingZeros().toPlainString(),
                                    reservasActivas.stripTrailingZeros().toPlainString()
                            )
                    );
                }
                cantidadMovimiento = valorIngresado;
                existenciasResultantes = existenciasAnteriores.subtract(valorIngresado);
            }
            case AJUSTE -> {
                if (valorIngresado.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessRuleException("CONTEO_INVALIDO", "El conteo físico no puede ser negativo.");
                }
                if (valorIngresado.compareTo(existenciasAnteriores) == 0) {
                    throw new BusinessRuleException(
                            "SIN_DIFERENCIA_AJUSTE",
                            "La cantidad física ingresada coincide con las existencias registradas; no hay diferencias por corregir."
                    );
                }
                if (valorIngresado.compareTo(reservasActivas) < 0) {
                    throw new InsufficientStockException(
                            String.format(
                                    "No es posible confirmar el ajuste: el conteo físico (%s) es inferior a las cantidades comprometidas en reservas activas (%s).",
                                    valorIngresado.stripTrailingZeros().toPlainString(),
                                    reservasActivas.stripTrailingZeros().toPlainString()
                            )
                    );
                }
                cantidadContada = valorIngresado;
                cantidadMovimiento = valorIngresado.subtract(existenciasAnteriores).abs();
                existenciasResultantes = valorIngresado;
            }
            default -> throw new BusinessRuleException("TIPO_MOVIMIENTO_INVALIDO", "Tipo de movimiento no soportado.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        repuesto.setExistenciasActuales(existenciasResultantes);
        repuesto.setFechaActualizacion(ahora);
        repuestoRepository.save(repuesto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setRepuesto(repuesto);
        movimiento.setUsuarioRegistro(admin);
        movimiento.setIdDetallePresupuesto(null);
        movimiento.setTipoMovimiento(request.tipoMovimiento());
        movimiento.setOrigenMovimiento(OrigenMovimientoInventario.MANUAL);
        movimiento.setCantidadMovimiento(cantidadMovimiento);
        movimiento.setCantidadContada(cantidadContada);
        movimiento.setExistenciasAnteriores(existenciasAnteriores);
        movimiento.setExistenciasResultantes(existenciasResultantes);
        movimiento.setMotivo(request.motivo().trim());
        movimiento.setFechaHoraMovimiento(ahora);

        MovimientoInventario guardado = movimientoInventarioRepository.save(movimiento);
        BigDecimal nuevasDisponibles = existenciasResultantes.subtract(reservasActivas);

        return mapearAResponse(guardado, reservasActivas, nuevasDisponibles);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> consultarHistorial(
            Integer idRepuesto,
            TipoMovimientoInventario tipoMovimiento,
            OrigenMovimientoInventario origenMovimiento,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {

        return movimientoInventarioRepository.buscarHistorialMovimientos(
                        idRepuesto, tipoMovimiento, origenMovimiento, fechaDesde, fechaHasta)
                .stream()
                .map(m -> {
                    BigDecimal reservadas = reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(
                            m.getRepuesto().getIdRepuesto());
                    BigDecimal disponibles = m.getRepuesto().getExistenciasActuales().subtract(reservadas);
                    return mapearAResponse(m, reservadas, disponibles);
                })
                .toList();
    }

    private MovimientoInventarioResponse mapearAResponse(
            MovimientoInventario m,
            BigDecimal reservasActivas,
            BigDecimal disponiblesActuales) {

        return new MovimientoInventarioResponse(
                m.getIdMovimiento(),
                m.getRepuesto().getIdRepuesto(),
                m.getRepuesto().getCodigoRepuesto(),
                m.getRepuesto().getNombre(),
                m.getRepuesto().getUnidadMedida(),
                m.getTipoMovimiento(),
                m.getOrigenMovimiento(),
                m.getCantidadMovimiento(),
                m.getCantidadContada(),
                m.getExistenciasAnteriores(),
                m.getExistenciasResultantes(),
                reservasActivas,
                disponiblesActuales,
                m.getMotivo(),
                m.getFechaHoraMovimiento(),
                m.getUsuarioRegistro().getIdUsuario(),
                m.getUsuarioRegistro().getNombreCompleto(),
                m.getIdDetallePresupuesto()
        );
    }
}