package com.servitec.servitec_backend.servicesummary.service;

import com.servitec.servitec_backend.budgets.dto.PresupuestoResponse;
import com.servitec.servitec_backend.budgets.service.BudgetCreationService;
import com.servitec.servitec_backend.closures.dto.CierreTecnicoResponse;
import com.servitec.servitec_backend.closures.dto.ItemUtilizadoLiquidacionResponse;
import com.servitec.servitec_backend.closures.service.ServiceClosureService;
import com.servitec.servitec_backend.diagnostics.dto.DiagnosticoResponse;
import com.servitec.servitec_backend.diagnostics.service.DiagnosticService;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.servicesummary.dto.ResumenServicioResponse;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import com.servitec.servitec_backend.shared.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ServiceSummaryService {

    private static final String NOTA_LEGAL_ECONOMICA =
            "Este resumen presenta los valores calculados del servicio y no constituye una constancia de pago recibido.";

    private final OrderService orderService;
    private final DiagnosticService diagnosticService;
    private final BudgetCreationService budgetCreationService;
    private final ServiceClosureService serviceClosureService;

    public ServiceSummaryService(
            OrderService orderService,
            DiagnosticService diagnosticService,
            BudgetCreationService budgetCreationService,
            ServiceClosureService serviceClosureService) {
        this.orderService = orderService;
        this.diagnosticService = diagnosticService;
        this.budgetCreationService = budgetCreationService;
        this.serviceClosureService = serviceClosureService;
    }

    /**
     * REQ-18, RN-04, RN-05 y CV-09: Genera la vista de solo lectura del Resumen de Servicio
     * accesible únicamente al Cliente propietario de una orden en estado FINALIZADO.
     */
    @Transactional(readOnly = true)
    public ResumenServicioResponse consultarResumenServicioCliente(
            Integer idOrden,
            Integer idUsuarioAutenticado,
            String rolAutenticado) {

        // REQ-18 y CV-09: Accesible únicamente al Cliente propietario de la orden
        if (!RolNombre.CLIENTE.name().equals(rolAutenticado)) {
            throw new ForbiddenAccessException(
                    "La vista del Resumen de Servicio está disponible exclusivamente para el Cliente propietario de la orden."
            );
        }

        // RN-05: Valida que la orden exista y pertenezca al Cliente autenticado (lanza 403 si pertenece a otro cliente)
        OrdenServicio orden = orderService.obtenerYValidarAccesoOrden(
                idOrden, idUsuarioAutenticado, rolAutenticado
        );

        // CV-09 Criterio 16: Disponible únicamente cuando la orden haya alcanzado el estado FINALIZADO
        if (orden.getEstadoOperativo() != EstadoOperativoOrden.FINALIZADO) {
            throw new InvalidStateTransitionException(
                    "El Resumen de Servicio solo está disponible para órdenes que hayan alcanzado el estado FINALIZADO."
            );
        }

        // 1. Diagnóstico técnico inicial
        DiagnosticoResponse diagnostico = diagnosticService.consultarDiagnosticoPorOrden(
                idOrden, idUsuarioAutenticado, rolAutenticado
        );

        // 2. Presupuesto original aprobado y presupuestos complementarios con sus decisiones
        List<PresupuestoResponse> todosLosPresupuestos = budgetCreationService.listarPresupuestosPorOrden(
                idOrden, idUsuarioAutenticado, rolAutenticado
        );

        PresupuestoResponse presupuestoOriginal = todosLosPresupuestos.stream()
                .filter(p -> p.tipoPresupuesto() == TipoPresupuesto.ORIGINAL)
                .findFirst()
                .orElse(null);

        List<PresupuestoResponse> presupuestosComplementarios = todosLosPresupuestos.stream()
                .filter(p -> p.tipoPresupuesto() == TipoPresupuesto.COMPLEMENTARIO)
                .toList();

        // 3. Cierre técnico, tiempos efectivos, mano de obra y repuestos utilizados
        CierreTecnicoResponse cierre = serviceClosureService.consultarCierrePorOrden(
                idOrden, idUsuarioAutenticado, rolAutenticado
        );

        // Filtrar para la lista económica principal exclusivamente los elementos efectivamente utilizados (> 0)
        List<ItemUtilizadoLiquidacionResponse> elementosEfectivamenteUtilizados = cierre.elementosAutorizados()
                .stream()
                .filter(item -> item.cantidadUtilizada() != null
                        && item.cantidadUtilizada().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal valorManoObraEstimado = presupuestoOriginal != null && presupuestoOriginal.valorManoObraEstimado() != null
                ? presupuestoOriginal.valorManoObraEstimado()
                : BigDecimal.ZERO;

        return new ResumenServicioResponse(
                orden.getIdOrden(),
                orden.getCodigoOrden(),
                orden.getEstadoOperativo(),
                true,
                orden.getCategoria().getIdCategoria(),
                orden.getCategoria().getNombre(),
                orden.getDireccionServicio(),
                orden.getFechaSolicitud(),
                cierre.fechaCierre(),
                orden.getCliente().getIdUsuario(),
                orden.getCliente().getNombreCompleto(),
                cierre.idTecnico(),
                cierre.nombreTecnico(),
                orden.getDescripcionProblema(),
                diagnostico,
                presupuestoOriginal,
                presupuestosComplementarios,
                cierre.segundosEfectivosAcumulados(),
                cierre.tiempoEfectivoFormateado(),
                cierre.horasEstimadas(),
                cierre.horasEfectivas(),
                cierre.diferenciaHoras(),
                cierre.tarifaHoraAprobada(),
                valorManoObraEstimado,
                cierre.valorManoObraCalculado(),
                elementosEfectivamenteUtilizados,
                cierre.elementosAutorizados(),
                cierre.valorRepuestosUtilizados(),
                cierre.valorTotalCalculado(),
                NOTA_LEGAL_ECONOMICA,
                cierre.pruebasFuncionamiento(),
                cierre.recomendaciones(),
                cierre.garantiaAlcance(),
                cierre.garantiaVigencia()
        );
    }
}