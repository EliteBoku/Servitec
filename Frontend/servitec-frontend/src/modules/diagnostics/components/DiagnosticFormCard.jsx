import { useState, useEffect } from 'react';
import { Stethoscope, Save, Info } from 'lucide-react';
import { diagnosticsApi } from '../../../api/diagnosticsApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

const QUALITATIVE_OPTIONS_BY_CODE = {
  ESTADO_FISICO_EQUIPO: ['Bueno', 'Regular (desgaste leve)', 'Deteriorado / Con corrosión'],
  CONDICION_FLUJO_AIRE: ['Normal / Fluido', 'Obstruido parcialmente', 'Sin flujo / Deficiente'],
  RESPUESTA_CICLO_OPERACION: ['Completa el ciclo normal', 'Se detiene a mitad de ciclo', 'No inicia el ciclo'],
  PRESENCIA_FUGAS: ['Sin fugas', 'Fuga leve detectada', 'Fuga crítica'],
  CONDICION_FISICA_GENERAL: ['Buena condición general', 'Desgaste en soportes / sellos', 'Requiere reemplazo de piezas'],
  CONTINUIDAD_CIRCUITO: ['Continua (Normal)', 'Intermitente', 'Circuito abierto / Sin continuidad'],
  CONDICION_CABLEADO: ['Óptimo estado', 'Aislamiento reseco / recalentado', 'Sulfatado / En cortocircuito'],
};

