import { useState, useEffect, useMemo } from 'react';
import {
  FilePlus,
  Search,
  Plus,
  Trash2,
  Lock,
  Send,
  ShieldCheck,
} from 'lucide-react';
import { inventoryApi } from '../../../api/inventoryApi';
import { budgetsApi } from '../../../api/budgetsApi';
import { extractApiError } from '../../../api/httpClient';
import { formatCurrencyCOP, formatQuantity } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function CreateComplementaryForm({
  order,
  activeAuthorization = null,
  onCreated,
}) {
  const [catalogoActivo, setCatalogoActivo] = useState([]);
  const [busqueda, setBusqueda] = useState('');
  const [items, setItems] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    inventoryApi
      .listarCatalogo({ activo: true })
      .then((data) => setCatalogoActivo(data))
      .catch(() => {});
  }, []);

  const sugerencias = useMemo(() => {
    const agregados = new Set(items.map((i) => i.idRepuesto));
    const q = busqueda.trim().toLowerCase();
    return catalogoActivo
      .filter((r) => !agregados.has(r.idRepuesto))
      .filter(
        (r) =>
          !q ||
          r.nombre?.toLowerCase().includes(q) ||
          r.codigoRepuesto?.toLowerCase().includes(q)
      )
      .slice(0, 5);
  }, [catalogoActivo, busqueda, items]);

  const agregarRepuesto = (rep) => {
    setItems((prev) => [
      ...prev,
      {
        idRepuesto: rep.idRepuesto,
        codigoRepuesto: rep.codigoRepuesto,
        nombre: rep.nombre,
        unidadMedida: rep.unidadMedida,
        precioUnitarioActual: Number(rep.precioUnitarioActual),
        existenciasDisponibles: Number(rep.existenciasDisponibles),
        cantidadAutorizada: '1',
        justificacion: '',
      },
    ]);
  };

  const actualizarItem = (idRepuesto, campo, valor) => {
    setItems((prev) =>
      prev.map((i) => (i.idRepuesto === idRepuesto ? { ...i, [campo]: valor } : i))
    );
  };

  const quitarItem = (idRepuesto) => {
    setItems((prev) => prev.filter((i) => i.idRepuesto !== idRepuesto));
  };

  const totalAdicional = useMemo(() => {
    return items.reduce((acc, i) => {
      const cant = Number(i.cantidadAutorizada) > 0 ? Number(i.cantidadAutorizada) : 0;
      return acc + Math.round(i.precioUnitarioActual * cant);
    }, 0);
  }, [items]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);

    if (items.length === 0) {
      setErrorInfo({
        message: 'Debes seleccionar al menos un repuesto o insumo adicional del catálogo activo.',
      });
      return;
    }

    for (const item of items) {
      if (!item.cantidadAutorizada || Number(item.cantidadAutorizada) <= 0) {
        setErrorInfo({
          message: `Ingresa una cantidad válida para "${item.nombre}".`,
        });
        return;
      }
      if (!item.justificacion || !item.justificacion.trim()) {
        setErrorInfo({
          message: `Ingresa la justificación obligatoria para el elemento adicional "${item.nombre}".`,
        });
        return;
      }
    }

    setSubmitting(true);
    try {
      let creado;
      if (activeAuthorization) {
        creado = await budgetsApi.crearNuevaCotizacionComplementaria(
          order.idOrden,
          activeAuthorization.idAutorizacion,
          { detalles: items }
        );
      } else {
        creado = await budgetsApi.crearPresupuestoComplementario(order.idOrden, {
          detalles: items,
        });
      }
      setItems([]);
      onCreated(creado);
    } catch (err) {
      setErrorInfo(
        extractApiError(
          err,
          'No fue posible generar el presupuesto complementario.'
        )
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
      <div className="px-5 py-4 bg-servitec-navy text-white flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-servitec-primary flex items-center justify-center shrink-0">
          <FilePlus className="w-5 h-5 text-white" />
        </div>
        <div>
          <h3 className="text-sm font-bold">
            {activeAuthorization
              ? 'Elaborar Nueva Propuesta Autorizada (CV-15)'
              : 'Emitir Presupuesto Complementario (CV-12)'}
          </h3>
          <p className="text-[11px] text-sky-300">
            Registra repuestos o insumos adicionales con justificación obligatoria y notificación por correo al Cliente
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="p-5 space-y-4">
        {errorInfo && (
          <AlertBanner
            type="error"
            message={errorInfo.message}
            fieldErrors={errorInfo.fieldErrors}
            onClose={() => setErrorInfo(null)}
          />
        )}

        {activeAuthorization && (
          <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-3.5 flex items-start gap-2.5 text-emerald-900 text-xs">
            <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-bold">
                Autorización Administrativa #{activeAuthorization.idAutorizacion} activa
              </p>
              <p className="mt-0.5">
                Autorizada por <b>{activeAuthorization.nombreAdministrador}</b>. Esta propuesta se guardará como un nuevo presupuesto independiente conservando el rechazado en el historial (REQ-24).
              </p>
            </div>
          </div>
        )}

        {/* Buscador de catálogo activo */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar repuesto o insumo adicional en el catálogo activo..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          />
        </div>

        {sugerencias.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 bg-slate-50 p-3 rounded-xl border border-slate-200">
            {sugerencias.map((rep) => (
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
                  <span className="text-[11px] text-slate-500">
                    {formatCurrencyCOP(rep.precioUnitarioActual)} · Disp:{' '}
                    <b>{formatQuantity(rep.existenciasDisponibles)}</b>
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => agregarRepuesto(rep)}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-servitec-primary text-white text-xs font-semibold cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Agregar</span>
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Ítems seleccionados con justificación obligatoria */}
        {items.length > 0 && (
          <div className="space-y-3">
            {items.map((item) => (
              <div
                key={item.idRepuesto}
                className="bg-slate-50 rounded-xl border border-slate-200 p-3.5 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-servitec-navy">
                    {item.codigoRepuesto} — {item.nombre}
                  </span>
                  <button
                    type="button"
                    onClick={() => quitarItem(item.idRepuesto)}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-rose-600 cursor-pointer"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Quitar</span>
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 items-center">
                  <div className="px-3 py-2 rounded-lg bg-slate-200/70 text-xs font-bold text-slate-700 flex items-center justify-between">
                    <span>{formatCurrencyCOP(item.precioUnitarioActual)}</span>
                    <Lock className="w-3.5 h-3.5 text-slate-500" />
                  </div>

                  <div>
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
                      placeholder="Cantidad *"
                      className="w-full px-3 py-2 rounded-lg bg-white border border-slate-300 text-xs font-bold"
                    />
                  </div>

                  <div className="text-right text-xs font-bold text-servitec-navy">
                    Subtotal:{' '}
                    {formatCurrencyCOP(
                      Math.round(
                        item.precioUnitarioActual *
                          (Number(item.cantidadAutorizada) || 0)
                      )
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                    Justificación técnica de la necesidad del elemento adicional *
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={500}
                    value={item.justificacion}
                    onChange={(e) =>
                      actualizarItem(
                        item.idRepuesto,
                        'justificacion',
                        e.target.value
                      )
                    }
                    placeholder="Explica por qué se requiere este repuesto o insumo adicional..."
                    className="w-full px-3 py-2 rounded-lg bg-white border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                  />
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="rounded-xl bg-servitec-navy text-white p-4 flex items-center justify-between">
          <span className="text-xs text-sky-300 font-semibold">
            Valor total adicional cotizado
          </span>
          <span className="text-lg font-bold">
            {formatCurrencyCOP(totalAdicional)}
          </span>
        </div>

        <button
          type="submit"
          disabled={submitting || items.length === 0}
          className="w-full min-h-[48px] py-3 px-5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs sm:text-sm font-semibold flex items-center justify-center gap-2 transition-colors disabled:opacity-50 cursor-pointer"
        >
          <Send className="w-4 h-4" />
          <span>
            {submitting
              ? 'Enviando presupuesto complementario...'
              : 'Emitir presupuesto complementario y notificar al Cliente'}
          </span>
        </button>
      </form>
    </div>
  );
}