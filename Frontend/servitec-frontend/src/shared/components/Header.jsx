import { User, Wrench } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { formatRoleName } from '../utils/formatters';

export default function Header({ title, subtitle }) {
  const { user } = useAuth();

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-slate-200 px-4 sm:px-6 py-3.5 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        {/* Ícono de marca visible solo en móvil cuando el sidebar está oculto */}
        <div className="md:hidden w-9 h-9 rounded-xl bg-servitec-navy text-white flex items-center justify-center shrink-0">
          <Wrench className="w-5 h-5 text-servitec-primary" />
        </div>
        <div className="min-w-0">
          <h1 className="text-base sm:text-lg font-bold text-servitec-navy truncate">
            {title || 'Servitec Los Alpes'}
          </h1>
          {subtitle && (
            <p className="text-xs text-slate-500 truncate hidden sm:block">{subtitle}</p>
          )}
        </div>
      </div>

      {user && (
        <div className="flex items-center gap-2.5 bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5 shrink-0">
          <div className="w-7 h-7 rounded-lg bg-servitec-primary/15 text-servitec-primary flex items-center justify-center">
            <User className="w-4 h-4" />
          </div>
          <div className="text-left">
            <p className="text-xs font-bold text-slate-800 leading-tight">
              {user.nombreCompleto}
            </p>
            <span className="block text-[11px] font-semibold text-servitec-primary">
              {formatRoleName(user.rol)}
            </span>
          </div>
        </div>
      )}
    </header>
  );
}