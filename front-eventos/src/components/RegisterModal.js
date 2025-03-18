import React, { useState, useEffect } from "react";
import "./RegisterModal.css";

const RegisterModal = ({ isOpen, onClose, onRegisterSuccess, onOpenLogin }) => {
  const [nomeCompleto, setNomeCompleto] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setNomeCompleto("");
      setUsername("");
      setPassword("");
      setError("");
      setSuccess(false);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess(false);

    if (!username.includes("@")) {
      setError("Digite um e-mail válido.");
      return;
    }

    if (password.length < 6) {
      setError("A senha deve ter pelo menos 6 caracteres.");
      return;
    }

    try {
      const response = await fetch("http://localhost:8080/api/auth/registrar", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nomeCompleto, username, password }),
      });

      if (!response.ok) {
        throw new Error("Erro ao cadastrar. Tente novamente.");
      }

      setSuccess(true);

      const loginResponse = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      if (!loginResponse.ok) {
        throw new Error("Cadastro feito, mas erro ao logar.");
      }

      const data = await loginResponse.json();
      localStorage.setItem("token", data.token);

      setTimeout(() => {
        onRegisterSuccess(data.token);
        onClose();
      }, 500);

    } catch (err) {
      setError(err.message || "Erro ao criar conta. Verifique os dados.");
    }
  };

  const handleOpenLogin = () => {
    onClose();
    onOpenLogin();
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content wider-modal">
        <button className="close-btn" onClick={onClose}>×</button>
        <h2>Cadastro</h2>
        <form onSubmit={handleRegister}>
          <label>Nome Completo:</label>
          <input
            type="text"
            value={nomeCompleto}
            onChange={(e) => setNomeCompleto(e.target.value)}
            placeholder="Digite seu nome completo"
            required
          />
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
            placeholder="Crie uma senha segura"
            required
          />

          {error && <p className="error-message">{error}</p>}
          {success && <p className="success-message">Cadastro realizado com sucesso!</p>}
          <button type="submit" className="register-btn">
            Cadastrar
          </button>
        </form>

        <p className="login-text">
          Já tem conta? <span className="login-link" onClick={handleOpenLogin}>Faça login</span>
        </p>

      </div>
    </div>
  );
};

export default RegisterModal;