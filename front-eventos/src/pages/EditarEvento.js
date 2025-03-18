import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';
import CamposAdicionais from '../components/CamposAdicionais';
import './CriarEvento.css'; 

function EditarEvento() {
  const navigate = useNavigate();
  const { id } = useParams(); 
  
  const [loading, setLoading] = useState(false);
  const [fetchingEvent, setFetchingEvent] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [campusList, setCampusList] = useState([]);
  const [departamentos, setDepartamentos] = useState([]);
  const [userRoles, setUserRoles] = useState([]);
  const [userInfo, setUserInfo] = useState({
    campusAdministrados: [],
    departamentosAdministrados: []
  });
  const [camposAdicionais, setCamposAdicionais] = useState([]);

  const [formData, setFormData] = useState({
    titulo: '',
    descricao: '',
    local: '',
    dataInicio: '',
    dataFim: '',
    dataLimiteInscricao: '',
    campusId: '',
    departamentoId: '',
    vagas: '',
    estudanteIfg: false
  });

  const [validationErrors, setValidationErrors] = useState({});
  const token = localStorage.getItem("token");

  const carregarTodosCampus = useCallback(() => {
    console.log("Carregando todos os campus via API...");
    
    api.get('/api/campus', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        console.log("Campus carregados com sucesso:", resp.data);
        setCampusList(resp.data);
      })
      .catch(err => {
        console.error("Erro ao carregar campus:", err);
        setError('Erro ao carregar campus: ' + (err.response?.data || err.message));
      });
  }, [token]);

  const carregarDepartamentosPadrao = useCallback((campusId) => {
    console.log("Carregando departamentos padrão para campus ID:", campusId);
    
    api.get(`/api/departamentos`, {
      params: { campusId },
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        console.log(`${resp.data.length} departamentos carregados:`, resp.data);
        setDepartamentos(resp.data);
      })
      .catch(err => {
        console.error("Erro ao carregar departamentos:", err);
        setError('Erro ao carregar departamentos: ' + (err.response?.data || err.message));
      });
  }, [token]);

  const carregarDepartamentos = useCallback((campusId) => {
    console.log("Carregando departamentos para o campus ID:", campusId);
    
    if (userRoles.includes('ADMIN_DEPARTAMENTO') && 
        !userRoles.includes('ADMIN_GERAL') && 
        !userRoles.includes('ADMIN_CAMPUS')) {
      
      const campusIdInt = parseInt(campusId);
      const deptosFiltrados = userInfo.departamentosAdministrados.filter(depto => {
        if (depto.campus && depto.campus.id) {
          return depto.campus.id === campusIdInt;
        }
        return false;
      });
      
      console.log("Departamentos filtrados para o campus selecionado:", deptosFiltrados);
      if (deptosFiltrados.length > 0) {
        setDepartamentos(deptosFiltrados);
      } else {
        console.log("Nenhum departamento encontrado para este campus.");
        setDepartamentos([]);
      }
      
      return;
    }
    
    carregarDepartamentosPadrao(campusId);
  }, [userRoles, userInfo.departamentosAdministrados, carregarDepartamentosPadrao]);

  const carregarEvento = useCallback(() => {
    setFetchingEvent(true);
    setError(null);
    
    api.get(`/api/eventos/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        const eventoData = resp.data;
        
        const formatDateForInput = (dateString) => {
          if (!dateString) return '';
          const date = new Date(dateString);
          return date.toISOString().slice(0, 16); 
        };
        
        console.log("Evento carregado:", eventoData);
        
        const newFormData = {
          titulo: eventoData.titulo || '',
          descricao: eventoData.descricao || '',
          local: eventoData.local || '',
          dataInicio: formatDateForInput(eventoData.dataInicio),
          dataFim: formatDateForInput(eventoData.dataFim),
          dataLimiteInscricao: formatDateForInput(eventoData.dataLimiteInscricao),
          campusId: eventoData.campus?.id?.toString() || '',
          departamentoId: eventoData.departamento?.id?.toString() || '',
          vagas: eventoData.vagas?.toString() || '',
          estudanteIfg: eventoData.estudanteIfg || false
        };
        
        setFormData(newFormData);
        
        if (eventoData.camposAdicionais && Array.isArray(eventoData.camposAdicionais)) {
          setCamposAdicionais(eventoData.camposAdicionais);
        }
      })
      .catch(err => {
        console.error("Erro ao carregar evento:", err);
        setError('Erro ao carregar evento: ' + (err.response?.data || err.message));
      })
      .finally(() => setFetchingEvent(false));
  }, [id, token]);
  
  const carregarUserInfo = useCallback(() => {
    setLoading(true);
    
    api.get('/api/auth/current-user', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(resp => {
        const userData = resp.data;
        
        const campusAdministrados = userData.campusQueAdministro || [];
        const departamentosAdministrados = userData.departamentosQueAdministro || [];
        
        console.log("Roles do usuário:", userData.roles?.map(role => role.name));
        console.log("Campus administrados:", campusAdministrados);
        console.log("Departamentos administrados:", departamentosAdministrados);
        
        setUserInfo({
          campusAdministrados: campusAdministrados,
          departamentosAdministrados: departamentosAdministrados
        });
        
        const roles = userData.roles?.map(role => role.name) || [];
        setUserRoles(roles);
        
        if (roles.includes('ADMIN_GERAL')) {
          carregarTodosCampus();
        } else if (roles.includes('ADMIN_CAMPUS')) {
          if (campusAdministrados.length > 0) {
            setCampusList(campusAdministrados);
          } else {
            carregarTodosCampus();
          }
        } else if (roles.includes('ADMIN_DEPARTAMENTO')) {
          if (departamentosAdministrados && departamentosAdministrados.length > 0) {
            const deptoId = departamentosAdministrados[0].id;
            console.log(`Obtendo campus diretamente para o departamento ID: ${deptoId}`);
            
            api.get(`/api/departamentos/${deptoId}/campus`, {
              headers: { Authorization: `Bearer ${token}` }
            })
              .then(campusResp => {
                const campus = campusResp.data;
                console.log(`Campus obtido por departamento: ID=${campus.id}, Nome=${campus.nome}`);
                
                setCampusList([campus]);
                
                departamentosAdministrados.forEach(depto => {
                  if (!depto.campus) {
                    depto.campus = campus;
                  }
                });
                
                setUserInfo(prev => ({
                  ...prev,
                  departamentosAdministrados: departamentosAdministrados
                }));
              })
              .catch(err => {
                console.error(`Erro ao obter campus do departamento ${deptoId}:`, err);
                carregarTodosCampus();
              });
          } else {
            console.log("Usuário não administra departamentos, carregando todos os campus...");
            carregarTodosCampus();
          }
        } else {
          carregarTodosCampus();
        }
      })
      .catch(err => {
        console.error("Erro ao carregar informações do usuário:", err);
        setError('Erro ao carregar informações do usuário: ' + (err.response?.data || err.message));
      })
      .finally(() => setLoading(false));
  }, [token, carregarTodosCampus]);

  const verificarPermissaoDepartamento = useCallback(() => {
    if (!formData.departamentoId) return true;
    
    if (userRoles.includes('ADMIN_GERAL')) return true;
    
    if (userRoles.includes('ADMIN_CAMPUS')) {
      const campusId = parseInt(formData.campusId);
      
      const temPermissaoNoCampus = userInfo.campusAdministrados.some(campus => {
        const adminCampusId = campus.id ? 
          (typeof campus.id === 'number' ? campus.id : parseInt(campus.id)) : 
          (typeof campus === 'number' ? campus : parseInt(campus));
        
        return adminCampusId === campusId;
      });
      
      return temPermissaoNoCampus;
    }
    
    if (userRoles.includes('ADMIN_DEPARTAMENTO')) {
      const deptosAdministrados = userInfo.departamentosAdministrados.map(d => 
        typeof d.id === 'number' ? d.id : parseInt(d.id)
      );
      const deptoId = parseInt(formData.departamentoId);
      return deptosAdministrados.includes(deptoId);
    }
    
    return false;
  }, [formData.departamentoId, formData.campusId, userRoles, userInfo]);

  useEffect(() => {
    carregarUserInfo();
    carregarEvento();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!fetchingEvent && formData.campusId) {
      console.log("Evento carregado, carregando departamentos para campus:", formData.campusId);
      setTimeout(() => {
        carregarDepartamentos(formData.campusId);
      }, 100);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchingEvent]);

  useEffect(() => {
    if (!fetchingEvent && userRoles.length > 0 && formData.departamentoId) {
      const temPermissao = verificarPermissaoDepartamento();
      if (!temPermissao) {
        setError('Você não tem permissão para editar este evento. Serão mostrados apenas os departamentos que você administra.');
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchingEvent, userRoles, formData.departamentoId]);

  function handleChange(e) {
    const { name, value, type, checked } = e.target;
    
    if (name === 'campusId') {
      console.log(`Campus alterado para: "${value}"`);
      
      setFormData(prev => ({ 
        ...prev, 
        campusId: value,
        departamentoId: '' 
      }));
      
      if (value) {
        setTimeout(() => {
          carregarDepartamentos(value);
        }, 50);
      } else {
        setDepartamentos([]);
      }
    } else {
      setFormData(prev => ({ 
        ...prev, 
        [name]: type === 'checkbox' ? checked : value 
      }));
    }
    
    if (validationErrors[name]) {
      setValidationErrors(prev => ({ ...prev, [name]: null }));
    }
  }

  function validateForm() {
    const errors = {};
    
    if (!formData.titulo.trim()) errors.titulo = 'Título é obrigatório';
    if (!formData.local.trim()) errors.local = 'Local é obrigatório';
    if (!formData.campusId) errors.campusId = 'Campus é obrigatório';
    if (!formData.departamentoId) errors.departamentoId = 'Departamento é obrigatório';
    
    if (formData.dataInicio && formData.dataFim) {
      const inicio = new Date(formData.dataInicio);
      const fim = new Date(formData.dataFim);
      
      if (fim < inicio) {
        errors.dataFim = 'A data de término deve ser posterior à data de início';
      }
    }
    
    if (formData.vagas && parseInt(formData.vagas) <= 0) {
      errors.vagas = 'O número de vagas deve ser maior que zero';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function handleSubmit(e) {
    e.preventDefault();
    
    if (!validateForm()) return;
    
    if (!verificarPermissaoDepartamento()) {
      setError('Você não tem permissão para editar eventos neste departamento');
      return;
    }
    
    setLoading(true);
    setError(null);
    
    const eventData = {
      ...formData,
      campusId: parseInt(formData.campusId),
      departamentoId: parseInt(formData.departamentoId),
      vagas: formData.vagas ? parseInt(formData.vagas) : null,
      camposAdicionais: camposAdicionais 
    };
    
    api.put(`/api/eventos/${id}`, eventData, {
      headers: { 
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}` 
      }
    })
      .then(response => {
        setSuccess(true);
        
        setTimeout(() => {
          navigate('/admin/eventos');
        }, 2000);
      })
      .catch(err => {
        console.error("Erro ao atualizar evento:", err);
        setError('Erro ao atualizar evento: ' + (err.response?.data || err.message));
      })
      .finally(() => setLoading(false));
  }

  return (
    <div className="criar-evento-container">
      <div className="page-header">
        <div className="header-content">
          <h2>Editar Evento</h2>
          <p className="header-subtitle">Modifique os campos necessários e salve as alterações</p>
        </div>
        <div className="page-actions">
          <button 
            onClick={() => navigate('/admin/eventos')}
            className="btn-secondary"
          >
            <i className="fa-solid fa-arrow-left"></i> Voltar para eventos
          </button>
        </div>
      </div>

      {(loading || fetchingEvent) && (
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
      
      {success && (
        <div className="status-success">
          <i className="icon-check"></i>
          <span>Evento atualizado com sucesso! Redirecionando...</span>
        </div>
      )}

      {!fetchingEvent && (
        <div className="form-container">
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-column">
                <div className="form-section">
                  <h3>Informações Básicas</h3>
                  
                  <div className={`form-group ${validationErrors.titulo ? 'has-error' : ''}`}>
                    <label htmlFor="titulo">
                      <span className="field-label">Título</span>
                      <span className="required-indicator">*</span>
                    </label>
                    <input
                      id="titulo"
                      name="titulo"
                      value={formData.titulo}
                      onChange={handleChange}
                      className="form-control"
                      placeholder="Digite o título do evento"
                    />
                    {validationErrors.titulo && (
                      <div className="error-message">{validationErrors.titulo}</div>
                    )}
                  </div>
                  
                  <div className="form-group">
                    <label htmlFor="descricao">
                      <span className="field-label">Descrição</span>
                    </label>
                    <textarea
                      id="descricao"
                      name="descricao"
                      rows={3}
                      value={formData.descricao}
                      onChange={handleChange}
                      className="form-control"
                      placeholder="Descreva o evento detalhadamente"
                    />
                  </div>
                </div>
                
                <div className="form-section">
                  <h3>Localização</h3>
                  
                  <div className={`form-group ${validationErrors.campusId ? 'has-error' : ''}`}>
                    <label htmlFor="campusId">
                      <span className="field-label">Campus</span>
                      <span className="required-indicator">*</span>
                    </label>
                    <select 
                      id="campusId"
                      name="campusId" 
                      value={formData.campusId} 
                      onChange={handleChange}
                      className="form-control"
                    >
                      <option value="">Selecione um campus</option>
                      {campusList.map(campus => (
                        <option key={campus.id} value={campus.id}>{campus.nome}</option>
                      ))}
                    </select>
                    {validationErrors.campusId && (
                      <div className="error-message">{validationErrors.campusId}</div>
                    )}
                  </div>
                  
                  <div className={`form-group ${validationErrors.departamentoId ? 'has-error' : ''}`}>
                    <label htmlFor="departamentoId">
                      <span className="field-label">Departamento</span>
                      <span className="required-indicator">*</span>
                    </label>
                    <select 
                      id="departamentoId"
                      name="departamentoId" 
                      value={formData.departamentoId} 
                      onChange={handleChange}
                      className="form-control"
                      disabled={!formData.campusId}
                    >
                      <option value="">Selecione um departamento</option>
                      {departamentos.map(dept => (
                        <option key={dept.id} value={dept.id}>{dept.nome}</option>
                      ))}
                    </select>
                    {validationErrors.departamentoId && (
                      <div className="error-message">{validationErrors.departamentoId}</div>
                    )}
                  </div>
                  
                  <div className={`form-group ${validationErrors.local ? 'has-error' : ''}`}>
                    <label htmlFor="local">
                      <span className="field-label">Local</span>
                      <span className="required-indicator">*</span>
                    </label>
                    <input
                      id="local"
                      name="local"
                      value={formData.local}
                      onChange={handleChange}
                      className="form-control"
                      placeholder="Sala, auditório, laboratório, etc."
                    />
                    {validationErrors.local && (
                      <div className="error-message">{validationErrors.local}</div>
                    )}
                  </div>
                </div>
              </div>
              
              <div className="form-column">
                <div className="form-section">
                  <h3>Período e Vagas</h3>
                  
                  <div className="form-row">
                    <div className="form-group">
                      <label htmlFor="dataInicio">
                        <span className="field-label">Data Início</span>
                      </label>
                      <input
                        id="dataInicio"
                        type="datetime-local"
                        name="dataInicio"
                        value={formData.dataInicio}
                        onChange={handleChange}
                        className="form-control"
                      />
                    </div>
                    
                    <div className={`form-group ${validationErrors.dataFim ? 'has-error' : ''}`}>
                      <label htmlFor="dataFim">
                        <span className="field-label">Data Fim</span>
                      </label>
                      <input
                        id="dataFim"
                        type="datetime-local"
                        name="dataFim"
                        value={formData.dataFim}
                        onChange={handleChange}
                        className="form-control"
                      />
                      {validationErrors.dataFim && (
                        <div className="error-message">{validationErrors.dataFim}</div>
                      )}
                    </div>
                  </div>
                  
                  <div className="form-group">
                    <label htmlFor="dataLimiteInscricao">
                      <span className="field-label">Data Limite para Inscrição</span>
                    </label>
                    <input
                      id="dataLimiteInscricao"
                      type="datetime-local"
                      name="dataLimiteInscricao"
                      value={formData.dataLimiteInscricao}
                      onChange={handleChange}
                      className="form-control"
                    />
                  </div>
                  
                  <div className={`form-group ${validationErrors.vagas ? 'has-error' : ''}`}>
                    <label htmlFor="vagas">
                      <span className="field-label">Número de Vagas</span>
                    </label>
                    <input
                      id="vagas"
                      type="number"
                      name="vagas"
                      value={formData.vagas}
                      onChange={handleChange}
                      className="form-control"
                      min="1"
                      placeholder="Quantidade de vagas disponíveis"
                    />
                    {validationErrors.vagas && (
                      <div className="error-message">{validationErrors.vagas}</div>
                    )}
                  </div>
                  
                  <div className="form-group-checkbox">
                    <input
                      id="estudanteIfg"
                      type="checkbox"
                      name="estudanteIfg"
                      checked={formData.estudanteIfg}
                      onChange={handleChange}
                      className="checkbox-input"
                    />
                    <label htmlFor="estudanteIfg">
                      <span className="field-label">Exclusivo para estudantes do IFG</span>
                    </label>
                  </div>
                </div>
                
                <CamposAdicionais 
                  campos={camposAdicionais} 
                  setCampos={setCamposAdicionais} 
                />
              </div>
            </div>
            
            {!verificarPermissaoDepartamento() && formData.departamentoId && (
              <div className="alert-warning">
                <i className="icon-warning"></i>
                <span>Atenção: Você não tem permissão para editar eventos no departamento selecionado.</span>
              </div>
            )}
            
            <div className="form-actions">
              <button 
                type="button" 
                className="btn-secondary"
                onClick={() => navigate('/admin/eventos')}
              >
                Cancelar
              </button>
              <button 
                type="submit" 
                className="btn-primary"
                disabled={loading || !verificarPermissaoDepartamento() || success}
              >
                {loading ? (
                  <>
                    <div className="spinner-small"></div> 
                    <span>Salvando alterações...</span>
                  </>
                ) : (
                  <>
                    <i className="icon-save"></i> Salvar Alterações
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

export default EditarEvento;