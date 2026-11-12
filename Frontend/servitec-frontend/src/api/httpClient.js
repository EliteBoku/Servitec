import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

const httpClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // Permite enviar y recibir la cookie HttpOnly SERVITEC_REFRESH
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

let isRefreshing = false;
let pendingQueue = [];

const processQueue = (error, token = null) => {
  pendingQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  pendingQueue = [];
};

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const requestUrl = originalRequest?.url || '';

    const isAuthEndpoint =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/refresh') ||
      requestUrl.includes('/auth/activate') ||
      requestUrl.includes('/auth/password/recovery');

    if (status === 401 && !isAuthEndpoint && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return httpClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Intenta rotar el token usando la cookie SERVITEC_REFRESH
        const refreshResponse = await axios.post(
          `${BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const authData = refreshResponse.data;
        localStorage.setItem('servitec_access_token', authData.accessToken);
        localStorage.setItem(
          'servitec_user',
          JSON.stringify({
            idUsuario: authData.idUsuario,
            nombreCompleto: authData.nombreCompleto,
            correo: authData.correo,
            rol: authData.rol,
          })
        );

        processQueue(null, authData.accessToken);
        originalRequest.headers.Authorization = `Bearer ${authData.accessToken}`;
        return httpClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        localStorage.removeItem('servitec_access_token');
        localStorage.removeItem('servitec_user');
        window.dispatchEvent(new CustomEvent('servitec:session-expired'));
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

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
    message: data.mensaje || data.message || defaultMessage,
    fieldErrors: data.erroresCampos || data.fieldErrors || null,
    missingStockItems: data.elementosSinDisponibilidad || null,
    code: data.codigoError || null,
  };
}

export default httpClient;