export default function DiagnosticFormCard({ order, onDiagnosticSaved }) {
  const [plantilla, setPlantilla] = useState([]);
  const [valores, setValores] = useState({});
  const [observaciones, setObservaciones] = useState('');
  const [loadingTemplate, setLoadingTemplate] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    let active = true;
    async function cargarPlantilla() {
      setLoadingTemplate(true);
      setErrorInfo(null);
      try {
        const data = await diagnosticsApi.obtenerPlantillaPorOrden(order.idOrden);
        if (!active) return;
        setPlantilla(data);
        const inicial = {};
        data.forEach((item) => {
          inicial[item.codigoParametro] = '';
        });
        setValores(inicial);
      } catch (err) {
        if (active) {
          setErrorInfo(
            extractApiError(err, 'No fue posible cargar la plantilla de diagnóstico.')
          );
        }
      } finally {
        if (active) setLoadingTemplate(false);
      }
    }

    cargarPlantilla();
    return () => {
      active = false;
    };
  }, [order.idOrden]);

  const handleValorChange = (codigo, nuevoValor) => {
    setValores((prev) => ({
      ...prev,
      [codigo]: nuevoValor,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);

    // Validación previa de obligatorios en cliente (CV-04 Mockup 7)
    const faltaObligatorio = plantilla.some(
      (p) => p.obligatorio && (!valores[p.codigoParametro] || !valores[p.codigoParametro].trim())
    );

    if (faltaObligatorio) {
      setErrorInfo({
        message: 'Faltan parámetros obligatorios para esta categoría de equipo',
      });
      return;
    }

    if (!observaciones.trim()) {
      setErrorInfo({
        message: 'Registra las observaciones de la falla encontrada.',
      });
      return;
    }

    const parametrosPayload = plantilla
      .filter((p) => valores[p.codigoParametro] && valores[p.codigoParametro].trim() !== '')
      .map((p) => ({
        nombreParametro: p.codigoParametro,
        valor: valores[p.codigoParametro].trim(),
        unidadMedida: p.unidadMedidaPorDefecto,
      }));

    setSubmitting(true);
    try {
      const registrado = await diagnosticsApi.registrarDiagnostico(order.idOrden, {
        observaciones,
        parametros: parametrosPayload,
      });
      onDiagnosticSaved(registrado);
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible registrar el diagnóstico técnico.')
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
      <div className="px-5 py-4 bg-servitec-navy text-white flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-servitec-primary flex items-center justify-center shrink-0">
          <Stethoscope className="w-5 h-5 text-white" />
        </div>
        <div>
          <h3 className="text-sm font-bold">
            Registrar Diagnóstico en Sitio · {order.nombreCategoria}
          </h3>
          <p className="text-[11px] text-sky-300">
            Captura las mediciones obligatorias (*) y las observaciones de la falla (CV-04)
          </p>
        </div>
      </div>

      {loadingTemplate ? (
        <div className="p-6 text-center text-xs text-slate-500">
          Cargando parámetros de medición para {order.nombreCategoria}...
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="p-5 space-y-5">
          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              fieldErrors={errorInfo.fieldErrors}
              onClose={() => setErrorInfo(null)}
            />
          )}

          <div className="rounded-xl bg-sky-50 border border-sky-200 p-3 flex items-start gap-2.5 text-sky-900 text-xs">
            <Info className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <p>
              Al guardar el diagnóstico técnico, la orden cambiará automáticamente al estado <b>En diagnóstico</b> y quedará habilitada la elaboración del presupuesto original (RN-01).
            </p>
          </div>

          {/* Campos dinámicos según la categoría */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {plantilla.map((param) => {
              const opcionesSugeridas = QUALITATIVE_OPTIONS_BY_CODE[param.codigoParametro];
              const tieneUnidad =
                param.unidadMedidaPorDefecto && param.unidadMedidaPorDefecto !== 'N/A';

              return (
                <div key={param.codigoParametro}>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                    {param.etiqueta}{' '}
                    {tieneUnidad && (
                      <span className="text-servitec-primary lowercase">
                        ({param.unidadMedidaPorDefecto})
                      </span>
                    )}{' '}
                    {param.obligatorio ? (
                      <span className="text-rose-600">*</span>
                    ) : (
                      <span className="text-slate-400 font-normal lowercase">
                        (opcional)
                      </span>
                    )}
                  </label>

                  {param.numerico ? (
                    <div className="relative">
                      <input
                        type="number"
                        step="0.01"
                        value={valores[param.codigoParametro] || ''}
                        onChange={(e) =>
                          handleValorChange(param.codigoParametro, e.target.value)
                        }
                        placeholder="Ej. 120"
                        className="w-full px-3.5 py-3 pr-14 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                      />
                      {tieneUnidad && (
                        <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md">
                          {param.unidadMedidaPorDefecto}
                        </span>
                      )}
                    </div>
                  ) : opcionesSugeridas ? (
                    <select
                      value={valores[param.codigoParametro] || ''}
                      onChange={(e) =>
                        handleValorChange(param.codigoParametro, e.target.value)
                      }
                      className="w-full px-3.5 py-3 rounded-xl border border-slate-300 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                    >
                      <option value="">Selecciona una condición...</option>
                      {opcionesSugeridas.map((op) => (
                        <option key={op} value={op}>
                          {op}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type="text"
                      maxLength={150}
                      value={valores[param.codigoParametro] || ''}
                      onChange={(e) =>
                        handleValorChange(param.codigoParametro, e.target.value)
                      }
                      placeholder={`Ingresa ${param.etiqueta.toLowerCase()}...`}
                      className="w-full px-3.5 py-3 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                    />
                  )}
                </div>
              );
            })}
          </div>

          {/* Observaciones de la falla */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Observaciones de la falla encontrada <span className="text-rose-600">*</span>
              </label>
              <span className="text-[11px] text-slate-400">
                {observaciones.length} / 800
              </span>
            </div>
            <textarea
              rows={4}
              maxLength={800}
              value={observaciones}
              onChange={(e) => setObservaciones(e.target.value)}
              placeholder="Detalla el estado de los componentes revisados, causas de la falla y trabajos requeridos..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full min-h-[48px] py-3 px-5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-sm font-semibold shadow-xs flex items-center justify-center gap-2 transition-colors disabled:opacity-60 cursor-pointer"
          >
            <Save className="w-4 h-4" />
            <span>
              {submitting ? 'Guardando diagnóstico...' : 'Guardar diagnóstico en sitio'}
            </span>
          </button>
        </form>
      )}
    </div>
  );
}