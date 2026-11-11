import { AlertCircle, CheckCircle2, X } from 'lucide-react';
import { formatQuantity } from '../utils/formatters';

export default function AlertBanner({ type = 'error', message, fieldErrors, missingStockItems, onClose }) {
  if (!message && (!fieldErrors || Object.keys(fieldErrors).length === 0)) {
    return null;
  }

  const isError = type === 'error';

  return (
    <div
      className={`rounded-xl border p-4 mb-4 ${
        isError
          ? 'bg-rose-50 border-rose-200 text-rose-900'
          : 'bg-emerald-50 border-emerald-200 text-emerald-900'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-2.5">
          {isError ? (
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
          ) : (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
          )}
          <div className="text-sm">
            {message && <p className="font-semibold">{message}</p>}

            {fieldErrors && Object.keys(fieldErrors).length > 0 && (
              <ul className="mt-1.5 list-disc list-inside space-y-0.5 text-xs">
                {Object.entries(fieldErrors).map(([campo, desc]) => (
                  <li key={campo}>{desc}</li>
                ))}
              </ul>
            )}

            {missingStockItems && missingStockItems.length > 0 && (
              <div className="mt-2 bg-white/80 rounded-lg p-2.5 border border-rose-200">
                <p className="text-xs font-semibold mb-1 text-rose-800">
                  Elementos sin disponibilidad suficiente en inventario:
                </p>
                <ul className="text-xs space-y-1">
                  {missingStockItems.map((item) => (
                    <li key={item.idRepuesto} className="flex justify-between gap-4">
                      <span className="font-medium">
                        {item.codigoRepuesto} - {item.nombre}
                      </span>
                      <span>
                        Solicitado: <b>{formatQuantity(item.cantidadSolicitada)}</b> | Disponible:{' '}
                        <b>{formatQuantity(item.cantidadDisponible)}</b>
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>

        {onClose && (
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
}