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
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';
import OrderTimelineModal from '../components/OrderTimelineModal';
import DiagnosticFormCard from '../../diagnostics/components/DiagnosticFormCard';
import DiagnosticReadOnlyCard from '../../diagnostics/components/DiagnosticReadOnlyCard';

export default function TechOrderWorkPage() {
  const { idOrden } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [diagnostic, setDiagnostic] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorInfo, setErrorInfo] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [isTimelineOpen, setIsTimelineOpen] = useState(false);

  const cargarExpedienteTecnico = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);
    try {
      const [ordenData, diagData] = await Promise.all([
        ordersApi.consultarDetalleOrden(idOrden),
        diagnosticsApi.consultarDiagnosticoPorOrden(idOrden),
      ]);
      setOrder(ordenData);
      setDiagnostic(diagData);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible cargar la información de la orden de servicio.')
      );
    } finally {
      setLoading(false);
    }
  }, [idOrden]);

  useEffect(() => {
    cargarExpedienteTecnico();
  }, [cargarExpedienteTecnico]);

  const handleDiagnosticSaved = (diagRegistrado) => {
    setDiagnostic(diagRegistrado);
    setOrder((prev) =>
      prev
        ? {
            ...prev,
            estadoOperativo: diagRegistrado.estadoOrdenActualizado || 'EN_DIAGNOSTICO',
          }
        : prev
    );
    setSuccessMessage(
      'Diagnóstico registrado correctamente. El estado de la orden cambió a En diagnóstico.'
    );
  };

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
      {/* Barra superior de navegación */}
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

      {/* Tarjeta Resumen de la Orden */}
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

      {/* Sección de Diagnóstico Técnico (CV-04): Formulario si aún no existe, o Lectura si ya fue registrado */}
      {!diagnostic && order.estadoOperativo === 'SOLICITADO' ? (
        <DiagnosticFormCard
          order={order}
          onDiagnosticSaved={handleDiagnosticSaved}
        />
      ) : (
        <DiagnosticReadOnlyCard diagnostic={diagnostic} />
      )}

      {/* Aviso de siguiente etapa cuando ya está en EN_DIAGNOSTICO */}
      {order.estadoOperativo === 'EN_DIAGNOSTICO' && (
        <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 text-xs text-amber-900">
          <p className="font-bold">Siguiente etapa: Elaboración del Presupuesto Original (CV-05)</p>
          <p className="mt-0.5">
            El diagnóstico técnico ya fue registrado. En el Micro-paso F4.1 se habilitará aquí el buscador del catálogo activo y el cálculo automático de mano de obra.
          </p>
        </div>
      )}

      {/* Modal de Historial de Estados */}
      <OrderTimelineModal
        isOpen={isTimelineOpen}
        onClose={() => setIsTimelineOpen(false)}
        order={order}
      />
    </div>
  );
}