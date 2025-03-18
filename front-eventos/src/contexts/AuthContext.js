import React, { createContext, useState, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [isLogged, setIsLogged] = useState(false);
  const [userRoles, setUserRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      setIsLogged(true);
      fetchUserRoles(token);
    } else {
      setLoading(false);
    }
  }, []);

  const fetchUserRoles = async (token) => {
    try {
      setLoading(true);
      const response = await api.get('/api/auth/current-user', {
        headers: { Authorization: `Bearer ${token}` }
      });

      const roles = response.data.roles?.map(role => role.name) || [];
      setUserRoles(roles);
    } catch (error) {
      console.error("Erro ao carregar informações do usuário:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async (token) => {
    localStorage.setItem('token', token);
    setIsLogged(true);
    await fetchUserRoles(token);
    window.showNotification && window.showNotification('success', 'Login realizado com sucesso!');
    return true;
  };

  const handleLogout = () => {
    setLoggingOut(true);

    navigate('/', { replace: true });

    setTimeout(() => {
      localStorage.removeItem('token');
      setIsLogged(false);
      setUserRoles([]);
      window.showNotification && window.showNotification('info', 'Você saiu do sistema.');

      setTimeout(() => {
        setLoggingOut(false);
      }, 100);
    }, 10);
  };

  const value = {
    isLogged,
    userRoles,
    loading,
    loggingOut,
    login: handleLogin,
    logout: handleLogout,
    isAdmin: () => userRoles.some(role => ['ADMIN_GERAL', 'ADMIN_CAMPUS', 'ADMIN_DEPARTAMENTO'].includes(role))
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return context;
};

export default AuthContext;