import React, { useState } from 'react';

function CamposAdicionais({ campos, setCampos }) {
  const [novoCampo, setNovoCampo] = useState({
    nome: '',
    tipo: 'text',
    descricao: '',
    obrigatorio: false,
    opcoes: ''
  });

  const [mostrarFormNovoCampo, setMostrarFormNovoCampo] = useState(false);

  const adicionarCampo = () => {
    if (!novoCampo.nome.trim()) {
      alert('O nome do campo é obrigatório');
      return;
    }

    setCampos([...campos, { ...novoCampo, id: Date.now() }]);
    setNovoCampo({
      nome: '',
      tipo: 'text',
      descricao: '',
      obrigatorio: false,
      opcoes: ''
    });
    setMostrarFormNovoCampo(false);
  };

  const removerCampo = (id) => {
    setCampos(campos.filter(campo => campo.id !== id));
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setNovoCampo({
      ...novoCampo,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  return (
    <div className="form-section">
      <h3>Campos Adicionais para Inscrição</h3>
      <p className="section-description">
        Defina campos personalizados que os participantes deverão preencher ao se inscreverem no evento.
      </p>

      {campos.length > 0 ? (
        <div className="campos-adicionais-list">
          {campos.map(campo => (
            <div key={campo.id} className="campo-adicional-item">
              <div className="campo-header">
                <strong>{campo.nome}</strong>
                {campo.obrigatorio && <span className="badge-required">Obrigatório</span>}
                <button
                  type="button"
                  className="btn-icon-only"
                  onClick={() => removerCampo(campo.id)}
                  title="Remover campo"
                >
                  <i className="icon-trash"></i>
                </button>
              </div>
              <div className="campo-info">
                <div>Tipo: {campo.tipo === 'text' ? 'Texto' : campo.tipo === 'select' ? 'Seleção' : 'Checkbox'}</div>
                {campo.descricao && <div>Descrição: {campo.descricao}</div>}
                {campo.tipo === 'select' && campo.opcoes &&
                  <div>Opções: {campo.opcoes}</div>
                }
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="empty-state-small">
          <p>Nenhum campo adicional definido.</p>
        </div>
      )}

      {mostrarFormNovoCampo ? (
        <div className="novo-campo-form">
          <h4>Adicionar Novo Campo</h4>

          <div className="form-group">
            <label htmlFor="nome">Nome do Campo</label>
            <input
              id="nome"
              name="nome"
              value={novoCampo.nome}
              onChange={handleChange}
              className="form-control"
              placeholder="Ex: Empresa, Cargo, Experiência..."
            />
          </div>

          <div className="form-group">
            <label htmlFor="tipo">Tipo do Campo</label>
            <select
              id="tipo"
              name="tipo"
              value={novoCampo.tipo}
              onChange={handleChange}
              className="form-control"
            >
              <option value="text">Texto</option>
              <option value="select">Seleção</option>
              <option value="checkbox">Checkbox</option>
            </select>
          </div>

          {novoCampo.tipo === 'select' && (
            <div className="form-group">
              <label htmlFor="opcoes">Opções (separadas por vírgula)</label>
              <input
                id="opcoes"
                name="opcoes"
                value={novoCampo.opcoes}
                onChange={handleChange}
                className="form-control"
                placeholder="Ex: Opção 1, Opção 2, Opção 3"
              />
            </div>
          )}

          <div className="form-group">
            <label htmlFor="descricao">Descrição (opcional)</label>
            <input
              id="descricao"
              name="descricao"
              value={novoCampo.descricao}
              onChange={handleChange}
              className="form-control"
              placeholder="Explicação sobre o campo para o participante"
            />
          </div>

          <div className="form-group-checkbox">
            <input
              id="obrigatorio"
              type="checkbox"
              name="obrigatorio"
              checked={novoCampo.obrigatorio}
              onChange={handleChange}
              className="checkbox-input"
            />
            <label htmlFor="obrigatorio">Campo obrigatório</label>
          </div>

          <div className="form-actions inline">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setMostrarFormNovoCampo(false)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn-primary"
              onClick={adicionarCampo}
            >
              Adicionar Campo
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          className="btn-outline"
          onClick={() => setMostrarFormNovoCampo(true)}
        >
          <i className="icon-plus"></i> Adicionar Campo
        </button>
      )}
    </div>
  );
}

export default CamposAdicionais;