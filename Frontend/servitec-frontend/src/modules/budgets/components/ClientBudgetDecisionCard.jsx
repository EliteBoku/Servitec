import { useState } from 'react';
import {
  FileSpreadsheet,
  CheckCircle2,
  XCircle,
  Info,
  Clock,
  UserCheck,
} from 'lucide-react';
import { budgetsApi } from '../../../api/budgetsApi';
import { extractApiError } from '../../../api/httpClient';
import {
  formatCurrencyCOP,
  formatQuantity,
  formatDateTime,
} from '../../../shared/utils/formatters';
import StatusBadge from '../../../shared/components/StatusBadge';
import AlertBanner from '../../../shared/components/AlertBanner';
import ConfirmModal from '../../../shared/components/ConfirmModal';

export default function ClientBudgetDecisionCard({
  budget,
  canDecide = false,
  onDecisionRegistered,
}) {
  const [confirmDecision, setConfirmDecision] = useState(null); // 'APROBADO' | 'RECHAZADO' | null
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  if (!budget) return null;

  const isOriginal = budget.tipoPresupuesto === 'ORIGINAL';
  const isPending = budget.estado === 'PENDIENTE_APROBACION';

  const handleConfirmDecision = async () => {
    if (!confirmDecision) return;
    setLoading(true);
    setErrorInfo(null);

    try {
      const actualizado = await budgetsApi.decidirSobrePresupuesto(
        budget.idPresupuesto,
        confirmDecision
      );
      setConfirmDecision(null);
      if (onDecisionRegistered) {
        onDecisionRegistered(actualizado, confirmDecision);
      }
    } catch (err) {
      setErrorInfo(
        extractApiError(
          err,
          'No fue posible registrar tu decisión sobre el presupuesto.'
        )
      );
      setConfirmDecision(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
      {/* Cabecera del Presupuesto */}
      <div className="px-5 py-4 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-servitec-navy text-white flex items-center justify-center shrink-0">
            <FileSpreadsheet className="w-4 h-4 text-servitec-primary" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-sm font-bold text-servitec-primary">
                {budget.codigoPresupuesto}
              </span>
              <span className="px-2 py-0.5 rounded-md bg-slate-200 text-slate-800 text-[10px] font-bold uppercase">
                {isOriginal ? 'Presupuesto Original' : 'Presupuesto Complementario'}
              </span>
            </div>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Emitido el {formatDateTime(budget.fechaEmision)} por{' '}
              <b>{budget.nombreTecnicoCreador}</b>
            </p>
          </div>
        </div>

        {/* Estado propio del presupuesto (separado del estado de la orden) */}
        <StatusBadge status={budget.estado} type="budget" />
      </div>

      <div className="p-5 space-y-5">
        {errorInfo && (
          <AlertBanner
            type="error"
            message={errorInfo.message}
            fieldErrors={errorInfo.fieldErrors}
            missingStockItems={errorInfo.missingStockItems}
            onClose={() => setErrorInfo(null)}
          />
        )}

        {/* Tabla de Repuestos e Insumos Cotizados */}
        <div>
          <p className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2.5">
            Detalle de repuestos e insumos cotizados
          </p>

          {!budget.detalles || budget.detalles.length === 0 ? (
            <div className="rounded-xl bg-slate-50 border border-slate-200 p-3.5 text-xs text-slate-500">
              Este presupuesto no incluye repuestos ni insumos adicionales (únicamente mano de obra técnica).
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold text-slate-600 uppercase">
                    <th className="py-2.5 px-3">Concepto</th>
                    <th className="py-2.5 px-3 text-center">Cantidad</th>
                    <th className="py-2.5 px-3 text-right">Precio Unitario</th>
                    <th className="py-2.5 px-3 text-right">Importe</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {budget.detalles.map((det) => (
                    <tr key={det.idDetalle}>
                      <td className="py-2.5 px-3">
                        <span className="font-mono text-[11px] font-bold text-servitec-primary block">
                          {det.codigoRepuesto}
                        </span>
                        <span className="font-bold text-servitec-navy">
                          {det.nombreRepuesto}
                        </span>
                        {det.justificacion && (
                          <span className="block text-[11px] text-slate-500 mt-0.5">
                            Motivo: {det.justificacion}
                          </span>
                        )}
                      </td>
                      <td className="py-2.5 px-3 text-center font-semibold text-slate-700">
                        {formatQuantity(det.cantidadAutorizada)} {det.unidadMedida}
                      </td>
                      <td className="py-2.5 px-3 text-right text-slate-700 whitespace-nowrap">
                        {formatCurrencyCOP(det.precioUnitarioConservado)}
                      </td>
                      <td className="py-2.5 px-3 text-right font-bold text-servitec-navy whitespace-nowrap">
                        {formatCurrencyCOP(det.importeCalculado)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Bloque de Mano de Obra Estimada (solo para Presupuesto Original) */}
        {isOriginal && (
          <div className="bg-slate-50 rounded-xl border border-slate-200 p-4 space-y-2.5">
            <p className="text-xs font-bold text-servitec-navy uppercase tracking-wider">
              Mano de obra estimada
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
              <div>
                <span className="block text-[11px] text-slate-500">
                  Tarifa por hora aprobada
                </span>
                <span className="font-bold text-slate-800">
                  {formatCurrencyCOP(budget.tarifaHoraAprobada)} / hora
                </span>
              </div>
              <div>
                <span className="block text-[11px] text-slate-500">
                  Tiempo estimado de trabajo
                </span>
                <span className="font-bold text-slate-800">
                  {formatQuantity(budget.horasEstimadas)} horas
                </span>
              </div>
              <div>
                <span className="block text-[11px] text-slate-500">
                  Valor estimado mano de obra
                </span>
                <span className="font-bold text-servitec-navy">
                  {formatCurrencyCOP(budget.valorManoObraEstimado)}
                </span>
              </div>
            </div>

            {budget.condicionManoObra && (
              <div className="pt-2 border-t border-slate-200/80 flex items-start gap-2 text-[11px] text-slate-600">
                <Info className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
                <span>{budget.condicionManoObra}</span>
              </div>
            )}
          </div>
        )}

        {/* Totalizador */}
        <div className="rounded-xl bg-servitec-navy text-white p-4 flex flex-wrap items-center justify-between gap-3">
          <div className="text-xs">
            <span className="text-sky-300 font-semibold block">
              {isOriginal
                ? 'Total estimado del presupuesto original'
                : 'Total adicional del presupuesto complementario'}
            </span>
            <span className="text-[11px] text-slate-300">
              Subtotal repuestos: {formatCurrencyCOP(budget.subtotalRepuestos)}
            </span>
          </div>
          <span className="text-lg sm:text-xl font-bold">
            {formatCurrencyCOP(budget.totalPresupuesto)}
          </span>
        </div>

        {/* Decisión digital ya registrada (Inmutable según REQ-13 / REQ-22) */}
        {budget.decisionRegistrada && (
          <div
            className={`rounded-xl border p-3.5 flex items-center justify-between gap-3 text-xs ${
              budget.decisionRegistrada.decision === 'APROBADO'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                : 'bg-rose-50 border-rose-200 text-rose-900'
            }`}
          >
            <div className="flex items-center gap-2.5">
              <UserCheck className="w-4 h-4 shrink-0" />
              <div>
                <span className="font-bold block">
                  Decisión registrada: {budget.decisionRegistrada.decision}
                </span>
                <span className="text-[11px]">
                  Por {budget.decisionRegistrada.nombreClienteDecisor}
                </span>
              </div>
            </div>
            <span className="inline-flex items-center gap-1 text-[11px] font-semibold">
              <Clock className="w-3.5 h-3.5" />
              {formatDateTime(budget.decisionRegistrada.fechaHoraDecision)}
            </span>
          </div>
        )}

        {/* Botones de Aprobación / Rechazo exclusivos para el Cliente propietario */}
        {canDecide && isPending && !budget.decisionRegistrada && (
          <div className="pt-2 border-t border-slate-100 space-y-3">
            <p className="text-xs font-semibold text-slate-700">
              Como titular de la orden, selecciona tu decisión sobre este presupuesto:
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setConfirmDecision('APROBADO')}
                className="min-h-[44px] py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-2xs transition-colors cursor-pointer"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Aprobar presupuesto</span>
              </button>

              <button
                type="button"
                onClick={() => setConfirmDecision('RECHAZADO')}
                className="min-h-[44px] py-2.5 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-2xs transition-colors cursor-pointer"
              >
                <XCircle className="w-4 h-4" />
                <span>Rechazar presupuesto</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modal de Confirmación de Decisión del Cliente */}
      <ConfirmModal
        isOpen={Boolean(confirmDecision)}
        title={
          confirmDecision === 'APROBADO'
            ? `Aprobar ${budget.codigoPresupuesto}`
            : `Rechazar ${budget.codigoPresupuesto}`
        }
        message={
          confirmDecision === 'APROBADO'
            ? `¿Confirmas aprobar digitalmente el presupuesto ${budget.codigoPresupuesto} por ${formatCurrencyCOP(
                budget.totalPresupuesto
              )}? El sistema verificará la disponibilidad y reservará los repuestos e insumos para tu reparación. Esta decisión no podrá modificarse posteriormente.`
            : isOriginal
            ? `¿Confirmas rechazar el presupuesto original ${budget.codigoPresupuesto}? Conforme a las reglas del servicio (RN-02 y CV-06), al rechazar el presupuesto original la orden de servicio cambiará automáticamente al estado Cancelado sin compromisos de inventario.`
            : `¿Confirmas rechazar el presupuesto complementario ${budget.codigoPresupuesto}? La orden permanecerá pausada mientras el técnico especialista evalúa si los elementos rechazados son indispensables para continuar.`
        }
        confirmText={
          confirmDecision === 'APROBADO'
            ? 'Sí, aprobar presupuesto'
            : 'Sí, rechazar presupuesto'
        }
        variant={confirmDecision === 'APROBADO' ? 'success' : 'danger'}
        loading={loading}
        onConfirm={handleConfirmDecision}
        onCancel={() => setConfirmDecision(null)}
      />
    </div>
  );
}