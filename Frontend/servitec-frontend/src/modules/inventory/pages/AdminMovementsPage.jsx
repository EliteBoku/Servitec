import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  ArrowLeftRight,
  ArrowDownLeft,
  ArrowUpRight,
  SlidersHorizontal,
  RefreshCw,
  Filter,
  XCircle,
} from 'lucide-react';
import { inventoryApi } from '../../../api/inventoryApi';
import { extractApiError } from '../../../api/httpClient';
import { formatQuantity, formatDateTime } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';
import EmptyState from '../../../shared/components/EmptyState';
import ManualMovementModal from '../components/ManualMovementModal';

const MOVEMENT_TYPE_BADGE = {
  ENTRADA: {
    label: 'Entrada',
    classes: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    icon: ArrowDownLeft,
  },
  SALIDA: {
    label: 'Salida',
    classes: 'bg-rose-100 text-rose-800 border-rose-200',
    icon: ArrowUpRight,
  },
  AJUSTE: {
    label: 'Ajuste físico',
    classes: 'bg-amber-100 text-amber-800 border-amber-200',
    icon: SlidersHorizontal,
  },
};

const ORIGIN_BADGE = {
  INICIALIZACION: {
    label: 'Apertura Catálogo',
    classes: 'bg-sky-100 text-sky-800',
  },
  MANUAL: {
    label: 'Manual (Admin)',
    classes: 'bg-slate-100 text-slate-800',
  },
  RN14: {
    label: 'Automático Orden (RN-14)',
    classes: 'bg-indigo-100 text-indigo-800',
  },
};

