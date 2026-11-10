package com.servitec.servitec_backend.inventory.service;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.inventory.dto.ActualizarRepuestoRequest;
import com.servitec.servitec_backend.inventory.dto.CrearRepuestoRequest;
import com.servitec.servitec_backend.inventory.dto.RepuestoResponse;
import com.servitec.servitec_backend.inventory.entity.MovimientoInventario;
import com.servitec.servitec_backend.inventory.entity.Repuesto;
import com.servitec.servitec_backend.inventory.repository.MovimientoInventarioRepository;
import com.servitec.servitec_backend.inventory.repository.RepuestoRepository;
import com.servitec.servitec_backend.inventory.repository.ReservaInventarioRepository;
import com.servitec.servitec_backend.shared.enums.OrigenMovimientoInventario;
import com.servitec.servitec_backend.shared.enums.TipoItemCatalogo;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CatalogService {

    private final RepuestoRepository repuestoRepository;
    private final ReservaInventarioRepository reservaInventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;

    public CatalogService(
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
    public RepuestoResponse crearRepuesto(CrearRepuestoRequest request, Integer idAdminResponsable) {
        String codigoLimpio = request.codigoRepuesto().trim().toUpperCase();
        if (repuestoRepository.existsByCodigoRepuestoIgnoreCase(codigoLimpio)) {
            throw new BusinessRuleException(
                    "CODIGO_REPUESTO_DUPLICADO",
                    "Ya existe un repuesto o insumo registrado con el código: " + codigoLimpio
            );
        }

        Usuario admin = usuarioRepository.findById(idAdminResponsable)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario administrador responsable."));

        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal saldoInicial = request.existenciasIniciales();

        Repuesto repuesto = new Repuesto();
        repuesto.setCodigoRepuesto(codigoLimpio);
        repuesto.setNombre(request.nombre().trim());
        repuesto.setTipoItem(request.tipoItem());
        repuesto.setUnidadMedida(request.unidadMedida().trim());
        repuesto.setDescripcion(request.descripcion().trim());
        repuesto.setImagenUrl(normalizarImagenUrl(request.imagenUrl()));
        repuesto.setPrecioUnitarioActual(request.precioUnitarioActual());
        repuesto.setExistenciasActuales(saldoInicial);
        repuesto.setActivo(Boolean.TRUE);
        repuesto.setFechaActualizacion(ahora);

        Repuesto guardado = repuestoRepository.save(repuesto);

        // REQ-27: Si las existencias iniciales son mayores que cero, registrar movimiento de apertura
        if (saldoInicial.compareTo(BigDecimal.ZERO) > 0) {
            MovimientoInventario apertura = new MovimientoInventario();
            apertura.setRepuesto(guardado);
            apertura.setUsuarioRegistro(admin);
            apertura.setIdDetallePresupuesto(null);
            apertura.setTipoMovimiento(TipoMovimientoInventario.ENTRADA);
            apertura.setOrigenMovimiento(OrigenMovimientoInventario.INICIALIZACION);
            apertura.setCantidadMovimiento(saldoInicial);
            apertura.setCantidadContada(null);
            apertura.setExistenciasAnteriores(BigDecimal.ZERO);
            apertura.setExistenciasResultantes(saldoInicial);
            apertura.setMotivo("Registro inicial de apertura en el catálogo");
            apertura.setFechaHoraMovimiento(ahora);
            movimientoInventarioRepository.save(apertura);
        }

        return mapearAResponse(guardado, BigDecimal.ZERO);
    }

    @Transactional
    public RepuestoResponse actualizarRepuesto(Integer idRepuesto, ActualizarRepuestoRequest request) {
        Repuesto repuesto = obtenerEntidadPorId(idRepuesto);
        String codigoLimpio = request.codigoRepuesto().trim().toUpperCase();

        if (repuestoRepository.existsByCodigoRepuestoIgnoreCaseAndIdRepuestoNot(codigoLimpio, idRepuesto)) {
            throw new BusinessRuleException(
                    "CODIGO_REPUESTO_DUPLICADO",
                    "El código " + codigoLimpio + " ya está asignado a otro elemento del catálogo."
            );
        }

        repuesto.setCodigoRepuesto(codigoLimpio);
        repuesto.setNombre(request.nombre().trim());
        repuesto.setTipoItem(request.tipoItem());
        repuesto.setUnidadMedida(request.unidadMedida().trim());
        repuesto.setDescripcion(request.descripcion().trim());
        repuesto.setImagenUrl(normalizarImagenUrl(request.imagenUrl()));
        repuesto.setPrecioUnitarioActual(request.precioUnitarioActual());
        repuesto.setFechaActualizacion(LocalDateTime.now());

        Repuesto actualizado = repuestoRepository.save(repuesto);
        BigDecimal reservadas = reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(idRepuesto);
        return mapearAResponse(actualizado, reservadas);
    }

    @Transactional
    public RepuestoResponse cambiarEstadoActivo(Integer idRepuesto, boolean nuevoEstadoActivo) {
        Repuesto repuesto = obtenerEntidadPorId(idRepuesto);
        repuesto.setActivo(nuevoEstadoActivo);
        repuesto.setFechaActualizacion(LocalDateTime.now());

        Repuesto actualizado = repuestoRepository.save(repuesto);
        BigDecimal reservadas = reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(idRepuesto);
        return mapearAResponse(actualizado, reservadas);
    }

    @Transactional(readOnly = true)
    public RepuestoResponse consultarPorId(Integer idRepuesto, boolean soloActivos) {
        Repuesto repuesto = obtenerEntidadPorId(idRepuesto);
        if (soloActivos && !Boolean.TRUE.equals(repuesto.getActivo())) {
            throw new BusinessRuleException(
                    "REPUESTO_INACTIVO",
                    "Repuesto no disponible o fuera del catálogo activo."
            );
        }
        BigDecimal reservadas = reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(idRepuesto);
        return mapearAResponse(repuesto, reservadas);
    }

    @Transactional(readOnly = true)
    public List<RepuestoResponse> listarCatalogo(String busqueda, TipoItemCatalogo tipoItem, Boolean activo) {
        return repuestoRepository.buscarCatalogo(busqueda, tipoItem, activo)
                .stream()
                .map(r -> {
                    BigDecimal reservadas = reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(r.getIdRepuesto());
                    return mapearAResponse(r, reservadas);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Repuesto obtenerEntidadActivaParaPresupuesto(Integer idRepuesto) {
        Repuesto repuesto = repuestoRepository.findById(idRepuesto)
                .orElseThrow(() -> new BusinessRuleException(
                        "REPUESTO_NO_DISPONIBLE",
                        "Repuesto no disponible o fuera del catálogo."
                ));
        if (!Boolean.TRUE.equals(repuesto.getActivo())) {
            throw new BusinessRuleException(
                    "REPUESTO_NO_DISPONIBLE",
                    "Repuesto no disponible o fuera del catálogo."
            );
        }
        return repuesto;
    }

    private Repuesto obtenerEntidadPorId(Integer idRepuesto) {
        return repuestoRepository.findById(idRepuesto)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el repuesto o insumo con ID: " + idRepuesto));
    }

    private String normalizarImagenUrl(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return null;
        }
        return imagenUrl.trim();
    }

    private RepuestoResponse mapearAResponse(Repuesto repuesto, BigDecimal cantidadesReservadas) {
        BigDecimal reservadasSeguras = cantidadesReservadas != null ? cantidadesReservadas : BigDecimal.ZERO;
        BigDecimal disponibles = repuesto.getExistenciasActuales().subtract(reservadasSeguras);
        return new RepuestoResponse(
                repuesto.getIdRepuesto(),
                repuesto.getCodigoRepuesto(),
                repuesto.getNombre(),
                repuesto.getTipoItem(),
                repuesto.getUnidadMedida(),
                repuesto.getDescripcion(),
                repuesto.getImagenUrl(),
                repuesto.getPrecioUnitarioActual(),
                repuesto.getExistenciasActuales(),
                reservadasSeguras,
                disponibles,
                repuesto.getActivo(),
                repuesto.getFechaActualizacion()
        );
    }
}