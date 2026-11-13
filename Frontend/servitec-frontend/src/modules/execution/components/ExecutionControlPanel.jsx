import { useState, useEffect } from 'react';
import {
  Play,
  Pause,
  RotateCcw,
  Timer,
  Clock,
  Lock,
  CheckCircle2,
} from 'lucide-react';
import { executionApi } from '../../../api/executionApi';
import { extractApiError } from '../../../api/httpClient';
import {
  formatCurrencyCOP,
  formatQuantity,
  formatDateTime,
} from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';
import PauseOrderModal from './PauseOrderModal';

function formatSecondsToHHMMSS(totalSeconds) {
  const secNum = Math.max(0, Math.floor(Number(totalSeconds) || 0));
  const hours = Math.floor(secNum / 3600);
  const minutes = Math.floor((secNum % 3600) / 60);
  const seconds = secNum % 60;
  return [hours, minutes, seconds]
    .map((v) => String(v).padStart(2, '0'))
    .join(':');
}

export default function ExecutionControlPanel({
  order,
  timeSummary,
  originalBudgetApproved = false,
  canControl = false,
  resumeBlockedReason = null,
  onTimeUpdated,
}) {
  const [liveSeconds, setLiveSeconds] = useState(
    timeSummary?.segundosEfectivosAcumulados || 0
  );
  const [actionLoading, setActionLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);
  const [isPauseModalOpen, setIsPauseModalOpen] = useState(false);

  useEffect(() => {
    setLiveSeconds(timeSummary?.segundosEfectivosAcumulados || 0);
  }, [timeSummary]);

  // Incrementar visualmente cada segundo solo cuando la orden está EN_EJECUCION
  useEffect(() => {
    if (order?.estadoOperativo !== 'EN_EJECUCION') return undefined;

    const interval = setInterval(() => {
      setLiveSeconds((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [order?.estadoOperativo]);

  if (!order) return null;

  const tarifaHora = Number(timeSummary?.tarifaHoraAprobada || 0);
  const horasEstimadas = Number(timeSummary?.horasEstimadas || 0);
  const horasEfectivasVivo = Number((liveSeconds / 3600).toFixed(2));
  const diferenciaHorasVivo = Number((horasEfectivasVivo - horasEstimadas).toFixed(2));
  const manoObraViva = Math.round((tarifaHora * liveSeconds) / 3600);

  const handleStartRepair = async () => {
    setErrorInfo(null);
    setActionLoading(true);
    try {
      const actualizado = await executionApi.iniciarReparacion(order.idOrden);
      onTimeUpdated(actualizado, 'Reparación iniciada correctamente. El cómputo de tiempo efectivo está activo.');
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible iniciar la reparación.')
      );
    } finally {
      setActionLoading(false);
    }
  };

  const handleResumeRepair = async () => {
    setErrorInfo(null);
    setActionLoading(true);
    try {
      const actualizado = await executionApi.reanudarReparacion(order.idOrden);
      onTimeUpdated(actualizado, 'Reparación reanudada correctamente.');
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible reanudar la reparación.')
      );
    } finally {
      setActionLoading(false);
    }
  };

  const handlePaused = (actualizado) => {
    onTimeUpdated(
      actualizado,
      'Reparación pausada correctamente. El tiempo de esta pausa no se sumará a la mano de obra.'
    );
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
      {/* Cabecera */}
      <div className="px-5 py-4 bg-servitec-navy text-white flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-servitec-primary flex items-center justify-center shrink-0">
            <Timer className="w-5 h-5 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold">
              Control de Ejecución y Tiempo Efectivo (CV-07 / CV-17)
            </h3>
            <p className="text-[11px] text-sky-300">
              Cómputo automático con precisión de segundos excluyendo pausas
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 bg-white/10 px-3.5 py-1.5 rounded-xl border border-white/15">
          <span
            className={`w-2 h-2 rounded-full ${
              order.estadoOperativo === 'EN_EJECUCION'
                ? 'bg-emerald-400 animate-ping'
                : 'bg-amber-400'
            }`}
          />
          <span className="font-mono text-base sm:text-lg font-bold tracking-wider">
            {formatSecondsToHHMMSS(liveSeconds)}
          </span>
        </div>
      </div>

      <div className="p-5 space-y-5">
        {errorInfo && (
          <AlertBanner
            type="error"
            message={errorInfo.message}
            onClose={() => setErrorInfo(null)}
          />
        )}

        {/* Tarjetas comparativas: Estimado vs Efectivo y Mano de Obra */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-slate-50 rounded-xl border border-slate-200 p-3">
            <span className="block text-[11px] text-slate-500">
              Horas estimadas
            </span>
            <span className="text-sm font-bold text-slate-800">
              {formatQuantity(horasEstimadas)} h
            </span>
          </div>

          <div className="bg-slate-50 rounded-xl border border-slate-200 p-3">
            <span className="block text-[11px] text-slate-500">
              Horas efectivas reales
            </span>
            <span className="text-sm font-bold text-servitec-primary">
              {formatQuantity(horasEfectivasVivo)} h ({liveSeconds} s)
            </span>
          </div>

          <div className="bg-slate-50 rounded-xl border border-slate-200 p-3">
            <span className="block text-[11px] text-slate-500">
              Diferencia (Efectivo - Estimado)
            </span>
            <span
              className={`text-sm font-bold ${
                diferenciaHorasVivo > 0
                  ? 'text-amber-700'
                  : 'text-emerald-700'
              }`}
            >
              {diferenciaHorasVivo > 0 ? '+' : ''}
              {formatQuantity(diferenciaHorasVivo)} h
            </span>
          </div>

          <div className="bg-sky-50 rounded-xl border border-sky-200 p-3">
            <span className="block text-[11px] text-sky-800 font-medium">
              Mano de obra acumulada
            </span>
            <span className="text-sm font-bold text-servitec-navy">
              {formatCurrencyCOP(manoObraViva)}
            </span>
            <span className="block text-[10px] text-slate-500">
              Tarifa: {formatCurrencyCOP(tarifaHora)}/h
            </span>
          </div>
        </div>

        {/* Botones de Acción para el Técnico Especialista */}
        {canControl && (
          <div className="space-y-3">
            {order.estadoOperativo === 'PRESUPUESTADO' && (
              <>
                {originalBudgetApproved ? (
                  <button
                    type="button"
                    disabled={actionLoading}
                    onClick={handleStartRepair}
                    className="w-full min-h-[48px] py-3 px-5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-xs transition-colors disabled:opacity-60 cursor-pointer"
                  >
                    <Play className="w-4 h-4" />
                    <span>
                      {actionLoading
                        ? 'Iniciando reparación...'
                        : 'Iniciar reparación y activar registro de tiempo'}
                    </span>
                  </button>
                ) : (
                  <div className="rounded-xl bg-amber-50 border border-amber-200 p-3.5 flex items-center gap-2.5 text-amber-900 text-xs">
                    <Lock className="w-4 h-4 text-amber-600 shrink-0" />
                    <span>
                      <b>Inicio de reparación bloqueado (REQ-14 / RN-02):</b> La reparación no puede comenzar hasta que el Cliente apruebe digitalmente el presupuesto original.
                    </span>
                  </div>
                )}
              </>
            )}

            {order.estadoOperativo === 'EN_EJECUCION' && (
              <button
                type="button"
                disabled={actionLoading}
                onClick={() => setIsPauseModalOpen(true)}
                className="w-full min-h-[48px] py-3 px-5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-xs transition-colors cursor-pointer"
              >
                <Pause className="w-4 h-4" />
                <span>Pausar reparación (requiere justificación)</span>
              </button>
            )}

            {order.estadoOperativo === 'PAUSADA' && (
              <div className="space-y-2.5">
                {resumeBlockedReason && (
                  <div className="rounded-xl bg-rose-50 border border-rose-200 p-3.5 flex items-start gap-2.5 text-rose-900 text-xs">
                    <Lock className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                    <span>{resumeBlockedReason}</span>
                  </div>
                )}

                <button
                  type="button"
                  disabled={actionLoading || Boolean(resumeBlockedReason)}
                  onClick={handleResumeRepair}
                  className="w-full min-h-[48px] py-3 px-5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-sm font-bold flex items-center justify-center gap-2 shadow-xs transition-colors disabled:opacity-50 cursor-pointer"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>
                    {actionLoading
                      ? 'Reanudando reparación...'
                      : 'Reanudar reparación explícitamente'}
                  </span>
                </button>
              </div>
            )}
          </div>
        )}

        {/* Historial de Intervalos de Tiempo (REGISTRO_TIEMPO) */}
        {timeSummary?.intervalos && timeSummary.intervalos.length > 0 && (
          <div>
            <p className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-servitec-primary" />
              <span>Historial de intervalos de tiempo registrados</span>
            </p>

            <div className="overflow-x-auto rounded-xl border border-slate-200">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold text-slate-600 uppercase">
                    <th className="py-2 px-3">Evento Inicio</th>
                    <th className="py-2 px-3">Fecha / Hora Inicio</th>
                    <th className="py-2 px-3">Evento Fin</th>
                    <th className="py-2 px-3">Fecha / Hora Fin</th>
                    <th className="py-2 px-3 text-right">Duración</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {timeSummary.intervalos.map((intv) => (
                    <tr key={intv.idRegistroTiempo}>
                      <td className="py-2 px-3 font-semibold text-servitec-navy">
                        {intv.tipoInicio}
                      </td>
                      <td className="py-2 px-3 text-slate-600">
                        {formatDateTime(intv.fechaHoraInicio)}
                      </td>
                      <td className="py-2 px-3">
                        {intv.intervaloActivo ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                            <CheckCircle2 className="w-3 h-3" />
                            En curso
                          </span>
                        ) : (
                          <span className="font-semibold text-slate-700">
                            {intv.tipoFin}
                          </span>
                        )}
                      </td>
                      <td className="py-2 px-3 text-slate-600">
                        {intv.fechaHoraFin
                          ? formatDateTime(intv.fechaHoraFin)
                          : 'Activo'}
                      </td>
                      <td className="py-2 px-3 text-right font-mono font-bold text-servitec-navy">
                        {formatSecondsToHHMMSS(intv.duracionSegundos)} (
                        {intv.duracionSegundos} s)
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      <PauseOrderModal
        isOpen={isPauseModalOpen}
        onClose={() => setIsPauseModalOpen(false)}
        order={order}
        onPaused={handlePaused}
      />
    </div>
  );
}