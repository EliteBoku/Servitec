import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ProtectedRoute from './ProtectedRoute';

// Layouts por Rol
import AdminLayout from '../layouts/AdminLayout';
import TechLayout from '../layouts/TechLayout';
import ClientLayout from '../layouts/ClientLayout';

// Páginas de Autenticación (CV-01, CV-10, CV-11)
import LoginPage from '../modules/auth/pages/LoginPage';
import ActivateAccountPage from '../modules/auth/pages/ActivateAccountPage';
import ForgotPasswordPage from '../modules/auth/pages/ForgotPasswordPage';
import ResetPasswordPage from '../modules/auth/pages/ResetPasswordPage';

import AdminUsersPage from '../modules/users/pages/AdminUsersPage';

import AdminCatalogPage from '../modules/inventory/pages/AdminCatalogPage';
import AdminMovementsPage from '../modules/inventory/pages/AdminMovementsPage';
import TechCatalogViewPage from '../modules/inventory/pages/TechCatalogViewPage';

import ClientOrdersPage from '../modules/orders/pages/ClientOrdersPage';
import ClientNewOrderPage from '../modules/orders/pages/ClientNewOrderPage';
import AdminOrdersBoardPage from '../modules/orders/pages/AdminOrdersBoardPage';

import TechAssignedOrdersPage from '../modules/orders/pages/TechAssignedOrdersPage';
import TechOrderWorkPage from '../modules/orders/pages/TechOrderWorkPage';

import ClientOrderDetailPage from '../modules/orders/pages/ClientOrderDetailPage';

import AdminOrderDetailPage from '../modules/orders/pages/AdminOrderDetailPage';

function ModulePlaceholder({ title, step }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-xs">
      <span className="inline-block px-3 py-1 rounded-full bg-sky-100 text-sky-800 text-xs font-semibold mb-3">
        {step}
      </span>
      <h2 className="text-xl font-bold text-servitec-navy">{title}</h2>
      <p className="text-sm text-slate-600 mt-1">
        Estructura y sesión activa verificada. Esta vista se conectará en su micro-paso correspondiente.
      </p>
    </div>
  );
}

export default function AppRouter() {
  const { isAuthenticated, user, getHomeRouteByRole } = useAuth();

  return (
    <Routes>
      {/* Rutas Públicas de Autenticación */}
      <Route
        path="/login"
        element={
          isAuthenticated && user ? (
            <Navigate to={getHomeRouteByRole(user.rol)} replace />
          ) : (
            <LoginPage />
          )
        }
      />
      <Route path="/activar-cuenta" element={<ActivateAccountPage />} />
      <Route path="/recuperar-contrasena" element={<ForgotPasswordPage />} />
      <Route path="/restablecer-contrasena" element={<ResetPasswordPage />} />

      {/* Rutas Protegidas: ADMINISTRADOR */}
      <Route element={<ProtectedRoute allowedRoles={['ADMINISTRADOR']} />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="ordenes" replace />} />
          <Route path="ordenes" element={<AdminOrdersBoardPage />} />
          <Route path="ordenes/:idOrden" element={<AdminOrderDetailPage />} />
          <Route path="usuarios" element={<AdminUsersPage />} />
          <Route path="catalogo" element={<AdminCatalogPage />} />
          <Route path="movimientos" element={<AdminMovementsPage />} />
        </Route>
      </Route>

      {/* Rutas Protegidas: TECNICO_ESPECIALISTA */}
      <Route element={<ProtectedRoute allowedRoles={['TECNICO_ESPECIALISTA']} />}>
        <Route path="/tecnico" element={<TechLayout />}>
          <Route index element={<Navigate to="ordenes" replace />} />
          <Route path="ordenes" element={<TechAssignedOrdersPage />} />
          <Route path="ordenes/:idOrden" element={<TechOrderWorkPage />} />
          <Route path="catalogo" element={<TechCatalogViewPage />} />
        </Route>
      </Route>

      {/* Rutas Protegidas: CLIENTE */}
      <Route element={<ProtectedRoute allowedRoles={['CLIENTE']} />}>
        <Route path="/cliente" element={<ClientLayout />}>
          <Route index element={<Navigate to="ordenes" replace />} />
          <Route path="ordenes" element={<ClientOrdersPage />} />
          <Route path="nueva-solicitud" element={<ClientNewOrderPage />} />
          <Route path="ordenes/:idOrden" element={<ClientOrderDetailPage />} />
        </Route>
      </Route>

      {/* Redirección por defecto */}
      <Route
        path="*"
        element={
          <Navigate
            to={isAuthenticated && user ? getHomeRouteByRole(user.rol) : '/login'}
            replace
          />
        }
      />
    </Routes>
  );
}