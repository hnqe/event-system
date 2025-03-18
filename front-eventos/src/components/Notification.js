import React, { useState, useEffect } from 'react';
import './Notification.css';

const Notification = ({ type, message, duration = 4000, onClose }) => {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setVisible(false);
      if (onClose) setTimeout(onClose, 300);
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const handleClose = () => {
    setVisible(false);
    if (onClose) setTimeout(onClose, 300);
  };

  const getIcon = () => {
    switch (type) {
      case 'success':
        return <i className="fa fa-check-circle"></i>;
      case 'error':
        return <i className="fa fa-exclamation-circle"></i>;
      case 'info':
        return <i className="fa fa-info-circle"></i>;
      case 'warning':
        return <i className="fa fa-exclamation-triangle"></i>;
      default:
        return null;
    }
  };

  return (
    <div className={`notification ${type} ${visible ? 'visible' : 'hidden'}`}>
      <div className="notification-content">
        <div className="notification-icon">
          {getIcon()}
        </div>
        <div className="notification-message">
          {message}
        </div>
        <button className="notification-close" onClick={handleClose}>
          ×
        </button>
      </div>
    </div>
  );
};

export const NotificationConfirm = ({ message, onConfirm, onCancel }) => {
  return (
    <div className="notification confirm visible">
      <div className="notification-content">
        <div className="notification-icon">
          <i className="fa fa-question-circle"></i>
        </div>
        <div className="notification-message">
          {message}
        </div>
        <div className="notification-actions">
          <button className="btn-cancel" onClick={onCancel}>Cancelar</button>
          <button className="btn-confirm" onClick={onConfirm}>Confirmar</button>
        </div>
      </div>
    </div>
  );
};

export const NotificationManager = () => {
  const [notifications, setNotifications] = useState([]);
  const [confirmProps, setConfirmProps] = useState(null);

  useEffect(() => {
    window.showNotification = (type, message, duration) => {
      const id = Date.now();
      setNotifications(prev => [...prev, { id, type, message, duration }]);
      return id;
    };

    window.showConfirmation = (message, onConfirm, onCancel) => {
      setConfirmProps({
        message,
        onConfirm: () => {
          onConfirm();
          setConfirmProps(null);
        },
        onCancel: () => {
          if (onCancel) onCancel();
          setConfirmProps(null);
        }
      });
    };

    window.closeNotification = (id) => {
      setNotifications(prev => prev.filter(n => n.id !== id));
    };

    return () => {
      delete window.showNotification;
      delete window.showConfirmation;
      delete window.closeNotification;
    };
  }, []);

  return (
    <>
      <div className="notification-container">
        {notifications.map(({ id, type, message, duration }) => (
          <Notification
            key={id}
            type={type}
            message={message}
            duration={duration}
            onClose={() => setNotifications(prev => prev.filter(n => n.id !== id))}
          />
        ))}
      </div>

      {confirmProps && (
        <div className="notification-backdrop">
          <NotificationConfirm {...confirmProps} />
        </div>
      )}
    </>
  );
};

export default Notification;