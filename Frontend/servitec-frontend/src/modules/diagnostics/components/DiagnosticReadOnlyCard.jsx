import { ClipboardCheck, Gauge, User, Clock } from 'lucide-react';
import { formatDateTime } from '../../../shared/utils/formatters';

export default function DiagnosticReadOnlyCard({ diagnostic }) {
  if (!diagnostic) return null;

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
      <div className="px-5 py-4 bg-servitec-navy text-white flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-servitec-primary flex items-center justify-center">
            <ClipboardCheck className="w-4 h-4 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold">Diagnóstico Técnico en Sitio</h3>
            <p className="text-[11px] text-sky-300">
              Categoría evaluada: {diagnostic.nombreCategoria}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 text-xs text-slate-200">
          <Clock className="w-3.5 h-3.5 text-servitec-primary" />
          <span>{formatDateTime(diagnostic.fechaHoraRegistro)}</span>
        </div>
      </div>

      <div className="p-5 space-y-4">
        {/* Mediciones capturadas por categoría */}
        <div>
          <p className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2.5 flex items-center gap-1.5">
            <Gauge className="w-4 h-4 text-servitec-primary" />
            <span>Parámetros técnicos medidos en el equipo</span>
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {diagnostic.parametros?.map((param) => (
              <div
                key={param.idParametro || param.nombreParametro}
                className="bg-slate-50 rounded-xl border border-slate-200 p-3 flex items-center justify-between gap-2"
              >
                <span className="text-xs font-semibold text-slate-600">
                  {param.nombreParametro}
                </span>
                <span className="text-xs font-bold text-servitec-navy bg-white px-2.5 py-1 rounded-lg border border-slate-200">
                  {param.valor}{' '}
                  {param.unidadMedida && param.unidadMedida !== 'N/A'
                    ? param.unidadMedida
                    : ''}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Observaciones de la falla */}
        <div>
          <p className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
            Observaciones técnicas de la falla encontrada
          </p>
          <div className="bg-slate-50 rounded-xl border border-slate-200 p-3.5 text-xs text-slate-800 leading-relaxed whitespace-pre-line">
            {diagnostic.observaciones}
          </div>
        </div>

        {/* Pie de auditoría */}
        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
          <span className="inline-flex items-center gap-1.5">
            <User className="w-3.5 h-3.5 text-servitec-primary" />
            Técnico responsable: <b>{diagnostic.nombreTecnico}</b>
          </span>
          <span>Registro #{diagnostic.idDiagnostico}</span>
        </div>
      </div>
    </div>
  );
}