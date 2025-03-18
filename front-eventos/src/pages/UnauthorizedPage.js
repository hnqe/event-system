import React from 'react';
import { Link } from 'react-router-dom';
import './UnauthorizedPage.css';

function UnauthorizedPage() {
  return (
    <div className="unauthorized-container">
      <div className="unauthorized-content">
        <h1>Acesso Não Autorizado</h1>
        <p>Você não possui permissão para acessar esta área.</p>
        <p>Esta seção é restrita a administradores do sistema.</p>
        <Link to="/" className="back-link">Voltar para a página inicial</Link>
      </div>
    </div>
  );
}

export default UnauthorizedPage;