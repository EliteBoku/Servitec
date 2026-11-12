import { useState, useEffect, useMemo } from 'react';
import {
  UserPlus,
  Search,
  Users,
  Wrench,
  UserCheck,
  Clock,
  RefreshCw,
} from 'lucide-react';
import { usersApi } from '../../../api/usersApi';
import { formatDateTime, formatRoleName } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';
import EmptyState from '../../../shared/components/EmptyState';
import CreateUserModal from '../components/CreateUserModal';

const ACCOUNT_STATUS_BADGE = {
  ACTIVA: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  PENDIENTE_ACTIVACION: 'bg-amber-100 text-amber-800 border-amber-200',
  INACTIVA: 'bg-rose-100 text-rose-800 border-rose-200',
};

const ACCOUNT_STATUS_LABEL = {
  ACTIVA: 'Activa',
  PENDIENTE_ACTIVACION: 'Pendiente de activación',
  INACTIVA: 'Inactiva',
};

export default function AdminUsersPage() {
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [rolFiltro, setRolFiltro] = useState('TODOS');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);

  const cargarUsuarios = async () => {
    setLoading(true);
    try {
      const data = await usersApi.listarUsuarios();
      setUsuarios(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarUsuarios();
  }, []);

  const handleUserCreated = (nuevoUsuario) => {
    setUsuarios((prev) => {
      const sinDuplicado = prev.filter(
        (u) => u.correo?.toLowerCase() !== nuevoUsuario.correo?.toLowerCase()
      );
      return [nuevoUsuario, ...sinDuplicado];
    });
    setSuccessMessage(
      `La cuenta de "${nuevoUsuario.nombreCompleto}" (${nuevoUsuario.correo}) fue creada en estado Pendiente de activación y se envió el enlace por correo.`
    );
  };

  const usuariosFiltrados = useMemo(() => {
    return usuarios.filter((u) => {
      const cumpleRol = rolFiltro === 'TODOS' || u.rol === rolFiltro;
      const termino = busqueda.trim().toLowerCase();
      const cumpleBusqueda =
        !termino ||
        u.nombreCompleto?.toLowerCase().includes(termino) ||
        u.correo?.toLowerCase().includes(termino);
      return cumpleRol && cumpleBusqueda;
    });
  }, [usuarios, rolFiltro, busqueda]);

  const metricas = useMemo(() => {
    return {
      total: usuarios.length,
      tecnicos: usuarios.filter((u) => u.rol === 'TECNICO_ESPECIALISTA').length,
      clientes: usuarios.filter((u) => u.rol === 'CLIENTE').length,
      pendientes: usuarios.filter((u) => u.estadoCuenta === 'PENDIENTE_ACTIVACION').length,
    };
  }, [usuarios]);

  return (
    <div className="space-y-6">
      {/* Encabezado y Acción Principal */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-servitec-navy">
            Gestión de Cuentas de Usuario
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Registra cuentas de Clientes y Técnicos especialistas mediante enlace de activación seguro (CV-10).
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={cargarUsuarios}
            title="Actualizar listado"
            className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
          >
            <UserPlus className="w-4 h-4" />
            <span>Crear usuario</span>
          </button>
        </div>
      </div>

      {successMessage && (
        <AlertBanner
          type="success"
          message={successMessage}
          onClose={() => setSuccessMessage(null)}
        />
      )}

      {/* Tarjetas de Resumen */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-servitec-navy/10 text-servitec-navy flex items-center justify-center">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Total registrados</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.total}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-sky-100 text-servitec-primary flex items-center justify-center">
            <Wrench className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Técnicos especialistas</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.tecnicos}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
            <UserCheck className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Clientes</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.clientes}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
            <Clock className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-500 font-medium">Pendientes activación</p>
            <p className="text-xl font-bold text-servitec-navy">{metricas.pendientes}</p>
          </div>
        </div>
      </div>

      {/* Barra de Búsqueda y Filtro por Rol */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre completo o correo electrónico..."
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          />
        </div>

        <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl">
          {[
            { id: 'TODOS', label: 'Todos' },
            { id: 'TECNICO_ESPECIALISTA', label: 'Técnicos' },
            { id: 'CLIENTE', label: 'Clientes' },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setRolFiltro(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                rolFiltro === tab.id
                  ? 'bg-white text-servitec-navy shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tabla de Usuarios */}
      {usuariosFiltrados.length === 0 ? (
        <EmptyState
          title="No hay cuentas de usuario para mostrar"
          description="Utiliza el botón 'Crear usuario' para registrar un nuevo Cliente o Técnico especialista y enviarle su enlace de activación."
          action={
            <button
              type="button"
              onClick={() => setIsCreateModalOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-servitec-primary text-white text-xs font-semibold cursor-pointer"
            >
              <UserPlus className="w-4 h-4" />
              <span>Crear primer usuario</span>
            </button>
          }
        />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                  <th className="py-3.5 px-4">ID</th>
                  <th className="py-3.5 px-4">Nombre completo</th>
                  <th className="py-3.5 px-4">Correo electrónico</th>
                  <th className="py-3.5 px-4">Rol</th>
                  <th className="py-3.5 px-4">Estado de cuenta</th>
                  <th className="py-3.5 px-4">Fecha de creación</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-xs">
                {usuariosFiltrados.map((u) => (
                  <tr key={u.idUsuario || u.correo} className="hover:bg-slate-50/80">
                    <td className="py-3.5 px-4 font-semibold text-slate-500">
                      #{u.idUsuario}
                    </td>
                    <td className="py-3.5 px-4 font-bold text-servitec-navy">
                      {u.nombreCompleto}
                    </td>
                    <td className="py-3.5 px-4 text-slate-600">{u.correo}</td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center px-2.5 py-1 rounded-lg bg-slate-100 text-slate-800 font-semibold text-[11px]">
                        {formatRoleName(u.rol)}
                      </span>
                    </td>
                    <td className="py-3.5 px-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold border ${
                          ACCOUNT_STATUS_BADGE[u.estadoCuenta] ||
                          'bg-slate-100 text-slate-700 border-slate-200'
                        }`}
                      >
                        {ACCOUNT_STATUS_LABEL[u.estadoCuenta] || u.estadoCuenta}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-slate-500">
                      {formatDateTime(u.fechaCreacion)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal de Creación */}
      <CreateUserModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onUserCreated={handleUserCreated}
      />
    </div>
  );
}