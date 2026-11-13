import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  FilePlus2,
  MapPin,
  Calendar,
  UserCheck,
  History,
  RefreshCw,
  ArrowRight,
  Wrench,
} from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';
import EmptyState from '../../../shared/components/EmptyState';
import OrderTimelineModal from '../components/OrderTimelineModal';

const ESTADOS_FILTRO = [
  { id: 'TODOS', label: 'Todas' },
  { id: 'SOLICITADO', label: 'Solicitadas' },
  { id: 'EN_DIAGNOSTICO', label: 'En diagnóstico' },
  { id: 'PRESUPUESTADO', label: 'Presupuestadas' },
  { id: 'EN_EJECUCION', label: 'En ejecución' },
  { id: 'PAUSADA', label: 'Pausadas' },
  { id: 'FINALIZADO', label: 'Finalizadas' },
  { id: 'CANCELADO', label: 'Canceladas' },
];

export default function ClientOrdersPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const [ordenes, setOrdenes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [estadoFiltro, setEstadoFiltro] = useState('TODOS');
  const [errorInfo, setErrorInfo] = useState(null);
  const [successMessage, setSuccessMessage] = useState(
    location.state?.createdOrderCode
      ? `Solicitud registrada correctamente. Tu número de orden es ${location.state.createdOrderCode}.`
      : null
  );

  const [selectedOrderTimeline, setSelectedOrderTimeline] = useState(null);

  const cargarMisOrdenes = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);
    try {
      const data = await ordersApi.listarOrdenes({ estado: estadoFiltro });
      setOrdenes(data);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible consultar tus órdenes de servicio.')
      );
    } finally {
      setLoading(false);
    }
  }, [estadoFiltro]);

  useEffect(() => {
    cargarMisOrdenes();
  }, [cargarMisOrdenes]);

  return (
    <div className="space-y-6">
      {/* Encabezado */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-servitec-navy">
            Mis Órdenes de Servicio
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Consulta el estado de tus solicitudes, presupuestos emitidos y resúmenes de servicio.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={cargarMisOrdenes}
            title="Actualizar listado"
            className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            type="button"
            onClick={() => navigate('/cliente/nueva-solicitud')}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
          >
            <FilePlus2 className="w-4 h-4" />
            <span>Nueva Solicitud</span>
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

      {/* Pestañas de filtro por estado */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
        {ESTADOS_FILTRO.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setEstadoFiltro(tab.id)}
            className={`px-3.5 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors cursor-pointer ${
              estadoFiltro === tab.id
                ? 'bg-servitec-navy text-white shadow-2xs'
                : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-100'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Listado de Tarjetas de Órdenes */}
      {ordenes.length === 0 && !loading ? (
        <EmptyState
          title="No tienes órdenes de servicio en esta vista"
          description="Registra una nueva solicitud de servicio para recibir atención técnica especializada en tu domicilio."
          action={
            <button
              type="button"
              onClick={() => navigate('/cliente/nueva-solicitud')}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-servitec-primary text-white text-xs font-semibold cursor-pointer"
            >
              <FilePlus2 className="w-4 h-4" />
              <span>Solicitar servicio técnico</span>
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {ordenes.map((orden) => (
            <div
              key={orden.idOrden}
              className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs hover:border-slate-300 transition-all space-y-4"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3">
                <div className="flex items-center gap-3">
                  <span className="font-mono text-sm font-bold text-servitec-primary">
                    {orden.codigoOrden}
                  </span>
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-slate-100 text-slate-800 text-xs font-semibold">
                    <Wrench className="w-3.5 h-3.5 text-servitec-navy" />
                    {orden.nombreCategoria}
                  </span>
                </div>

                <StatusBadge status={orden.estadoOperativo} />
              </div>

              <div>
                <p className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Problema reportado
                </p>
                <p className="text-sm text-slate-800 leading-relaxed">
                  {orden.descripcionProblema}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-slate-50 rounded-xl p-3.5 text-xs">
                <div className="flex items-start gap-2">
                  <MapPin className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
                  <div>
                    <span className="block text-[11px] text-slate-500">
                      Dirección de atención
                    </span>
                    <span className="font-semibold text-slate-800">
                      {orden.direccionServicio}
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
                      {orden.nombreTecnicoAsignado || 'Pendiente de asignación'}
                    </span>
                  </div>
                </div>

                <div className="flex items-start gap-2">
                  <Calendar className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
                  <div>
                    <span className="block text-[11px] text-slate-500">
                      Fecha estimada de visita
                    </span>
                    <span className="font-semibold text-slate-800">
                      {orden.fechaEstimada
                        ? formatDateTime(orden.fechaEstimada)
                        : 'Por programar'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
                <span className="text-[11px] text-slate-400">
                  Solicitada el: {formatDateTime(orden.fechaSolicitud)}
                </span>

                <div className="flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setSelectedOrderTimeline(orden)}
                    className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-100 text-xs font-semibold transition-colors cursor-pointer"
                  >
                    <History className="w-3.5 h-3.5" />
                    <span>Historial de estados</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => navigate(`/cliente/ordenes/${orden.idOrden}`)}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-servitec-navy hover:bg-servitec-navy-light text-white text-xs font-semibold transition-colors cursor-pointer"
                  >
                    <span>Ver seguimiento y presupuestos</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal de Línea de Tiempo */}
      <OrderTimelineModal
        isOpen={Boolean(selectedOrderTimeline)}
        onClose={() => setSelectedOrderTimeline(null)}
        order={selectedOrderTimeline}
      />
    </div>
  );
}