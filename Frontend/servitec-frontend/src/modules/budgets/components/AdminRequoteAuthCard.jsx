import { useState } from 'react';
import { ShieldAlert, CheckCircle2, User } from 'lucide-react';
import { budgetsApi } from '../../../api/budgetsApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function AdminRequoteAuthCard({
  evaluation,
  existingAuthorization = null,
  onAuthorized,
}) {
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  if (!evaluation) return null;

  const handleAuthorize = async () => {
    setErrorInfo(null);
    setLoading(true);
    try {
      const authCreated = await budgetsApi.autorizarNuevaCotizacion(
        evaluation.idPresupuestoRechazado
      );
      onAuthorized(authCreated);
    } catch (err) {
      setErrorInfo(
        extractApiError(
          err,
          'No fue posible registrar la autorización para nueva cotización.'
        )
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
        <div className="flex items-center gap-2">
          <ShieldAlert
            className={`w-5 h-5 ${
              evaluation.elementosIndispensables
                ? 'text-rose-600'
                : 'text-emerald-600'
            }`}
          />
          <h4 className="text-sm font-bold text-servitec-navy">
            Evaluación Técnica sobre Presupuesto Rechazado #
            {evaluation.idPresupuestoRechazado}
          </h4>
        </div>

        <span
          className={`px-2.5 py-1 rounded-full text-xs font-bold ${
            evaluation.elementosIndispensables
              ? 'bg-rose-100 text-rose-800'
              : 'bg-emerald-100 text-emerald-800'
          }`}
        >
          {evaluation.elementosIndispensables
            ? 'Elementos Indispensables'
            : 'Elementos No Indispensables'}
        </span>
      </div>

      {errorInfo && (
        <AlertBanner
          type="error"
          message={errorInfo.message}
          onClose={() => setErrorInfo(null)}
        />
      )}

      <div className="bg-slate-50 rounded-xl border border-slate-200 p-3.5 text-xs space-y-1.5">
        <p className="font-bold text-slate-700">Dictamen del técnico especialista:</p>
        <p className="text-slate-800 leading-relaxed">{evaluation.justificacion}</p>
        <div className="pt-1 flex items-center justify-between text-[11px] text-slate-500">
          <span className="inline-flex items-center gap-1">
            <User className="w-3.5 h-3.5 text-servitec-primary" />
            Técnico: <b>{evaluation.nombreTecnico}</b>
          </span>
          <span>{formatDateTime(evaluation.fechaHora)}</span>
        </div>
      </div>

      {existingAuthorization ? (
        <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-3.5 flex items-center justify-between gap-3 text-xs text-emerald-900">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <div>
              <span className="font-bold block">
                Autorización #{existingAuthorization.idAutorizacion} otorgada por{' '}
                {existingAuthorization.nombreAdministrador}
              </span>
              <span className="text-[11px]">
                Estado:{' '}
                {existingAuthorization.utilizadaEnNuevaPropuesta
                  ? 'Nueva propuesta ya emitida por el técnico'
                  : 'Pendiente de que el técnico elabore la nueva propuesta'}
              </span>
            </div>
          </div>
          <span className="text-[11px] font-semibold">
            {formatDateTime(existingAuthorization.fechaHoraAutorizacion)}
          </span>
        </div>
      ) : (
        evaluation.elementosIndispensables && (
          <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
            <p className="text-xs text-slate-600">
              Como los elementos son indispensables, puedes autorizar al técnico para emitir una nueva propuesta complementaria (CV-14):
            </p>
            <button
              type="button"
              disabled={loading}
              onClick={handleAuthorize}
              className="px-4 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold transition-colors disabled:opacity-60 cursor-pointer"
            >
              {loading
                ? 'Autorizando...'
                : 'Autorizar nueva cotización complementaria'}
            </button>
          </div>
        )
      )}
    </div>
  );
}