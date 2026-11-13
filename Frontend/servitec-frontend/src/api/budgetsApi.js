import httpClient from './httpClient';

function unwrapApiData(response) {
  const body = response.data;
  if (body && typeof body === 'object') {
    if ('datos' in body) return body.datos;
    if ('data' in body) return body.data;
  }
  return body;
}

export const budgetsApi = {
  /**
   * CV-05: POST /api/v1/orders/{idOrden}/budgets/original
   * Elabora el presupuesto original tomando precios del catálogo activo y cambia la orden a PRESUPUESTADO.
   */
  async crearPresupuestoOriginal(idOrden, { tarifaHora, horasEstimadas, detalles }) {
    const response = await httpClient.post(`/orders/${idOrden}/budgets/original`, {
      tarifaHora: Number(tarifaHora),
      horasEstimadas: Number(horasEstimadas),
      detalles: (detalles || []).map((d) => ({
        idRepuesto: Number(d.idRepuesto),
        cantidadAutorizada: Number(d.cantidadAutorizada),
        justificacion: d.justificacion?.trim() || null,
      })),
    });
    return unwrapApiData(response);
  },

  /**
   * CV-12: POST /api/v1/orders/{idOrden}/budgets/complementary
   */
  async crearPresupuestoComplementario(idOrden, { detalles }) {
    const response = await httpClient.post(`/orders/${idOrden}/budgets/complementary`, {
      detalles: detalles.map((d) => ({
        idRepuesto: Number(d.idRepuesto),
        cantidadAutorizada: Number(d.cantidadAutorizada),
        justificacion: d.justificacion.trim(),
      })),
    });
    return unwrapApiData(response);
  },

  /**
   * CV-15: POST /api/v1/orders/{idOrden}/budgets/complementary/authorized/{idAutorizacion}
   */
  async crearNuevaCotizacionComplementaria(idOrden, idAutorizacion, { detalles }) {
    const response = await httpClient.post(
      `/orders/${idOrden}/budgets/complementary/authorized/${idAutorizacion}`,
      {
        detalles: detalles.map((d) => ({
          idRepuesto: Number(d.idRepuesto),
          cantidadAutorizada: Number(d.cantidadAutorizada),
          justificacion: d.justificacion.trim(),
        })),
      }
    );
    return unwrapApiData(response);
  },

  /**
   * GET /api/v1/orders/{idOrden}/budgets
   */
  async listarPresupuestosPorOrden(idOrden) {
    const response = await httpClient.get(`/orders/${idOrden}/budgets`);
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },

  /**
   * GET /api/v1/budgets/{idPresupuesto}
   */
  async consultarPresupuestoPorId(idPresupuesto) {
    const response = await httpClient.get(`/budgets/${idPresupuesto}`);
    return unwrapApiData(response);
  },

  /**
   * CV-06 y CV-13: POST /api/v1/budgets/{idPresupuesto}/decision
   * Registra la decisión digital inmutable del Cliente (APROBADO o RECHAZADO).
   */
  async decidirSobrePresupuesto(idPresupuesto, decision) {
    const response = await httpClient.post(`/budgets/${idPresupuesto}/decision`, {
      decision,
    });
    return unwrapApiData(response);
  },

  /**
   * CV-07 / RN-09: POST /api/v1/budgets/{idPresupuesto}/technical-evaluation
   */
  async registrarEvaluacionTecnica(idPresupuesto, { elementosIndispensables, justificacion }) {
    const response = await httpClient.post(
      `/budgets/${idPresupuesto}/technical-evaluation`,
      {
        elementosIndispensables: Boolean(elementosIndispensables),
        justificacion: justificacion.trim(),
      }
    );
    return unwrapApiData(response);
  },

  /**
   * GET /api/v1/orders/{idOrden}/technical-evaluations
   */
  async listarEvaluacionesPorOrden(idOrden) {
    const response = await httpClient.get(`/orders/${idOrden}/technical-evaluations`);
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },

  /**
   * CV-14: POST /api/v1/budgets/{idPresupuesto}/requote-authorization
   */
  async autorizarNuevaCotizacion(idPresupuesto) {
    const response = await httpClient.post(`/budgets/${idPresupuesto}/requote-authorization`);
    return unwrapApiData(response);
  },

  /**
   * GET /api/v1/orders/{idOrden}/requote-authorizations
   */
  async listarAutorizacionesPorOrden(idOrden) {
    const response = await httpClient.get(`/orders/${idOrden}/requote-authorizations`);
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },
};