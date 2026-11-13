import { useState, useEffect } from 'react';
import { History, X, User, ArrowRight } from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function OrderTimelineModal({ isOpen, onClose, order }) {
  const [historial, setHistorial] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    if (isOpen && order?.idOrden) {
      setLoading(true);
      setErrorInfo(null);
      ordersApi
        .consultarHistorialEstados(order.idOrden)
        .then((data) => setHistorial(data))
        .catch((err) =>
          setErrorInfo(
            extractApiError(err, 'No fue posible consultar el historial de estados.')
          )
        )
        .finally(() => setLoading(false));
    }
  }, [isOpen, order]);

  if (!isOpen || !order) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-xl overflow-hidden my-8">
        {/* Cabecera */}
        <div className="flex items-center justify-between px-6 py-4 bg-servitec-navy text-white">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-servitec-primary flex items-center justify-center">
              <History className="w-4 h-4 text-white" />
            </div>
            <div>
              <h3 className="text-sm font-bold">
                Historial de Estados · {order.codigoOrden}
              </h3>
              <p className="text-[11px] text-sky-300">
                Trazabilidad cronológica registrada en la orden de servicio
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-300 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 max-h-[75vh] overflow-y-auto space-y-4">
          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              onClose={() => setErrorInfo(null)}
            />
          )}

          {loading ? (
            <p className="text-xs text-slate-500 text-center py-6">
              Cargando línea de tiempo de la orden...
            </p>
          ) : historial.length === 0 ? (
            <p className="text-xs text-slate-500 text-center py-6">
              No hay transiciones registradas para esta orden.
            </p>
          ) : (
            <ol className="relative border-l-2 border-slate-200 ml-3 space-y-5">
              {historial.map((item) => (
                <li key={item.idHistorialEstado} className="ml-5">
                  <span className="absolute -left-2 flex items-center justify-center w-4 h-4 rounded-full bg-servitec-primary ring-4 ring-white" />
                  <div className="bg-slate-50 rounded-xl border border-slate-200 p-3.5 space-y-2">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5">
                        {item.estadoAnterior ? (
                          <>
                            <StatusBadge status={item.estadoAnterior} />
                            <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                            <StatusBadge status={item.estadoNuevo} />
                          </>
                        ) : (
                          <StatusBadge status={item.estadoNuevo} />
                        )}
                      </div>
                      <span className="text-[11px] font-semibold text-slate-500">
                        {formatDateTime(item.fechaHoraCambio)}
                      </span>
                    </div>

                    {item.justificacion && (
                      <p className="text-xs text-slate-700 bg-white rounded-lg p-2.5 border border-slate-200/80 leading-relaxed">
                        {item.justificacion}
                      </p>
                    )}

                    <div className="flex items-center gap-1.5 text-[11px] text-slate-500">
                      <User className="w-3.5 h-3.5 text-servitec-primary" />
                      <span>
                        Responsable: <b>{item.nombreUsuarioResponsable}</b>
                      </span>
                    </div>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </div>

        <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-servitec-navy text-white text-xs font-semibold cursor-pointer"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}