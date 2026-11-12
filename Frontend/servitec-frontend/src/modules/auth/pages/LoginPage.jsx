import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Wrench, Mail, Lock, ArrowRight, Clock } from 'lucide-react';
import { useAuth } from '../../../context/AuthContext';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function LoginPage() {
  const { login, getHomeRouteByRole, sessionExpiredMessage, clearExpiredMessage } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  const expiredFromUrl = searchParams.get('expired') === 'true';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);
    clearExpiredMessage();
    setLoading(true);

    try {
      const userData = await login(correo.trim(), contrasena);
      navigate(getHomeRouteByRole(userData.rol), { replace: true });
    } catch (err) {
      setErrorInfo(extractApiError(err, 'Credenciales inválidas. Verifica tu correo y contraseña.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-slate-50">
      {/* Panel Izquierdo Institucional (#0B2545) */}
      <div className="lg:w-1/2 bg-servitec-navy text-white p-8 lg:p-16 flex flex-col justify-between">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-servitec-primary flex items-center justify-center shadow-lg">
            <Wrench className="w-6 h-6 text-white" />
          </div>
          <div>
            <span className="text-xl font-bold tracking-tight">SERVITEC</span>
            <span className="block text-xs text-sky-300 font-medium">Los Alpes</span>
          </div>
        </div>

        <div className="my-12 lg:my-0 max-w-md">
          <span className="inline-block px-3 py-1 rounded-full bg-servitec-primary/20 text-sky-300 text-xs font-semibold mb-4 border border-sky-400/30">
            Gestión Integral de Servicio Técnico
          </span>
          <h1 className="text-3xl lg:text-4xl font-bold leading-tight mb-4">
            Todo tu servicio técnico, en un solo lugar.
          </h1>
          <p className="text-slate-300 text-sm leading-relaxed">
            Seguimiento transparente de órdenes de servicio, diagnósticos en sitio, presupuestos digitales y control de repuestos para Refrigeración, Climatización, Línea blanca y Electricidad.
          </p>
        </div>

        <div className="text-xs text-slate-400 border-t border-white/10 pt-6">
          © {new Date().getFullYear()} Servitec Los Alpes. Todos los derechos reservados.
        </div>
      </div>

      {/* Panel Derecho: Formulario de Acceso */}
      <div className="lg:w-1/2 flex items-center justify-center p-6 sm:p-12 bg-white">
        <div className="w-full max-w-md">
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-servitec-navy">Iniciar sesión</h2>
            <p className="text-sm text-slate-500 mt-1">
              Ingresa tus credenciales registradas para acceder a tu panel.
            </p>
          </div>

          {(sessionExpiredMessage || expiredFromUrl) && (
            <div className="mb-5 rounded-xl bg-amber-50 border border-amber-200 p-4 flex items-start gap-3 text-amber-900">
              <Clock className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
              <div className="text-xs leading-relaxed">
                <p className="font-semibold text-sm">Sesión finalizada por inactividad</p>
                <p className="mt-0.5">
                  {sessionExpiredMessage ||
                    'Tu sesión superó el tiempo máximo de inactividad (60 minutos). Inicia sesión nuevamente para continuar.'}
                </p>
              </div>
            </div>
          )}

          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              fieldErrors={errorInfo.fieldErrors}
              onClose={() => setErrorInfo(null)}
            />
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Correo electrónico
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={correo}
                  onChange={(e) => setCorreo(e.target.value)}
                  placeholder="ejemplo@correo.com"
                  className="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary focus:border-transparent"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Contraseña
                </label>
                <Link
                  to="/recuperar-contrasena"
                  className="text-xs font-semibold text-servitec-primary hover:underline"
                >
                  ¿Olvidaste tu contraseña?
                </Link>
              </div>
              <div className="relative">
                <Lock className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={contrasena}
                  onChange={(e) => setContrasena(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary focus:border-transparent"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white font-semibold text-sm shadow-sm transition-colors flex items-center justify-center gap-2 disabled:opacity-60 cursor-pointer"
            >
              {loading ? 'Verificando credenciales...' : 'Ingresar al sistema'}
              {!loading && <ArrowRight className="w-4 h-4" />}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}