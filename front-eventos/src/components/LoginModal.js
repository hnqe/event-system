import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./LoginModal.css";

const LoginModal = ({ isOpen, onClose, onLoginSuccess, onOpenRegister }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    if (isOpen) {
      setUsername("");
      setPassword("");
      setError("");
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        throw new Error("Credenciais inválidas");
      }

      const data = await response.json();
      localStorage.setItem("token", data.token);
      onLoginSuccess(data.token);

      const redirectEventoId = localStorage.getItem("redirectEventoId");
      if (redirectEventoId) {
        localStorage.removeItem("redirectEventoId");
        onClose();

        setTimeout(() => {
          navigate("/");

          localStorage.setItem("openInscricaoForEvento", redirectEventoId);
        }, 100);
      } else {
        onClose();
      }
    } catch (err) {
      setError("Usuário ou senha incorretos.");
    }
  };

  const handleOpenRegister = () => {
    onClose();
    onOpenRegister();
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <button className="close-btn" onClick={onClose}>×</button>
        <h2>Login</h2>
        <form onSubmit={handleLogin}>
          <label>E-mail:</label>
          <input
            type="email"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Digite seu e-mail"
            required
          />
          <label>Senha:</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Digite sua senha"
            required
          />
          {error && <p className="error-message">{error}</p>}
          <button type="submit" className="login-btn">Entrar</button>
        </form>

        <p className="register-text">
          Ainda não tem conta? <span className="register-link" onClick={handleOpenRegister}>Cadastrar-se</span>
        </p>
      </div>
    </div>
  );
};

export default LoginModal;