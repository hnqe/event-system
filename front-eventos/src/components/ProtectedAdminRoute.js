import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const ProtectedAdminRoute = ({ redirectPath = '/unauthorized' }) => {
  const { isAdmin, loggingOut } = useAuth();

  if (loggingOut) {
    return <Outlet />;
  }

  if (!isAdmin()) {
    return <Navigate to={redirectPath} replace />;
  }

  return <Outlet />;
};

export default ProtectedAdminRoute;