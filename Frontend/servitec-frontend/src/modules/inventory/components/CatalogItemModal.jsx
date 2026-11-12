import { useState, useEffect } from 'react';
import { PackagePlus, Edit3, Image as ImageIcon, X, Info } from 'lucide-react';
import { inventoryApi } from '../../../api/inventoryApi';
import { extractApiError } from '../../../api/httpClient';
import AlertBanner from '../../../shared/components/AlertBanner';

const UNIDADES_SUGERIDAS = ['Unidad', 'Metro', 'Kit', 'Litro', 'Kilogramo', 'Par', 'Rollo'];

export default function CatalogItemModal({ isOpen, onClose, itemToEdit, onSaved }) {
  const isEditMode = Boolean(itemToEdit);

  const [codigoRepuesto, setCodigoRepuesto] = useState('');
  const [nombre, setNombre] = useState('');
  const [tipoItem, setTipoItem] = useState('REPUESTO');
  const [unidadMedida, setUnidadMedida] = useState('Unidad');
  const [descripcion, setDescripcion] = useState('');
  const [imagenUrl, setImagenUrl] = useState('');
  const [precioUnitarioActual, setPrecioUnitarioActual] = useState('');
  const [existenciasIniciales, setExistenciasIniciales] = useState('0');
  const [imgPreviewError, setImgPreviewError] = useState(false);

  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    if (isOpen) {
      setErrorInfo(null);
      setImgPreviewError(false);
      if (itemToEdit) {
        setCodigoRepuesto(itemToEdit.codigoRepuesto || '');
        setNombre(itemToEdit.nombre || '');
        setTipoItem(itemToEdit.tipoItem || 'REPUESTO');
        setUnidadMedida(itemToEdit.unidadMedida || 'Unidad');
        setDescripcion(itemToEdit.descripcion || '');
        setImagenUrl(itemToEdit.imagenUrl || '');
        setPrecioUnitarioActual(
          itemToEdit.precioUnitarioActual !== undefined
            ? String(itemToEdit.precioUnitarioActual)
            : ''
        );
        setExistenciasIniciales(String(itemToEdit.existenciasFisicas ?? 0));
      } else {
        setCodigoRepuesto('');
        setNombre('');
        setTipoItem('REPUESTO');
        setUnidadMedida('Unidad');
        setDescripcion('');
        setImagenUrl('');
        setPrecioUnitarioActual('');
        setExistenciasIniciales('0');
      }
    }
  }, [isOpen, itemToEdit]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorInfo(null);
    setLoading(true);

    try {
      let resultado;
      if (isEditMode) {
        resultado = await inventoryApi.actualizarRepuesto(itemToEdit.idRepuesto, {
          codigoRepuesto,
          nombre,
          tipoItem,
          unidadMedida,
          descripcion,
          imagenUrl,
          precioUnitarioActual,
        });
      } else {
        resultado = await inventoryApi.crearRepuesto({
          codigoRepuesto,
          nombre,
          tipoItem,
          unidadMedida,
          descripcion,
          imagenUrl,
          precioUnitarioActual,
          existenciasIniciales,
        });
      }

      onSaved(resultado, isEditMode);
      onClose();
    } catch (err) {
      setErrorInfo(
        extractApiError(err, 'No fue posible guardar el elemento en el catálogo.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-2xl overflow-hidden my-8">
        {/* Cabecera */}
        <div className="flex items-center justify-between px-6 py-4 bg-servitec-navy text-white">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-servitec-primary flex items-center justify-center">
              {isEditMode ? (
                <Edit3 className="w-4 h-4 text-white" />
              ) : (
                <PackagePlus className="w-4 h-4 text-white" />
              )}
            </div>
            <div>
              <h3 className="text-sm font-bold">
                {isEditMode
                  ? `Editar elemento: ${itemToEdit.codigoRepuesto}`
                  : 'Registrar nuevo repuesto o insumo'}
              </h3>
              <p className="text-[11px] text-sky-300">
                Catálogo oficial de precios base y existencias (CV-18)
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

        <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[82vh] overflow-y-auto">
          {errorInfo && (
            <AlertBanner
              type="error"
              message={errorInfo.message}
              fieldErrors={errorInfo.fieldErrors}
              onClose={() => setErrorInfo(null)}
            />
          )}

          {isEditMode && (
            <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 flex items-start gap-2.5 text-amber-900 text-xs">
              <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <p>
                Los cambios en el <b>precio unitario base</b> se aplicarán a presupuestos futuros y no modifican los presupuestos emitidos o aprobados previamente (RN-03). Para modificar las existencias físicas, utiliza el módulo de <b>Movimientos de stock</b>.
              </p>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Código único *
              </label>
              <input
                type="text"
                required
                maxLength={50}
                value={codigoRepuesto}
                onChange={(e) => setCodigoRepuesto(e.target.value.toUpperCase())}
                placeholder="Ej. REF-CAP-035"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm font-mono uppercase focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Tipo de elemento *
              </label>
              <select
                value={tipoItem}
                onChange={(e) => setTipoItem(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              >
                <option value="REPUESTO">REPUESTO (Pieza o componente)</option>
                <option value="INSUMO">INSUMO (Material consumible)</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Nombre del elemento *
              </label>
              <input
                type="text"
                required
                maxLength={150}
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej. Capacitor de arranque 35 uF"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Unidad de medida *
              </label>
              <input
                type="text"
                list="unidades-catalogo"
                required
                maxLength={30}
                value={unidadMedida}
                onChange={(e) => setUnidadMedida(e.target.value)}
                placeholder="Ej. Unidad"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
              <datalist id="unidades-catalogo">
                {UNIDADES_SUGERIDAS.map((u) => (
                  <option key={u} value={u} />
                ))}
              </datalist>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Precio unitario base (COP) *
              </label>
              <input
                type="number"
                required
                min="0"
                step="1"
                value={precioUnitarioActual}
                onChange={(e) => setPrecioUnitarioActual(e.target.value)}
                placeholder="Ej. 85000"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
              />
            </div>

            {!isEditMode && (
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Existencias físicas iniciales *
                </label>
                <input
                  type="number"
                  required
                  min="0"
                  step="0.01"
                  value={existenciasIniciales}
                  onChange={(e) => setExistenciasIniciales(e.target.value)}
                  placeholder="0"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                />
                <span className="block text-[11px] text-slate-500 mt-1">
                  Si es mayor que 0, registrará automáticamente el movimiento inicial de apertura.
                </span>
              </div>
            )}
          </div>

          {/* Campo de URL de imagen con vista previa */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
              URL de imagen del repuesto / insumo (Opcional)
            </label>
            <div className="flex items-center gap-3">
              <div className="relative flex-1">
                <ImageIcon className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="url"
                  maxLength={500}
                  value={imagenUrl}
                  onChange={(e) => {
                    setImagenUrl(e.target.value);
                    setImgPreviewError(false);
                  }}
                  placeholder="https://cdn.servitec.com/catalogo/repuesto.png"
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
                />
              </div>

              <div className="w-12 h-12 rounded-xl border border-slate-200 bg-slate-50 flex items-center justify-center overflow-hidden shrink-0">
                {imagenUrl && !imgPreviewError ? (
                  <img
                    src={imagenUrl}
                    alt="Vista previa"
                    onError={() => setImgPreviewError(true)}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <ImageIcon className="w-5 h-5 text-slate-400" />
                )}
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
              Descripción técnica *
            </label>
            <textarea
              rows={3}
              required
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              placeholder="Especificaciones técnicas, compatibilidad y uso del repuesto o insumo..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-servitec-primary"
            />
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
              disabled={loading}
              className="px-5 py-2.5 rounded-xl bg-servitec-primary hover:bg-servitec-primary-hover text-white text-xs font-semibold transition-colors disabled:opacity-60 cursor-pointer"
            >
              {loading
                ? 'Guardando...'
                : isEditMode
                ? 'Guardar cambios'
                : 'Registrar en catálogo'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}