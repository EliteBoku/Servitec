import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  MapPin,
  User,
  Calendar,
  Wrench,
  History,
  RefreshCw,
} from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { diagnosticsApi } from '../../../api/diagnosticsApi';
import { budgetsApi } from '../../../api/budgetsApi';
import { executionApi } from '../../../api/executionApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';
import OrderTimelineModal from '../components/OrderTimelineModal';
import DiagnosticFormCard from '../../diagnostics/components/DiagnosticFormCard';
import DiagnosticReadOnlyCard from '../../diagnostics/components/DiagnosticReadOnlyCard';
import CreateOriginalBudgetForm from '../../budgets/components/CreateOriginalBudgetForm';
import CreateComplementaryForm from '../../budgets/components/CreateComplementaryForm';
import TechnicalEvaluationForm from '../../budgets/components/TechnicalEvaluationForm';
import ClientBudgetDecisionCard from '../../budgets/components/ClientBudgetDecisionCard';
import ExecutionControlPanel from '../../execution/components/ExecutionControlPanel';

export default function TechOrderWorkPage() {
  const params = useParams();
  const navigate = useNavigate();

  const rawOrderId = params.idOrden ?? params.id;
  const isValidOrderId =
    rawOrderId !== undefined &&
    rawOrderId !== null &&
    rawOrderId !== 'undefined' &&
    rawOrderId !== 'null' &&
    !isNaN(Number(rawOrderId));

  const idOrden = isValidOrderId ? Number(rawOrderId) : null;

  const [order, setOrder] = useState(null);
  const [diagnostic, setDiagnostic] = useState(null);
  const [budgets, setBudgets] = useState([]);
  const [timeSummary, setTimeSummary] = useState(null);
  const [evaluations, setEvaluations] = useState([]);
  const [authorizations, setAuthorizations] = useState([]);

  const [loading, setLoading] = useState(true);
  const [errorInfo, setErrorInfo] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [isTimelineOpen, setIsTimelineOpen] = useState(false);

  const cargarExpedienteTecnico = useCallback(async () => {
    if (!idOrden) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setErrorInfo(null);
    try {
      const [
        ordenData,
        diagData,
        budgetsData,
        timeData,
        evalsData,
        authsData,
      ] = await Promise.all([
        ordersApi.consultarDetalleOrden(idOrden),
        diagnosticsApi.consultarDiagnosticoPorOrden(idOrden),
        budgetsApi.listarPresupuestosPorOrden(idOrden),
        executionApi.consultarResumenTiempo(idOrden),
        budgetsApi.listarEvaluacionesPorOrden(idOrden),
        budgetsApi.listarAutorizacionesPorOrden(idOrden),
      ]);
      setOrder(ordenData);
      setDiagnostic(diagData);
      setBudgets(budgetsData);
      setTimeSummary(timeData);
      setEvaluations(evalsData);
      setAuthorizations(authsData);
    } catch (err) {
      setErrorInfo(
        extractApiError(
          err,
          'No fue posible cargar la información de la orden de servicio.'
        )
      );
    } finally {
      setLoading(false);
    }
  }, [idOrden]);

  useEffect(() => {
    if (!isValidOrderId) {
      navigate('/tecnico/ordenes', { replace: true });
      return;
    }
    cargarExpedienteTecnico();
  }, [isValidOrderId, cargarExpedienteTecnico, navigate]);

  const handleDiagnosticSaved = (diagRegistrado) => {
    setDiagnostic(diagRegistrado);
    setOrder((prev) =>
      prev
        ? {
            ...prev,
            estadoOperativo:
              diagRegistrado.estadoOrdenActualizado || 'EN_DIAGNOSTICO',
          }
        : prev
    );
    setSuccessMessage(
      'Diagnóstico registrado correctamente. Ya puedes elaborar el presupuesto original.'
    );
  };

  const handleOriginalBudgetCreated = (nuevoPresupuesto) => {
    setBudgets((prev) => [...prev, nuevoPresupuesto]);
    setOrder((prev) =>
      prev
        ? {
            ...prev,
            estadoOperativo:
              nuevoPresupuesto.estadoOperativoOrden || 'PRESUPUESTADO',
          }
        : prev
    );
    setSuccessMessage(
      `Presupuesto ${nuevoPresupuesto.codigoPresupuesto} generado correctamente.`
    );
    cargarExpedienteTecnico();
  };

  const handleTimeUpdated = (nuevoResumenTiempo, mensaje) => {
    setTimeSummary(nuevoResumenTiempo);
    if (nuevoResumenTiempo?.estadoOperativo) {
      setOrder((prev) =>
        prev ? { ...prev, estadoOperativo: nuevoResumenTiempo.estadoOperativo } : prev
      );
    }
    setSuccessMessage(mensaje);
  };

  const handleComplementaryCreated = (nuevoComp) => {
    setSuccessMessage(
      `Presupuesto complementario ${nuevoComp.codigoPresupuesto} emitido y notificado por correo al Cliente.`
    );
    cargarExpedienteTecnico();
  };

  const handleEvaluationSaved = (evalGuardada) => {
    setSuccessMessage(
      evalGuardada.elementosIndispensables
        ? 'Evaluación técnica registrada: los elementos fueron marcados como indispensables. La orden aguarda autorización o decisión del Administrador.'
        : 'Evaluación técnica registrada: los elementos no son indispensables. Ya puedes reanudar la reparación.'
    );
    cargarExpedienteTecnico();
  };

  // Análisis de estado presupuestario para habilitar/bloquear acciones en PAUSADA (RN-08 y RN-09)
  const estadoFlujoComplementarios = useMemo(() => {
    const original = budgets.find((b) => b.tipoPresupuesto === 'ORIGINAL');
    const complementarios = budgets.filter(
      (b) => b.tipoPresupuesto === 'COMPLEMENTARIO'
    );
    const hayPendiente = complementarios.some(
      (b) => b.estado === 'PENDIENTE_APROBACION'
    );

    const idsEvaluados = new Set(
      evaluations.map((e) => e.idPresupuestoRechazado)
    );
    const rechazadoSinEvaluar = complementarios.find(
      (b) => b.estado === 'RECHAZADO' && !idsEvaluados.has(b.idPresupuesto)
    );

    const autorizacionDisponible = authorizations.find(
      (a) => !a.utilizadaEnNuevaPropuesta
    );

    let motivoBloqueoReanudar = null;
    if (hayPendiente) {
      motivoBloqueoReanudar =
        'Existe un presupuesto complementario pendiente de decisión del Cliente.';
    } else if (rechazadoSinEvaluar) {
      motivoBloqueoReanudar =
        'Debes registrar la evaluación técnica sobre el presupuesto complementario rechazado antes de reanudar.';
    } else if (complementarios.length > 0) {
      const ultimoComp = complementarios[complementarios.length - 1];
      if (ultimoComp.estado === 'RECHAZADO') {
        const evalUltimo = evaluations.find(
          (e) => e.idPresupuestoRechazado === ultimoComp.idPresupuesto
        );
        if (evalUltimo?.elementosIndispensables) {
          motivoBloqueoReanudar =
            'Los elementos rechazados fueron declarados indispensables; se requiere una nueva cotización autorizada por el Administrador y aprobada por el Cliente.';
        }
      }
    }

    const puedeEmitirComplementarioNormal =
      !hayPendiente &&
      !rechazadoSinEvaluar &&
      !autorizacionDisponible &&
      !motivoBloqueoReanudar;

    return {
      original,
      originalAprobado: original?.estado === 'APROBADO',
      hayPendiente,
      rechazadoSinEvaluar,
      autorizacionDisponible,
      motivoBloqueoReanudar,
      puedeEmitirComplementarioNormal,
    };
  }, [budgets, evaluations, authorizations]);

  if (loading) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-xs text-slate-500">
        Cargando expediente técnico de la orden...
      </div>
    );
  }

  if (!order) {
    return (
      <div className="space-y-4">
        <AlertBanner
          type="error"
          message={errorInfo?.message || 'Orden de servicio no encontrada.'}
        />
        <button
          type="button"
          onClick={() => navigate('/tecnico/ordenes')}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-servitec-navy text-white text-xs font-semibold cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Volver a Mis Órdenes</span>
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Barra superior */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={() => navigate('/tecnico/ordenes')}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-slate-700 text-xs font-semibold hover:bg-slate-100 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Mis órdenes asignadas</span>
        </button>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setIsTimelineOpen(true)}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-slate-700 text-xs font-semibold hover:bg-slate-100 cursor-pointer"
          >
            <History className="w-4 h-4 text-servitec-primary" />
            <span>Historial de estados</span>
          </button>

          <button
            type="button"
            onClick={cargarExpedienteTecnico}
            title="Refrescar orden"
            className="p-2 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 cursor-pointer"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {successMessage && (
        <AlertBanner
          type="success"
          message={successMessage}
          onClose={() => setSuccessMessage(null)}
        />
      )}

      {errorInfo && (
        <AlertBanner
          type="error"
          message={errorInfo.message}
          onClose={() => setErrorInfo(null)}
        />
      )}

      {/* Cabecera Resumen de la Orden */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3.5">
          <div className="flex items-center gap-2.5">
            <span className="font-mono text-base font-bold text-servitec-primary">
              {order.codigoOrden}
            </span>
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-100 text-slate-800 text-xs font-bold">
              <Wrench className="w-3.5 h-3.5 text-servitec-navy" />
              {order.nombreCategoria}
            </span>
          </div>

          <StatusBadge status={order.estadoOperativo} />
        </div>

        <div>
          <p className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
            Descripción del problema reportado
          </p>
          <p className="text-sm text-slate-800 leading-relaxed">
            {order.descripcionProblema}
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-slate-50 rounded-xl p-3.5 text-xs">
          <div className="flex items-start gap-2">
            <User className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <div>
              <span className="block text-[11px] text-slate-500">Cliente</span>
              <span className="font-bold text-servitec-navy">
                {order.nombreCliente}
              </span>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <MapPin className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <div>
              <span className="block text-[11px] text-slate-500">
                Dirección de atención
              </span>
              <span className="font-semibold text-slate-800">
                {order.direccionServicio}
              </span>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Calendar className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <div>
              <span className="block text-[11px] text-slate-500">
                Fecha estimada asignada
              </span>
              <span className="font-semibold text-slate-800">
                {formatDateTime(order.fechaEstimada)}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 1. Diagnóstico Técnico en Sitio (CV-04) */}
      {!diagnostic && order.estadoOperativo === 'SOLICITADO' ? (
        <DiagnosticFormCard
          order={order}
          onDiagnosticSaved={handleDiagnosticSaved}
        />
      ) : (
        <DiagnosticReadOnlyCard diagnostic={diagnostic} />
      )}

      {/* 2. Elaboración de Presupuesto Original (CV-05) */}
      {diagnostic &&
        order.estadoOperativo === 'EN_DIAGNOSTICO' &&
        !estadoFlujoComplementarios.original && (
          <CreateOriginalBudgetForm
            order={order}
            onBudgetCreated={handleOriginalBudgetCreated}
          />
        )}

      {/* 3. Panel de Ejecución y Cronómetro de Tiempo Efectivo (CV-07 y CV-17) */}
      {['PRESUPUESTADO', 'EN_EJECUCION', 'PAUSADA', 'FINALIZADO'].includes(
        order.estadoOperativo
      ) && (
        <ExecutionControlPanel
          order={order}
          timeSummary={timeSummary}
          originalBudgetApproved={estadoFlujoComplementarios.originalAprobado}
          canControl={true}
          resumeBlockedReason={estadoFlujoComplementarios.motivoBloqueoReanudar}
          onTimeUpdated={handleTimeUpdated}
        />
      )}

      {/* 4. Evaluación Técnica cuando el Cliente rechaza un Presupuesto Complementario (CV-07 / RN-09) */}
      {order.estadoOperativo === 'PAUSADA' &&
        estadoFlujoComplementarios.rechazadoSinEvaluar && (
          <TechnicalEvaluationForm
            rejectedBudget={estadoFlujoComplementarios.rechazadoSinEvaluar}
            onEvaluationSaved={handleEvaluationSaved}
          />
        )}

      {/* 5. Emisión de Presupuesto Complementario (CV-12) o Nueva Propuesta Autorizada (CV-15) */}
      {order.estadoOperativo === 'PAUSADA' &&
        (estadoFlujoComplementarios.puedeEmitirComplementarioNormal ||
          Boolean(estadoFlujoComplementarios.autorizacionDisponible)) && (
          <CreateComplementaryForm
            order={order}
            activeAuthorization={estadoFlujoComplementarios.autorizacionDisponible}
            onCreated={handleComplementaryCreated}
          />
        )}

      {/* 6. Listado de Presupuestos Emitidos */}
      {budgets.length > 0 && (
        <div className="space-y-4">
          <h3 className="text-sm font-bold text-servitec-navy uppercase tracking-wider">
            Presupuestos emitidos en la orden
          </h3>
          {budgets.map((b) => (
            <ClientBudgetDecisionCard
              key={b.idPresupuesto}
              budget={b}
              canDecide={false}
            />
          ))}
        </div>
      )}

      <OrderTimelineModal
        isOpen={isTimelineOpen}
        onClose={() => setIsTimelineOpen(false)}
        order={order}
      />
    </div>
  );
}