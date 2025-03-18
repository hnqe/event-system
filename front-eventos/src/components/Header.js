import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import LoginModal from "./LoginModal";
import RegisterModal from "./RegisterModal";
import api from "../services/api";
import "./Header.css";

const Header = ({ isLogged, onLogout, onOpenLogin, onLoginSuccess }) => {
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [userName, setUserName] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchUserData = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem("token");

      const payload = JSON.parse(atob(token.split(".")[1]));
      const roles = payload.roles || [];

      setIsAdmin(roles.some(role =>
        role.startsWith("ADMIN") ||
        role.startsWith("ADMINISTRADOR")
      ));

      const response = await api.get('/api/auth/current-user', {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (response.data && response.data.nomeCompleto) {
        setUserName(response.data.nomeCompleto);
      } else {
        setUserName(payload.sub || 'Usuário');
      }
    } catch (error) {
      console.error("Erro ao buscar dados do usuário:", error);
      try {
        const token = localStorage.getItem("token");
        const payload = JSON.parse(atob(token.split(".")[1]));
        setUserName(payload.sub || 'Usuário');
      } catch (e) {
        setUserName('Usuário');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isLogged) {
      fetchUserData();
    } else {
      setIsAdmin(false);
      setUserName('');
    }
  }, [isLogged]);

  const handleLogout = () => {
    if (onLogout) {
      onLogout();
    }
  };

  const openLogin = () => {
    if (onOpenLogin) {
      onOpenLogin();
    } else {
      setIsLoginOpen(true);
    }
  };

  const handleLoginSuccess = (userToken) => {
    setIsLoginOpen(false);

    if (onLoginSuccess) {
      onLoginSuccess(userToken);
    }
  };

  const handleRegisterSuccess = (userToken) => {
    setIsRegisterOpen(false);

    if (onLoginSuccess) {
      onLoginSuccess(userToken);
    }
  };

  return (
    <header className="navbar">
      <div className="logo-container">
        <Link to="/" className="logo">
          <img src="/IFG-logo.png" alt="IFG Logo" />
        </Link>
        <h1 className="system-title">Sistema Unificado de Gestão de Extensão e Pesquisa</h1>
      </div>

      <nav className="nav-links">
        {isAdmin && <Link to="/admin">Administração</Link>}

        {!isLogged ? (
          <>
            <button className="nav-button" onClick={() => setIsRegisterOpen(true)}>Cadastrar</button>
            <button className="nav-button" onClick={openLogin}>Entrar</button>
          </>
        ) : (
          <>
            <div className="user-info">
              <span className="user-name">{loading ? '...' : userName}</span>
            </div>
            <button className="nav-button logout-btn" onClick={handleLogout}>Sair</button>
          </>
        )}
      </nav>

      <LoginModal
        isOpen={isLoginOpen}
        onClose={() => setIsLoginOpen(false)}
        onLoginSuccess={handleLoginSuccess}
        onOpenRegister={() => {
          setIsLoginOpen(false);
          setIsRegisterOpen(true);
        }}
      />

      <RegisterModal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        onRegisterSuccess={handleRegisterSuccess}
        onOpenLogin={() => {
          setIsRegisterOpen(false);
          setIsLoginOpen(true);
        }}
      />
    </header>
  );
};

export default Header;