export default function AdminMovementsPage() {
  const [catalogItems, setCatalogItems] = useState([]);
  const [movements, setMovements] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filtros de consulta
  const [idRepuestoFiltro, setIdRepuestoFiltro] = useState('');
  const [tipoFiltro, setTipoFiltro] = useState('TODOS');
  const [origenFiltro, setOrigenFiltro] = useState('TODOS');
  const [fechaDesde, setFechaDesde] = useState('');
  const [fechaHasta, setFechaHasta] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const cargarDatos = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);

    try {
      const [catalogoData, historialData] = await Promise.all([
        inventoryApi.listarCatalogo(),
        inventoryApi.consultarHistorialMovimientos({
          idRepuesto: idRepuestoFiltro || null,
          tipoMovimiento: tipoFiltro,
          origenMovimiento: origenFiltro,
          fechaDesde: fechaDesde ? `${fechaDesde}T00:00:00` : null,
          fechaHasta: fechaHasta ? `${fechaHasta}T23:59:59` : null,
        }),
      ]);

      setCatalogItems(catalogoData);
      setMovements(historialData);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible consultar el historial de movimientos.')
      );
    } finally {
      setLoading(false);
    }
  }, [idRepuestoFiltro, tipoFiltro, origenFiltro, fechaDesde, fechaHasta]);

  useEffect(() => {
    cargarDatos();
  }, [cargarDatos]);

  const handleMovementRegistered = (nuevoMov) => {
    setSuccessMessage(
      `Movimiento ${nuevoMov.tipoMovimiento} registrado sobre "${nuevoMov.nombreRepuesto}" (${nuevoMov.codigoRepuesto}). Existencias resultantes: ${formatQuantity(
        nuevoMov.existenciasResultantes
      )}.`
    );
    cargarDatos();
  };

  const limpiarFiltros = () => {
    setIdRepuestoFiltro('');
    setTipoFiltro('TODOS');
    setOrigenFiltro('TODOS');
    setFechaDesde('');
    setFechaHasta('');
  };

  const metricas = useMemo(() => {
    return {
      total: movements.length,
      entradas: movements.filter((m) => m.tipoMovimiento === 'ENTRADA').length,
      salidas: movements.filter((m) => m.tipoMovimiento === 'SALIDA').length,
      ajustes: movements.filter((m) => m.tipoMovimiento === 'AJUSTE').length,
    };
  }, [movements]);

  return (
    <div className="space-y-6">
      {/* Encabezado */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-servitec-navy">
            Movimientos de Inventario y Auditoría de Stock
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Registro manual de entradas, salidas y ajustes por conteo físico e historial inmutable (CV-19).
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={cargarDatos}
            title="Refrescar historial"
            className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            type="button"
            onClick={() => setIsModalOpen(true)}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
          >
            <ArrowLeftRight className="w-4 h-4" />
            <span>Registrar movimiento manual</span>
          </button>
        </div>
      </div>

      {errorInfo && (
        <AlertBanner
          type="error"
          message={errorInfo.message}
          fieldErrors={errorInfo.fieldErrors}
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

      {/* Resumen rápido */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-servitec-navy/10 text-servitec-navy flex items-center justify-center">
            <ArrowLeftRight className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Movimientos listados</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.total}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
            <ArrowDownLeft className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Entradas</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.entradas}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center">
            <ArrowUpRight className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Salidas</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.salidas}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
            <SlidersHorizontal className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Ajustes físicos</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.ajustes}</p>
          </div>
        </div>
      </div>

      {/* Barra de Filtros */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-bold text-servitec-navy uppercase tracking-wider">
            <Filter className="w-4 h-4 text-servitec-primary" />
            <span>Filtros de auditoría</span>
          </div>

          {(idRepuestoFiltro ||
            tipoFiltro !== 'TODOS' ||
            origenFiltro !== 'TODOS' ||
            fechaDesde ||
            fechaHasta) && (
            <button
              type="button"
              onClick={limpiarFiltros}
              className="inline-flex items-center gap-1 text-xs font-semibold text-rose-600 hover:underline cursor-pointer"
            >
              <XCircle className="w-3.5 h-3.5" />
              <span>Limpiar filtros</span>
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <div>
            <label className="block text-[11px] font-semibold text-slate-500 mb-1">
              Repuesto / Insumo
            </label>
            <select
              value={idRepuestoFiltro}
              onChange={(e) => setIdRepuestoFiltro(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            >
              <option value="">Todos los elementos</option>
              {catalogItems.map((item) => (
                <option key={item.idRepuesto} value={item.idRepuesto}>
                  {item.codigoRepuesto} — {item.nombre}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-500 mb-1">
              Tipo de movimiento
            </label>
            <select
              value={tipoFiltro}
              onChange={(e) => setTipoFiltro(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            >
              <option value="TODOS">Todos</option>
              <option value="ENTRADA">ENTRADA</option>
              <option value="SALIDA">SALIDA</option>
              <option value="AJUSTE">AJUSTE</option>
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-500 mb-1">
              Origen del registro
            </label>
            <select
              value={origenFiltro}
              onChange={(e) => setOrigenFiltro(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            >
              <option value="TODOS">Todos los orígenes</option>
              <option value="MANUAL">MANUAL (Administrador)</option>
              <option value="INICIALIZACION">INICIALIZACION (Apertura)</option>
              <option value="RN14">RN14 (Consumo Orden)</option>
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-500 mb-1">
              Desde fecha
            </label>
            <input
              type="date"
              value={fechaDesde}
              onChange={(e) => setFechaDesde(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-500 mb-1">
              Hasta fecha
            </label>
            <input
              type="date"
              value={fechaHasta}
              onChange={(e) => setFechaHasta(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
          </div>
        </div>
      </div>

      {/* Tabla de Historial de Movimientos */}
      {movements.length === 0 && !loading ? (
        <EmptyState
          title="No se encontraron movimientos registrados"
          description="Ajusta los filtros de búsqueda o registra un nuevo movimiento manual de inventario."
          action={
            <button
              type="button"
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-servitec-primary text-white text-xs font-semibold cursor-pointer"
            >
              <ArrowLeftRight className="w-4 h-4" />
              <span>Registrar movimiento</span>
            </button>
          }
        />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Fecha / Hora</th>
                  <th className="py-3.5 px-4">Elemento</th>
                  <th className="py-3.5 px-4">Tipo / Origen</th>
                  <th className="py-3.5 px-4 text-center">Anterior</th>
                  <th className="py-3.5 px-4 text-center">Movimiento</th>
                  <th className="py-3.5 px-4 text-center">Resultante</th>
                  <th className="py-3.5 px-4">Motivo y Responsable</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-xs">
                {movements.map((mov) => {
                  const typeCfg = MOVEMENT_TYPE_BADGE[mov.tipoMovimiento] || {
                    label: mov.tipoMovimiento,
                    classes: 'bg-slate-100 text-slate-700',
                    icon: ArrowLeftRight,
                  };
                  const originCfg = ORIGIN_BADGE[mov.origenMovimiento] || {
                    label: mov.origenMovimiento,
                    classes: 'bg-slate-100 text-slate-700',
                  };
                  const TypeIcon = typeCfg.icon;

                  const delta =
                    Number(mov.existenciasResultantes) - Number(mov.existenciasAnteriores);

                  return (
                    <tr key={mov.idMovimiento} className="hover:bg-slate-50/80">
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span className="font-semibold text-slate-800 block">
                          {formatDateTime(mov.fechaHoraMovimiento)}
                        </span>
                        <span className="text-[11px] text-slate-400">
                          Mov #{mov.idMovimiento}
                        </span>
                      </td>

                      <td className="py-3.5 px-4">
                        <span className="font-mono text-[11px] font-bold text-servitec-primary block">
                          {mov.codigoRepuesto}
                        </span>
                        <span className="font-bold text-servitec-navy">
                          {mov.nombreRepuesto}
                        </span>
                        <span className="block text-[11px] text-slate-400">
                          Unidad: {mov.unidadMedida}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${typeCfg.classes}`}
                        >
                          <TypeIcon className="w-3 h-3" />
                          {typeCfg.label}
                        </span>
                        <span
                          className={`block w-fit mt-1 px-2 py-0.5 rounded-md text-[10px] font-bold ${originCfg.classes}`}
                        >
                          {originCfg.label}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-center font-semibold text-slate-600">
                        {formatQuantity(mov.existenciasAnteriores)}
                      </td>

                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`inline-block font-bold ${
                            delta > 0
                              ? 'text-emerald-700'
                              : delta < 0
                              ? 'text-rose-700'
                              : 'text-slate-700'
                          }`}
                        >
                          {delta > 0 ? '+' : ''}
                          {formatQuantity(delta)}
                        </span>
                        {mov.cantidadContada !== null &&
                          mov.cantidadContada !== undefined && (
                            <span className="block text-[10px] text-amber-700 font-medium">
                              Conteo: {formatQuantity(mov.cantidadContada)}
                            </span>
                          )}
                      </td>

                      <td className="py-3.5 px-4 text-center">
                        <span className="inline-flex items-center px-2.5 py-1 rounded-lg bg-slate-100 font-bold text-servitec-navy">
                          {formatQuantity(mov.existenciasResultantes)}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 max-w-sm">
                        <p className="text-slate-800 font-medium leading-snug">
                          {mov.motivo}
                        </p>
                        <span className="block text-[11px] text-slate-500 mt-0.5">
                          Registrado por: <b>{mov.nombreUsuarioResponsable}</b>
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal de Registro Manual */}
      <ManualMovementModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        catalogItems={catalogItems}
        onMovementRegistered={handleMovementRegistered}
      />
    </div>
  );
}