import React, { useState } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import HomePage from "./pages/HomePage";
import AdminPage from "./pages/AdminPage";
import UnauthorizedPage from "./pages/UnauthorizedPage";
import ProtectedAdminRoute from "./components/ProtectedAdminRoute";
import Header from "./components/Header";
import Footer from "./components/Footer";
import LoginModal from "./components/LoginModal";
import RegisterModal from "./components/RegisterModal";
import { NotificationManager } from "./components/Notification";
import { AuthProvider, useAuth } from "./contexts/AuthContext";
import "./App.css";

function AppWithAuth() {
  return (
    <Router>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </Router>
  );
}

function AppContent() {
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [homeKey, setHomeKey] = useState(0);

  const { isLogged, loading, userRoles, login, logout } = useAuth();

  const handleLoginSuccess = (token) => {
    login(token);
    setShowLoginModal(false);
    setShowRegisterModal(false);
    setHomeKey(prevKey => prevKey + 1);
  };

  if (loading) {
    return <div className="loading-container">Carregando...</div>;
  }

  return (
    <div className="main-container">
      <Header
        isLogged={isLogged}
        onLogout={logout}
        onOpenLogin={() => setShowLoginModal(true)}
        onLoginSuccess={handleLoginSuccess}
      />

      <div className="content">
        <Routes>
          <Route
            path="/"
            element={
              <HomePage
                key={homeKey}
                onOpenLoginModal={() => setShowLoginModal(true)}
                isLogged={isLogged}
              />
            }
          />

          <Route path="/unauthorized" element={<UnauthorizedPage />} />

          <Route element={<ProtectedAdminRoute redirectPath="/unauthorized" />}>
            <Route path="/admin/*" element={<AdminPage userRoles={userRoles} />} />
          </Route>
        </Routes>
      </div>

      <Footer />

      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onLoginSuccess={handleLoginSuccess}
        onOpenRegister={() => {
          setShowLoginModal(false);
          setShowRegisterModal(true);
        }}
      />

      <RegisterModal
        isOpen={showRegisterModal}
        onClose={() => setShowRegisterModal(false)}
        onRegisterSuccess={handleLoginSuccess}
        onOpenLogin={() => {
          setShowRegisterModal(false);
          setShowLoginModal(true);
        }}
      />

      <NotificationManager />
    </div>
  );
}

export default AppWithAuth;