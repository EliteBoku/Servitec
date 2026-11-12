import httpClient from './httpClient';

export const usersApi = {
  /**
   * CV-10: POST /api/v1/admin/users
   */
  async crearUsuario({ nombreCompleto, correo, rol }) {
    const response = await httpClient.post('/admin/users', {
      nombreCompleto: nombreCompleto.trim(),
      correo: correo.trim(),
      rol,
    });

    const data = response.data;
    return {
      idUsuario: data.idUsuario,
      nombreCompleto: data.nombreCompleto,
      correo: data.correo,
      rol: data.rol,
      estadoCuenta: data.estadoCuenta,
      fechaCreacion: new Date().toISOString(),
      mensaje: data.mensaje,
    };
  },

  /**
   * CV-10 y CV-03: GET /api/v1/admin/users
   */
  async listarUsuarios(rolFiltro = null) {
    const params = rolFiltro ? { rol: rolFiltro } : {};
    const response = await httpClient.get('/admin/users', { params });
    const lista = Array.isArray(response.data) ? response.data : [];

    return lista.map((u) => ({
      idUsuario: u.idUsuario,
      nombreCompleto: u.nombreCompleto,
      correo: u.correo,
      rol: u.rol,
      estadoCuenta: u.estadoCuenta,
      fechaCreacion: u.fechaCreacion || u.mensaje || null,
    }));
  },
};