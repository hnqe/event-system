import React, { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import ConfirmationModal from '../components/ConfirmationModal';

import './AdminEventoInscritos.css';

function AdminEventoInscritos() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [evento, setEvento] = useState(null);
  const [inscritos, setInscritos] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filtroStatus, setFiltroStatus] = useState('todos');
  const [mensagemSucesso, setMensagemSucesso] = useState(null);
  const [showCancelarModal, setShowCancelarModal] = useState(false);
  const [inscricaoSelecionada, setInscricaoSelecionada] = useState(null);
  const token = localStorage.getItem("token");

  useEffect(() => {
    carregarEvento();
    carregarInscritos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    if (mensagemSucesso) {
      const timer = setTimeout(() => {
        setMensagemSucesso(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [mensagemSucesso]);

  function carregarEvento() {
    setCarregando(true);
    setErro(null);

    api.get(`/api/eventos/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        setEvento(resp.data);
      })
      .catch(err => {
        console.error("Erro ao carregar evento:", err);
        setErro('Erro ao carregar evento: ' + (err.response?.data || err.message));
      })
      .finally(() => setCarregando(false));
  }

  function carregarInscritos() {
    setCarregando(true);
    setErro(null);

    api.get(`/api/eventos/${id}/inscritos`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        if (resp.status === 204) {
          setInscritos([]);
        } else {
          console.log("Dados completos dos inscritos:", JSON.stringify(resp.data, null, 2));
          setInscritos(resp.data);
        }
      })
      .catch(err => {
        console.error("Erro ao carregar inscritos:", err);
        setErro('Erro ao carregar inscritos: ' + (err.response?.data || err.message));
      })
      .finally(() => setCarregando(false));
  }

  function handleCancelarInscricao(inscricaoId) {
    setInscricaoSelecionada(inscricaoId);
    setShowCancelarModal(true);
  }

  function confirmarCancelamento() {
    if (!inscricaoSelecionada) return;

    api.delete(`/api/inscricoes/${inscricaoSelecionada}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(() => {
        setInscritos(prev => prev.map(i => {
          if (i.id === inscricaoSelecionada) {
            return {
              ...i,
              status: 'CANCELADA'
            };
          }
          return i;
        }));
        setShowCancelarModal(false);
        setMensagemSucesso('Inscrição cancelada com sucesso!');
      })
      .catch(err => {
        console.error("Erro ao cancelar inscrição:", err);
        setErro('Erro ao cancelar inscrição: ' + (err.response?.data || err.message));
        setShowCancelarModal(false);
      });
  }

  function handleSearchChange(e) {
    setSearchTerm(e.target.value);
  }

  function handleFiltroChange(e) {
    setFiltroStatus(e.target.value);
  }

  function handleSearch(e) {
    e.preventDefault();
  }

  function formatarData(dataStr) {
    if (!dataStr) return 'N/D';
    const data = new Date(dataStr);
    return data.toLocaleString('pt-BR');
  }

  function getNomeUsuario(inscrito) {
    if (!inscrito) return 'N/D';

    return inscrito.nomeUsuario ||
      (inscrito.user && inscrito.user.nomeCompleto) ||
      'N/D';
  }

  function getEmailUsuario(inscrito) {
    if (!inscrito) return 'N/D';

    return (inscrito.user && inscrito.user.username) ||
      inscrito.username ||
      'N/D';
  }

  function filtrarInscritos() {
    let filtrados = inscritos;

    if (filtroStatus !== 'todos') {
      filtrados = filtrados.filter(insc => insc.status === filtroStatus.toUpperCase());
    }

    if (searchTerm.trim()) {
      const termoBusca = searchTerm.toLowerCase();
      filtrados = filtrados.filter(insc =>
        (getNomeUsuario(insc) || '').toLowerCase().includes(termoBusca) ||
        (getEmailUsuario(insc) || '').toLowerCase().includes(termoBusca) ||
        ((insc.status || '').toLowerCase().includes(termoBusca))
      );
    }

    return filtrados;
  }

  const inscritosFiltrados = filtrarInscritos();

  const todosCamposAdicionais = useMemo(() => {
    if (!inscritos || inscritos.length === 0) return [];

    const camposMap = new Map();

    inscritos.forEach(inscrito => {
      if (inscrito.camposValores && Array.isArray(inscrito.camposValores)) {
        inscrito.camposValores.forEach(campo => {
          console.log("Campo valor:", campo);

          const campoId = campo.campoId ||
            (campo.campo && campo.campo.id) ||
            campo.id;

          const nomeCampo = campo.nomeCampo ||
            (campo.campo && campo.campo.nome) ||
            campo.nome;

          if (campoId && nomeCampo) {
            camposMap.set(campoId, {
              campoId: campoId,
              nomeCampo: nomeCampo
            });
          }
        });
      }
    });

    const resultado = Array.from(camposMap.values());
    console.log("Campos adicionais encontrados:", resultado);
    return resultado;
  }, [inscritos]);

  function getValorCampo(inscrito, campoId) {
    if (!inscrito || !inscrito.camposValores || !Array.isArray(inscrito.camposValores))
      return '-';

    const campoValor = inscrito.camposValores.find(cv => {
      const cvId = cv.campoId || (cv.campo && cv.campo.id) || cv.id;
      return cvId === campoId;
    });

    return campoValor ? (campoValor.valor || '-') : '-';
  }

  function exportarParaCSV() {
    if (inscritosFiltrados.length === 0) {
      setErro('Não há dados para exportar.');
      return;
    }

    let cabecalho = ['Nome', 'Email', 'Data de Inscrição', 'Status'];

    todosCamposAdicionais.forEach(campo => {
      cabecalho.push(campo.nomeCampo);
    });

    const linhas = inscritosFiltrados.map(insc => {
      let linha = [
        getNomeUsuario(insc),
        getEmailUsuario(insc),
        formatarData(insc.dataInscricao),
        insc.status || ''
      ];

      todosCamposAdicionais.forEach(campo => {
        linha.push(getValorCampo(insc, campo.campoId));
      });

      return linha;
    });

    const csv = [
      cabecalho.join(','),
      ...linhas.map(l => l.join(','))
    ].join('\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `inscritos_evento_${id}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  return (
    <div className="admin-inscritos-container">
      <div className="page-header">
        <h2>Gerenciamento de Inscritos</h2>
        <div className="page-actions">
          <button
            onClick={() => navigate('/admin/eventos')}
            className="btn-secondary mr-2"
            title="Voltar para lista de eventos"
          >
            <i className="fa-solid fa-arrow-left"></i> Voltar
          </button>

          <button
            onClick={carregarInscritos}
            className="btn-secondary mr-2"
            title="Atualizar lista"
          >
            <i className="icon-refresh"></i> Atualizar
          </button>

          <button
            onClick={exportarParaCSV}
            className="btn-primary"
            title="Exportar lista para CSV"
            disabled={inscritosFiltrados.length === 0}
          >
            <i className="icon-download"></i> Exportar CSV
          </button>
        </div>
      </div>

      {carregando && (
        <div className="status-loading">
          <div className="spinner"></div>
          <span>Carregando dados...</span>
        </div>
      )}

      {erro && (
        <div className="status-error">
          <i className="icon-alert"></i>
          <span>{erro}</span>
        </div>
      )}

      {mensagemSucesso && (
        <div className="status-success">
          <i className="icon-check-circle"></i>
          <span>{mensagemSucesso}</span>
        </div>
      )}

      {evento && (
        <div className="evento-info-card">
          <h3>{evento.titulo}</h3>
          <div className="evento-details">
            <p><strong>Local:</strong> {evento.local || 'N/D'}</p>
            <p><strong>Período:</strong> {formatarData(evento.dataInicio)} - {formatarData(evento.dataFim)}</p>
            <p><strong>Inscrições até:</strong> {formatarData(evento.dataLimiteInscricao)}</p>
            <p><strong>Vagas:</strong> {evento.vagas || 'Ilimitadas'}</p>
            <p><strong>Inscritos:</strong> {inscritos.length}</p>
          </div>
        </div>
      )}

      <div className="inscritos-container">
        <div className="inscritos-header">
          <h3>Lista de Inscritos</h3>
          <div className="filters-container">
            <div className="filtro-container">
              <label>Status:</label>
              <select value={filtroStatus} onChange={handleFiltroChange} className="filtro-select">
                <option value="todos">Todos</option>
                <option value="ativa">Ativos</option>
                <option value="cancelada">Cancelados</option>
              </select>
            </div>
            <form onSubmit={handleSearch} className="search-container">
              <input
                type="text"
                placeholder="Buscar inscritos..."
                className="search-input"
                value={searchTerm}
                onChange={handleSearchChange}
              />
              <button type="submit" className="search-button">
                <i className="icon-search"></i>
              </button>
            </form>
          </div>
        </div>

        {inscritosFiltrados.length > 0 ? (
          <div className="inscritos-table-container">
            <table className="inscritos-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Email</th>
                  <th>Data de Inscrição</th>
                  <th>Status</th>
                  {todosCamposAdicionais.map(campo => (
                    <th key={campo.campoId}>{campo.nomeCampo}</th>
                  ))}
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {inscritosFiltrados.map(insc => (
                  <tr key={insc.id} className={insc.status === 'CANCELADA' ? 'inscricao-cancelada' : ''}>
                    <td>{getNomeUsuario(insc)}</td>
                    <td>{getEmailUsuario(insc)}</td>
                    <td>{formatarData(insc.dataInscricao)}</td>
                    <td>
                      <span className={`status-badge ${(insc.status || '').toLowerCase()}`}>
                        {insc.status || 'N/D'}
                      </span>
                    </td>
                    {todosCamposAdicionais.map(campo => (
                      <td key={campo.campoId}>{getValorCampo(insc, campo.campoId)}</td>
                    ))}
                    <td className="acoes-cell">
                      {insc.status !== 'CANCELADA' && (
                        <button
                          onClick={() => handleCancelarInscricao(insc.id)}
                          className="btn-delete-sm"
                          title="Cancelar inscrição"
                        >
                          <i className="icon-cancel"></i>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          !carregando && !erro && (
            <div className="empty-state">
              <div className="empty-state-icon">
                <i className="icon-users-empty"></i>
              </div>
              <p>Nenhum inscrito encontrado para este evento.</p>
            </div>
          )
        )}
      </div>

      <ConfirmationModal
        isOpen={showCancelarModal}
        title="Cancelar Inscrição"
        message="Tem certeza que deseja cancelar esta inscrição? Esta ação irá definir o status como CANCELADA."
        onConfirm={confirmarCancelamento}
        onCancel={() => setShowCancelarModal(false)}
      />
    </div>
  );
}

export default AdminEventoInscritos;