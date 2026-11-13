import React, { useState } from 'react';
import { ClipboardList, Send } from 'lucide-react';
import { budgetsApi } from '../../../api/budgetsApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function TechnicalEvaluationForm({
  rejectedBudget,
  onEvaluationSaved,
}) {
  const [elementosIndispensables, setElementosIndispensables] = useState(true);
  const [justificacion, setJustificacion] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  if (!rejectedBudget) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!justificacion.trim()) {
      setErrorInfo({
        message: 'Debes ingresar la justificación técnica de tu evaluación.',
      });
      return;
    }

    setErrorInfo(null);
    setLoading(true);
    try {
      const registrada = await budgetsApi.registrarEvaluacionTecnica(
        rejectedBudget.idPresupuesto,
        {
          elementosIndispensables,
          justificacion,
        }
      );
      onEvaluationSaved(registrada);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible registrar la evaluación técnica.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border-2 border-amber-300 overflow-hidden shadow-2xs">
      <div className="px-5 py-4 bg-amber-600 text-white flex items-center gap-3">
        <ClipboardList className="w-5 h-5 shrink-0" />
        <div>
          <h3 className="text-sm font-bold">
            Evaluación Técnica por Rechazo · {rejectedBudget.codigoPresupuesto}
          </h3>
          <p className="text-[11px] text-amber-100">
            Determina si los elementos rechazados por el Cliente son indispensables para continuar (RN-09 / CV-07)
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="p-5 space-y-4">
        {errorInfo && (
          <AlertBanner
            type="error"
            message={errorInfo.message}
            onClose={() => setErrorInfo(null)}
          />
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <label
            className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
              elementosIndispensables
                ? 'border-rose-500 bg-rose-50/70 ring-1 ring-rose-500'
                : 'border-slate-200 hover:bg-slate-50'
            }`}
          >
            <div className="flex items-center gap-2">
              <input
                type="radio"
                checked={elementosIndispensables === true}
                onChange={() => setElementosIndispensables(true)}
                className="accent-rose-600"
              />
              <span className="text-xs font-bold text-rose-900">
                Sí, son indispensables
              </span>
            </div>
            <p className="text-[11px] text-slate-600 mt-1">
              La orden permanecerá Pausada hasta que el Administrador autorice una nueva cotización o cancele el servicio.
            </p>
          </label>

          <label
            className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
              !elementosIndispensables
                ? 'border-emerald-500 bg-emerald-50/70 ring-1 ring-emerald-500'
                : 'border-slate-200 hover:bg-slate-50'
            }`}
          >
            <div className="flex items-center gap-2">
              <input
                type="radio"
                checked={elementosIndispensables === false}
                onChange={() => setElementosIndispensables(false)}
                className="accent-emerald-600"
              />
              <span className="text-xs font-bold text-emerald-900">
                No son indispensables
              </span>
            </div>
            <p className="text-[11px] text-slate-600 mt-1">
              Habilita inmediatamente la reanudación de la reparación con el alcance previamente aprobado.
            </p>
          </label>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Justificación del dictamen técnico *
          </label>
          <textarea
            rows={3}
            required
            maxLength={800}
            value={justificacion}
            onChange={(e) => setJustificacion(e.target.value)}
            placeholder="Sustenta técnicamente por qué se puede o no continuar la reparación sin estos elementos..."
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full min-h-[48px] py-3 px-5 rounded-xl bg-servitec-navy hover:bg-servitec-navy-light text-white text-xs sm:text-sm font-semibold flex items-center justify-center gap-2 cursor-pointer"
        >
          <Send className="w-4 h-4" />
          <span>
            {loading
              ? 'Guardando evaluación técnica...'
              : 'Registrar evaluación técnica'}
          </span>
        </button>
      </form>
    </div>
  );
}