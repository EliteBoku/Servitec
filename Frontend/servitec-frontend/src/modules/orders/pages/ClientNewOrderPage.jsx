import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FilePlus2,
  MapPin,
  ShieldCheck,
  CheckCircle2,
  Wrench,
  ArrowLeft,
} from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { extractApiError } from '../../../api/httpClient';
import { formatDateTime } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function ClientNewOrderPage() {
  const navigate = useNavigate();

  const [categorias, setCategorias] = useState([]);
  const [consentStatus, setConsentStatus] = useState(null);
  const [loadingInit, setLoadingInit] = useState(true);

  const [idCategoria, setIdCategoria] = useState('');
  const [descripcionProblema, setDescripcionProblema] = useState('');
  const [direccionServicio, setDireccionServicio] = useState('');
  const [autorizaTratamientoDatos, setAutorizaTratamientoDatos] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    let active = true;
    async function cargarFormularioInicial() {
      setLoadingInit(true);
      try {
        const [catsData, consentData] = await Promise.all([
          ordersApi.listarCategoriasActivas(),
          ordersApi.consultarEstadoConsentimiento(),
        ]);
        if (!active) return;
        setCategorias(catsData);
        if (catsData.length > 0) {
          setIdCategoria(String(catsData[0].idCategoria));
        }
        setConsentStatus(consentData);
        setAutorizaTratamientoDatos(Boolean(consentData?.consentimientoOtorgado));
      } catch (err) {
        if (active) {
          setErrorInfo(
            extractApiError(err, 'No se pudieron cargar los datos iniciales del formulario.')
          );
        }
      } finally {
        if (active) setLoadingInit(false);
      }
    }

    cargarFormularioInicial();
    return () => {
      active = false;
    };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);

    if (
      consentStatus?.requiereConsentimientoPrimeraSolicitud &&
      !autorizaTratamientoDatos
    ) {
      setErrorInfo({
        message:
          'Debes autorizar el tratamiento de tus datos personales para registrar tu primera solicitud.',
      });
      return;
    }

    setSubmitting(true);
    try {
      const creada = await ordersApi.crearSolicitudServicio({
        idCategoria,
        descripcionProblema,
        direccionServicio,
        autorizaTratamientoDatos,
      });

      navigate('/cliente/ordenes', {
        state: {
          createdOrderCode: creada.codigoOrden,
        },
      });
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible registrar la solicitud de servicio.')
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-servitec-navy">
            Solicitar Servicio Técnico
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Selecciona la categoría de tu equipo, describe la falla e indica la dirección de atención (CV-02).
          </p>
        </div>

        <button
          type="button"
          onClick={() => navigate('/cliente/ordenes')}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-slate-700 text-xs font-semibold hover:bg-slate-100 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Mis órdenes</span>
        </button>
      </div>

      {errorInfo && (
        <AlertBanner
          type="error"
          message={errorInfo.message}
          fieldErrors={errorInfo.fieldErrors}
          onClose={() => setErrorInfo(null)}
        />
      )}

      <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
        <div className="px-6 py-4 bg-servitec-navy text-white flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-servitec-primary flex items-center justify-center">
            <FilePlus2 className="w-5 h-5 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold">Formulario de Nueva Solicitud</h3>
            <p className="text-[11px] text-sky-300">
              Se generará una orden en estado Solicitado con código único
            </p>
          </div>
        </div>

        {loadingInit ? (
          <div className="p-8 text-center text-xs text-slate-500">
            Cargando categorías de equipos...
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            {/* Selección de Categoría */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                1. Categoría del equipo *
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {categorias.map((cat) => {
                  const selected = String(cat.idCategoria) === String(idCategoria);
                  return (
                    <label
                      key={cat.idCategoria}
                      className={`flex items-center gap-3 p-3.5 rounded-xl border cursor-pointer transition-all ${
                        selected
                          ? 'border-servitec-primary bg-sky-50/70 ring-1 ring-servitec-primary'
                          : 'border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <input
                        type="radio"
                        name="idCategoria"
                        value={cat.idCategoria}
                        checked={selected}
                        onChange={(e) => setIdCategoria(e.target.value)}
                        className="accent-servitec-primary"
                      />
                      <Wrench
                        className={`w-4 h-4 ${
                          selected ? 'text-servitec-primary' : 'text-slate-400'
                        }`}
                      />
                      <span className="text-xs font-bold text-servitec-navy">
                        {cat.nombre}
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Descripción del Problema */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  2. Descripción del problema presentado *
                </label>
                <span className="text-[11px] text-slate-400">
                  {descripcionProblema.length} / 500 caracteres
                </span>
              </div>
              <textarea
                rows={4}
                required
                maxLength={500}
                value={descripcionProblema}
                onChange={(e) => setDescripcionProblema(e.target.value)}
                placeholder="Describe qué falla presenta el equipo, ruidos inusuales, códigos de error o desde cuándo dejó de funcionar..."
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>

            {/* Dirección física del servicio */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                3. Dirección donde se encuentra el equipo *
              </label>
              <div className="relative">
                <MapPin className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  maxLength={255}
                  value={direccionServicio}
                  onChange={(e) => setDireccionServicio(e.target.value)}
                  placeholder="Ej. Calle 24 # 18-32, Barrio Los Alpes, Apto 302"
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                />
              </div>
            </div>

            {/* Bloque de Hábeas Data (Sección 11.2 del SRS y CV-02) */}
            {consentStatus?.requiereConsentimientoPrimeraSolicitud ? (
              <div className="rounded-2xl bg-sky-50/80 border border-sky-200 p-4 space-y-2">
                <div className="flex items-center gap-2 text-xs font-bold text-servitec-navy">
                  <ShieldCheck className="w-4 h-4 text-servitec-primary" />
                  <span>Autorización de Tratamiento de Datos Personales (Hábeas Data)</span>
                </div>
                <label className="flex items-start gap-3 cursor-pointer pt-1">
                  <input
                    type="checkbox"
                    checked={autorizaTratamientoDatos}
                    onChange={(e) => setAutorizaTratamientoDatos(e.target.checked)}
                    className="mt-1 w-4 h-4 accent-servitec-primary rounded"
                  />
                  <span className="text-xs text-slate-700 leading-relaxed">
                    Autorizo de manera previa, expresa e informada a <b>Servitec Los Alpes</b> (Política{' '}
                    {consentStatus.versionPolitica || 'v1.0-2026'}) para recolectar y tratar mis datos personales y dirección de domicilio con la finalidad exclusiva de gestionar mis órdenes de servicio técnico, notificaciones y garantías. *
                  </span>
                </label>
              </div>
            ) : (
              <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-3.5 flex items-center gap-2.5 text-emerald-900 text-xs">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>
                  Ya cuentas con autorización de tratamiento de datos personales activa (otorgada el{' '}
                  <b>{formatDateTime(consentStatus?.fechaHoraConsentimiento)}</b> · Política{' '}
                  {consentStatus?.versionPolitica}).
                </span>
              </div>
            )}

            <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => navigate('/cliente/ordenes')}
                disabled={submitting}
                className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="px-5 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold shadow-xs transition-colors disabled:opacity-60 cursor-pointer"
              >
                {submitting ? 'Registrando solicitud...' : 'Enviar solicitud de servicio'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}