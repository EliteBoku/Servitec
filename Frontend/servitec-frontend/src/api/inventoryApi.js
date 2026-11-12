import httpClient from './httpClient';

function unwrapApiData(response) {
  const body = response.data;
  if (body && typeof body === 'object') {
    if ('datos' in body) return body.datos;
    if ('data' in body) return body.data;
  }
  return body;
}

export const inventoryApi = {
  /**
   * CV-18: GET /api/v1/inventory/catalog
   */
  async listarCatalogo({ busqueda, tipoItem, activo } = {}) {
    const params = {};
    if (busqueda && busqueda.trim() !== '') params.busqueda = busqueda.trim();
    if (tipoItem && tipoItem !== 'TODOS') params.tipoItem = tipoItem;
    if (activo !== undefined && activo !== null && activo !== 'TODOS') {
      params.activo = activo;
    }

    const response = await httpClient.get('/inventory/catalog', { params });
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },

  /**
   * CV-18: GET /api/v1/inventory/catalog/{idRepuesto}
   */
  async consultarPorId(idRepuesto) {
    const response = await httpClient.get(`/inventory/catalog/${idRepuesto}`);
    return unwrapApiData(response);
  },

  /**
   * CV-18: POST /api/v1/inventory/catalog
   */
  async crearRepuesto(payload) {
    const response = await httpClient.post('/inventory/catalog', {
      codigoRepuesto: payload.codigoRepuesto.trim().toUpperCase(),
      nombre: payload.nombre.trim(),
      tipoItem: payload.tipoItem,
      unidadMedida: payload.unidadMedida.trim(),
      descripcion: payload.descripcion.trim(),
      imagenUrl: payload.imagenUrl?.trim() || null,
      precioUnitarioActual: Number(payload.precioUnitarioActual),
      existenciasIniciales: Number(payload.existenciasIniciales ?? 0),
    });
    return unwrapApiData(response);
  },

  /**
   * CV-18: PUT /api/v1/inventory/catalog/{idRepuesto}
   */
  async actualizarRepuesto(idRepuesto, payload) {
    const response = await httpClient.put(`/inventory/catalog/${idRepuesto}`, {
      codigoRepuesto: payload.codigoRepuesto.trim().toUpperCase(),
      nombre: payload.nombre.trim(),
      tipoItem: payload.tipoItem,
      unidadMedida: payload.unidadMedida.trim(),
      descripcion: payload.descripcion.trim(),
      imagenUrl: payload.imagenUrl?.trim() || null,
      precioUnitarioActual: Number(payload.precioUnitarioActual),
    });
    return unwrapApiData(response);
  },

  /**
   * CV-18: PATCH /api/v1/inventory/catalog/{idRepuesto}/deactivate
   */
  async desactivarRepuesto(idRepuesto) {
    const response = await httpClient.patch(`/inventory/catalog/${idRepuesto}/deactivate`);
    return unwrapApiData(response);
  },

  /**
   * CV-18: PATCH /api/v1/inventory/catalog/{idRepuesto}/activate
   */
  async reactivarRepuesto(idRepuesto) {
    const response = await httpClient.patch(`/inventory/catalog/${idRepuesto}/activate`);
    return unwrapApiData(response);
  },

  /**
   * CV-19: POST /api/v1/inventory/movements
   */
  async registrarMovimientoManual({ idRepuesto, tipoMovimiento, cantidad, motivo }) {
    const response = await httpClient.post('/inventory/movements', {
      idRepuesto: Number(idRepuesto),
      tipoMovimiento,
      cantidad: Number(cantidad),
      motivo: motivo.trim(),
    });
    return unwrapApiData(response);
  },

  /**
   * CV-19: GET /api/v1/inventory/movements
   */
  async consultarHistorialMovimientos({
    idRepuesto,
    tipoMovimiento,
    origenMovimiento,
    fechaDesde,
    fechaHasta,
  } = {}) {
    const params = {};
    if (idRepuesto) params.idRepuesto = Number(idRepuesto);
    if (tipoMovimiento && tipoMovimiento !== 'TODOS') params.tipoMovimiento = tipoMovimiento;
    if (origenMovimiento && origenMovimiento !== 'TODOS') {
      params.origenMovimiento = origenMovimiento;
    }
    if (fechaDesde) params.fechaDesde = fechaDesde;
    if (fechaHasta) params.fechaHasta = fechaHasta;

    const response = await httpClient.get('/inventory/movements', { params });
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },
};