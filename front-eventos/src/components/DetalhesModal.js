import React, { useEffect } from 'react';
import './DetalhesModal.css';

const DetalhesModal = ({
  evento,
  isOpen,
  onClose,
  onInscrever,
  onCancelar,
  jaInscrito,
  inscricaoAtiva,
  isAuthenticated,
  formatarData
}) => {
  useEffect(() => {
    if (isOpen) {
      document.body.classList.add('modal-open');
    } else {
      document.body.classList.remove('modal-open');
    }

    return () => {
      document.body.classList.remove('modal-open');
    };
  }, [isOpen]);

  if (!isOpen || !evento) return null;

  const formatarDescricao = (texto) => {
    if (!texto) return [];
    return texto.split('\n').filter(p => p.trim() !== '');
  };

  return (
    <div className="evento-modal-overlay" onClick={onClose}>
      <div className="evento-modal-content" onClick={e => e.stopPropagation()}>
        <div className="evento-modal-header">
          <h2>{evento.titulo}</h2>
          <button className="evento-btn-close-modal" onClick={onClose}>×</button>
        </div>

        <div className="evento-modal-body">
          <div className="evento-info-grid">
            <div className="evento-info-item">
              <i className="fa fa-map-marker"></i>
              <div>
                <strong>Local:</strong>
                <p>{evento.local || 'Não informado'}</p>
              </div>
            </div>

            <div className="evento-info-item">
              <i className="fa fa-calendar"></i>
              <div>
                <strong>Período:</strong>
                <p>{formatarData(evento.dataInicio)} - {formatarData(evento.dataFim)}</p>
              </div>
            </div>

            <div className="evento-info-item">
              <i className="fa fa-building"></i>
              <div>
                <strong>Campus:</strong>
                <p>{evento.campus?.nome || 'Não informado'}</p>
              </div>
            </div>

            <div className="evento-info-item">
              <i className="fa fa-university"></i>
              <div>
                <strong>Departamento:</strong>
                <p>{evento.departamento?.nome || 'Não informado'}</p>
              </div>
            </div>

            {evento.dataLimiteInscricao && (
              <div className="evento-info-item">
                <i className="fa fa-clock-o"></i>
                <div>
                  <strong>Inscrições até:</strong>
                  <p>{formatarData(evento.dataLimiteInscricao)}</p>
                </div>
              </div>
            )}

            {evento.vagas && (
              <div className="evento-info-item">
                <i className="fa fa-users"></i>
                <div>
                  <strong>Vagas:</strong>
                  <p>{evento.vagas}</p>
                </div>
              </div>
            )}

            {evento.estudanteIfg && (
              <div className="evento-info-item">
                <i className="fa fa-star"></i>
                <div>
                  <strong>Público:</strong>
                  <p>Exclusivo para estudantes IFG</p>
                </div>
              </div>
            )}
          </div>

          {evento.descricao && (
            <div className="evento-descricao-completa">
              <h3>Descrição</h3>
              <div className="evento-descricao-text">
                {formatarDescricao(evento.descricao).map((paragraph, idx) => (
                  <p key={idx}>{paragraph}</p>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="evento-modal-footer">
          {jaInscrito ? (
            <div className="evento-modal-inscricao-actions">
              <div className="evento-inscrito-badge">
                <i className="fa fa-check-circle"></i> Você já está inscrito
              </div>
              {inscricaoAtiva && (
                <button
                  onClick={() => {
                    onCancelar(inscricaoAtiva.id);
                    onClose();
                  }}
                  className="evento-btn-cancelar-inscricao"
                >
                  <i className="fa fa-times-circle"></i> Cancelar Inscrição
                </button>
              )}
            </div>
          ) : (
            <button
              onClick={() => {
                onInscrever(evento);
                onClose();
              }}
              className="evento-btn-inscrever"
            >
              {isAuthenticated ? 'Inscrever-se' : 'Login para Inscrever-se'}
            </button>
          )}

          <button className="evento-btn-fechar" onClick={onClose}>Fechar</button>
        </div>
      </div>
    </div>
  );
};

export default DetalhesModal;