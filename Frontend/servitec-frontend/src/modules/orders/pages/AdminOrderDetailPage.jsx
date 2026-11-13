import { useState, useEffect, useCallback } from 'react';
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
import DiagnosticReadOnlyCard from '../../diagnostics/components/DiagnosticReadOnlyCard';
import ExecutionControlPanel from '../../execution/components/ExecutionControlPanel';
import ClientBudgetDecisionCard from '../../budgets/components/ClientBudgetDecisionCard';
import AdminRequoteAuthCard from '../../budgets/components/AdminRequoteAuthCard';

export default function AdminOrderDetailPage() {
  const params = useParams();
  const navigate = useNavigate();

  const rawOrderId = params.idOrden ?? params.id;
  const isValidOrderId =
    rawOrderId !== undefined &&
    rawOrderId !== null &&
    rawOrderId !== 'undefined' &&
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

  const cargarExpedienteAdmin = useCallback(async () => {
    if (!idOrden) return;
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
          'No fue posible cargar el expediente administrativo de la orden.'
        )
      );
    } finally {
      setLoading(false);
    }
  }, [idOrden]);

  useEffect(() => {
    if (!isValidOrderId) {
      navigate('/admin/ordenes', { replace: true });
      return;
    }
    cargarExpedienteAdmin();
  }, [isValidOrderId, cargarExpedienteAdmin, navigate]);

  const handleAuthorized = (nuevaAuth) => {
    setAuthorizations((prev) => [nuevaAuth, ...prev]);
    setSuccessMessage(
      `Autorización #${nuevaAuth.idAutorizacion} registrada correctamente. El técnico especialista ya puede elaborar la nueva propuesta complementaria (CV-14).`
    );
  };

  if (loading) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-xs text-slate-500">
        Cargando expediente administrativo...
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
          onClick={() => navigate('/admin/ordenes')}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-servitec-navy text-white text-xs font-semibold cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Volver al Tablero de Órdenes</span>
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={() => navigate('/admin/ordenes')}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-slate-700 text-xs font-semibold hover:bg-slate-100 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Tablero de Órdenes</span>
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
            onClick={cargarExpedienteAdmin}
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

      {/* Cabecera de la Orden */}
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
            Problema reportado
          </p>
          <p className="text-sm text-slate-800">{order.descripcionProblema}</p>
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
              <span className="block text-[11px] text-slate-500">Dirección</span>
              <span className="font-semibold text-slate-800">
                {order.direccionServicio}
              </span>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Calendar className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <div>
              <span className="block text-[11px] text-slate-500">
                Técnico asignado
              </span>
              <span className="font-semibold text-slate-800">
                {order.nombreTecnicoAsignado || 'Sin asignar'} (
                {formatDateTime(order.fechaEstimada)})
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Diagnóstico */}
      {diagnostic && <DiagnosticReadOnlyCard diagnostic={diagnostic} />}

      {/* Resumen de Tiempos de Ejecución */}
      {timeSummary &&
        ['PRESUPUESTADO', 'EN_EJECUCION', 'PAUSADA', 'FINALIZADO', 'CANCELADO'].includes(
          order.estadoOperativo
        ) && (
          <ExecutionControlPanel
            order={order}
            timeSummary={timeSummary}
            canControl={false}
          />
        )}

      {/* Evaluaciones Técnicas y Autorización de Nueva Cotización (CV-14) */}
      {evaluations.length > 0 && (
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-servitec-navy uppercase tracking-wider">
            Evaluaciones Técnicas y Autorizaciones Administrativas (CV-14)
          </h3>
          {evaluations.map((ev) => {
            const authAsociada = authorizations.find(
              (a) => a.idPresupuestoRechazado === ev.idPresupuestoRechazado
            );
            return (
              <AdminRequoteAuthCard
                key={ev.idEvaluacion}
                evaluation={ev}
                existingAuthorization={authAsociada}
                onAuthorized={handleAuthorized}
              />
            );
          })}
        </div>
      )}

      {/* Historial de Presupuestos */}
      {budgets.length > 0 && (
        <div className="space-y-4">
          <h3 className="text-sm font-bold text-servitec-navy uppercase tracking-wider">
            Presupuestos de la Orden
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