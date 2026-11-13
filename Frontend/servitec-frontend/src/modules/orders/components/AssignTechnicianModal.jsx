import { useState, useEffect } from 'react';
import { UserCheck, Calendar, X, Info } from 'lucide-react';
import { ordersApi } from '../../../api/ordersApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

export default function AssignTechnicianModal({
  isOpen,
  onClose,
  order,
  activeTechnicians = [],
  onAssigned,
}) {
  const [idTecnicoAsignado, setIdTecnicoAsignado] = useState('');
  const [fechaEstimada, setFechaEstimada] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    if (isOpen && order) {
      setErrorInfo(null);
      if (order.idTecnicoAsignado) {
        setIdTecnicoAsignado(String(order.idTecnicoAsignado));
      } else if (activeTechnicians.length > 0) {
        setIdTecnicoAsignado(String(activeTechnicians[0].idUsuario));
      } else {
        setIdTecnicoAsignado('');
      }

      if (order.fechaEstimada) {
        setFechaEstimada(order.fechaEstimada.slice(0, 16));
      } else {
        const manana = new Date(Date.now() + 24 * 60 * 60 * 1000);
        manana.setHours(9, 0, 0, 0);
        const offset = manana.getTimezoneOffset() * 60000;
        const localISOTime = new Date(manana - offset).toISOString().slice(0, 16);
        setFechaEstimada(localISOTime);
      }
    }
  }, [isOpen, order, activeTechnicians]);

  if (!isOpen || !order) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);
    setLoading(true);

    try {
      const fechaFormateada =
        fechaEstimada.length === 16 ? `${fechaEstimada}:00` : fechaEstimada;

      const actualizada = await ordersApi.asignarTecnicoYFecha(order.idOrden, {
        idTecnicoAsignado,
        fechaEstimada: fechaFormateada,
      });

      onAssigned(actualizada);
      onClose();
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible asignar el técnico a la orden de servicio.')
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
              <UserCheck className="w-4 h-4 text-white" />
            </div>
            <div>
              <h3 className="text-sm font-bold">
                Asignar Técnico · {order.codigoOrden}
              </h3>
              <p className="text-[11px] text-sky-300">
                Categoría: {order.nombreCategoria} | Cliente: {order.nombreCliente}
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

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              fieldErrors={errorInfo.fieldErrors}
              onClose={() => setErrorInfo(null)}
            />
          )}

          <div className="rounded-xl bg-slate-50 border border-slate-200 p-3.5 text-xs space-y-1">
            <p className="font-bold text-servitec-navy">Resumen del problema reportado:</p>
            <p className="text-slate-600">{order.descripcionProblema}</p>
            <p className="text-slate-500 pt-1">
              <b>Dirección:</b> {order.direccionServicio}
            </p>
          </div>

          <div className="rounded-xl bg-sky-50 border border-sky-200 p-3 flex items-start gap-2.5 text-sky-900 text-xs">
            <Info className="w-4 h-4 text-servitec-primary shrink-0 mt-0.5" />
            <p>
              La asignación habilita la orden en la bandeja del técnico especialista, conservando su estado operativo en <b>Solicitado</b> hasta que se registre el diagnóstico en sitio (CV-03).
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Técnico especialista disponible (Cuenta Activa) *
            </label>
            {activeTechnicians.length === 0 ? (
              <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs">
                No hay técnicos especialistas con cuenta <b>ACTIVA</b>. Crea o activa primero una cuenta de técnico en Gestión de Usuarios.
              </div>
            ) : (
              <select
                required
                value={idTecnicoAsignado}
                onChange={(e) => setIdTecnicoAsignado(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              >
                {activeTechnicians.map((tec) => (
                  <option key={tec.idUsuario} value={tec.idUsuario}>
                    {tec.nombreCompleto} ({tec.correo})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Fecha y hora estimada de atención *
            </label>
            <div className="relative">
              <Calendar className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="datetime-local"
                required
                value={fechaEstimada}
                onChange={(e) => setFechaEstimada(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>
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
              disabled={loading || activeTechnicians.length === 0}
              className="px-5 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold transition-colors disabled:opacity-60 cursor-pointer"
            >
              {loading ? 'Guardando asignación...' : 'Confirmar asignación'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}