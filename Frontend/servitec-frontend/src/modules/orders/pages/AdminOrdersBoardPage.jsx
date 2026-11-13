import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  UserPlus,
  History,
  LayoutGrid,
  Table as TableIcon,
  RefreshCw,
  Calendar,
  Eye,
} from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { usersApi } from '../../../api/usersApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';
import EmptyState from '../../../shared/components/EmptyState';
import AssignTechnicianModal from '../components/AssignTechnicianModal';
import OrderTimelineModal from '../components/OrderTimelineModal';

const KANBAN_COLUMNS = [
  { id: 'SOLICITADO', title: 'Solicitado' },
  { id: 'EN_DIAGNOSTICO', title: 'En Diagnóstico' },
  { id: 'PRESUPUESTADO', title: 'Presupuestado' },
  { id: 'EN_EJECUCION', title: 'En Ejecución' },
  { id: 'PAUSADA', title: 'Pausada' },
  { id: 'FINALIZADO', title: 'Finalizado' },
  { id: 'CANCELADO', title: 'Cancelado' },
];

export default function AdminOrdersBoardPage() {
  const navigate = useNavigate();

  const [ordenes, setOrdenes] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [tecnicos, setTecnicos] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filtros
  const [busqueda, setBusqueda] = useState('');
  const [estadoFiltro, setEstadoFiltro] = useState('TODOS');
  const [idCategoriaFiltro, setIdCategoriaFiltro] = useState('TODAS');
  const [idTecnicoFiltro, setIdTecnicoFiltro] = useState('TODOS');
  const [viewMode, setViewMode] = useState('TABLE'); // TABLE o KANBAN

  const [errorInfo, setErrorInfo] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [orderToAssign, setOrderToAssign] = useState(null);
  const [orderForTimeline, setOrderForTimeline] = useState(null);

  const cargarDatos = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);
    try {
      const [ordenesData, categoriasData, tecnicosData] = await Promise.all([
        ordersApi.listarOrdenes({
          estado: estadoFiltro,
          idCategoria: idCategoriaFiltro,
          idTecnico: idTecnicoFiltro,
          busqueda,
        }),
        ordersApi.listarCategoriasActivas(),
        usersApi.listarUsuarios('TECNICO_ESPECIALISTA'),
      ]);

      setOrdenes(ordenesData);
      setCategorias(categoriasData);
      setTecnicos(tecnicosData);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible cargar el tablero de órdenes de servicio.')
      );
    } finally {
      setLoading(false);
    }
  }, [estadoFiltro, idCategoriaFiltro, idTecnicoFiltro, busqueda]);

  useEffect(() => {
    const timer = setTimeout(() => {
      cargarDatos();
    }, 250);
    return () => clearTimeout(timer);
  }, [cargarDatos]);

  // Solo técnicos con cuenta ACTIVA pueden ser asignados (REQ-06 / CV-03)
  const tecnicosActivos = useMemo(() => {
    return tecnicos.filter((t) => t.estadoCuenta === 'ACTIVA');
  }, [tecnicos]);

  const handleAssigned = (ordenActualizada) => {
    setSuccessMessage(
      `Se asignó a ${ordenActualizada.nombreTecnicoAsignado} a la orden ${ordenActualizada.codigoOrden} para el ${formatDateTime(
        ordenActualizada.fechaEstimada
      )}.`
    );
    cargarDatos();
  };

  return (
    <div className="space-y-6">
      {/* Encabezado */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-servitec-navy">
            Tablero de Órdenes de Servicio
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Supervisión global del ciclo de servicio, asignación de técnicos especialistas y fechas estimadas (CV-03).
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="bg-white border border-slate-200 p-1 rounded-xl flex items-center">
            <button
              type="button"
              onClick={() => setViewMode('TABLE')}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                viewMode === 'TABLE'
                  ? 'bg-servitec-navy text-white'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <TableIcon className="w-3.5 h-3.5" />
              <span>Tabla</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode('KANBAN')}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                viewMode === 'KANBAN'
                  ? 'bg-servitec-navy text-white'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              <span>Kanban</span>
            </button>
          </div>

          <button
            type="button"
            onClick={cargarDatos}
            title="Refrescar órdenes"
            className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {errorInfo && (
        <AlertBanner
          type="error"
          message={errorInfo.message}
          onClose={() => setErrorInfo(null)}
        />
      )}

      {successMessage && (
        <AlertBanner
          type="success"
          message={successMessage}
          onClose={() => setSuccessMessage(null)}
        />
      )}

      {/* Barra de Filtros */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Código OS-2026-... o cliente"
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          />
        </div>

        <select
          value={estadoFiltro}
          onChange={(e) => setEstadoFiltro(e.target.value)}
          className="px-3 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
        >
          <option value="TODOS">Todos los estados operativos</option>
          <option value="SOLICITADO">Solicitado</option>
          <option value="EN_DIAGNOSTICO">En diagnóstico</option>
          <option value="PRESUPUESTADO">Presupuestado</option>
          <option value="EN_EJECUCION">En Ejecución</option>
          <option value="PAUSADA">Pausada</option>
          <option value="FINALIZADO">Finalizado</option>
          <option value="CANCELADO">Cancelado</option>
        </select>

        <select
          value={idCategoriaFiltro}
          onChange={(e) => setIdCategoriaFiltro(e.target.value)}
          className="px-3 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
        >
          <option value="TODAS">Todas las categorías</option>
          {categorias.map((c) => (
            <option key={c.idCategoria} value={c.idCategoria}>
              {c.nombre}
            </option>
          ))}
        </select>

        <select
          value={idTecnicoFiltro}
          onChange={(e) => setIdTecnicoFiltro(e.target.value)}
          className="px-3 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
        >
          <option value="TODOS">Todos los técnicos</option>
          {tecnicos.map((t) => (
            <option key={t.idUsuario} value={t.idUsuario}>
              {t.nombreCompleto}
            </option>
          ))}
        </select>
      </div>

      {/* Contenido Principal: Tabla o Kanban */}
      {ordenes.length === 0 && !loading ? (
        <EmptyState
          title="No se encontraron órdenes de servicio"
          description="No existen órdenes que coincidan con los filtros seleccionados."
        />
      ) : viewMode === 'TABLE' ? (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Orden / Fecha</th>
                  <th className="py-3.5 px-4">Cliente / Dirección</th>
                  <th className="py-3.5 px-4">Categoría / Problema</th>
                  <th className="py-3.5 px-4">Técnico y Visita Estimada</th>
                  <th className="py-3.5 px-4 text-center">Estado Operativo</th>
                  <th className="py-3.5 px-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-xs">
                {ordenes.map((orden) => (
                  <tr key={orden.idOrden} className="hover:bg-slate-50/80">
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="font-mono font-bold text-servitec-primary block">
                        {orden.codigoOrden}
                      </span>
                      <span className="text-[11px] text-slate-500">
                        {formatDateTime(orden.fechaSolicitud)}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 max-w-xs">
                      <span className="font-bold text-servitec-navy block truncate">
                        {orden.nombreCliente}
                      </span>
                      <span className="text-[11px] text-slate-500 block truncate">
                        {orden.direccionServicio}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 max-w-xs">
                      <span className="inline-block px-2 py-0.5 rounded-md bg-slate-100 text-slate-800 text-[10px] font-bold uppercase mb-1">
                        {orden.nombreCategoria}
                      </span>
                      <p className="text-slate-600 line-clamp-2">
                        {orden.descripcionProblema}
                      </p>
                    </td>

                    <td className="py-3.5 px-4">
                      {orden.idTecnicoAsignado ? (
                        <div>
                          <span className="font-bold text-slate-800 block">
                            {orden.nombreTecnicoAsignado}
                          </span>
                          <span className="inline-flex items-center gap-1 text-[11px] text-slate-500 mt-0.5">
                            <Calendar className="w-3 h-3 text-servitec-primary" />
                            {formatDateTime(orden.fechaEstimada)}
                          </span>
                        </div>
                      ) : (
                        <span className="inline-block px-2.5 py-1 rounded-full bg-amber-100 text-amber-800 text-[11px] font-semibold">
                          Sin técnico asignado
                        </span>
                      )}
                    </td>

                    <td className="py-3.5 px-4 text-center whitespace-nowrap">
                      <StatusBadge status={orden.estadoOperativo} />
                    </td>

                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="inline-flex items-center gap-1.5">
                        {orden.estadoOperativo === 'SOLICITADO' && (
                          <button
                            type="button"
                            onClick={() => setOrderToAssign(orden)}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold transition-colors cursor-pointer"
                          >
                            <UserPlus className="w-3.5 h-3.5" />
                            <span>
                              {orden.idTecnicoAsignado ? 'Reasignar' : 'Asignar'}
                            </span>
                          </button>
                        )}

                        <button
                          type="button"
                          onClick={() => setOrderForTimeline(orden)}
                          title="Ver historial de estados"
                          className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 cursor-pointer"
                        >
                          <History className="w-4 h-4" />
                        </button>

                        <button
                          type="button"
                          onClick={() => navigate(`/admin/ordenes/${orden.idOrden}`)}
                          title="Ver expediente completo de la orden"
                          className="p-2 rounded-xl border border-slate-200 text-servitec-navy hover:bg-sky-50 hover:border-sky-200 cursor-pointer"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* Vista Tablero Kanban por Estado Operativo */
        <div className="flex gap-4 overflow-x-auto pb-4">
          {KANBAN_COLUMNS.map((col) => {
            const colOrders = ordenes.filter((o) => o.estadoOperativo === col.id);
            return (
              <div
                key={col.id}
                className="w-72 shrink-0 bg-slate-100/80 rounded-2xl border border-slate-200 p-3 flex flex-col gap-3"
              >
                <div className="flex items-center justify-between px-1">
                  <StatusBadge status={col.id} />
                  <span className="text-xs font-bold text-slate-500">
                    {colOrders.length}
                  </span>
                </div>

                <div className="space-y-2.5">
                  {colOrders.map((orden) => (
                    <div
                      key={orden.idOrden}
                      className="bg-white rounded-xl border border-slate-200 p-3.5 shadow-2xs space-y-2"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono text-xs font-bold text-servitec-primary">
                          {orden.codigoOrden}
                        </span>
                        <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                          {orden.nombreCategoria}
                        </span>
                      </div>

                      <p className="text-xs font-bold text-servitec-navy truncate">
                        {orden.nombreCliente}
                      </p>
                      <p className="text-[11px] text-slate-600 line-clamp-2">
                        {orden.descripcionProblema}
                      </p>

                      <div className="pt-2 border-t border-slate-100 text-[11px] text-slate-500">
                        Técnico:{' '}
                        <b>{orden.nombreTecnicoAsignado || 'Pendiente'}</b>
                      </div>

                      <div className="flex items-center justify-between gap-2 pt-1">
                        {orden.estadoOperativo === 'SOLICITADO' && (
                          <button
                            type="button"
                            onClick={() => setOrderToAssign(orden)}
                            className="flex-1 py-1.5 px-2.5 rounded-lg bg-servitec-primary text-white text-[11px] font-semibold cursor-pointer"
                          >
                            {orden.idTecnicoAsignado ? 'Reasignar' : 'Asignar técnico'}
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => navigate(`/admin/ordenes/${orden.idOrden}`)}
                          className="py-1.5 px-2.5 rounded-lg border border-slate-200 text-slate-700 text-[11px] font-semibold hover:bg-slate-50 cursor-pointer"
                        >
                          Detalle
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal de Asignación CV-03 */}
      <AssignTechnicianModal
        isOpen={Boolean(orderToAssign)}
        onClose={() => setOrderToAssign(null)}
        order={orderToAssign}
        activeTechnicians={tecnicosActivos}
        onAssigned={handleAssigned}
      />

      {/* Modal de Historial de Estados */}
      <OrderTimelineModal
        isOpen={Boolean(orderForTimeline)}
        onClose={() => setOrderForTimeline(null)}
        order={orderForTimeline}
      />
    </div>
  );
}