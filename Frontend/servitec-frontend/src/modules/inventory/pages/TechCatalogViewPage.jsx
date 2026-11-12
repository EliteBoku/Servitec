import { useState, useEffect, useCallback } from 'react';
import { Search, Package, Lock, RefreshCw } from 'lucide-react';
import { inventoryApi } from '../../../api/inventoryApi';
import { extractApiError } from '../../../api/httpClient';
import { formatCurrencyCOP, formatQuantity } from '../../../shared/utils/formatters';
import AlertBanner from '../../../shared/components/AlertBanner';
import EmptyState from '../../../shared/components/EmptyState';

export default function TechCatalogViewPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [tipoFiltro, setTipoFiltro] = useState('TODOS');
  const [errorInfo, setErrorInfo] = useState(null);

  const cargarCatalogoActivo = useCallback(async () => {
    setLoading(true);
    setErrorInfo(null);
    try {
      const data = await inventoryApi.listarCatalogo({
        busqueda,
        tipoItem: tipoFiltro,
        activo: true,
      });
      setItems(data);
    } catch (err) {
      setErrorInfo(extractApiError(err, 'No se pudo consultar el catálogo activo.'));
    } finally {
      setLoading(false);
    }
  }, [busqueda, tipoFiltro]);

  useEffect(() => {
    const timer = setTimeout(() => {
      cargarCatalogoActivo();
    }, 250);
    return () => clearTimeout(timer);
  }, [cargarCatalogoActivo]);

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg sm:text-xl font-bold text-servitec-navy">
            Catálogo Activo de Repuestos e Insumos
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Consulta existencias disponibles y precios unitarios oficiales (RN-03).
          </p>
        </div>

        <button
          type="button"
          onClick={cargarCatalogoActivo}
          className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 cursor-pointer"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {errorInfo && (
        <AlertBanner
          type="error"
          message={errorInfo.message}
          onClose={() => setErrorInfo(null)}
        />
      )}

      {/* Buscador y pestañas de tipo */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 space-y-3">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar repuesto o insumo por nombre o código..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
          />
        </div>

        <div className="grid grid-cols-3 gap-2 bg-slate-100 p-1 rounded-xl">
          {[
            { id: 'TODOS', label: 'Todos' },
            { id: 'REPUESTO', label: 'Repuestos' },
            { id: 'INSUMO', label: 'Insumos' },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setTipoFiltro(tab.id)}
              className={`py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                tipoFiltro === tab.id
                  ? 'bg-white text-servitec-navy shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tarjetas de elementos activos */}
      {items.length === 0 && !loading ? (
        <EmptyState
          title="Sin elementos coincidentes"
          description="No se encontraron repuestos o insumos activos con el criterio ingresado."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {items.map((item) => {
            const disponibles = Number(item.existenciasDisponibles);
            return (
              <div
                key={item.idRepuesto}
                className="bg-white rounded-2xl border border-slate-200 p-4 flex gap-3.5 shadow-2xs"
              >
                <div className="w-16 h-16 rounded-xl border border-slate-200 bg-slate-100 flex items-center justify-center overflow-hidden shrink-0">
                  {item.imagenUrl ? (
                    <img
                      src={item.imagenUrl}
                      alt={item.nombre}
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                      }}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <Package className="w-7 h-7 text-servitec-primary" />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-mono text-xs font-bold text-servitec-primary">
                      {item.codigoRepuesto}
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[10px] font-bold uppercase">
                      {item.tipoItem} · {item.unidadMedida}
                    </span>
                  </div>

                  <h3 className="text-sm font-bold text-servitec-navy mt-0.5 truncate">
                    {item.nombre}
                  </h3>
                  <p className="text-xs text-slate-500 line-clamp-2 mt-0.5">
                    {item.descripcion}
                  </p>

                  <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1 text-xs font-bold text-servitec-navy">
                      <Lock
                        className="w-3.5 h-3.5 text-slate-400"
                        title="Precio oficial bloqueado"
                      />
                      <span>{formatCurrencyCOP(item.precioUnitarioActual)}</span>
                    </div>

                    <span
                      className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${
                        disponibles > 0
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-rose-100 text-rose-800'
                      }`}
                    >
                      Disponible: {formatQuantity(item.existenciasDisponibles)}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}