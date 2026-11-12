import { useState, useEffect, useMemo } from 'react';
import {
  ArrowLeftRight,
  ArrowDownLeft,
  ArrowUpRight,
  SlidersHorizontal,
  AlertTriangle,
  CheckCircle2,
  X,
} from 'lucide-react';
import { inventoryApi } from '../../../api/inventoryApi';
import { extractApiError } from '../../../api/httpClient';
import { formatQuantity } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function ManualMovementModal({
  isOpen,
  onClose,
  catalogItems = [],
  preselectedItemId = '',
  onMovementRegistered,
}) {
  const [idRepuesto, setIdRepuesto] = useState('');
  const [tipoMovimiento, setTipoMovimiento] = useState('ENTRADA');
  const [cantidad, setCantidad] = useState('');
  const [motivo, setMotivo] = useState('');

  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    if (isOpen) {
      setErrorInfo(null);
      setTipoMovimiento('ENTRADA');
      setCantidad('');
      setMotivo('');
      if (preselectedItemId) {
        setIdRepuesto(String(preselectedItemId));
      } else if (catalogItems.length > 0) {
        setIdRepuesto(String(catalogItems[0].idRepuesto));
      } else {
        setIdRepuesto('');
      }
    }
  }, [isOpen, preselectedItemId, catalogItems]);

  const selectedItem = useMemo(() => {
    return catalogItems.find((i) => String(i.idRepuesto) === String(idRepuesto)) || null;
  }, [catalogItems, idRepuesto]);

  // Cálculo de vista previa de saldos en tiempo real (CV-19 / REQ-28 / RN-15)
  const preview = useMemo(() => {
    if (!selectedItem) return null;

    const fisicasActuales = Number(selectedItem.existenciasFisicas ?? 0);
    const reservadasActivas = Number(selectedItem.cantidadesReservadas ?? 0);
    const disponiblesActuales = Number(selectedItem.existenciasDisponibles ?? 0);

    const valorNum = cantidad === '' ? NaN : Number(cantidad);
    if (isNaN(valorNum)) {
      return {
        fisicasActuales,
        reservadasActivas,
        disponiblesActuales,
        hasInput: false,
      };
    }

    let fisicasResultantes = fisicasActuales;
    let variacion = 0;
    let validationError = null;

    if (tipoMovimiento === 'ENTRADA') {
      if (valorNum <= 0) {
        validationError = 'La cantidad de una entrada debe ser mayor que cero.';
      }
      variacion = valorNum;
      fisicasResultantes = fisicasActuales + valorNum;
    } else if (tipoMovimiento === 'SALIDA') {
      if (valorNum <= 0) {
        validationError = 'La cantidad de una salida debe ser mayor que cero.';
      } else if (valorNum > disponiblesActuales) {
        validationError = `La salida solicitada (${formatQuantity(
          valorNum
        )}) supera las existencias disponibles (${formatQuantity(
          disponiblesActuales
        )}). No es posible afectar las ${formatQuantity(
          reservadasActivas
        )} unidades comprometidas en reservas activas.`;
      }
      variacion = -valorNum;
      fisicasResultantes = fisicasActuales - valorNum;
    } else if (tipoMovimiento === 'AJUSTE') {
      if (valorNum < 0) {
        validationError = 'El conteo físico no puede ser negativo.';
      } else if (valorNum === fisicasActuales) {
        validationError =
          'El conteo físico ingresado coincide con las existencias actuales; no hay diferencia por ajustar.';
      } else if (valorNum < reservadasActivas) {
        validationError = `El conteo físico (${formatQuantity(
          valorNum
        )}) no puede ser inferior a las cantidades comprometidas en reservas activas (${formatQuantity(
          reservadasActivas
        )}).`;
      }
      variacion = valorNum - fisicasActuales;
      fisicasResultantes = valorNum;
    }

    const disponiblesResultantes = fisicasResultantes - reservadasActivas;

    return {
      fisicasActuales,
      reservadasActivas,
      disponiblesActuales,
      variacion,
      fisicasResultantes,
      disponiblesResultantes,
      validationError,
      hasInput: true,
    };
  }, [selectedItem, tipoMovimiento, cantidad]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (preview?.validationError) {
      setErrorInfo({ message: preview.validationError });
      return;
    }

    setErrorInfo(null);
    setLoading(true);

    try {
      const registrado = await inventoryApi.registrarMovimientoManual({
        idRepuesto,
        tipoMovimiento,
        cantidad,
        motivo,
      });
      onMovementRegistered(registrado);
      onClose();
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible registrar el movimiento de inventario.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-2xl overflow-hidden my-8">
        {/* Cabecera */}
        <div className="flex items-center justify-between px-6 py-4 bg-servitec-navy text-white">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-servitec-primary flex items-center justify-center">
              <ArrowLeftRight className="w-4 h-4 text-white" />
            </div>
            <div>
              <h3 className="text-sm font-bold">Registrar movimiento manual de inventario</h3>
              <p className="text-[11px] text-sky-300">
                Entradas, salidas y ajustes por conteo físico con auditoría (CV-19)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="text-slate-300 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5 max-h-[84vh] overflow-y-auto">
          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              fieldErrors={errorInfo.fieldErrors}
              onClose={() => setErrorInfo(null)}
            />
          )}

          {/* Selección del repuesto / insumo */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Repuesto o insumo del catálogo *
            </label>
            <select
              required
              value={idRepuesto}
              onChange={(e) => setIdRepuesto(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            >
              {catalogItems.map((item) => (
                <option key={item.idRepuesto} value={item.idRepuesto}>
                  {item.codigoRepuesto} — {item.nombre} ({item.unidadMedida}) | Disp:{' '}
                  {formatQuantity(item.existenciasDisponibles)}
                </option>
              ))}
            </select>
          </div>

          {/* Selector de Tipo de Movimiento */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Tipo de movimiento manual *
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <button
                type="button"
                onClick={() => setTipoMovimiento('ENTRADA')}
                className={`p-3.5 rounded-xl border text-left transition-all cursor-pointer ${
                  tipoMovimiento === 'ENTRADA'
                    ? 'border-emerald-500 bg-emerald-50/70 ring-1 ring-emerald-500'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-xs text-emerald-800">
                  <ArrowDownLeft className="w-4 h-4 text-emerald-600" />
                  <span>ENTRADA</span>
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                  Ingreso por compra a proveedor o reposición de stock.
                </p>
              </button>

              <button
                type="button"
                onClick={() => setTipoMovimiento('SALIDA')}
                className={`p-3.5 rounded-xl border text-left transition-all cursor-pointer ${
                  tipoMovimiento === 'SALIDA'
                    ? 'border-rose-500 bg-rose-50/70 ring-1 ring-rose-500'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-xs text-rose-800">
                  <ArrowUpRight className="w-4 h-4 text-rose-600" />
                  <span>SALIDA</span>
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                  Retiro manual por daño, baja o devolución (respeta reservas).
                </p>
              </button>

              <button
                type="button"
                onClick={() => setTipoMovimiento('AJUSTE')}
                className={`p-3.5 rounded-xl border text-left transition-all cursor-pointer ${
                  tipoMovimiento === 'AJUSTE'
                    ? 'border-amber-500 bg-amber-50/70 ring-1 ring-amber-500'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-xs text-amber-800">
                  <SlidersHorizontal className="w-4 h-4 text-amber-600" />
                  <span>AJUSTE FÍSICO</span>
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                  Sincroniza el saldo según arqueo o conteo físico en bodega.
                </p>
              </button>
            </div>
          </div>

          {/* Cantidad o Conteo Físico */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              {tipoMovimiento === 'AJUSTE'
                ? `Nuevo conteo físico real en bodega (${selectedItem?.unidadMedida || 'Unidades'}) *`
                : `Cantidad del movimiento (${selectedItem?.unidadMedida || 'Unidades'}) *`}
            </label>
            <input
              type="number"
              required
              min={tipoMovimiento === 'AJUSTE' ? '0' : '0.01'}
              step="0.01"
              value={cantidad}
              onChange={(e) => setCantidad(e.target.value)}
              placeholder={
                tipoMovimiento === 'AJUSTE'
                  ? 'Ingresa las unidades totales contadas físicamente'
                  : 'Ej. 5'
              }
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
          </div>

          {/* Tarjeta de Vista Previa de Saldos */}
          {preview && (
            <div className="rounded-2xl bg-slate-50 border border-slate-200 p-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-servitec-navy uppercase tracking-wider">
                  Simulación de saldos de inventario
                </span>
                <span className="text-[11px] text-slate-500 font-medium">
                  Unidad: {selectedItem?.unidadMedida}
                </span>
              </div>

              <div className="grid grid-cols-3 gap-3 text-center">
                <div className="bg-white rounded-xl p-2.5 border border-slate-200">
                  <span className="block text-[11px] text-slate-500">Físicas actuales</span>
                  <span className="text-base font-bold text-slate-800">
                    {formatQuantity(preview.fisicasActuales)}
                  </span>
                </div>

                <div className="bg-white rounded-xl p-2.5 border border-amber-200">
                  <span className="block text-[11px] text-amber-700">Reservas activas</span>
                  <span className="text-base font-bold text-amber-700">
                    {formatQuantity(preview.reservadasActivas)}
                  </span>
                </div>

                <div className="bg-white rounded-xl p-2.5 border border-emerald-200">
                  <span className="block text-[11px] text-emerald-700">Disponibles hoy</span>
                  <span className="text-base font-bold text-emerald-700">
                    {formatQuantity(preview.disponiblesActuales)}
                  </span>
                </div>
              </div>

              {preview.hasInput && (
                <div
                  className={`rounded-xl p-3 border flex items-start gap-2.5 text-xs ${
                    preview.validationError
                      ? 'bg-rose-50 border-rose-200 text-rose-900'
                      : 'bg-sky-50 border-sky-200 text-sky-900'
                  }`}
                >
                  {preview.validationError ? (
                    <>
                      <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                      <p className="font-medium">{preview.validationError}</p>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                      <div className="flex-1 flex flex-wrap items-center justify-between gap-2">
                        <span>
                          Variación neta:{' '}
                          <b>
                            {preview.variacion > 0 ? '+' : ''}
                            {formatQuantity(preview.variacion)}
                          </b>
                        </span>
                        <span>
                          Nuevas físicas: <b>{formatQuantity(preview.fisicasResultantes)}</b>
                        </span>
                        <span>
                          Nuevas disponibles:{' '}
                          <b>{formatQuantity(preview.disponiblesResultantes)}</b>
                        </span>
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Motivo o Justificación Obligatoria */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Motivo o justificación del movimiento *
            </label>
            <textarea
              rows={2}
              required
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ej. Compra a proveedor según factura FV-1089 / Baja por deterioro en estantería..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-100 transition-colors cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading || Boolean(preview?.validationError) || !selectedItem}
              className="px-5 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold transition-colors disabled:opacity-60 cursor-pointer"
            >
              {loading ? 'Registrando movimiento...' : 'Confirmar movimiento'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}