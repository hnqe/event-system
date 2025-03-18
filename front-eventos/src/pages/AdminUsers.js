import React, { useEffect, useState, useCallback } from "react";
import api from "../services/api";
import "./AdminUsers.css";

function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [editUser, setEditUser] = useState(null);
  const [selectedRoles, setSelectedRoles] = useState([]);
  const [campusList, setCampusList] = useState([]);
  const [departamentos, setDepartamentos] = useState([]);
  const [selectedCampus, setSelectedCampus] = useState("");
  const [selectedDepartamento, setSelectedDepartamento] = useState("");
  const [showCampusModal, setShowCampusModal] = useState(false);
  const [showDepartamentoModal, setShowDepartamentoModal] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);
  const [currentUsername, setCurrentUsername] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [currentSearch, setCurrentSearch] = useState("");
  const token = localStorage.getItem("token");

  const fetchUsers = useCallback((pageNum = page, search = currentSearch) => {
    setLoading(true);

    let url = `/admin/users?page=${pageNum}&size=10`;

    if (search && search.trim()) {
      url += `&search=${encodeURIComponent(search.trim())}`;
    }

    api
      .get(url, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      })
      .then((resp) => {
        setUsers(resp.data.content);
        setTotalPages(resp.data.totalPages);
        setError("");
      })
      .catch((err) => {
        console.error("Erro ao carregar usuários:", err);
        setError("Erro ao carregar usuários: " + (err.response?.data || err.message));
      })
      .finally(() => setLoading(false));
  }, [token, page, currentSearch]);

  const fetchCampus = useCallback(async () => {
    try {
      const response = await api.get("/api/campus", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setCampusList(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error("Erro ao carregar campus:", error);
      setError("Erro ao carregar campus: " + (error.response?.data || error.message));
    }
  }, [token]);

  useEffect(() => {
    try {
      const tokenParts = token.split('.');
      if (tokenParts.length === 3) {
        const payload = JSON.parse(atob(tokenParts[1]));
        setCurrentUsername(payload.sub);
      }
    } catch (e) {
      console.error("Erro ao decodificar token", e);
    }
  }, [token]);

  useEffect(() => {
    fetchUsers();
    fetchCampus();
  }, [page, fetchUsers, fetchCampus]);

  useEffect(() => {
    if (currentUsername && users.length > 0) {
      const loggedUser = users.find(u => u.username === currentUsername);
      if (loggedUser) {
        setCurrentUser(loggedUser);
      }
    }
  }, [currentUsername, users]);

  const fetchDepartamentos = async (campusId) => {
    if (!campusId) {
      setDepartamentos([]);
      return;
    }

    try {
      const campus = campusList.find(c => c.id === parseInt(campusId));
      if (campus && campus.departamentos) {
        setDepartamentos(campus.departamentos);
      }
    } catch (error) {
      console.error("Erro ao carregar departamentos:", error);
    }
  };

  const isCurrentUserAdminGeral = () => {
    return currentUser && currentUser.roles.some(r => r.name === "ADMIN_GERAL");
  };

  const canEditUser = (user) => {
    if (user.username === currentUsername) {
      return false;
    }

    if (user.roles.some(r => r.name === "ADMIN_GERAL")) {
      return false;
    }

    return true;
  };

  const showConfirmation = (message, onConfirm) => {
    setConfirmAction({
      message,
      onConfirm
    });
  };

  const handleEditClick = (user) => {
    if (!canEditUser(user)) {
      setActionError("Você não tem permissão para editar este usuário.");
      setTimeout(() => setActionError(""), 3000);
      return;
    }

    const roleNames = user.roles.map(r => r.name);
    setSelectedRoles(roleNames);
    setEditUser(user);
  };

  const handleSaveEdit = () => {
    api
      .patch(`/admin/users/${editUser.id}/roles`, selectedRoles, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      })
      .then(() => {
        const successMessage = document.createElement('div');
        successMessage.textContent = 'Usuário atualizado com sucesso!';
        successMessage.className = 'success-message';
        document.body.appendChild(successMessage);
        setTimeout(() => document.body.removeChild(successMessage), 3000);

        setEditUser(null);
        fetchUsers();
      })
      .catch((err) => {
        console.error("Erro ao atualizar usuário:", err);
        setActionError("Erro ao atualizar usuário: " + (err.response?.data || err.message));
        setTimeout(() => setActionError(""), 5000);
      });
  };

  const handleRoleChange = (e) => {
    const options = Array.from(e.target.selectedOptions, option => option.value);
    setSelectedRoles(options);
  };

  const handleCampusClick = (user) => {
    if (!canEditUser(user)) {
      setActionError("Você não tem permissão para editar os campus deste usuário.");
      setTimeout(() => setActionError(""), 3000);
      return;
    }

    setEditUser(user);
    setShowCampusModal(true);
    setShowDepartamentoModal(false);
  };

  const handleDepartamentoClick = (user) => {
    if (!canEditUser(user)) {
      setActionError("Você não tem permissão para editar os departamentos deste usuário.");
      setTimeout(() => setActionError(""), 3000);
      return;
    }

    setEditUser(user);
    setShowDepartamentoModal(true);
    setShowCampusModal(false);
    setSelectedCampus("");
    setSelectedDepartamento("");
    setDepartamentos([]);
  };

  const handleCampusChange = (e) => {
    setSelectedCampus(e.target.value);
    fetchDepartamentos(e.target.value);
  };

  const handleAddCampus = () => {
    if (!selectedCampus) {
      setActionError("Selecione um campus para adicionar");
      setTimeout(() => setActionError(""), 3000);
      return;
    }

    api
      .post(`/admin/users/${editUser.id}/campus/${selectedCampus}`, {}, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      })
      .then(() => {
        const successMessage = document.createElement('div');
        successMessage.textContent = 'Campus adicionado com sucesso!';
        successMessage.className = 'success-message';
        document.body.appendChild(successMessage);
        setTimeout(() => document.body.removeChild(successMessage), 3000);

        fetchUsers();
        setShowCampusModal(false);
        setEditUser(null);
        setSelectedCampus("");
      })
      .catch((err) => {
        console.error("Erro ao adicionar campus:", err);
        setActionError("Erro ao adicionar campus: " + (err.response?.data || err.message));
        setTimeout(() => setActionError(""), 5000);
      });
  };

  const handleRemoveCampus = (campusId) => {
    showConfirmation("Tem certeza que deseja remover este campus do usuário?", () => {
      api
        .delete(`/admin/users/${editUser.id}/campus/${campusId}`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        .then(() => {
          const successMessage = document.createElement('div');
          successMessage.textContent = 'Campus removido com sucesso!';
          successMessage.className = 'success-message';
          document.body.appendChild(successMessage);
          setTimeout(() => document.body.removeChild(successMessage), 3000);

          fetchUsers();
          setShowCampusModal(false);
          setEditUser(null);
        })
        .catch((err) => {
          console.error("Erro ao remover campus:", err);
          setActionError("Erro ao remover campus: " + (err.response?.data || err.message));
          setTimeout(() => setActionError(""), 5000);
        });
    });
  };

  const handleAddDepartamento = () => {
    if (!selectedDepartamento) {
      setActionError("Selecione um departamento para adicionar");
      setTimeout(() => setActionError(""), 3000);
      return;
    }

    api
      .post(`/admin/users/${editUser.id}/departamentos`,
        { departamentoId: selectedDepartamento },
        {
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        }
      )
      .then(() => {
        const successMessage = document.createElement('div');
        successMessage.textContent = 'Departamento adicionado com sucesso!';
        successMessage.className = 'success-message';
        document.body.appendChild(successMessage);
        setTimeout(() => document.body.removeChild(successMessage), 3000);

        fetchUsers();
        setShowDepartamentoModal(false);
        setEditUser(null);
        setSelectedDepartamento("");
        setSelectedCampus("");
      })
      .catch((err) => {
        console.error("Erro ao adicionar departamento:", err);
        setActionError("Erro ao adicionar departamento: " + (err.response?.data || err.message));
        setTimeout(() => setActionError(""), 5000);
      });
  };

  const handleRemoveDepartamento = (departamentoId) => {
    showConfirmation("Tem certeza que deseja remover este departamento do usuário?", () => {
      api
        .delete(`/admin/users/${editUser.id}/departamentos/${departamentoId}`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        .then(() => {
          const successMessage = document.createElement('div');
          successMessage.textContent = 'Departamento removido com sucesso!';
          successMessage.className = 'success-message';
          document.body.appendChild(successMessage);
          setTimeout(() => document.body.removeChild(successMessage), 3000);

          fetchUsers();
          setShowDepartamentoModal(false);
          setEditUser(null);
        })
        .catch((err) => {
          console.error("Erro ao remover departamento:", err);
          setActionError("Erro ao remover departamento: " + (err.response?.data || err.message));
          setTimeout(() => setActionError(""), 5000);
        });
    });
  };

  const getAvailableCampusForDepartamentos = () => {
    if (isCurrentUserAdminGeral()) {
      return campusList;
    }

    return currentUser?.campusQueAdministro || [];
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setCurrentSearch(searchTerm);
    setPage(0);
    fetchUsers(0, searchTerm);
  };

  const handleClearSearch = () => {
    setSearchTerm("");
    setCurrentSearch("");
    setPage(0);
    fetchUsers(0, "");
  };

  return (
    <div className="admin-users">
      <h2>Gerenciar Usuários</h2>

      <div className="search-form">
        <form onSubmit={handleSearch} className="search-form">
          <div className="search-container">
            <input
              type="text"
              placeholder="Buscar por nome ou e-mail..."
              value={searchTerm}
              onChange={handleSearchChange}
              className="search-input"
            />
            <button type="submit" className="search-button">
              <i className="icon-search"></i>Buscar
            </button>
          </div>

          {currentSearch && (
            <div className="search-info">
              <div>Exibindo resultados para: <span className="search-term">{currentSearch}</span></div>
              <button
                className="clear-search"
                onClick={handleClearSearch}
                title="Limpar busca"
              >
                Limpar
              </button>
            </div>
          )}
        </form>
      </div>

      {actionError && <div className="action-error-message">{actionError}</div>}

      {loading ? (
        <p>Carregando usuários...</p>
      ) : error ? (
        <p className="error-message">{error}</p>
      ) : (
        <>
          <table className="user-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Usuário</th>
                <th>Nome Completo</th>
                <th>Permissões</th>
                <th>Campus</th>
                <th>Departamentos</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {users.length > 0 ? (
                users.map((u) => {
                  const isAdminUser = u.roles.some(r => r.name === "ADMIN_GERAL");
                  const canEdit = canEditUser(u);

                  return (
                    <tr key={u.id} className={isAdminUser ? "admin-geral-row" : ""}>
                      <td>{u.id}</td>
                      <td>{u.username}</td>
                      <td>{u.nomeCompleto}</td>
                      <td>{u.roles.map((r) => r.name).join(", ")}</td>
                      <td>
                        {u.campusQueAdministro && u.campusQueAdministro.length > 0 ? (
                          <ul className="small-list">
                            {u.campusQueAdministro.map(campus => (
                              <li key={campus.id}>{campus.nome}</li>
                            ))}
                          </ul>
                        ) : (
                          "Nenhum"
                        )}
                      </td>
                      <td>
                        {u.departamentosQueAdministro && u.departamentosQueAdministro.length > 0 ? (
                          <ul className="small-list">
                            {u.departamentosQueAdministro.map(depto => (
                              <li key={depto.id}>{depto.nome}</li>
                            ))}
                          </ul>
                        ) : (
                          "Nenhum"
                        )}
                      </td>
                      <td className="action-buttons">
                        {canEdit ? (
                          <>
                            {isCurrentUserAdminGeral() && (
                              <button onClick={() => handleEditClick(u)} className="edit-btn">
                                Editar Roles
                              </button>
                            )}
                            {isCurrentUserAdminGeral() && (
                              <button onClick={() => handleCampusClick(u)} className="campus-btn">
                                Campus
                              </button>
                            )}
                            <button onClick={() => handleDepartamentoClick(u)} className="depto-btn">
                              Departamentos
                            </button>
                          </>
                        ) : (
                          <span className="restricted-access">Acesso restrito</span>
                        )}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="7">Nenhum usuário encontrado.</td>
                </tr>
              )}
            </tbody>
          </table>

          <div className="pagination">
            <button
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              className="pagination-btn"
            >
              Anterior
            </button>
            <span>
              Página {page + 1} de {totalPages || 1}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
              className="pagination-btn"
            >
              Próxima
            </button>
          </div>
        </>
      )}

      {editUser && !showCampusModal && !showDepartamentoModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Editar Permissões do Usuário</h2>
            <p><strong>Nome:</strong> {editUser.nomeCompleto}</p>
            <p><strong>Email:</strong> {editUser.username}</p>

            {actionError && <div className="action-error-message">{actionError}</div>}

            <label>Permissões:</label>
            <select
              multiple
              value={selectedRoles}
              onChange={handleRoleChange}
              className="roles-select"
            >
              <option value="USER">USER</option>
              <option value="ADMIN_CAMPUS">ADMIN_CAMPUS</option>
              <option value="ADMIN_DEPARTAMENTO">ADMIN_DEPARTAMENTO</option>
            </select>
            <p className="help-text">Mantenha Ctrl pressionado para selecionar múltiplas opções</p>
            <div className="modal-buttons">
              <button onClick={handleSaveEdit} className="save-btn">
                Salvar
              </button>
              <button onClick={() => setEditUser(null)} className="close-btn">
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      {editUser && showCampusModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Gerenciar Campus do Usuário</h2>
            <p><strong>Usuário:</strong> {editUser.nomeCompleto}</p>

            {actionError && <div className="action-error-message">{actionError}</div>}

            <div className="current-items">
              <h3>Campus Atuais:</h3>
              {editUser.campusQueAdministro && editUser.campusQueAdministro.length > 0 ? (
                <ul className="item-list">
                  {editUser.campusQueAdministro.map(campus => (
                    <li key={campus.id}>
                      {campus.nome}
                      <button
                        onClick={() => handleRemoveCampus(campus.id)}
                        className="remove-btn-small"
                      >
                        Remover
                      </button>
                    </li>
                  ))}
                </ul>
              ) : (
                <p>Nenhum campus atribuído a este usuário.</p>
              )}
            </div>

            <div className="add-new-item">
              <h3>Adicionar Novo Campus:</h3>
              <select
                value={selectedCampus}
                onChange={handleCampusChange}
                className="item-select"
              >
                <option value="">Selecione um campus</option>
                {campusList.map(campus => (
                  <option key={campus.id} value={campus.id}>
                    {campus.nome}
                  </option>
                ))}
              </select>
              <button onClick={handleAddCampus} className="add-btn">
                Adicionar Campus
              </button>
            </div>

            <div className="modal-buttons">
              <button
                onClick={() => {
                  setShowCampusModal(false);
                  setEditUser(null);
                  setSelectedCampus("");
                  setActionError("");
                }}
                className="close-btn"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {editUser && showDepartamentoModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Gerenciar Departamentos do Usuário</h2>
            <p><strong>Usuário:</strong> {editUser.nomeCompleto}</p>

            {actionError && <div className="action-error-message">{actionError}</div>}

            <div className="current-items">
              <h3>Departamentos Atuais:</h3>
              {editUser.departamentosQueAdministro && editUser.departamentosQueAdministro.length > 0 ? (
                <ul className="item-list">
                  {editUser.departamentosQueAdministro.map(depto => (
                    <li key={depto.id}>
                      {depto.nome}
                      <button
                        onClick={() => handleRemoveDepartamento(depto.id)}
                        className="remove-btn-small"
                      >
                        Remover
                      </button>
                    </li>
                  ))}
                </ul>
              ) : (
                <p>Nenhum departamento atribuído a este usuário.</p>
              )}
            </div>

            <div className="add-new-item">
              <h3>Adicionar Novo Departamento:</h3>
              <select
                value={selectedCampus}
                onChange={handleCampusChange}
                className="item-select"
              >
                <option value="">Selecione um campus primeiro</option>
                {getAvailableCampusForDepartamentos().map(campus => (
                  <option key={campus.id} value={campus.id}>
                    {campus.nome}
                  </option>
                ))}
              </select>

              <select
                value={selectedDepartamento}
                onChange={(e) => setSelectedDepartamento(e.target.value)}
                className="item-select"
                disabled={!selectedCampus}
              >
                <option value="">Selecione um departamento</option>
                {departamentos.map(depto => (
                  <option key={depto.id} value={depto.id}>
                    {depto.nome}
                  </option>
                ))}
              </select>

              <button
                onClick={handleAddDepartamento}
                className="add-btn"
                disabled={!selectedDepartamento}
              >
                Adicionar Departamento
              </button>
            </div>

            <div className="modal-buttons">
              <button
                onClick={() => {
                  setShowDepartamentoModal(false);
                  setEditUser(null);
                  setSelectedCampus("");
                  setSelectedDepartamento("");
                  setActionError("");
                }}
                className="close-btn"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmAction && (
        <div className="modal-overlay">
          <div className="modal-content confirmation-modal">
            <h2>Confirmação</h2>
            <p>{confirmAction.message}</p>
            <div className="modal-buttons">
              <button
                onClick={() => {
                  confirmAction.onConfirm();
                  setConfirmAction(null);
                }}
                className="save-btn"
              >
                Confirmar
              </button>
              <button
                onClick={() => setConfirmAction(null)}
                className="close-btn"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminUsers;