import { useState, useEffect } from 'react';
import { PauseCircle, X, Info } from 'lucide-react';
import { executionApi } from '../../../api/executionApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function PauseOrderModal({ isOpen, onClose, order, onPaused }) {
  const [justificacion, setJustificacion] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    if (isOpen) {
      setJustificacion('');
      setErrorInfo(null);
    }
  }, [isOpen]);

  if (!isOpen || !order) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!justificacion.trim()) {
      setErrorInfo({ message: 'Debes ingresar el motivo de la pausa.' });
      return;
    }

    setErrorInfo(null);
    setLoading(true);
    try {
      const resumenActualizado = await executionApi.pausarReparacion(order.idOrden, {
        justificacion,
      });
      onPaused(resumenActualizado);
      onClose();
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible pausar la reparación.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-lg overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 bg-servitec-navy text-white">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-amber-500 flex items-center justify-center">
              <PauseCircle className="w-4 h-4 text-white" />
            </div>
            <div>
              <h3 className="text-sm font-bold">
                Pausar Reparación · {order.codigoOrden}
              </h3>
              <p className="text-[11px] text-sky-300">
                Detiene el cómputo de tiempo efectivo y cambia el estado a Pausada
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="text-slate-300 hover:text-white cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              fieldErrors={errorInfo.fieldErrors}
              onClose={() => setErrorInfo(null)}
            />
          )}

          <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 flex items-start gap-2.5 text-amber-900 text-xs">
            <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <p>
              Al pausar la orden se cierra el intervalo de tiempo actual. Mientras la orden permanezca en estado <b>Pausada</b>, no se sumará tiempo de mano de obra y podrás emitir un presupuesto complementario si se requieren repuestos adicionales (RN-08 y RN-12).
            </p>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Motivo o justificación de la pausa *
              </label>
              <span className="text-[11px] text-slate-400">
                {justificacion.length} / 800
              </span>
            </div>
            <textarea
              rows={3}
              required
              maxLength={800}
              value={justificacion}
              onChange={(e) => setJustificacion(e.target.value)}
              placeholder="Ej. Se identificó desgaste interno en el termostato que requiere emitir presupuesto complementario..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-100 cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold transition-colors disabled:opacity-60 cursor-pointer"
            >
              {loading ? 'Pausando orden...' : 'Confirmar pausa'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}