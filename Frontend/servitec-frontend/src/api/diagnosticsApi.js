import httpClient from './httpClient';

function unwrapApiData(response) {
  const body = response.data;
  if (body && typeof body === 'object') {
    if ('datos' in body) return body.datos;
    if ('data' in body) return body.data;
  }
  return body;
}

export const diagnosticsApi = {
  /**
   * CV-04: GET /api/v1/orders/{idOrden}/diagnostic/template
   * Obtiene los parámetros dinámicos obligatorios y opcionales según la categoría de la orden.
   */
  async obtenerPlantillaPorOrden(idOrden) {
    const response = await httpClient.get(`/orders/${idOrden}/diagnostic/template`);
    const data = unwrapApiData(response);
    return Array.isArray(data) ? data : [];
  },

  /**
   * CV-04: POST /api/v1/orders/{idOrden}/diagnostic
   * Registra el diagnóstico único en sitio y cambia automáticamente la orden a EN_DIAGNOSTICO.
   */
  async registrarDiagnostico(idOrden, { observaciones, parametros }) {
    const response = await httpClient.post(`/orders/${idOrden}/diagnostic`, {
      observaciones: observaciones.trim(),
      parametros,
    });
    return unwrapApiData(response);
  },

  /**
   * CV-04: GET /api/v1/orders/{idOrden}/diagnostic
   * Consulta el diagnóstico registrado en una orden (devuelve null si aún no tiene).
   */
  async consultarDiagnosticoPorOrden(idOrden) {
    try {
      const response = await httpClient.get(`/orders/${idOrden}/diagnostic`);
      return unwrapApiData(response);
    } catch (err) {
      if (err.response?.status === 404) {
        return null;
      }
      throw err;
    }
  },
};