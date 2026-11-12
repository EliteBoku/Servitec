import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { KeyRound, Lock, CheckCircle2 } from 'lucide-react';
import { authApi } from '../../../api/authApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const initialToken = searchParams.get('token') || '';

  const [token, setToken] = useState(initialToken);
  const [nuevaContrasena, setNuevaContrasena] = useState('');
  const [confirmarContrasena, setConfirmarContrasena] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);
  const [resetSuccess, setResetSuccess] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);

    if (nuevaContrasena !== confirmarContrasena) {
      setErrorInfo({ message: 'Las contraseñas ingresadas no coinciden.' });
      return;
    }

    setLoading(true);
    try {
      const response = await authApi.confirmarRecuperacion({
        token: token.trim(),
        nuevaContrasena,
        confirmarContrasena,
      });
      setResetSuccess(
        response?.mensaje ||
          'Tu contraseña ha sido restablecida correctamente. Todas tus sesiones previas han sido cerradas por seguridad.'
      );
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'El enlace de recuperación es inválido, ya fue utilizado o ha expirado.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        <h1 className="text-xl font-bold text-servitec-navy mb-1">Restablecer contraseña</h1>
        <p className="text-xs text-slate-500 mb-6">
          Ingresa tu nueva contraseña para recuperar el acceso a tu cuenta.
        </p>

        {resetSuccess ? (
          <div className="text-center py-4 space-y-4">
            <div className="w-14 h-14 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <p className="text-sm text-slate-700">{resetSuccess}</p>
            <Link
              to="/login"
              className="inline-block w-full py-3 px-4 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white font-semibold text-sm"
            >
              Iniciar sesión con mi nueva contraseña
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
              {!initialToken && (
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Token de recuperación
                  </label>
                  <div className="relative">
                    <KeyRound className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      required
                      value={token}
                      onChange={(e) => setToken(e.target.value)}
                      placeholder="Pega el token recibido por correo"
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Nueva contraseña
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={nuevaContrasena}
                    onChange={(e) => setNuevaContrasena(e.target.value)}
                    placeholder="Mínimo 8 caracteres"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Confirmar nueva contraseña
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={confirmarContrasena}
                    onChange={(e) => setConfirmarContrasena(e.target.value)}
                    placeholder="Repite la nueva contraseña"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white font-semibold text-sm transition-colors disabled:opacity-60 cursor-pointer"
              >
                {loading ? 'Guardando contraseña...' : 'Confirmar nueva contraseña'}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}