package com.servitec.servitec_backend.orders.service;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.orders.dto.CategoriaEquipoResponse;
import com.servitec.servitec_backend.orders.dto.CrearOrdenServicioRequest;
import com.servitec.servitec_backend.orders.dto.EstadoConsentimientoResponse;
import com.servitec.servitec_backend.orders.dto.HistorialEstadoOrdenResponse;
import com.servitec.servitec_backend.orders.dto.OrdenServicioResponse;
import com.servitec.servitec_backend.orders.entity.CategoriaEquipo;
import com.servitec.servitec_backend.orders.entity.ConsentimientoDatos;
import com.servitec.servitec_backend.orders.entity.HistorialEstadoOrden;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.repository.CategoriaEquipoRepository;
import com.servitec.servitec_backend.orders.repository.ConsentimientoDatosRepository;
import com.servitec.servitec_backend.orders.repository.HistorialEstadoOrdenRepository;
import com.servitec.servitec_backend.orders.repository.OrdenServicioRepository;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import com.servitec.servitec_backend.shared.exception.ResourceNotFoundException;
import com.servitec.servitec_backend.shared.util.OrderCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final String VERSION_POLITICA_DATOS = "v1.0-2026";

    private final CategoriaEquipoRepository categoriaEquipoRepository;
    private final OrdenServicioRepository ordenServicioRepository;
    private final ConsentimientoDatosRepository consentimientoDatosRepository;
    private final HistorialEstadoOrdenRepository historialEstadoOrdenRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrderStateTransitionService orderStateTransitionService;

    public OrderService(
            CategoriaEquipoRepository categoriaEquipoRepository,
            OrdenServicioRepository ordenServicioRepository,
            ConsentimientoDatosRepository consentimientoDatosRepository,
            HistorialEstadoOrdenRepository historialEstadoOrdenRepository,
            UsuarioRepository usuarioRepository,
            OrderStateTransitionService orderStateTransitionService) {
        this.categoriaEquipoRepository = categoriaEquipoRepository;
        this.ordenServicioRepository = ordenServicioRepository;
        this.consentimientoDatosRepository = consentimientoDatosRepository;
        this.historialEstadoOrdenRepository = historialEstadoOrdenRepository;
        this.usuarioRepository = usuarioRepository;
        this.orderStateTransitionService = orderStateTransitionService;
    }

    @Transactional(readOnly = true)
    public List<CategoriaEquipoResponse> listarCategoriasActivas() {
        return categoriaEquipoRepository.findByActivaTrueOrderByNombreAsc()
                .stream()
                .map(c -> new CategoriaEquipoResponse(c.getIdCategoria(), c.getNombre(), c.getActiva()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EstadoConsentimientoResponse consultarEstadoConsentimiento(Integer idCliente) {
        Optional<ConsentimientoDatos> consentimientoOpt = consentimientoDatosRepository.findByCliente_IdUsuario(idCliente);
        if (consentimientoOpt.isPresent()) {
            ConsentimientoDatos c = consentimientoOpt.get();
            return new EstadoConsentimientoResponse(false, Boolean.TRUE.equals(c.getAutorizado()), c.getFechaHora(), c.getVersionPolitica());
        }
        return new EstadoConsentimientoResponse(true, false, null, VERSION_POLITICA_DATOS);
    }

    @Transactional
    public OrdenServicioResponse crearSolicitudServicio(CrearOrdenServicioRequest request, Integer idClienteAutenticado) {
        Usuario cliente = usuarioRepository.findById(idClienteAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cliente autenticado."));

        CategoriaEquipo categoria = categoriaEquipoRepository.findByIdCategoriaAndActivaTrue(request.idCategoria())
                .orElseThrow(() -> new BusinessRuleException(
                        "CATEGORIA_NO_DISPONIBLE",
                        "La categoría de equipo seleccionada no existe o no se encuentra activa."
                ));

        boolean yaTieneConsentimiento = consentimientoDatosRepository.existsByCliente_IdUsuario(idClienteAutenticado);

        // SRS Sección 11.2 y CV-02: En la primera solicitud es obligatorio autorizar el tratamiento de datos personales
        if (!yaTieneConsentimiento && !Boolean.TRUE.equals(request.autorizaTratamientoDatos())) {
            throw new BusinessRuleException(
                    "CONSENTIMIENTO_DATOS_OBLIGATORIO",
                    "Debes autorizar el tratamiento de tus datos personales para registrar tu primera solicitud."
            );
        }

        LocalDateTime ahora = LocalDateTime.now();

        OrdenServicio nuevaOrden = new OrdenServicio();
        nuevaOrden.setCodigoOrden(OrderCodeGenerator.generarCodigoTemporal());
        nuevaOrden.setCliente(cliente);
        nuevaOrden.setTecnicoAsignado(null);
        nuevaOrden.setCategoria(categoria);
        nuevaOrden.setDescripcionProblema(request.descripcionProblema().trim());
        nuevaOrden.setDireccionServicio(request.direccionServicio().trim());
        nuevaOrden.setEstadoOperativo(EstadoOperativoOrden.SOLICITADO);
        nuevaOrden.setFechaSolicitud(ahora);
        nuevaOrden.setFechaEstimada(null);
        nuevaOrden.setFechaAsignacion(null);

        OrdenServicio guardada = ordenServicioRepository.save(nuevaOrden);

        // Asignar código definitivo OS-AAAA-XXXX basado en el ID generado
        String codigoDefinitivo = OrderCodeGenerator.formatearCodigoOrden(
                OrderCodeGenerator.anioActual(),
                guardada.getIdOrden()
        );
        guardada.setCodigoOrden(codigoDefinitivo);
        guardada = ordenServicioRepository.save(guardada);

        // Registrar consentimiento si es la primera solicitud del cliente
        if (!yaTieneConsentimiento) {
            ConsentimientoDatos consentimiento = new ConsentimientoDatos();
            consentimiento.setCliente(cliente);
            consentimiento.setOrdenPrimeraSolicitud(guardada);
            consentimiento.setAutorizado(Boolean.TRUE);
            consentimiento.setFechaHora(ahora);
            consentimiento.setVersionPolitica(VERSION_POLITICA_DATOS);
            consentimientoDatosRepository.save(consentimiento);
        }

        // Registrar transición inicial en HISTORIAL_ESTADO_ORDEN
        orderStateTransitionService.registrarCreacionInicial(guardada, cliente, ahora);

        return mapearAResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<OrdenServicioResponse> listarOrdenesSegunRol(
            Integer idUsuarioActual,
            String rolActual,
            EstadoOperativoOrden estado,
            Integer idCategoria,
            Integer idTecnicoFiltro,
            String busqueda) {

        List<OrdenServicio> ordenes;
        if (RolNombre.CLIENTE.name().equals(rolActual)) {
            ordenes = ordenServicioRepository.findAllByClienteId(idUsuarioActual, estado);
        } else if (RolNombre.TECNICO_ESPECIALISTA.name().equals(rolActual)) {
            ordenes = ordenServicioRepository.findAllByTecnicoAsignadoId(idUsuarioActual, estado);
        } else if (RolNombre.ADMINISTRADOR.name().equals(rolActual)) {
            ordenes = ordenServicioRepository.buscarOrdenesAdmin(estado, idCategoria, idTecnicoFiltro, busqueda);
        } else {
            throw new ForbiddenAccessException("Rol no autorizado para consultar órdenes de servicio.");
        }

        return ordenes.stream().map(this::mapearAResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrdenServicioResponse consultarDetalleOrden(Integer idOrden, Integer idUsuarioActual, String rolActual) {
        OrdenServicio orden = obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);
        return mapearAResponse(orden);
    }

    @Transactional(readOnly = true)
    public List<HistorialEstadoOrdenResponse> consultarHistorialEstados(
            Integer idOrden,
            Integer idUsuarioActual,
            String rolActual) {

        obtenerYValidarAccesoOrden(idOrden, idUsuarioActual, rolActual);
        return historialEstadoOrdenRepository.findHistorialPorOrden(idOrden)
                .stream()
                .map(this::mapearHistorialAResponse)
                .toList();
    }

    /**
     * RN-05: Valida que un Cliente solo acceda a sus propias órdenes y un Técnico solo a las que tiene asignadas.
     */
    @Transactional(readOnly = true)
    public OrdenServicio obtenerYValidarAccesoOrden(Integer idOrden, Integer idUsuarioActual, String rolActual) {
        OrdenServicio orden = ordenServicioRepository.findByIdWithDetails(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de servicio con ID: " + idOrden));

        if (RolNombre.CLIENTE.name().equals(rolActual)) {
            if (!orden.getCliente().getIdUsuario().equals(idUsuarioActual)) {
                throw new ForbiddenAccessException("No tienes permiso para acceder a órdenes de servicio de otros clientes.");
            }
        } else if (RolNombre.TECNICO_ESPECIALISTA.name().equals(rolActual)) {
            if (orden.getTecnicoAsignado() == null
                    || !orden.getTecnicoAsignado().getIdUsuario().equals(idUsuarioActual)) {
                throw new ForbiddenAccessException("Solo puedes consultar o gestionar las órdenes de servicio asignadas a tu usuario.");
            }
        } else if (!RolNombre.ADMINISTRADOR.name().equals(rolActual)) {
            throw new ForbiddenAccessException("No tienes permisos para acceder a esta orden de servicio.");
        }

        return orden;
    }

    public OrdenServicioResponse mapearAResponse(OrdenServicio o) {
        Usuario tecnico = o.getTecnicoAsignado();
        return new OrdenServicioResponse(
                o.getIdOrden(),
                o.getCodigoOrden(),
                o.getCliente().getIdUsuario(),
                o.getCliente().getNombreCompleto(),
                o.getCliente().getCorreo(),
                tecnico != null ? tecnico.getIdUsuario() : null,
                tecnico != null ? tecnico.getNombreCompleto() : null,
                o.getCategoria().getIdCategoria(),
                o.getCategoria().getNombre(),
                o.getDescripcionProblema(),
                o.getDireccionServicio(),
                o.getEstadoOperativo(),
                o.getFechaSolicitud(),
                o.getFechaEstimada(),
                o.getFechaAsignacion()
        );
    }

    private HistorialEstadoOrdenResponse mapearHistorialAResponse(HistorialEstadoOrden h) {
        return new HistorialEstadoOrdenResponse(
                h.getIdHistorialEstado(),
                h.getOrdenServicio().getIdOrden(),
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getJustificacion(),
                h.getFechaHoraCambio(),
                h.getUsuarioResponsable().getIdUsuario(),
                h.getUsuarioResponsable().getNombreCompleto(),
                h.getIdDecisionPresupuesto()
        );
    }
}