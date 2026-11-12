import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, Send } from 'lucide-react';
import { authApi } from '../../../api/authApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function ForgotPasswordPage() {
  const [correo, setCorreo] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);
  const [submittedMessage, setSubmittedMessage] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);
    setLoading(true);

    try {
      const response = await authApi.solicitarRecuperacion({ correo: correo.trim() });
      setSubmittedMessage(
        response?.mensaje ||
          'Si el correo ingresado se encuentra asociado a una cuenta activa, recibirás un enlace temporal para restablecer tu contraseña.'
      );
    } catch (err) {
      setErrorInfo(extractApiError(err, 'No fue posible procesar la solicitud.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        <h1 className="text-xl font-bold text-servitec-navy mb-1">Recuperar contraseña</h1>
        <p className="text-xs text-slate-500 mb-6">
          Ingresa tu correo electrónico para enviarte un enlace seguro de restablecimiento (vigencia de 30 minutos).
        </p>

        {submittedMessage ? (
          <div className="space-y-4">
            <AlertBanner type="success" message={submittedMessage} />
            <Link
              to="/restablecer-contrasena"
              className="block w-full text-center py-2.5 px-4 rounded-xl border border-servitec-primary text-servitec-primary font-semibold text-xs hover:bg-sky-50 transition-colors"
            >
              Ya tengo mi token de recuperación
            </Link>
            <Link
              to="/login"
              className="block w-full text-center py-3 px-4 rounded-xl bg-servitec-navy text-white font-semibold text-sm"
            >
              Volver al inicio de sesión
            </Link>
          </div>
        ) : (
          <>
            {errorInfo && (
              <AlertBanner
                type="error"
                message={errorInfo.message}
                fieldErrors={errorInfo.fieldErrors}
                onClose={() => setErrorInfo(null)}
              />
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
                  Correo electrónico registrado
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    required
                    value={correo}
                    onChange={(e) => setCorreo(e.target.value)}
                    placeholder="usuario@ejemplo.com"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white font-semibold text-sm flex items-center justify-center gap-2 transition-colors disabled:opacity-60 cursor-pointer"
              >
                <Send className="w-4 h-4" />
                {loading ? 'Enviando solicitud...' : 'Enviar enlace de recuperación'}
              </button>
            </form>

            <div className="mt-6 text-center">
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-servitec-navy"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                Volver al inicio de sesión
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}