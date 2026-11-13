import httpClient from './httpClient';

function unwrapApiData(response) {
  const body = response.data;
  if (body && typeof body === 'object') {
    if ('datos' in body) return body.datos;
    if ('data' in body) return body.data;
  }
  return body;
}

export const executionApi = {
  /**
   * CV-07 y CV-17: POST /api/v1/orders/{idOrden}/execution/start
   * Inicia la reparación validando presupuesto original aprobado y reservas activas.
   */
  async iniciarReparacion(idOrden) {
    const response = await httpClient.post(`/orders/${idOrden}/execution/start`);
    return unwrapApiData(response);
  },

  /**
   * CV-07 y CV-17: POST /api/v1/orders/{idOrden}/execution/pause
   * Pausa la reparación registrando la justificación obligatoria en HISTORIAL_ESTADO_ORDEN.
   */
  async pausarReparacion(idOrden, { justificacion }) {
    const response = await httpClient.post(`/orders/${idOrden}/execution/pause`, {
      justificacion: justificacion.trim(),
    });
    return unwrapApiData(response);
  },

  /**
   * CV-07 y CV-17: POST /api/v1/orders/{idOrden}/execution/resume
   * Reanuda explícitamente una reparación pausada.
   */
  async reanudarReparacion(idOrden) {
    const response = await httpClient.post(`/orders/${idOrden}/execution/resume`);
    return unwrapApiData(response);
  },

  /**
   * CV-17: GET /api/v1/orders/{idOrden}/execution/time-summary
   * Consulta el tiempo efectivo acumulado, intervalos e importe de mano de obra calculado.
   */
  async consultarResumenTiempo(idOrden) {
    const response = await httpClient.get(`/orders/${idOrden}/execution/time-summary`);
    return unwrapApiData(response);
  },
};