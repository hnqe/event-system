import React, { useEffect, useState, useCallback } from "react";
import api from "../services/api";
import FormularioInscricao from "../components/FormularioInscricao";
import DetalhesModal from "../components/DetalhesModal";
import "./HomePage.css";

function HomePage({ onOpenLoginModal, isLogged }) {
  const [eventos, setEventos] = useState([]);
  const [eventosFiltrados, setEventosFiltrados] = useState([]);
  const [minhasInscricoes, setMinhasInscricoes] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadingInscricoes, setLoadingInscricoes] = useState(false);
  const [inscricoesCarregadas, setInscricoesCarregadas] = useState(false);
  const [error, setError] = useState(null);
  const [eventoSelecionado, setEventoSelecionado] = useState(null);
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [activeTab, setActiveTab] = useState('disponiveis');

  const [eventoDetalhes, setEventoDetalhes] = useState(null);
  const [mostrarDetalhes, setMostrarDetalhes] = useState(false);

  const token = localStorage.getItem("token");
  const isAuthenticated = isLogged && !!token;

  const verificarCamposAdicionais = useCallback((eventoId) => {
    return new Promise((resolve) => {
      api.get(`/api/inscricoes/evento/${eventoId}/campos`, {
        headers: { Authorization: `Bearer ${token}` }
      })
        .then(resp => {
          const campos = resp.data;
          resolve(campos && campos.length > 0);
        })
        .catch(() => {
          resolve(false);
        });
    });
  }, [token]);

  const carregarMinhasInscricoes = useCallback(() => {
    if (!isAuthenticated) return;

    setLoadingInscricoes(true);
    api.get("/api/inscricoes/minhas", {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then((resp) => {
        setMinhasInscricoes(resp.data);
        setInscricoesCarregadas(true);
        setLoadingInscricoes(false);
      })
      .catch((err) => {
        console.error("Erro ao carregar inscrições:", err);
        setLoadingInscricoes(false);
        window.showNotification('error', 'Não foi possível carregar suas inscrições.');
      });
  }, [isAuthenticated, token]);

  const filtrarEventos = useCallback(() => {
    if (!eventos.length) return;

    let eventosBase = eventos;

    if (activeTab === 'inscritos' && inscricoesCarregadas) {
      const eventosInscritos = minhasInscricoes
        .filter(inscricao => inscricao.status === 'ATIVA')
        .map(inscricao => {
          if (inscricao.evento) {
            return inscricao.evento;
          } else if (inscricao.eventoId) {
            return eventos.find(e => e.id === inscricao.eventoId) || null;
          }
          return null;
        })
        .filter(evento => evento !== null);

      eventosBase = eventosInscritos;
    }

    if (!search.trim()) {
      setEventosFiltrados(eventosBase);
      return;
    }

    const searchLower = search.toLowerCase();
    const filtrados = eventosBase.filter(evt =>
      evt.titulo.toLowerCase().includes(searchLower) ||
      (evt.descricao && evt.descricao.toLowerCase().includes(searchLower)) ||
      (evt.local && evt.local.toLowerCase().includes(searchLower)) ||
      (evt.campus?.nome && evt.campus.nome.toLowerCase().includes(searchLower)) ||
      (evt.departamento?.nome && evt.departamento.nome.toLowerCase().includes(searchLower))
    );

    setEventosFiltrados(filtrados);
  }, [activeTab, eventos, inscricoesCarregadas, minhasInscricoes, search]);

  const fazerInscricao = useCallback(async (evento) => {
    if (!isAuthenticated) {
      localStorage.setItem("redirectEventoId", evento.id);
      if (onOpenLoginModal) {
        onOpenLoginModal();
      }
      return;
    }

    const jaInscrito = isAuthenticated && minhasInscricoes.some(
      inscricao => (
        (inscricao.eventoId === evento.id || inscricao.evento?.id === evento.id) &&
        inscricao.status === 'ATIVA'
      )
    );

    if (jaInscrito) {
      window.showNotification('info', 'Você já está inscrito neste evento!');
      return;
    }

    const temCampos = await verificarCamposAdicionais(evento.id);

    setMostrarFormulario(false);
    setEventoSelecionado(null);

    setTimeout(() => {
      if (temCampos) {
        setEventoSelecionado(evento);
        setMostrarFormulario(true);
        setTimeout(() => {
          document.getElementById('inscricao-form')?.scrollIntoView({ behavior: 'smooth' });
        }, 100);
      } else {
        api.post(`/api/inscricoes/inscrever?eventoId=${evento.id}`, {}, {
          headers: { Authorization: `Bearer ${token}` }
        })
          .then(response => {
            window.showNotification('success', 'Inscrição realizada com sucesso!');
            carregarMinhasInscricoes();
            setActiveTab('inscritos');
          })
          .catch(err => {
            window.showNotification('error', `Erro ao realizar inscrição: ${err.response?.data || 'Falha no servidor'}`);
          });
      }
    }, 50);
  }, [carregarMinhasInscricoes, isAuthenticated, minhasInscricoes, onOpenLoginModal, token, verificarCamposAdicionais]);

  const cancelarInscricao = useCallback((inscricaoId) => {
    window.showConfirmation(
      'Tem certeza que deseja cancelar sua inscrição neste evento?',
      () => {
        api.delete(`/api/inscricoes/${inscricaoId}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
          .then(response => {
            window.showNotification('success', 'Inscrição cancelada com sucesso!');
            carregarMinhasInscricoes();
          })
          .catch(err => {
            window.showNotification('error', `Erro ao cancelar inscrição: ${err.response?.data || 'Falha no servidor'}`);
          });
      }
    );
  }, [carregarMinhasInscricoes, token]);

  const handleInscricaoRealizada = useCallback((inscricaoData) => {
    window.showNotification('success', 'Inscrição realizada com sucesso!');
    setTimeout(() => {
      setMostrarFormulario(false);
      setEventoSelecionado(null);
      carregarMinhasInscricoes();
      setActiveTab('inscritos');
    }, 2000);
  }, [carregarMinhasInscricoes]);

  const carregarEventos = useCallback(() => {
    setLoading(true);

    api.get("/api/eventos")
      .then((resp) => {
        const eventosData = resp.data;
        setEventos(eventosData);
        setEventosFiltrados(eventosData);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Erro ao carregar eventos:", err);
        setError("Não foi possível carregar os eventos.");
        setLoading(false);
        window.showNotification('error', 'Não foi possível carregar os eventos.');
      });
  }, []);

  const checkRedirectAfterLogin = useCallback(async () => {
    if (isAuthenticated) {
      const redirectEventoId = localStorage.getItem("redirectEventoId");
      if (redirectEventoId) {
        const evento = eventos.find(evt => evt.id.toString() === redirectEventoId.toString());
        if (evento) {
          localStorage.removeItem("redirectEventoId");
          await fazerInscricao(evento);
        }
      }
    }
  }, [eventos, fazerInscricao, isAuthenticated]);

  const abrirDetalhes = useCallback((evento) => {
    setEventoDetalhes(evento);
    setMostrarDetalhes(true);
  }, []);

  const fecharDetalhes = useCallback(() => {
    setMostrarDetalhes(false);
    setEventoDetalhes(null);
  }, []);

  useEffect(() => {
    carregarEventos();
  }, [carregarEventos]);

  useEffect(() => {
    if (isAuthenticated) {
      carregarMinhasInscricoes();
    } else {
      setMinhasInscricoes([]);
      setInscricoesCarregadas(false);
    }
  }, [isAuthenticated, carregarMinhasInscricoes]);

  useEffect(() => {
    if (!isAuthenticated) {
      setMinhasInscricoes([]);
      setInscricoesCarregadas(false);
      setActiveTab('disponiveis');
    }
  }, [isAuthenticated]);

  useEffect(() => {
    filtrarEventos();
  }, [search, eventos, activeTab, minhasInscricoes, filtrarEventos]);

  useEffect(() => {
    if (eventos.length > 0 && isAuthenticated) {
      checkRedirectAfterLogin();
    }
  }, [eventos, isAuthenticated, checkRedirectAfterLogin]);

  function formatarData(dataStr) {
    if (!dataStr) return "Não informada";

    const data = new Date(dataStr);
    const dia = data.getDate().toString().padStart(2, "0");
    const mes = (data.getMonth() + 1).toString().padStart(2, "0");
    const ano = data.getFullYear();
    const hora = data.getHours().toString().padStart(2, "0");
    const minutos = data.getMinutes().toString().padStart(2, "0");

    return `${dia}/${mes}/${ano} ${hora}:${minutos}`;
  }

  function verificarEventoAtivo(evento) {
    if (evento.status === "ENCERRADO") return false;

    const hoje = new Date();
    const dataFim = evento.dataFim ? new Date(evento.dataFim) : null;

    return !dataFim || dataFim > hoje;
  }

  return (
    <div className="home-container">
      <div className="events-header">
        <h1>Eventos</h1>
        <div className="search-box">
          <input
            type="text"
            placeholder="Pesquise por título, local, campus ou departamento..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {isAuthenticated && (
        <div className="events-tabs">
          <button
            className={`tab-button ${activeTab === 'disponiveis' ? 'active' : ''}`}
            onClick={() => setActiveTab('disponiveis')}
          >
            Eventos Disponíveis
          </button>
          <button
            className={`tab-button ${activeTab === 'inscritos' ? 'active' : ''}`}
            onClick={() => setActiveTab('inscritos')}
          >
            Meus Eventos
          </button>
        </div>
      )}

      {loading && (
        <div className="status-loading">
          <div className="spinner"></div>
          <span>Carregando eventos...</span>
        </div>
      )}

      {loadingInscricoes && activeTab === 'inscritos' && (
        <div className="status-loading">
          <div className="spinner"></div>
          <span>Carregando suas inscrições...</span>
        </div>
      )}

      {error && (
        <div className="status-error">
          <i className="icon-alert"></i>
          <span>{error}</span>
        </div>
      )}

      <section className="events-section">
        <h2>
          {activeTab === 'inscritos'
            ? 'Eventos em que estou inscrito'
            : 'Próximos Eventos'}
        </h2>

        {!loading && eventosFiltrados.length === 0 ? (
          <p className="no-events">
            {activeTab === 'inscritos'
              ? 'Você ainda não está inscrito em nenhum evento.'
              : 'Nenhum evento encontrado para a busca.'}
          </p>
        ) : (
          <div className="events-grid">
            {eventosFiltrados
              .filter(evt => activeTab === 'inscritos' || verificarEventoAtivo(evt))
              .map((evt) => {
                const inscricaoAtiva = isAuthenticated && minhasInscricoes.find(
                  inscricao => (
                    (inscricao.eventoId === evt.id || inscricao.evento?.id === evt.id) &&
                    inscricao.status === 'ATIVA'
                  )
                );
                const jaInscrito = !!inscricaoAtiva;

                return (
                  <div key={evt.id} className="event-card">
                    <div className="event-header">
                      <h3>{evt.titulo}</h3>
                      <span className="event-campus">{evt.campus?.nome || 'Campus não informado'}</span>
                    </div>

                    <div className="event-body">
                      <div className="event-content">
                        <p className="event-location">
                          <i className="fa fa-map-marker"></i> {evt.local || 'Local não informado'}
                        </p>

                        <p className="event-date">
                          <i className="fa fa-calendar"></i> {formatarData(evt.dataInicio)} - {formatarData(evt.dataFim)}
                        </p>

                        {evt.dataLimiteInscricao && (
                          <p className="event-inscriptions">
                            <i className="fa fa-clock-o"></i> Inscrições até: {formatarData(evt.dataLimiteInscricao)}
                          </p>
                        )}

                        {evt.departamento && (
                          <p className="event-department">
                            <i className="fa fa-building"></i> {evt.departamento.nome}
                          </p>
                        )}

                        {evt.vagas && (
                          <p className="event-vacancies">
                            <i className="fa fa-users"></i> Vagas: {evt.vagas}
                          </p>
                        )}

                        {evt.estudanteIfg && (
                          <p className="event-exclusive">
                            <i className="fa fa-star"></i> Exclusivo para estudantes IFG
                          </p>
                        )}

                        {evt.descricao && (
                          <div className="event-description-preview">
                            <h4>Descrição:</h4>
                            <p>
                              {evt.descricao.length > 80
                                ? evt.descricao.substring(0, 80)
                                : evt.descricao}
                            </p>
                          </div>
                        )}
                      </div>

                      <div className="btn-ver-detalhes-container">
                        <button
                          className="btn-ver-detalhes"
                          onClick={() => abrirDetalhes(evt)}
                        >
                          <i className="fa fa-search"></i> Ver Detalhes
                        </button>
                      </div>
                    </div>

                    <div className="event-footer">
                      {activeTab === 'inscritos' ? (
                        <div className="inscricao-actions">
                          <div className="inscrito-badge">
                            <i className="fa fa-check-circle"></i> Inscrito
                          </div>
                          {inscricaoAtiva && (
                            <button
                              onClick={() => cancelarInscricao(inscricaoAtiva.id)}
                              className="btn-cancelar-inscricao"
                            >
                              <i className="fa fa-times-circle"></i> Cancelar Inscrição
                            </button>
                          )}
                        </div>
                      ) : isAuthenticated && jaInscrito ? (
                        <div className="inscricao-actions">
                          <div className="inscrito-badge">
                            <i className="fa fa-check-circle"></i> Você já está inscrito
                          </div>
                          {inscricaoAtiva && (
                            <button
                              onClick={() => cancelarInscricao(inscricaoAtiva.id)}
                              className="btn-cancelar-inscricao"
                            >
                              <i className="fa fa-times-circle"></i> Cancelar Inscrição
                            </button>
                          )}
                        </div>
                      ) : (
                        <button
                          onClick={() => fazerInscricao(evt)}
                          className="btn-inscrever"
                        >
                          {isAuthenticated ? 'Inscrever-se' : 'Login para Inscrever-se'}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
          </div>
        )}
      </section>

      <DetalhesModal
        evento={eventoDetalhes}
        isOpen={mostrarDetalhes}
        onClose={fecharDetalhes}
        onInscrever={fazerInscricao}
        onCancelar={cancelarInscricao}
        jaInscrito={isAuthenticated && eventoDetalhes ? minhasInscricoes.some(
          inscricao => (
            (inscricao.eventoId === eventoDetalhes.id || inscricao.evento?.id === eventoDetalhes.id) &&
            inscricao.status === 'ATIVA'
          )
        ) : false}
        inscricaoAtiva={isAuthenticated && eventoDetalhes ? minhasInscricoes.find(
          inscricao => (
            (inscricao.eventoId === eventoDetalhes.id || inscricao.evento?.id === eventoDetalhes.id) &&
            inscricao.status === 'ATIVA'
          )
        ) : null}
        isAuthenticated={isAuthenticated}
        formatarData={formatarData}
      />

      {mostrarFormulario && eventoSelecionado && (
        <div id="inscricao-form" className="inscricao-section">
          <div className="inscricao-header">
            <h2>Inscrição para: {eventoSelecionado.titulo}</h2>
            <button
              className="btn-close-form"
              onClick={() => setMostrarFormulario(false)}
              title="Fechar formulário"
            >
              ×
            </button>
          </div>

          <FormularioInscricao
            eventoId={eventoSelecionado.id}
            onInscricaoRealizada={handleInscricaoRealizada}
          />
        </div>
      )}
    </div>
  );
}

export default HomePage;