import { useState } from 'react';
import { UserPlus, Mail, User, Shield, X, Info } from 'lucide-react';
import { usersApi } from '../../../api/usersApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function CreateUserModal({ isOpen, onClose, onUserCreated }) {
  const [nombreCompleto, setNombreCompleto] = useState('');
  const [correo, setCorreo] = useState('');
  const [rol, setRol] = useState('CLIENTE');
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  if (!isOpen) return null;

  const handleResetAndClose = () => {
    setNombreCompleto('');
    setCorreo('');
    setRol('CLIENTE');
    setErrorInfo(null);
    onClose();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);
    setLoading(true);

    try {
      const nuevoUsuario = await usersApi.crearUsuario({
        nombreCompleto,
        correo,
        rol,
      });
      onUserCreated(nuevoUsuario);
      handleResetAndClose();
    } catch (err) {
      setErrorInfo(
        extractApiError(
          err,
          'No fue posible crear el usuario. Verifica que el correo no esté registrado.'
        )
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-lg overflow-hidden">
        {/* Cabecera */}
        <div className="flex items-center justify-between px-6 py-4 bg-servitec-navy text-white">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-servitec-primary flex items-center justify-center">
              <UserPlus className="w-4 h-4 text-white" />
            </div>
            <div>
              <h3 className="text-sm font-bold">Registrar nueva cuenta de usuario</h3>
              <p className="text-[11px] text-sky-300">
                Creación controlada con activación por correo electrónico
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleResetAndClose}
            disabled={loading}
            className="text-slate-300 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              fieldErrors={errorInfo.fieldErrors}
              onClose={() => setErrorInfo(null)}
            />
          )}

          {/* Aviso de Regla de Negocio (CV-10 / RN-06) */}
          <div className="rounded-xl bg-sky-50 border border-sky-200 p-3.5 flex items-start gap-2.5 text-sky-900 text-xs">
            <Info className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              La cuenta se creará en estado <b>Pendiente de activación</b> sin contraseña inicial. El sistema enviará automáticamente un enlace único al correo del usuario (válido por 24 horas) para que defina su clave personal.
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Nombre completo *
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                maxLength={150}
                value={nombreCompleto}
                onChange={(e) => setNombreCompleto(e.target.value)}
                placeholder="Ej. Diego Fernando Ramírez"
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Correo electrónico *
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                maxLength={150}
                value={correo}
                onChange={(e) => setCorreo(e.target.value)}
                placeholder="usuario@ejemplo.com"
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Rol asignado en el sistema *
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label
                className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-colors ${
                  rol === 'CLIENTE'
                    ? 'border-servitec-primary bg-sky-50/60'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <input
                  type="radio"
                  name="rol"
                  value="CLIENTE"
                  checked={rol === 'CLIENTE'}
                  onChange={(e) => setRol(e.target.value)}
                  className="mt-1 accent-servitec-primary"
                />
                <div>
                  <span className="block text-xs font-bold text-servitec-navy">Cliente</span>
                  <span className="block text-[11px] text-slate-500 mt-0.5">
                    Solicita servicios, aprueba presupuestos y consulta resúmenes.
                  </span>
                </div>
              </label>

              <label
                className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-colors ${
                  rol === 'TECNICO_ESPECIALISTA'
                    ? 'border-servitec-primary bg-sky-50/60'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <input
                  type="radio"
                  name="rol"
                  value="TECNICO_ESPECIALISTA"
                  checked={rol === 'TECNICO_ESPECIALISTA'}
                  onChange={(e) => setRol(e.target.value)}
                  className="mt-1 accent-servitec-primary"
                />
                <div>
                  <span className="block text-xs font-bold text-servitec-navy">
                    Técnico especialista
                  </span>
                  <span className="block text-[11px] text-slate-500 mt-0.5">
                    Registra diagnósticos, presupuestos, tiempos y cierres en campo.
                  </span>
                </div>
              </label>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={handleResetAndClose}
              disabled={loading}
              className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-100 transition-colors cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold flex items-center gap-2 transition-colors disabled:opacity-60 cursor-pointer"
            >
              <Shield className="w-4 h-4" />
              {loading ? 'Registrando y enviando correo...' : 'Crear usuario y enviar enlace'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}