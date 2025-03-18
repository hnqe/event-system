import React, { useEffect, useState } from "react";
import api from "../services/api";
import "./AdminCampusDepartamentos.css";
import ConfirmationModal from "../components/ConfirmationModal";

function AdminCampusDepartamentos() {
  const [campusList, setCampusList] = useState([]);
  const [newCampus, setNewCampus] = useState("");
  const [newDepartamento, setNewDepartamento] = useState("");
  const [selectedCampus, setSelectedCampus] = useState("");
  const [editingCampus, setEditingCampus] = useState(null);
  const [editingDepartamento, setEditingDepartamento] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const token = localStorage.getItem("token");

  const [modalOpen, setModalOpen] = useState(false);
  const [modalConfig, setModalConfig] = useState({
    title: "",
    message: "",
    onConfirm: () => { },
  });

  const [isAdminGeral, setIsAdminGeral] = useState(false);
  const [isAdminCampus, setIsAdminCampus] = useState(false);
  const [isAdminDepartamento, setIsAdminDepartamento] = useState(false);
  const [campusGerenciados, setCampusGerenciados] = useState([]);
  const [departamentosGerenciados, setDepartamentosGerenciados] = useState([]);

  useEffect(() => {
    const initialize = async () => {
      setLoading(true);
      setError("");
      try {
        const userResponse = await api.get("/api/auth/current-user", {
          headers: { Authorization: `Bearer ${token}` },
        });

        const user = userResponse.data;

        const roles = user.roles?.map(role => role.name) || [];

        const adminGeral = roles.includes("ADMIN_GERAL");
        const adminCampus = roles.includes("ADMIN_CAMPUS");
        const adminDepartamento = roles.includes("ADMIN_DEPARTAMENTO");

        setIsAdminGeral(adminGeral);
        setIsAdminCampus(adminCampus);
        setIsAdminDepartamento(adminDepartamento);

        if (adminCampus && user.campusQueAdministro) {
          setCampusGerenciados(user.campusQueAdministro);
        }

        if (adminDepartamento && user.departamentosQueAdministro) {
          const deptoIds = user.departamentosQueAdministro.map(d => d.id);
          setDepartamentosGerenciados(user.departamentosQueAdministro);

          if (deptoIds.length > 0) {
            try {
              const campusList = [];

              for (const deptoId of deptoIds) {
                try {
                  const campusResponse = await api.get(`/api/departamentos/${deptoId}/campus`, {
                    headers: { Authorization: `Bearer ${token}` },
                  });

                  const campus = campusResponse.data;

                  if (campus && campus.id) {
                    campusList.push(campus);
                  }
                } catch (campusError) {
                }
              }

              if (campusList.length > 0) {
                const campusMap = new Map();
                campusList.forEach(c => campusMap.set(c.id, c));
                const uniqueCampusList = Array.from(campusMap.values());

                setCampusGerenciados(uniqueCampusList);
              }
            } catch (deptoError) {
            }
          }
        }

        if (adminGeral) {
          await fetchAllCampus();
        } else if (adminCampus) {
          await fetchManagedCampus(user);
        } else if (adminDepartamento) {
          await fetchManagedDepartamentos();
        }

      } catch (error) {
        setError("Erro ao carregar informações do usuário: " + (error.response?.data || error.message));
      } finally {
        setLoading(false);
      }
    };

    initialize();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchAllCampus = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get("/api/campus", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setCampusList(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      setError("Erro ao carregar campus: " + (error.response?.data || error.message));
      setCampusList([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchManagedCampus = async (user) => {
    setLoading(true);
    setError("");

    try {
      if (user && user.campusQueAdministro && user.campusQueAdministro.length > 0) {
        setCampusList(user.campusQueAdministro);
        setCampusGerenciados(user.campusQueAdministro);

        if (user.campusQueAdministro[0] && user.campusQueAdministro[0].id) {
          setSelectedCampus(user.campusQueAdministro[0].id.toString());
        }
      } else {
        const response = await api.get("/api/auth/current-user", {
          headers: { Authorization: `Bearer ${token}` },
        });

        const userData = response.data;
        if (userData.campusQueAdministro && userData.campusQueAdministro.length > 0) {
          setCampusList(userData.campusQueAdministro);
          setCampusGerenciados(userData.campusQueAdministro);

          if (userData.campusQueAdministro[0] && userData.campusQueAdministro[0].id) {
            setSelectedCampus(userData.campusQueAdministro[0].id.toString());
          }
        } else {
          setCampusList([]);
        }
      }
    } catch (error) {
      setError("Erro ao carregar campus: " + (error.response?.data || error.message));
      setCampusList([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchManagedDepartamentos = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get("/api/departamentos/gerenciados", {
        headers: { Authorization: `Bearer ${token}` },
      });

      const departamentos = Array.isArray(response.data) ? response.data : [];

      if (departamentos.length === 0) {
        setCampusList([]);
        setDepartamentosGerenciados([]);
        setLoading(false);
        return;
      }

      setDepartamentosGerenciados(departamentos);

      const departamentosDetalhados = [];
      for (const depto of departamentos) {
        try {
          const deptoResponse = await api.get(`/api/departamentos/${depto.id}`, {
            headers: { Authorization: `Bearer ${token}` },
          });

          const deptoDetalhado = deptoResponse.data;
          departamentosDetalhados.push(deptoDetalhado);

          if (!deptoDetalhado.campus) {
            try {
              const campusResponse = await api.get(`/api/departamentos/${depto.id}/campus`, {
                headers: { Authorization: `Bearer ${token}` },
              });

              const campus = campusResponse.data;

              deptoDetalhado.campus = campus;
            } catch (campusError) {
            }
          }
        } catch (deptoError) {
        }
      }

      const departamentosParaProcessar = departamentosDetalhados.length > 0 ?
        departamentosDetalhados :
        departamentos;

      const departamentosValidos = departamentosParaProcessar.filter(depto => depto && depto.campus && depto.campus.id);

      if (departamentosValidos.length === 0) {
        const campusFallback = {
          id: 0,
          nome: "Campus Não Identificado",
          departamentos: departamentosParaProcessar.map(d => ({ ...d, campus: { id: 0, nome: "Campus Não Identificado" } }))
        };

        setCampusList([campusFallback]);
        setDepartamentosGerenciados(departamentosParaProcessar);
        setCampusGerenciados([{ id: 0, nome: "Campus Não Identificado" }]);
        setLoading(false);
        return;
      }

      const campusMap = new Map();

      departamentosValidos.forEach(depto => {
        const campusId = depto.campus.id;

        if (!campusMap.has(campusId)) {
          const campus = {
            ...depto.campus,
            departamentos: []
          };
          campusMap.set(campusId, campus);
        }

        campusMap.get(campusId).departamentos.push(depto);
      });

      const campusArray = Array.from(campusMap.values());

      setCampusList(campusArray);
      setDepartamentosGerenciados(departamentosValidos);

      const campusExtracted = campusArray.map(c => ({
        id: c.id,
        nome: c.nome
      }));
      setCampusGerenciados(campusExtracted);

    } catch (error) {
      setError("Erro ao carregar departamentos: " + (error.response?.data || error.message));
      setCampusList([]);
      setDepartamentosGerenciados([]);
    } finally {
      setLoading(false);
    }
  };

  const handleAddCampus = async () => {
    if (!newCampus.trim()) {
      setError("O nome do campus não pode estar vazio");
      return;
    }

    if (!isAdminGeral) {
      setError("Você não tem permissão para adicionar um novo campus");
      return;
    }

    setLoading(true);
    setError("");
    try {
      await api.post(
        "/api/campus",
        { nome: newCampus },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setNewCampus("");
      fetchAllCampus();
    } catch (error) {
      setError("Erro ao adicionar campus: " + (error.response?.data || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleEditCampus = async () => {
    if (!editingCampus || !editingCampus.nome.trim()) {
      setError("O nome do campus não pode estar vazio");
      return;
    }

    if (!isAdminGeral) {
      setError("Você não tem permissão para editar este campus");
      return;
    }

    setLoading(true);
    setError("");
    try {
      await api.put(
        `/api/campus/${editingCampus.id}`,
        { nome: editingCampus.nome },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setEditingCampus(null);
      fetchAllCampus();
    } catch (error) {
      setError("Erro ao editar campus: " + (error.response?.data || error.message));
    } finally {
      setLoading(false);
    }
  };

  const initiateDeleteCampus = (id, nome) => {
    if (!isAdminGeral) {
      setError("Você não tem permissão para remover este campus");
      return;
    }

    setModalConfig({
      title: "Remover Campus",
      message: `Tem certeza que deseja remover o campus "${nome}"? 
      
Esta ação não poderá ser desfeita e só será bem-sucedida se:
• O campus não estiver associado a nenhum usuário no sistema
• Todos os departamentos deste campus serão removidos também`,
      onConfirm: () => handleDeleteCampus(id),
    });
    setModalOpen(true);
  };

  const checkForeignKeyError = (error) => {
    const errorMessage = error.response?.data || error.message || "";

    if (errorMessage.includes("viola restrição de chave estrangeira") &&
      errorMessage.includes("user_campus")) {
      return "Este campus não pode ser removido porque está associado a usuários no sistema. Remova primeiro as associações de usuários com este campus.";
    }

    if (errorMessage.includes("viola restrição de chave estrangeira")) {
      return "Este item não pode ser removido porque está sendo usado em outras partes do sistema.";
    }

    return "Erro ao executar a operação: " + errorMessage;
  };

  const handleDeleteCampus = async (id) => {
    setModalOpen(false);
    setLoading(true);
    setError("");
    try {
      await api.delete(`/api/campus/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      fetchAllCampus();
    } catch (error) {
      setError(checkForeignKeyError(error));
    } finally {
      setLoading(false);
    }
  };

  const handleAddDepartamento = async () => {
    if (!newDepartamento.trim()) {
      setError("O nome do departamento não pode estar vazio");
      return;
    }

    if (!selectedCampus) {
      setError("Selecione um campus para o departamento");
      return;
    }

    if (!isAdminGeral && !isAdminCampus) {
      setError("Você não tem permissão para adicionar um novo departamento");
      return;
    }

    if (isAdminCampus && !isAdminGeral) {
      const campusGerenciadoIds = campusGerenciados.map(c => c.id);
      if (!campusGerenciadoIds.includes(parseInt(selectedCampus))) {
        setError("Você não tem permissão para adicionar departamentos a este campus");
        return;
      }
    }

    setLoading(true);
    setError("");
    try {
      await api.post(
        "/api/departamentos",
        { nome: newDepartamento, campusId: selectedCampus },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setNewDepartamento("");

      setTimeout(() => {
        if (isAdminGeral) {
          fetchAllCampus();
        } else if (isAdminCampus) {
          fetchManagedCampus();
        } else if (isAdminDepartamento) {
          fetchManagedDepartamentos();
        }
      }, 500);
    } catch (error) {
      setError("Erro ao adicionar departamento: " + (error.response?.data || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleEditDepartamento = async () => {
    if (!editingDepartamento || !editingDepartamento.nome.trim()) {
      setError("Nome do departamento não pode estar vazio");
      return;
    }

    let campusId;
    if (editingDepartamento.campusId) {
      campusId = editingDepartamento.campusId;
    } else if (editingDepartamento.campus?.id) {
      campusId = editingDepartamento.campus.id;
    } else {
      setError("Selecione um campus para o departamento");
      return;
    }

    if (isAdminDepartamento && !isAdminGeral && !isAdminCampus) {
      const podeDepartamento = departamentosGerenciados.some(d => d.id === editingDepartamento.id);
      if (!podeDepartamento) {
        setError("Você só pode editar departamentos que administra");
        return;
      }
    } else if (isAdminCampus && !isAdminGeral) {
      const campusGerenciadoIds = campusGerenciados.map(c => c.id.toString());
      const campusIdStr = campusId.toString();

      if (!campusGerenciadoIds.includes(campusIdStr)) {
        setError("Você só pode editar departamentos de campus que administra");
        return;
      }
    } else if (!isAdminGeral && !isAdminCampus && !isAdminDepartamento) {
      setError("Você não tem permissão para editar departamentos");
      return;
    }

    setLoading(true);
    setError("");
    try {
      const requestData = {
        nome: editingDepartamento.nome,
        campusId: campusId
      };

      await api.put(
        `/api/departamentos/${editingDepartamento.id}`,
        requestData,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setEditingDepartamento(null);

      setTimeout(() => {
        if (isAdminGeral) {
          fetchAllCampus();
        } else if (isAdminCampus) {
          fetchManagedCampus();
        } else if (isAdminDepartamento) {
          fetchManagedDepartamentos();
        }
      }, 500);
    } catch (error) {
      if (error.response && error.response.status === 403) {
        setError("Erro de permissão: " + (error.response?.data || "Você não tem permissão para realizar esta operação."));
      } else {
        setError("Erro ao editar departamento: " + (error.response?.data || error.message));
      }
    } finally {
      setLoading(false);
    }
  };

  const initiateDeleteDepartamento = (id, nome) => {
    if (!isAdminGeral && !isAdminCampus) {
      setError("Você não tem permissão para remover este departamento");
      return;
    }

    setModalConfig({
      title: "Remover Departamento",
      message: `Tem certeza que deseja remover o departamento "${nome}"? Esta ação não poderá ser desfeita.`,
      onConfirm: () => handleDeleteDepartamento(id),
    });
    setModalOpen(true);
  };

  const handleDeleteDepartamento = async (id) => {
    setModalOpen(false);
    setLoading(true);
    setError("");
    try {
      await api.delete(`/api/departamentos/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      setTimeout(() => {
        if (isAdminGeral) {
          fetchAllCampus();
        } else if (isAdminCampus) {
          fetchManagedCampus();
        } else if (isAdminDepartamento) {
          fetchManagedDepartamentos();
        }
      }, 500);
    } catch (error) {
      setError(checkForeignKeyError(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="admin-campus">
      <h2>Gerenciar Campus e Departamentos</h2>

      {error && <div className="error-message">{error}</div>}
      {loading && <div className="loading-message">Carregando...</div>}

      <ConfirmationModal
        isOpen={modalOpen}
        title={modalConfig.title}
        message={modalConfig.message}
        onConfirm={modalConfig.onConfirm}
        onCancel={() => setModalOpen(false)}
      />

      <div className="section">
        <h3>Campus</h3>
        {campusList.length === 0 ? (
          <p className="info-message">Nenhum campus encontrado.</p>
        ) : (
          <ul className="campus-list">
            {campusList.map((campus) => (
              <li key={campus.id} className="campus-item">
                {editingCampus?.id === campus.id ? (
                  <div className="edit-form">
                    <input
                      type="text"
                      value={editingCampus.nome}
                      onChange={(e) => setEditingCampus({ ...editingCampus, nome: e.target.value })}
                      className="edit-input"
                    />
                    <div className="button-group">
                      <button onClick={handleEditCampus} className="save-button">Salvar</button>
                      <button onClick={() => setEditingCampus(null)} className="cancel-button">Cancelar</button>
                    </div>
                  </div>
                ) : (
                  <div className="campus-header">
                    <strong className="campus-name">{campus.nome}</strong>
                    {isAdminGeral && (
                      <div className="button-group">
                        <button onClick={() => setEditingCampus(campus)} className="edit-button">Editar</button>
                        <button onClick={() => initiateDeleteCampus(campus.id, campus.nome)} className="delete-button">Remover</button>
                      </div>
                    )}
                  </div>
                )}

                <h4 className="departamentos-heading">Departamentos:</h4>
                {campus.departamentos && campus.departamentos.length > 0 ? (
                  <ul className="departamento-list">
                    {campus.departamentos.map((dep) => (
                      <li key={dep.id} className="departamento-item">
                        {editingDepartamento?.id === dep.id ? (
                          <div className="edit-form">
                            <input
                              type="text"
                              value={editingDepartamento.nome}
                              onChange={(e) => setEditingDepartamento({ ...editingDepartamento, nome: e.target.value })}
                              className="edit-input"
                            />
                            {(isAdminGeral || isAdminCampus) && (
                              <select
                                value={editingDepartamento.campusId || editingDepartamento.campus?.id || ""}
                                onChange={(e) => setEditingDepartamento({ ...editingDepartamento, campusId: e.target.value })}
                                className="campus-select"
                              >
                                <option value="">Selecione um campus</option>
                                {(isAdminGeral ? campusList : campusGerenciados).map((c) => (
                                  <option key={c.id} value={c.id}>
                                    {c.nome}
                                  </option>
                                ))}
                              </select>
                            )}
                            <div className="button-group">
                              <button onClick={handleEditDepartamento} className="save-button">Salvar</button>
                              <button onClick={() => setEditingDepartamento(null)} className="cancel-button">Cancelar</button>
                            </div>
                          </div>
                        ) : (
                          <div className="departamento-content">
                            <span className="departamento-name">{dep.nome}</span>
                            {(isAdminGeral || isAdminCampus) ? (
                              <div className="button-group">
                                <button onClick={() => setEditingDepartamento(dep)} className="edit-button">Editar</button>
                                <button onClick={() => initiateDeleteDepartamento(dep.id, dep.nome)} className="delete-button">Remover</button>
                              </div>
                            ) : (isAdminDepartamento && departamentosGerenciados.some(d => d.id === dep.id)) ? (
                              <div className="button-group">
                                <button onClick={() => setEditingDepartamento(dep)} className="edit-button">Editar</button>
                              </div>
                            ) : null}
                          </div>
                        )}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="no-departamentos">Nenhum departamento cadastrado.</p>
                )}
              </li>
            ))}
          </ul>
        )}

        {isAdminGeral && (
          <div className="add-form">
            <input
              type="text"
              placeholder="Novo campus..."
              value={newCampus}
              onChange={(e) => setNewCampus(e.target.value)}
              className="add-input"
            />
            <button onClick={handleAddCampus} className="add-button">Adicionar Campus</button>
          </div>
        )}
      </div>

      {(isAdminGeral || isAdminCampus) && (
        <div className="section">
          <h3>Adicionar Novo Departamento</h3>
          <div className="add-form">
            {isAdminGeral ? (
              <select
                onChange={(e) => setSelectedCampus(e.target.value)}
                value={selectedCampus}
                className="campus-select"
              >
                <option value="">Selecione um campus</option>
                {campusList.map((campus) => (
                  <option key={campus.id} value={campus.id}>
                    {campus.nome}
                  </option>
                ))}
              </select>
            ) : isAdminCampus && campusGerenciados.length > 0 ? (
              <select
                onChange={(e) => setSelectedCampus(e.target.value)}
                value={selectedCampus}
                className="campus-select"
              >
                <option value="">Selecione um campus</option>
                {campusGerenciados.map((campus) => (
                  <option key={campus.id} value={campus.id}>
                    {campus.nome}
                  </option>
                ))}
              </select>
            ) : (
              <div className="no-campus-message">
                Nenhum campus disponível para adicionar departamentos.
              </div>
            )}
            <input
              type="text"
              placeholder="Novo departamento..."
              value={newDepartamento}
              onChange={(e) => setNewDepartamento(e.target.value)}
              className="add-input"
            />
            <button
              onClick={handleAddDepartamento}
              className="add-button"
              disabled={!selectedCampus}
            >
              Adicionar Departamento
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminCampusDepartamentos;