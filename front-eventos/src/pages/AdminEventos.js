import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import ConfirmationModal from '../components/ConfirmationModal';

import './AdminEventos.css';
import './AdminEventosIcons.css';

function AdminEventos() {
  const navigate = useNavigate();
  const [eventos, setEventos] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [filtroStatus, setFiltroStatus] = useState('todos');
  const [searchTerm, setSearchTerm] = useState('');
  const token = localStorage.getItem("token");

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showEncerrarModal, setShowEncerrarModal] = useState(false);
  const [eventoSelecionado, setEventoSelecionado] = useState(null);
  const [mensagemSucesso, setMensagemSucesso] = useState(null);

  const EventoStatus = {
    ATIVO: 'ATIVO',
    ENCERRADO: 'ENCERRADO'
  };

  useEffect(() => {
    carregarUserInfo();
    carregarEventos();
  }, []);

  useEffect(() => {
    if (mensagemSucesso) {
      const timer = setTimeout(() => {
        setMensagemSucesso(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [mensagemSucesso]);

  function carregarUserInfo() {
    api.get('/api/auth/current-user', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        const userData = resp.data;
        const roles = userData.roles?.map(role => role.name) || [];

        if (roles.includes('ADMIN_GERAL')) {
          carregarTodosCampus();
        } else if (roles.includes('ADMIN_CAMPUS')) {
          if (userData.campusQueAdministro && userData.campusQueAdministro.length > 0) {
          }
        } else if (roles.includes('ADMIN_DEPARTAMENTO')) {
          if (userData.departamentosQueAdministro && userData.departamentosQueAdministro.length > 0) {
            const campusMap = new Map();

            userData.departamentosQueAdministro.forEach(depto => {
              if (depto && depto.campus) {
                campusMap.set(depto.campus.id, depto.campus);
              }
            });

            const campusUnicos = Array.from(campusMap.values());

            console.log("Campus extraídos dos departamentos:", campusUnicos);
          }
        }
      })
      .catch(err => {
        console.error("Erro ao carregar informações do usuário:", err);
        setErro('Erro ao carregar informações do usuário: ' + (err.response?.data || err.message));
      });
  }

  function carregarTodosCampus() {
    api.get('/api/campus', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        console.log("Todos os campus carregados:", resp.data);
      })
      .catch(err => {
        console.error("Erro ao carregar campus:", err);
        setErro('Erro ao carregar campus: ' + (err.response?.data || err.message));
      });
  }

  function carregarEventos() {
    setCarregando(true);
    setErro(null);

    api.get('/api/eventos/todos-que-gerencio', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        if (resp.status === 204) {
          setEventos([]);
        } else {
          setEventos(resp.data);
        }
      })
      .catch(err => {
        console.error("Erro ao carregar eventos:", err);
        setErro('Erro ao carregar eventos: ' + (err.response?.data || err.message));
      })
      .finally(() => setCarregando(false));
  }

  function handleDelete(id) {
    setEventoSelecionado(id);
    setShowDeleteModal(true);
  }

  function confirmarDelete() {
    if (!eventoSelecionado) return;

    api.delete(`/api/eventos/${eventoSelecionado}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(() => {
        setEventos(prev => prev.filter(e => e.id !== eventoSelecionado));
        setShowDeleteModal(false);
        setMensagemSucesso('Evento excluído com sucesso!');
      })
      .catch(err => {
        console.error("Erro ao deletar evento:", err);
        setErro('Erro ao deletar: ' + (err.response?.data || err.message));
        setShowDeleteModal(false);
      });
  }

  function handleEncerrarEvento(id) {
    setEventoSelecionado(id);
    setShowEncerrarModal(true);
  }

  function confirmarEncerramento() {
    if (!eventoSelecionado) return;

    api.put(`/api/eventos/${eventoSelecionado}/encerrar`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(() => {
        setEventos(prev => prev.map(e => {
          if (e.id === eventoSelecionado) {
            return {
              ...e,
              dataFim: new Date().toISOString(),
              status: 'ENCERRADO'
            };
          }
          return e;
        }));
        setShowEncerrarModal(false);
        setMensagemSucesso('Evento encerrado com sucesso!');
      })
      .catch(err => {
        console.error("Erro ao encerrar evento:", err);
        setErro('Erro ao encerrar evento: ' + (err.response?.data || err.message));
        setShowEncerrarModal(false);
      });
  }

  function handleFiltroChange(e) {
    setFiltroStatus(e.target.value);
  }

  function handleSearchChange(e) {
    setSearchTerm(e.target.value);
  }

  function handleSearch(e) {
    e.preventDefault();
  }

  function formatarData(dataStr) {
    if (!dataStr) return 'N/D';
    const data = new Date(dataStr);
    return data.toLocaleString('pt-BR');
  }

  function filtrarEventos() {
    let filtrados = eventos;

    if (filtroStatus !== 'todos') {
      if (filtroStatus === 'ativos') {
        filtrados = filtrados.filter(evento => {
          if (evento.status !== undefined) {
            return evento.status === 'ATIVO' ||
              evento.status === EventoStatus.ATIVO ||
              evento.status?.name === 'ATIVO' ||
              evento.status?.toString() === 'ATIVO';
          } else {
            const dataFim = evento.dataFim ? new Date(evento.dataFim) : null;
            const hoje = new Date();
            return !dataFim || dataFim > hoje;
          }
        });
      } else {
        filtrados = filtrados.filter(evento => {
          if (evento.status !== undefined) {
            return evento.status === 'ENCERRADO' ||
              evento.status === EventoStatus.ENCERRADO ||
              evento.status?.name === 'ENCERRADO' ||
              evento.status?.toString() === 'ENCERRADO';
          } else {
            const dataFim = evento.dataFim ? new Date(evento.dataFim) : null;
            const hoje = new Date();
            return dataFim && dataFim <= hoje;
          }
        });
      }
    }

    if (searchTerm.trim()) {
      const termoBusca = searchTerm.toLowerCase();
      filtrados = filtrados.filter(evt =>
        evt.titulo.toLowerCase().includes(termoBusca) ||
        (evt.descricao && evt.descricao.toLowerCase().includes(termoBusca)) ||
        (evt.local && evt.local.toLowerCase().includes(termoBusca)) ||
        (evt.campus?.nome && evt.campus.nome.toLowerCase().includes(termoBusca)) ||
        (evt.departamento?.nome && evt.departamento.nome.toLowerCase().includes(termoBusca))
      );
    }

    return filtrados;
  }

  const eventosFiltrados = filtrarEventos();

  const hoje = new Date();

  eventosFiltrados.sort((a, b) => {
    if (filtroStatus === 'todos') {
      const aEncerrado = isEventoEncerrado(a);
      const bEncerrado = isEventoEncerrado(b);

      if (aEncerrado !== bEncerrado) {
        return aEncerrado ? 1 : -1;
      }
    }

    const dataInicioA = a.dataInicio ? new Date(a.dataInicio) : new Date();
    const dataInicioB = b.dataInicio ? new Date(b.dataInicio) : new Date();

    if (filtroStatus === 'ativos' ||
      (filtroStatus === 'todos' && isEventoEncerrado(a) === isEventoEncerrado(b))) {

      const aJaComecou = dataInicioA <= hoje;
      const bJaComecou = dataInicioB <= hoje;

      if (aJaComecou !== bJaComecou) {
        return aJaComecou ? 1 : -1;
      }

      return dataInicioA - dataInicioB;
    }

    return dataInicioA - dataInicioB;
  });

  function isEventoEncerrado(evento) {
    if (evento.status !== undefined) {
      return evento.status === 'ENCERRADO' ||
        evento.status === EventoStatus.ENCERRADO ||
        evento.status?.name === 'ENCERRADO' ||
        evento.status?.toString() === 'ENCERRADO';
    }

    const hoje = new Date();
    const dataFim = evento.dataFim ? new Date(evento.dataFim) : null;
    return dataFim && dataFim <= hoje;
  }

  return (
    <div className="admin-eventos-container">
      <div className="page-header">
        <h2>Gerenciamento de Eventos</h2>
        <div className="page-actions">
          <button
            onClick={carregarEventos}
            className="btn-secondary mr-2"
            title="Atualizar lista"
          >
            <i className="icon-refresh"></i> Atualizar
          </button>

          <button
            onClick={() => navigate('/admin/eventos/novo')}
            className="btn-primary"
          >
            <i className="icon-plus"></i> Criar Novo Evento
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

      <div className="events-container">
        <div className="eventos-header">
          <h3>Meus Eventos</h3>
          <div className="filters-container">
            <div className="filtro-container">
              <label>Status:</label>
              <select value={filtroStatus} onChange={handleFiltroChange} className="filtro-select">
                <option value="todos">Todos</option>
                <option value="ativos">Ativos</option>
                <option value="encerrados">Encerrados</option>
              </select>
            </div>
            <form onSubmit={handleSearch} className="search-container">
              <input
                type="text"
                placeholder="Buscar eventos..."
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

        {eventosFiltrados.length > 0 ? (
          <div className="events-list">
            {eventosFiltrados.map(evt => {
              const eventoEncerrado = isEventoEncerrado(evt);

              return (
                <div key={evt.id} className={`event-card ${eventoEncerrado ? 'event-encerrado' : ''}`}>
                  <div className="event-header">
                    <h4>{evt.titulo}</h4>
                    {eventoEncerrado ?
                      <span className="status-badge encerrado">Encerrado</span> :
                      <span className="status-badge ativo">Ativo</span>
                    }
                  </div>

                  <div className="event-info">
                    <div className="event-section">
                      <p><strong>Local:</strong> {evt.local || 'N/D'}</p>
                      <p><strong>Período:</strong> {formatarData(evt.dataInicio)} - {formatarData(evt.dataFim)}</p>
                      {evt.dataLimiteInscricao && (
                        <p><strong>Inscrições até:</strong> {formatarData(evt.dataLimiteInscricao)}</p>
                      )}
                    </div>

                    <div className="event-section">
                      <p><strong>Campus:</strong> {evt.campus?.nome || 'N/D'}</p>
                      <p><strong>Departamento:</strong> {evt.departamento?.nome || 'N/D'}</p>
                      {evt.vagas && <p><strong>Vagas:</strong> {evt.vagas}</p>}
                      {evt.estudanteIfg && <p><span className="ifg-only">Exclusivo para estudantes IFG</span></p>}
                    </div>
                  </div>

                  {evt.descricao && (
                    <div className="event-description">
                      <p><strong>Descrição:</strong> {evt.descricao}</p>
                    </div>
                  )}

                  <div className="event-actions">
                    <button
                      onClick={() => navigate(`/admin/eventos/${evt.id}/inscritos`)}
                      className="btn-secondary"
                      title="Ver lista de inscritos"
                    >
                      <i className="icon-users"></i> Inscritos
                    </button>
                    <button
                      onClick={() => navigate(`/admin/eventos/${evt.id}/editar`)}
                      className="btn-edit"
                      title="Editar evento"
                    >
                      <i className="icon-edit"></i> Editar
                    </button>
                    {!eventoEncerrado && (
                      <button
                        onClick={() => handleEncerrarEvento(evt.id)}
                        className="btn-conclude"
                        title="Encerrar evento"
                      >
                        <i className="fa-solid fa-ban"></i> Encerrar
                      </button>
                    )}
                    <button
                      onClick={() => handleDelete(evt.id)}
                      className="btn-delete"
                      title="Excluir evento"
                    >
                      <i className="icon-trash"></i> Excluir
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          !carregando && !erro && (
            <div className="empty-state">
              <div className="empty-state-icon">
                <i className="icon-calendar-empty"></i>
              </div>
              <p>Nenhum evento encontrado para gerenciar.</p>
              <p>Clique em "Criar Novo Evento" para adicionar um evento.</p>
            </div>
          )
        )}
      </div>

      <ConfirmationModal
        isOpen={showDeleteModal}
        title="Excluir Evento"
        message="Tem certeza que deseja excluir este evento? Esta ação não pode ser desfeita."
        onConfirm={confirmarDelete}
        onCancel={() => setShowDeleteModal(false)}
      />

      <ConfirmationModal
        isOpen={showEncerrarModal}
        title="Encerrar Evento"
        message="Tem certeza que deseja encerrar este evento? Esta ação irá definir o status como ENCERRADO."
        onConfirm={confirmarEncerramento}
        onCancel={() => setShowEncerrarModal(false)}
      />
    </div>
  );
}

export default AdminEventos;