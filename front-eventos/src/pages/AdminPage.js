import React from "react";
import { Routes, Route, NavLink } from "react-router-dom";
import AdminHome from "./AdminHome";
import AdminUsers from "./AdminUsers";
import AdminEventos from "./AdminEventos";
import AdminEventoInscritos from './AdminEventoInscritos';
import CriarEvento from "./CriarEvento";
import EditarEvento from "./EditarEvento";
import AdminCampusDepartamentos from "./AdminCampusDepartamentos";
import "./AdminPage.css";

function AdminPage({ userRoles }) {
  const canManageUsers = userRoles.some(role => [
    'ADMIN_GERAL',
    'ADMIN_CAMPUS'
  ].includes(role));

  const canManageCampusAndDepartments = userRoles.some(role => [
    'ADMIN_GERAL',
    'ADMIN_CAMPUS',
    'ADMIN_DEPARTAMENTO'
  ].includes(role));

  return (
    <div className="admin-container">
      <h1 className="admin-title">Área Administrativa</h1>

      <nav className="admin-nav">
        <NavLink to="/admin" end className={({ isActive }) => (isActive ? "active-link" : "")}>
          Início
        </NavLink>

        {canManageUsers && (
          <NavLink to="/admin/usuarios" className={({ isActive }) => (isActive ? "active-link" : "")}>
            Gerenciar Usuários
          </NavLink>
        )}

        <NavLink to="/admin/eventos" className={({ isActive }) => (isActive ? "active-link" : "")}>
          Gerenciar Eventos
        </NavLink>

        {canManageCampusAndDepartments && (
          <NavLink to="/admin/campus-departamentos" className={({ isActive }) => (isActive ? "active-link" : "")}>
            Gerenciar Campus e Departamentos
          </NavLink>
        )}
      </nav>

      <div className="admin-content">
        <Routes>
          <Route index element={<AdminHome />} />
          {canManageUsers && <Route path="usuarios" element={<AdminUsers />} />}
          <Route path="eventos" element={<AdminEventos />} />
          <Route path="eventos/:id/inscritos" element={<AdminEventoInscritos />} />
          <Route path="eventos/novo" element={<CriarEvento />} />
          <Route path="eventos/:id/editar" element={<EditarEvento />} />
          {canManageCampusAndDepartments && <Route path="campus-departamentos" element={<AdminCampusDepartamentos />} />}
        </Routes>
      </div>
    </div>
  );
}

export default AdminPage;