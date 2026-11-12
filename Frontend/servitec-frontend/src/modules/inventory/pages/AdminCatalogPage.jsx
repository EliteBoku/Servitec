import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  PackagePlus,
  Search,
  Package,
  Layers,
  Lock,
  CheckCircle2,
  Edit3,
  Power,
  RefreshCw,
  ArrowLeftRight,
} from 'lucide-react';
import { inventoryApi } from '../../../api/inventoryApi';
import { extractApiError } from '../../../api/httpClient';
import {
  formatCurrencyCOP,
  formatQuantity,
  formatDateTime,
} from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';
import EmptyState from '../../../shared/components/EmptyState';
import ConfirmModal from '../../../shared/components/ConfirmModal';
import CatalogItemModal from '../components/CatalogItemModal';

export default function AdminCatalogPage() {
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [tipoFiltro, setTipoFiltro] = useState('TODOS');
  const [estadoFiltro, setEstadoFiltro] = useState('TODOS');

  const [errorInfo, setErrorInfo] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState(null);

  const [itemToToggle, setItemToToggle] = useState(null);
  const [toggleLoading, setToggleLoading] = useState(false);

  const cargarCatalogo = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);
    try {
      const activoParam =
        estadoFiltro === 'ACTIVOS'
          ? true
          : estadoFiltro === 'INACTIVOS'
          ? false
          : null;

      const data = await inventoryApi.listarCatalogo({
        busqueda,
        tipoItem: tipoFiltro,
        activo: activoParam,
      });
      setItems(data);
    } catch (err) {
      setErrorInfo(extractApiError(err, 'No se pudo cargar el catálogo de inventario.'));
    } finally {
      setLoading(false);
    }
  }, [busqueda, tipoFiltro, estadoFiltro]);

  useEffect(() => {
    const timer = setTimeout(() => {
      cargarCatalogo();
    }, 250);
    return () => clearTimeout(timer);
  }, [cargarCatalogo]);

  const handleSavedItem = (savedItem, wasEdit) => {
    setSuccessMessage(
      wasEdit
        ? `El elemento "${savedItem.nombre}" (${savedItem.codigoRepuesto}) fue actualizado correctamente.`
        : `El elemento "${savedItem.nombre}" (${savedItem.codigoRepuesto}) fue registrado en el catálogo.`
    );
    cargarCatalogo();
  };

  const handleConfirmToggleStatus = async () => {
    if (!itemToToggle) return;
    setToggleLoading(true);
    setErrorInfo(null);
    try {
      if (itemToToggle.activo) {
        await inventoryApi.desactivarRepuesto(itemToToggle.idRepuesto);
        setSuccessMessage(
          `El elemento "${itemToToggle.nombre}" fue desactivado y ya no aparecerá en nuevos presupuestos.`
        );
      } else {
        await inventoryApi.reactivarRepuesto(itemToToggle.idRepuesto);
        setSuccessMessage(
          `El elemento "${itemToToggle.nombre}" fue reactivado y está disponible nuevamente en el catálogo activo.`
        );
      }
      setItemToToggle(null);
      cargarCatalogo();
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible cambiar el estado del elemento.')
      );
      setItemToToggle(null);
    } finally {
      setToggleLoading(false);
    }
  };

  const resumen = useMemo(() => {
    return {
      totalItems: items.length,
      activos: items.filter((i) => i.activo).length,
      conReservas: items.filter((i) => Number(i.cantidadesReservadas) > 0).length,
      sinDisponible: items.filter((i) => Number(i.existenciasDisponibles) <= 0).length,
    };
  }, [items]);

  return (
    <div className="space-y-6">
      {/* Encabezado */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-servitec-navy">
            Catálogo de Repuestos e Insumos
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Administración de precios unitarios base, estado activo y control de existencias físicas vs reservadas (CV-18).
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={cargarCatalogo}
            title="Refrescar catálogo"
            className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            type="button"
            onClick={() => navigate('/admin/movimientos')}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-servitec-navy text-servitec-navy hover:bg-slate-100 text-xs font-semibold transition-colors cursor-pointer"
          >
            <ArrowLeftRight className="w-4 h-4" />
            <span>Movimientos manuales</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setItemToEdit(null);
              setIsModalOpen(true);
            }}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
          >
            <PackagePlus className="w-4 h-4" />
            <span>Nuevo repuesto / insumo</span>
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

      {/* Tarjetas de métricas */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-servitec-navy/10 text-servitec-navy flex items-center justify-center">
            <Package className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Elementos listados</p>
            <p className="text-xl font-bold text-servitec-navy">{resumen.totalItems}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Activos en catálogo</p>
            <p className="text-xl font-bold text-servitec-navy">{resumen.activos}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Con reservas activas</p>
            <p className="text-xl font-bold text-servitec-navy">{resumen.conReservas}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center">
            <Layers className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Agotados (Disp. 0)</p>
            <p className="text-xl font-bold text-servitec-navy">{resumen.sinDisponible}</p>
          </div>
        </div>
      </div>

      {/* Filtros */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por código único o nombre del repuesto / insumo..."
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <select
            value={tipoFiltro}
            onChange={(e) => setTipoFiltro(e.target.value)}
            className="px-3 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          >
            <option value="TODOS">Todos los tipos</option>
            <option value="REPUESTO">Solo Repuestos</option>
            <option value="INSUMO">Solo Insumos</option>
          </select>

          <select
            value={estadoFiltro}
            onChange={(e) => setEstadoFiltro(e.target.value)}
            className="px-3 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          >
            <option value="TODOS">Activos e Inactivos</option>
            <option value="ACTIVOS">Solo Activos</option>
            <option value="INACTIVOS">Solo Inactivos</option>
          </select>
        </div>
      </div>

      {/* Tabla de Catálogo */}
      {items.length === 0 && !loading ? (
        <EmptyState
          title="No se encontraron elementos en el catálogo"
          description="Registra repuestos e insumos para habilitar la elaboración de presupuestos y el control de inventario."
          action={
            <button
              type="button"
              onClick={() => {
                setItemToEdit(null);
                setIsModalOpen(true);
              }}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-servitec-primary text-white text-xs font-semibold cursor-pointer"
            >
              <PackagePlus className="w-4 h-4" />
              <span>Registrar primer elemento</span>
            </button>
          }
        />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Elemento</th>
                  <th className="py-3.5 px-4">Tipo / Unidad</th>
                  <th className="py-3.5 px-4 text-right">Precio Unitario</th>
                  <th className="py-3.5 px-4 text-center">Físicas</th>
                  <th className="py-3.5 px-4 text-center">Reservadas</th>
                  <th className="py-3.5 px-4 text-center">Disponibles</th>
                  <th className="py-3.5 px-4 text-center">Estado</th>
                  <th className="py-3.5 px-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-xs">
                {items.map((item) => {
                  const disponibles = Number(item.existenciasDisponibles);
                  const reservadas = Number(item.cantidadesReservadas);

                  return (
                    <tr
                      key={item.idRepuesto}
                      className={`hover:bg-slate-50/80 ${
                        !item.activo ? 'bg-slate-50/40 opacity-75' : ''
                      }`}
                    >
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-11 h-11 rounded-xl border border-slate-200 bg-slate-100 flex items-center justify-center overflow-hidden shrink-0">
                            {item.imagenUrl ? (
                              <img
                                src={item.imagenUrl}
                                alt={item.nombre}
                                onError={(e) => {
                                  e.currentTarget.style.display = 'none';
                                }}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <Package className="w-5 h-5 text-servitec-primary" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <span className="inline-block font-mono text-[11px] font-bold text-servitec-primary">
                              {item.codigoRepuesto}
                            </span>
                            <p className="font-bold text-servitec-navy truncate max-w-xs">
                              {item.nombre}
                            </p>
                            <p className="text-[11px] text-slate-500 truncate max-w-xs">
                              {item.descripcion}
                            </p>
                          </div>
                        </div>
                      </td>

                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-block px-2 py-0.5 rounded-md text-[10px] font-bold uppercase ${
                            item.tipoItem === 'REPUESTO'
                              ? 'bg-sky-100 text-sky-800'
                              : 'bg-indigo-100 text-indigo-800'
                          }`}
                        >
                          {item.tipoItem}
                        </span>
                        <span className="block text-[11px] text-slate-500 mt-0.5">
                          {item.unidadMedida}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-right font-bold text-servitec-navy whitespace-nowrap">
                        {formatCurrencyCOP(item.precioUnitarioActual)}
                      </td>

                      <td className="py-3.5 px-4 text-center font-semibold text-slate-700">
                        {formatQuantity(item.existenciasFisicas)}
                      </td>

                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                            reservadas > 0
                              ? 'bg-amber-100 text-amber-800'
                              : 'text-slate-400'
                          }`}
                        >
                          {formatQuantity(item.cantidadesReservadas)}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold ${
                            disponibles > 0
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-rose-100 text-rose-800'
                          }`}
                        >
                          {formatQuantity(item.existenciasDisponibles)}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold border ${
                            item.activo
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              : 'bg-slate-100 text-slate-600 border-slate-300'
                          }`}
                        >
                          {item.activo ? 'Activo' : 'Inactivo'}
                        </span>
                        <span className="block text-[10px] text-slate-400 mt-0.5">
                          Act: {formatDateTime(item.fechaActualizacion)}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => {
                              setItemToEdit(item);
                              setIsModalOpen(true);
                            }}
                            title="Editar datos y precio base"
                            className="p-2 rounded-xl border border-slate-200 text-slate-700 hover:bg-sky-50 hover:text-servitec-primary hover:border-sky-200 transition-colors cursor-pointer"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>

                          <button
                            type="button"
                            onClick={() => setItemToToggle(item)}
                            title={item.activo ? 'Desactivar elemento' : 'Reactivar elemento'}
                            className={`p-2 rounded-xl border transition-colors cursor-pointer ${
                              item.activo
                                ? 'border-slate-200 text-rose-600 hover:bg-rose-50 hover:border-rose-200'
                                : 'border-slate-200 text-emerald-600 hover:bg-emerald-50 hover:border-emerald-200'
                            }`}
                          >
                            <Power className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal Crear / Editar */}
      <CatalogItemModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        itemToEdit={itemToEdit}
        onSaved={handleSavedItem}
      />

      {/* Modal Confirmar Desactivación / Reactivación */}
      <ConfirmModal
        isOpen={Boolean(itemToToggle)}
        title={
          itemToToggle?.activo
            ? 'Desactivar elemento del catálogo'
            : 'Reactivar elemento del catálogo'
        }
        message={
          itemToToggle?.activo
            ? `¿Confirmas desactivar "${itemToToggle?.nombre}" (${itemToToggle?.codigoRepuesto})? Dejará de estar disponible para nuevos presupuestos, conservando intactos los presupuestos históricos donde ya fue incluido.`
            : `¿Confirmas reactivar "${itemToToggle?.nombre}" (${itemToToggle?.codigoRepuesto}) para que vuelva a estar disponible en la elaboración de presupuestos?`
        }
        confirmText={itemToToggle?.activo ? 'Sí, desactivar' : 'Sí, reactivar'}
        variant={itemToToggle?.activo ? 'danger' : 'success'}
        loading={toggleLoading}
        onConfirm={handleConfirmToggleStatus}
        onCancel={() => setItemToToggle(null)}
      />
    </div>
  );
}