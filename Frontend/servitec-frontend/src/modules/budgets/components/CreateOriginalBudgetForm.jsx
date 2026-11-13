import { useState, useEffect, useMemo } from 'react';
import {
  Calculator,
  Search,
  Plus,
  Trash2,
  Lock,
  Info,
  Package,
  Send,
} from 'lucide-react';
import { inventoryApi } from '../../../api/inventoryApi';
import { budgetsApi } from '../../../api/budgetsApi';
import { extractApiError } from '../../../api/httpClient';
import { formatCurrencyCOP, formatQuantity } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function CreateOriginalBudgetForm({ order, onBudgetCreated }) {
  const [catalogoActivo, setCatalogoActivo] = useState([]);
  const [busquedaCatalogo, setBusquedaCatalogo] = useState('');
  const [loadingCatalog, setLoadingCatalog] = useState(true);

  const [tarifaHora, setTarifaHora] = useState('60000');
  const [horasEstimadas, setHorasEstimadas] = useState('2');
  const [itemsSeleccionados, setItemsSeleccionados] = useState([]);

  const [submitting, setSubmitting] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    let active = true;
    inventoryApi
      .listarCatalogo({ activo: true })
      .then((data) => {
        if (active) setCatalogoActivo(data);
      })
      .catch((err) => {
        if (active) {
          setErrorInfo(
            extractApiError(err, 'No fue posible cargar el catálogo activo de repuestos.')
          );
        }
      })
      .finally(() => {
        if (active) setLoadingCatalog(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const resultadosBusqueda = useMemo(() => {
    const idsAgregados = new Set(itemsSeleccionados.map((i) => i.idRepuesto));
    const q = busquedaCatalogo.trim().toLowerCase();

    return catalogoActivo
      .filter((item) => !idsAgregados.has(item.idRepuesto))
      .filter(
        (item) =>
          !q ||
          item.nombre?.toLowerCase().includes(q) ||
          item.codigoRepuesto?.toLowerCase().includes(q)
      )
      .slice(0, 6);
  }, [catalogoActivo, busquedaCatalogo, itemsSeleccionados]);

  const agregarItemDelCatalogo = (repuesto) => {
    setItemsSeleccionados((prev) => [
      ...prev,
      {
        idRepuesto: repuesto.idRepuesto,
        codigoRepuesto: repuesto.codigoRepuesto,
        nombre: repuesto.nombre,
        unidadMedida: repuesto.unidadMedida,
        precioUnitarioActual: Number(repuesto.precioUnitarioActual),
        existenciasDisponibles: Number(repuesto.existenciasDisponibles),
        cantidadAutorizada: '1',
        justificacion: '',
      },
    ]);
  };

  const actualizarItem = (idRepuesto, campo, valor) => {
    setItemsSeleccionados((prev) =>
      prev.map((item) =>
        item.idRepuesto === idRepuesto ? { ...item, [campo]: valor } : item
      )
    );
  };

  const eliminarItem = (idRepuesto) => {
    setItemsSeleccionados((prev) => prev.filter((i) => i.idRepuesto !== idRepuesto));
  };

  // Cálculos automáticos en tiempo real (REQ-12)
  const calculos = useMemo(() => {
    const tarifa = Number(tarifaHora) > 0 ? Number(tarifaHora) : 0;
    const horas = Number(horasEstimadas) > 0 ? Number(horasEstimadas) : 0;
    const manoObraEstimada = Math.round(tarifa * horas);

    const subtotalRepuestos = itemsSeleccionados.reduce((acc, item) => {
      const cant = Number(item.cantidadAutorizada) > 0 ? Number(item.cantidadAutorizada) : 0;
      return acc + Math.round(item.precioUnitarioActual * cant);
    }, 0);

    return {
      manoObraEstimada,
      subtotalRepuestos,
      totalEstimado: manoObraEstimada + subtotalRepuestos,
    };
  }, [tarifaHora, horasEstimadas, itemsSeleccionados]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);

    if (Number(tarifaHora) <= 0 || Number(horasEstimadas) <= 0) {
      setErrorInfo({
        message: 'La tarifa por hora y las horas estimadas deben ser mayores que cero.',
      });
      return;
    }

    for (const item of itemsSeleccionados) {
      if (!item.cantidadAutorizada || Number(item.cantidadAutorizada) <= 0) {
        setErrorInfo({
          message: `Ingresa una cantidad válida mayor que cero para "${item.nombre}".`,
        });
        return;
      }
    }

    setSubmitting(true);
    try {
      const creado = await budgetsApi.crearPresupuestoOriginal(order.idOrden, {
        tarifaHora,
        horasEstimadas,
        detalles: itemsSeleccionados,
      });
      onBudgetCreated(creado);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible generar el presupuesto original.')
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
      <div className="px-5 py-4 bg-servitec-navy text-white flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-servitec-primary flex items-center justify-center shrink-0">
          <Calculator className="w-5 h-5 text-white" />
        </div>
        <div>
          <h3 className="text-sm font-bold">
            Elaborar Presupuesto Original · {order.codigoOrden}
          </h3>
          <p className="text-[11px] text-sky-300">
            Selecciona repuestos del catálogo oficial (precios bloqueados RN-03) y define la mano de obra estimada (CV-05)
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="p-5 space-y-6">
        {errorInfo && (
          <AlertBanner
            type="error"
            message={errorInfo.message}
            fieldErrors={errorInfo.fieldErrors}
            onClose={() => setErrorInfo(null)}
          />
        )}

        {/* 1. Configuración de Mano de Obra Estimada */}
        <div className="bg-slate-50 rounded-2xl border border-slate-200 p-4 space-y-3">
          <p className="text-xs font-bold text-servitec-navy uppercase tracking-wider">
            1. Estimación de mano de obra técnica
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-end">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Tarifa por hora (COP) *
              </label>
              <input
                type="number"
                required
                min="1"
                step="1"
                value={tarifaHora}
                onChange={(e) => setTarifaHora(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-white border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Horas estimadas de trabajo *
              </label>
              <input
                type="number"
                required
                min="0.1"
                step="0.1"
                value={horasEstimadas}
                onChange={(e) => setHorasEstimadas(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-white border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>

            <div className="bg-white rounded-xl border border-slate-200 p-2.5 text-right">
              <span className="block text-[11px] text-slate-500">
                Costo estimado mano de obra
              </span>
              <span className="text-sm font-bold text-servitec-navy">
                {formatCurrencyCOP(calculos.manoObraEstimada)}
              </span>
            </div>
          </div>

          <div className="rounded-xl bg-sky-50 border border-sky-200 p-3 flex items-start gap-2 text-sky-900 text-[11px]">
            <Info className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <span>
              <b>Condición de liquidación (REQ-12 y RN-11):</b> El valor final de la mano de obra se determinará según el tiempo efectivo de ejecución registrado por el sistema, aplicando la tarifa por hora aprobada.
            </span>
          </div>
        </div>

        {/* 2. Buscador de Catálogo Activo */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-xs font-bold text-servitec-navy uppercase tracking-wider">
              2. Repuestos e insumos del catálogo activo (Opcional si solo requiere mano de obra)
            </p>
            <span className="text-[11px] text-slate-500 flex items-center gap-1">
              <Lock className="w-3 h-3 text-servitec-primary" />
              Precios oficiales no editables
            </span>
          </div>

          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={busquedaCatalogo}
              onChange={(e) => setBusquedaCatalogo(e.target.value)}
              placeholder="Buscar repuesto o insumo activo por código o nombre para agregar..."
              className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
          </div>

          {!loadingCatalog && resultadosBusqueda.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 bg-slate-50 p-3 rounded-xl border border-slate-200">
              {resultadosBusqueda.map((rep) => (
                <div
                  key={rep.idRepuesto}
                  className="bg-white rounded-xl border border-slate-200 p-3 flex items-center justify-between gap-2"
                >
                  <div className="min-w-0">
                    <span className="font-mono text-[11px] font-bold text-servitec-primary block">
                      {rep.codigoRepuesto}
                    </span>
                    <p className="text-xs font-bold text-servitec-navy truncate">
                      {rep.nombre}
                    </p>
                    <div className="flex items-center gap-2 text-[11px] text-slate-500 mt-0.5">
                      <span>{formatCurrencyCOP(rep.precioUnitarioActual)}</span>
                      <span>·</span>
                      <span>
                        Disp: <b>{formatQuantity(rep.existenciasDisponibles)}</b>{' '}
                        {rep.unidadMedida}
                      </span>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => agregarItemDelCatalogo(rep)}
                    className="inline-flex items-center gap-1 px-3 py-2 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold shrink-0 cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Agregar</span>
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* Lista de elementos agregados al presupuesto */}
          {itemsSeleccionados.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 p-5 text-center text-xs text-slate-500">
              <Package className="w-5 h-5 text-slate-400 mx-auto mb-1" />
              No has agregado repuestos o insumos. Puedes agregar elementos desde el buscador superior o emitir el presupuesto solo con mano de obra.
            </div>
          ) : (
            <div className="space-y-3">
              {itemsSeleccionados.map((item) => {
                const importe = Math.round(
                  item.precioUnitarioActual * (Number(item.cantidadAutorizada) || 0)
                );

                return (
                  <div
                    key={item.idRepuesto}
                    className="bg-slate-50 rounded-xl border border-slate-200 p-3.5 space-y-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <span className="font-mono text-[11px] font-bold text-servitec-primary">
                          {item.codigoRepuesto}
                        </span>
                        <h4 className="text-xs font-bold text-servitec-navy">
                          {item.nombre} ({item.unidadMedida})
                        </h4>
                      </div>

                      <button
                        type="button"
                        onClick={() => eliminarItem(item.idRepuesto)}
                        className="inline-flex items-center gap-1 text-xs font-semibold text-rose-600 hover:underline cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>Quitar</span>
                      </button>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 items-center">
                      <div>
                        <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                          Precio unitario (Catálogo)
                        </label>
                        <div className="px-3 py-2 rounded-lg bg-slate-200/70 text-slate-700 text-xs font-bold flex items-center justify-between">
                          <span>{formatCurrencyCOP(item.precioUnitarioActual)}</span>
                          <Lock className="w-3.5 h-3.5 text-slate-500" />
                        </div>
                      </div>

                      <div>
                        <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                          Cantidad requerida (Disp: {formatQuantity(item.existenciasDisponibles)}) *
                        </label>
                        <input
                          type="number"
                          required
                          min="0.01"
                          step="0.01"
                          value={item.cantidadAutorizada}
                          onChange={(e) =>
                            actualizarItem(
                              item.idRepuesto,
                              'cantidadAutorizada',
                              e.target.value
                            )
                          }
                          className="w-full px-3 py-2 rounded-lg bg-white border border-slate-300 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                        />
                      </div>

                      <div className="text-right">
                        <span className="block text-[11px] text-slate-500">
                          Importe línea
                        </span>
                        <span className="text-sm font-bold text-servitec-navy">
                          {formatCurrencyCOP(importe)}
                        </span>
                      </div>
                    </div>

                    <div>
                      <input
                        type="text"
                        maxLength={500}
                        value={item.justificacion}
                        onChange={(e) =>
                          actualizarItem(item.idRepuesto, 'justificacion', e.target.value)
                        }
                        placeholder="Nota o descripción de uso del elemento (opcional en presupuesto original)..."
                        className="w-full px-3 py-2 rounded-lg bg-white border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 3. Resumen Económico Total */}
        <div className="rounded-2xl bg-servitec-navy text-white p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1 text-xs">
            <p className="text-sky-300 font-semibold">Resumen automático del presupuesto</p>
            <p className="text-slate-300">
              Subtotal repuestos e insumos:{' '}
              <b className="text-white">{formatCurrencyCOP(calculos.subtotalRepuestos)}</b>{' '}
              + Mano de obra estimada:{' '}
              <b className="text-white">{formatCurrencyCOP(calculos.manoObraEstimada)}</b>
            </p>
          </div>

          <div className="text-left sm:text-right">
            <span className="block text-[11px] text-sky-300 uppercase tracking-wider">
              Valor total estimado
            </span>
            <span className="text-xl font-bold text-white">
              {formatCurrencyCOP(calculos.totalEstimado)}
            </span>
          </div>
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full min-h-[48px] py-3 px-5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-sm font-semibold shadow-xs flex items-center justify-center gap-2 transition-colors disabled:opacity-60 cursor-pointer"
        >
          <Send className="w-4 h-4" />
          <span>
            {submitting
              ? 'Generando presupuesto...'
              : 'Emitir presupuesto y enviar a aprobación del Cliente'}
          </span>
        </button>
      </form>
    </div>
  );
}