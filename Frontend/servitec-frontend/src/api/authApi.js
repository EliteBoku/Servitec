import httpClient from './httpClient';

export const authApi = {
  /**
   * CV-01: POST /api/v1/auth/login
   */
  async login({ correo, contrasena }) {
    const response = await httpClient.post('/auth/login', { correo, contrasena });
    return response.data;
  },

  /**
   * CV-01: POST /api/v1/auth/refresh
   */
  async refresh() {
    const response = await httpClient.post('/auth/refresh');
    return response.data;
  },

  /**
   * CV-01: POST /api/v1/auth/logout
   */
  async logout() {
    await httpClient.post('/auth/logout');
  },

  /**
   * CV-10: POST /api/v1/auth/activate
   * Enviamos tanto contrasena como nuevaContrasena/confirmarContrasena para máxima compatibilidad con tu record ActivarCuentaRequest.
   */
  async activarCuenta({ token, nuevaContrasena, confirmarContrasena }) {
    const response = await httpClient.post('/auth/activate', {
      token,
      contrasena: nuevaContrasena,
      nuevaContrasena,
      confirmarContrasena,
    });
    return response.data;
  },

  /**
   * CV-11: POST /api/v1/auth/password/recovery/request
   */
  async solicitarRecuperacion({ correo }) {
    const response = await httpClient.post('/auth/password/recovery/request', { correo });
    return response.data;
  },

  /**
   * CV-11: POST /api/v1/auth/password/recovery/confirm
   */
  async confirmarRecuperacion({ token, nuevaContrasena, confirmarContrasena }) {
    const response = await httpClient.post('/auth/password/recovery/confirm', {
      token,
      contrasena: nuevaContrasena,
      nuevaContrasena,
      confirmarContrasena,
    });
    return response.data;
  },
};