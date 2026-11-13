import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  MapPin,
  Calendar,
  User,
  Wrench,
  RefreshCw,
  ArrowRight,
  Search,
} from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';
import EmptyState from '../../../shared/components/EmptyState';

const TECH_STATUS_TABS = [
  { id: 'TODOS', label: 'Todas' },
  { id: 'SOLICITADO', label: 'Por diagnosticar' },
  { id: 'EN_DIAGNOSTICO', label: 'En diagnóstico' },
  { id: 'PRESUPUESTADO', label: 'Presupuestadas' },
  { id: 'EN_EJECUCION', label: 'En ejecución' },
  { id: 'PAUSADA', label: 'Pausadas' },
  { id: 'FINALIZADO', label: 'Finalizadas' },
];

export default function TechAssignedOrdersPage() {
  const navigate = useNavigate();

  const [ordenes, setOrdenes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [estadoFiltro, setEstadoFiltro] = useState('TODOS');
  const [busqueda, setBusqueda] = useState('');
  const [errorInfo, setErrorInfo] = useState(null);

  const cargarOrdenesAsignadas = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);
    try {
      const data = await ordersApi.listarOrdenes({
        estado: estadoFiltro,
        busqueda,
      });
      setOrdenes(data);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible cargar tus órdenes asignadas.')
      );
    } finally {
      setLoading(false);
    }
  }, [estadoFiltro, busqueda]);

  useEffect(() => {
    const timer = setTimeout(() => {
      cargarOrdenesAsignadas();
    }, 200);
    return () => clearTimeout(timer);
  }, [cargarOrdenesAsignadas]);

  return (
    <div className="space-y-5">
      {/* Encabezado */}
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg sm:text-xl font-bold text-servitec-navy">
            Mis Órdenes Asignadas
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Gestiona diagnósticos en sitio, presupuestos, tiempos de ejecución y cierres técnicos.
          </p>
        </div>

        <button
          type="button"
          onClick={cargarOrdenesAsignadas}
          title="Actualizar órdenes"
          className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 cursor-pointer"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {errorInfo && (
        <AlertBanner
          type="error"
          message={errorInfo.message}
          onClose={() => setErrorInfo(null)}
        />
      )}

      {/* Buscador y Filtros */}
      <div className="space-y-3">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por código OS-2026-... o nombre del cliente..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          />
        </div>

        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {TECH_STATUS_TABS.map((tab) => (
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
      </div>

      {/* Tarjetas de Órdenes */}
      {ordenes.length === 0 && !loading ? (
        <EmptyState
          title="No tienes órdenes asignadas en este estado"
          description="Cuando el Administrador te asigne una nueva orden de servicio, aparecerá aquí con su fecha estimada de atención."
        />
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {ordenes.map((orden) => (
            <div
              key={orden.idOrden}
              className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs space-y-4"
            >
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2.5">
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
                  Falla reportada por el cliente
                </p>
                <p className="text-sm text-slate-800 leading-relaxed">
                  {orden.descripcionProblema}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-slate-50 rounded-xl p-3.5 text-xs">
                <div className="flex items-start gap-2">
                  <User className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
                  <div>
                    <span className="block text-[11px] text-slate-500">Cliente</span>
                    <span className="font-bold text-servitec-navy">
                      {orden.nombreCliente}
                    </span>
                  </div>
                </div>

                <div className="flex items-start gap-2">
                  <MapPin className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
                  <div>
                    <span className="block text-[11px] text-slate-500">
                      Dirección del servicio
                    </span>
                    <span className="font-semibold text-slate-800">
                      {orden.direccionServicio}
                    </span>
                  </div>
                </div>

                <div className="flex items-start gap-2">
                  <Calendar className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
                  <div>
                    <span className="block text-[11px] text-slate-500">
                      Visita programada
                    </span>
                    <span className="font-semibold text-slate-800">
                      {formatDateTime(orden.fechaEstimada)}
                    </span>
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={() => navigate(`/tecnico/ordenes/${orden.idOrden}`)}
                className="w-full min-h-[48px] py-3 px-4 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs sm:text-sm font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
              >
                <span>
                  {orden.estadoOperativo === 'SOLICITADO'
                    ? 'Atender orden y registrar diagnóstico en sitio'
                    : 'Abrir gestión técnica de la orden'}
                </span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}