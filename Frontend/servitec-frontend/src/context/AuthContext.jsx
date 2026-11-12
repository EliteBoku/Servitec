import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('servitec_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [sessionExpiredMessage, setSessionExpiredMessage] = useState(null);

  const saveAuthSession = useCallback((authResponse) => {
    localStorage.setItem('servitec_access_token', authResponse.accessToken);
    const userData = {
      idUsuario: authResponse.idUsuario,
      nombreCompleto: authResponse.nombreCompleto,
      correo: authResponse.correo,
      rol: authResponse.rol,
    };
    localStorage.setItem('servitec_user', JSON.stringify(userData));
    setUser(userData);
    setSessionExpiredMessage(null);
    return userData;
  }, []);

  const login = async (correo, contrasena) => {
    const data = await authApi.login({ correo, contrasena });
    return saveAuthSession(data);
  };

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Ignorar si el token o cookie ya expiró en el servidor
    } finally {
      localStorage.removeItem('servitec_access_token');
      localStorage.removeItem('servitec_user');
      setUser(null);
    }
  }, []);

  useEffect(() => {
    const handleSessionExpired = () => {
      setUser(null);
      setSessionExpiredMessage(
        'Tu sesión ha expirado por superar los 60 minutos de inactividad. Por favor inicia sesión nuevamente.'
      );
    };

    window.addEventListener('servitec:session-expired', handleSessionExpired);
    return () => window.removeEventListener('servitec:session-expired', handleSessionExpired);
  }, []);

  const getHomeRouteByRole = (rol) => {
    switch (rol) {
      case 'ADMINISTRADOR':
        return '/admin/ordenes';
      case 'TECNICO_ESPECIALISTA':
        return '/tecnico/ordenes';
      case 'CLIENTE':
        return '/cliente/ordenes';
      default:
        return '/login';
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: Boolean(user),
        login,
        logout,
        sessionExpiredMessage,
        clearExpiredMessage: () => setSessionExpiredMessage(null),
        getHomeRouteByRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe utilizarse dentro de un AuthProvider');
  }
  return context;
}