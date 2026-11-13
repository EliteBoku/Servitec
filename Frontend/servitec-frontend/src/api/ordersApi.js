import httpClient from './httpClient';

function unwrapApiData(response) {
  const body = response.data;
  if (body && typeof body === 'object') {
    if ('datos' in body) return body.datos;
    if ('data' in body) return body.data;
  }
  return body;
}

export const ordersApi = {
  /**
   * CV-02: GET /api/v1/orders/categories
   * Obtiene las 4 categorías activas (Refrigeración, Climatización, Línea blanca, Electricidad).
   */
  async listarCategoriasActivas() {
    const response = await httpClient.get('/orders/categories');
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },

  /**
   * CV-02: GET /api/v1/orders/consent-status
   * Verifica si el cliente autenticado requiere aceptar el Hábeas Data en su primera solicitud.
   */
  async consultarEstadoConsentimiento() {
    const response = await httpClient.get('/orders/consent-status');
    return unwrapApiData(response);
  },

  /**
   * CV-02: POST /api/v1/orders
   * Registra una nueva solicitud de servicio en estado SOLICITADO.
   */
  async crearSolicitudServicio({
    idCategoria,
    descripcionProblema,
    direccionServicio,
    autorizaTratamientoDatos,
  }) {
    const response = await httpClient.post('/orders', {
      idCategoria: Number(idCategoria),
      descripcionProblema: descripcionProblema.trim(),
      direccionServicio: direccionServicio.trim(),
      autorizaTratamientoDatos: Boolean(autorizaTratamientoDatos),
    });
    return unwrapApiData(response);
  },

  /**
   * CV-02, CV-03, RN-05: GET /api/v1/orders
   * Lista las órdenes según el rol autenticado (Cliente: propias, Técnico: asignadas, Admin: todas con filtros).
   */
  async listarOrdenes({ estado, idCategoria, idTecnico, busqueda } = {}) {
    const params = {};
    if (estado && estado !== 'TODOS') params.estado = estado;
    if (idCategoria && idCategoria !== 'TODAS') params.idCategoria = Number(idCategoria);
    if (idTecnico && idTecnico !== 'TODOS') params.idTecnico = Number(idTecnico);
    if (busqueda && busqueda.trim() !== '') params.busqueda = busqueda.trim();

    const response = await httpClient.get('/orders', { params });
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },

  /**
   * GET /api/v1/orders/{idOrden}
   */
  async consultarDetalleOrden(idOrden) {
    const response = await httpClient.get(`/orders/${idOrden}`);
    return unwrapApiData(response);
  },

  /**
   * CV-03: PATCH /api/v1/orders/{idOrden}/assign
   * Asigna un técnico especialista activo y una fecha estimada (exclusivo Administrador).
   */
  async asignarTecnicoYFecha(idOrden, { idTecnicoAsignado, fechaEstimada }) {
    const response = await httpClient.patch(`/orders/${idOrden}/assign`, {
      idTecnicoAsignado: Number(idTecnicoAsignado),
      fechaEstimada,
    });
    return unwrapApiData(response);
  },

  /**
   * GET /api/v1/orders/{idOrden}/history
   * Consulta las transiciones registradas en HISTORIAL_ESTADO_ORDEN.
   */
  async consultarHistorialEstados(idOrden) {
    const response = await httpClient.get(`/orders/${idOrden}/history`);
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },
};