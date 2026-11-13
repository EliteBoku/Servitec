import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  MapPin,
  UserCheck,
  Calendar,
  Wrench,
  History,
  RefreshCw,
  FileCheck2,
} from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { diagnosticsApi } from '../../../api/diagnosticsApi';
import { budgetsApi } from '../../../api/budgetsApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';
import OrderTimelineModal from '../components/OrderTimelineModal';
import DiagnosticReadOnlyCard from '../../diagnostics/components/DiagnosticReadOnlyCard';
import ClientBudgetDecisionCard from '../../budgets/components/ClientBudgetDecisionCard';

export default function ClientOrderDetailPage() {
  const { idOrden } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [diagnostic, setDiagnostic] = useState(null);
  const [budgets, setBudgets] = useState([]);
  const [loading, setLoading] = useState(true);

  const [errorInfo, setErrorInfo] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [isTimelineOpen, setIsTimelineOpen] = useState(false);

  const cargarDetalleCliente = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);
    try {
      const [ordenData, diagData, budgetsData] = await Promise.all([
        ordersApi.consultarDetalleOrden(idOrden),
        diagnosticsApi.consultarDiagnosticoPorOrden(idOrden),
        budgetsApi.listarPresupuestosPorOrden(idOrden),
      ]);
      setOrder(ordenData);
      setDiagnostic(diagData);
      setBudgets(budgetsData);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible consultar el detalle de tu orden.')
      );
    } finally {
      setLoading(false);
    }
  }, [idOrden]);

  useEffect(() => {
    cargarDetalleCliente();
  }, [cargarDetalleCliente]);

  const handleDecisionRegistered = (presupuestoActualizado, decisionTomada) => {
    setBudgets((prev) =>
      prev.map((b) =>
        b.idPresupuesto === presupuestoActualizado.idPresupuesto
          ? presupuestoActualizado
          : b
      )
    );
    if (presupuestoActualizado.estadoOperativoOrden) {
      setOrder((prev) =>
        prev
          ? { ...prev, estadoOperativo: presupuestoActualizado.estadoOperativoOrden }
          : prev
      );
    }

    if (decisionTomada === 'APROBADO') {
      setSuccessMessage(
        `Has aprobado el presupuesto ${presupuestoActualizado.codigoPresupuesto}. Los repuestos e insumos han sido reservados en inventario para tu reparación.`
      );
    } else if (presupuestoActualizado.tipoPresupuesto === 'ORIGINAL') {
      setSuccessMessage(
        `Has rechazado el presupuesto original ${presupuestoActualizado.codigoPresupuesto}. La orden ha pasado automáticamente al estado Cancelado (CV-06).`
      );
    } else {
      setSuccessMessage(
        `Has rechazado el presupuesto complementario ${presupuestoActualizado.codigoPresupuesto}. El técnico especialista realizará la evaluación técnica correspondiente.`
      );
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-xs text-slate-500">
        Cargando información de tu orden de servicio...
      </div>
    );
  }

  if (!order) {
    return (
      <div className="space-y-4">
        <AlertBanner
          type="error"
          message={errorInfo?.message || 'No se encontró la orden solicitada.'}
        />
        <button
          type="button"
          onClick={() => navigate('/cliente/ordenes')}
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
          onClick={() => navigate('/cliente/ordenes')}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-slate-700 text-xs font-semibold hover:bg-slate-100 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Volver a Mis Órdenes</span>
        </button>

        <div className="flex flex-wrap items-center gap-2">
          {order.estadoOperativo === 'FINALIZADO' && (
            <button
              type="button"
              onClick={() => navigate(`/cliente/ordenes/${order.idOrden}/resumen`)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold cursor-pointer"
            >
              <FileCheck2 className="w-4 h-4" />
              <span>Ver Resumen Final y Garantía (CV-09)</span>
            </button>
          )}

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
            onClick={cargarDetalleCliente}
            title="Refrescar"
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
            Problema reportado en tu solicitud
          </p>
          <p className="text-sm text-slate-800 leading-relaxed">
            {order.descripcionProblema}
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-slate-50 rounded-xl p-3.5 text-xs">
          <div className="flex items-start gap-2">
            <MapPin className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <div>
              <span className="block text-[11px] text-slate-500">
                Dirección del servicio
              </span>
              <span className="font-semibold text-slate-800">
                {order.direccionServicio}
              </span>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <UserCheck className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <div>
              <span className="block text-[11px] text-slate-500">
                Técnico especialista asignado
              </span>
              <span className="font-semibold text-slate-800">
                {order.nombreTecnicoAsignado || 'Pendiente de asignación'}
              </span>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Calendar className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <div>
              <span className="block text-[11px] text-slate-500">
                Fecha estimada de atención
              </span>
              <span className="font-semibold text-slate-800">
                {order.fechaEstimada
                  ? formatDateTime(order.fechaEstimada)
                  : 'Por programar'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Diagnóstico Técnico en Sitio (si ya fue registrado) */}
      {diagnostic && <DiagnosticReadOnlyCard diagnostic={diagnostic} />}

      {/* Presupuestos Emitidos (Original y Complementarios) */}
      <div className="space-y-4">
        <h3 className="text-base font-bold text-servitec-navy">
          Presupuestos y Cotizaciones de la Orden
        </h3>

        {budgets.length === 0 ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 text-xs text-slate-500 text-center">
            Aún no se han emitido presupuestos para esta orden de servicio. Una vez el técnico especialista finalice el diagnóstico en sitio y elabore la cotización, aparecerá aquí para tu aprobación o rechazo digital.
          </div>
        ) : (
          budgets.map((b) => (
            <ClientBudgetDecisionCard
              key={b.idPresupuesto}
              budget={b}
              canDecide={true}
              onDecisionRegistered={handleDecisionRegistered}
            />
          ))
        )}
      </div>

      <OrderTimelineModal
        isOpen={isTimelineOpen}
        onClose={() => setIsTimelineOpen(false)}
        order={order}
      />
    </div>
  );
}