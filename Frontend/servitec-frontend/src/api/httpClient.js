import axios from 'axios';

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('servitec_access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || '';

    // Si recibe 401 fuera del login, notificar expiración por inactividad (CV-01)
    if (status === 401 && !requestUrl.includes('/auth/login')) {
      localStorage.removeItem('servitec_access_token');
      localStorage.removeItem('servitec_refresh_token');
      localStorage.removeItem('servitec_user');
      window.dispatchEvent(new CustomEvent('servitec:session-expired'));
    }

    return Promise.reject(error);
  }
);

/**
 * Utilidad para extraer de forma uniforme el mensaje, errores de campos y faltantes de stock
 * devueltos por GlobalExceptionHandler en el backend.
 */
export function extractApiError(error, defaultMessage = 'Ocurrió un error al procesar la solicitud.') {
  const data = error.response?.data;
  if (!data) {
    return {
      message: defaultMessage,
      fieldErrors: null,
      missingStockItems: null,
    };
  }
  return {
    message: data.mensaje || defaultMessage,
    fieldErrors: data.erroresCampos || null,
    missingStockItems: data.elementosSinDisponibilidad || null,
    code: data.codigoError || null,
  };
}

export default httpClient;