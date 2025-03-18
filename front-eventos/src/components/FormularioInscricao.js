import React, { useState, useEffect } from 'react';
import api from '../services/api';
import './FormularioInscricao.css';

function FormularioInscricao({ eventoId, onInscricaoRealizada }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [campos, setCampos] = useState([]);
  const [formData, setFormData] = useState({});
  const [validationErrors, setValidationErrors] = useState({});

  const token = localStorage.getItem("token");

  useEffect(() => {
    const carregarCamposAdicionais = () => {
      setLoading(true);

      api.get(`/api/inscricoes/evento/${eventoId}/campos`, {
        headers: { Authorization: `Bearer ${token}` }
      })
        .then(resp => {
          const camposDoEvento = resp.data;
          setCampos(camposDoEvento);

          const initialFormData = {};
          camposDoEvento.forEach(campo => {
            initialFormData[campo.id] = '';
          });
          setFormData(initialFormData);
        })
        .catch(err => {
          console.error("Erro ao carregar campos do evento:", err);
          setError('Erro ao carregar campos do evento: ' + (err.response?.data || err.message));
        })
        .finally(() => setLoading(false));
    };

    carregarCamposAdicionais();
  }, [eventoId, token]);

  function handleChange(e, campoId) {
    const { value, type, checked } = e.target;

    setFormData(prev => ({
      ...prev,
      [campoId]: type === 'checkbox' ? checked : value
    }));

    if (validationErrors[campoId]) {
      setValidationErrors(prev => ({ ...prev, [campoId]: null }));
    }
  }

  function validateForm() {
    const errors = {};

    campos.forEach(campo => {
      if (campo.obrigatorio && !formData[campo.id]) {
        errors[campo.id] = `O campo ${campo.nome} é obrigatório`;
      }
    });

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function handleSubmit(e) {
    e.preventDefault();

    if (!validateForm()) return;

    setLoading(true);
    setError(null);

    const camposValores = Object.keys(formData).map(campoId => ({
      campoId: parseInt(campoId),
      valor: formData[campoId]
    }));

    const requestData = {
      eventoId: eventoId,
      camposValores: camposValores
    };

    api.post('/api/inscricoes/inscrever-completo', requestData, {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      }
    })
      .then(response => {
        setSuccess(true);
        if (onInscricaoRealizada) {
          onInscricaoRealizada(response.data);
        }
      })
      .catch(err => {
        console.error("Erro ao realizar inscrição:", err);
        setError('Erro ao realizar inscrição: ' + (err.response?.data || err.message));
      })
      .finally(() => setLoading(false));
  }

  function renderizarCampo(campo) {
    switch (campo.tipo) {
      case 'text':
        return (
          <div className={`form-group ${validationErrors[campo.id] ? 'has-error' : ''}`} key={campo.id}>
            <label htmlFor={`campo-${campo.id}`}>
              <span className="field-label">{campo.nome}</span>
              {campo.obrigatorio && <span className="required-indicator">*</span>}
            </label>
            <input
              id={`campo-${campo.id}`}
              type="text"
              value={formData[campo.id] || ''}
              onChange={(e) => handleChange(e, campo.id)}
              className="form-control"
              placeholder={campo.descricao || `Digite ${campo.nome}`}
            />
            {validationErrors[campo.id] && (
              <div className="error-message">{validationErrors[campo.id]}</div>
            )}
          </div>
        );

      case 'select':
        const opcoes = campo.opcoes ? campo.opcoes.split(',').map(opt => opt.trim()) : [];
        return (
          <div className={`form-group ${validationErrors[campo.id] ? 'has-error' : ''}`} key={campo.id}>
            <label htmlFor={`campo-${campo.id}`}>
              <span className="field-label">{campo.nome}</span>
              {campo.obrigatorio && <span className="required-indicator">*</span>}
            </label>
            <select
              id={`campo-${campo.id}`}
              value={formData[campo.id] || ''}
              onChange={(e) => handleChange(e, campo.id)}
              className="form-control"
            >
              <option value="">Selecione uma opção</option>
              {opcoes.map((opcao, index) => (
                <option key={index} value={opcao}>{opcao}</option>
              ))}
            </select>
            {campo.descricao && <div className="field-description">{campo.descricao}</div>}
            {validationErrors[campo.id] && (
              <div className="error-message">{validationErrors[campo.id]}</div>
            )}
          </div>
        );

      case 'checkbox':
        return (
          <div className={`form-group-checkbox ${validationErrors[campo.id] ? 'has-error' : ''}`} key={campo.id}>
            <input
              id={`campo-${campo.id}`}
              type="checkbox"
              checked={formData[campo.id] || false}
              onChange={(e) => handleChange(e, campo.id)}
              className="checkbox-input"
            />
            <label htmlFor={`campo-${campo.id}`}>
              <span className="field-label">{campo.nome}</span>
              {campo.obrigatorio && <span className="required-indicator">*</span>}
            </label>
            {campo.descricao && <div className="field-description">{campo.descricao}</div>}
            {validationErrors[campo.id] && (
              <div className="error-message">{validationErrors[campo.id]}</div>
            )}
          </div>
        );

      default:
        return null;
    }
  }

  return (
    <div className="inscricao-form-container">
      <h3>Formulário de Inscrição</h3>

      {loading && (
        <div className="status-loading">
          <div className="spinner"></div>
          <span>Processando...</span>
        </div>
      )}

      {error && (
        <div className="status-error">
          <i className="icon-alert"></i>
          <span>{error}</span>
          <button className="btn-close" onClick={() => setError(null)}>×</button>
        </div>
      )}

      {success ? (
        <div className="status-success">
          <i className="fa fa-check"></i>
          <span>Inscrição realizada com sucesso!</span>
        </div>
      ) : (
        <form onSubmit={handleSubmit}>
          {campos.length > 0 ? (
            <div className="form-fields">
              {campos.map(campo => renderizarCampo(campo))}
            </div>
          ) : (
            !loading && !error && (
              <p className="no-fields-message">
                Este evento não possui campos adicionais para preenchimento.
              </p>
            )
          )}

          <div className="form-actions">
            <button
              type="submit"
              className="btn-primary"
              disabled={loading || success}
            >
              {loading ? (
                <>
                  <div className="spinner-small"></div>
                  <span>Processando...</span>
                </>
              ) : 'Confirmar Inscrição'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

export default FormularioInscricao;