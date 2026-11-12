import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  ClipboardList,
  Users,
  Package,
  ArrowLeftRight,
  Wrench,
  LogOut,
} from 'lucide-react';
import Header from '../shared/components/Header';
import { useAuth } from '../context/AuthContext';

const ADMIN_NAV_ITEMS = [
  {
    to: '/admin/ordenes',
    label: 'Órdenes de servicio',
    shortLabel: 'Órdenes',
    icon: ClipboardList,
  },
  {
    to: '/admin/usuarios',
    label: 'Gestión de usuarios',
    shortLabel: 'Usuarios',
    icon: Users,
  },
  {
    to: '/admin/catalogo',
    label: 'Catálogo e Inventario',
    shortLabel: 'Catálogo',
    icon: Package,
  },
  {
    to: '/admin/movimientos',
    label: 'Movimientos de stock',
    shortLabel: 'Stock',
    icon: ArrowLeftRight,
  },
];

export default function AdminLayout() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-screen flex bg-slate-50 pb-16 md:pb-0">
      {/* Sidebar fijo en Escritorio (#0B2545) */}
      <aside className="hidden md:flex md:w-64 md:flex-col md:sticky md:top-0 md:h-screen bg-servitec-navy text-white shrink-0">
        <div className="p-6 border-b border-white/10 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-servitec-primary flex items-center justify-center shadow-md">
            <Wrench className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="font-bold text-base tracking-tight block">SERVITEC</span>
            <span className="text-[11px] text-sky-300 font-medium">Panel Administrador</span>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1.5 overflow-y-auto">
          {ADMIN_NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold transition-colors ${
                  isActive
                    ? 'bg-servitec-primary text-white shadow-xs'
                    : 'text-slate-300 hover:bg-white/10 hover:text-white'
                }`
              }
            >
              <Icon className="w-4 h-4 shrink-0" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        {/* Pie del Sidebar con botón Cerrar Sesión */}
        <div className="p-4 border-t border-white/10 space-y-3">
          <button
            type="button"
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold text-rose-300 hover:bg-rose-500/15 hover:text-rose-200 transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4 shrink-0" />
            <span>Cerrar Sesión</span>
          </button>
          <p className="px-2 text-[11px] text-slate-400">
            Servitec Los Alpes · Gestión Operativa
          </p>
        </div>
      </aside>

      {/* Contenedor Principal */}
      <div className="flex-1 flex flex-col min-w-0">
        <Header
          title="Administración Central"
          subtitle="Supervisión de órdenes, usuarios, catálogo y movimientos de inventario"
        />

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>

      {/* Navegación Inferior Responsive en Móvil */}
      <nav className="md:hidden fixed bottom-0 inset-x-0 z-40 bg-servitec-navy text-white border-t border-white/10 shadow-lg">
        <div className="grid grid-cols-5">
          {ADMIN_NAV_ITEMS.map(({ to, shortLabel, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `min-h-[56px] flex flex-col items-center justify-center gap-1 text-[11px] font-semibold transition-colors ${
                  isActive ? 'text-servitec-primary bg-white/5' : 'text-slate-300 hover:text-white'
                }`
              }
            >
              <Icon className="w-4 h-4" />
              <span className="truncate">{shortLabel}</span>
            </NavLink>
          ))}
          <button
            type="button"
            onClick={handleLogout}
            className="min-h-[56px] flex flex-col items-center justify-center gap-1 text-[11px] font-semibold text-rose-300 hover:text-rose-200 cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Salir</span>
          </button>
        </div>
      </nav>
    </div>
  );
}