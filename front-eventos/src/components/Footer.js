import React from "react";
import "./Footer.css";

const Footer = () => {
  return (
    <footer className="footer">
      <div className="footer-container">
        <div className="footer-info">
          <h3>Instituto Federal de Goiás</h3>
          <p>Rua C-198, Quadra 500, Jardim América</p>
          <p>Goiânia, Goiás, Brasil</p>
          <p>CEP: 74270-040 - Tel.: (62) 3612-2200</p>
        </div>

        <div className="footer-links">
          <a href="https://facebook.com/IFG.oficial?_rdr=p" target="_blank" rel="noopener noreferrer">
            <img src="/facebook.png" alt="Facebook" className="social-icon" />
          </a>
          <a href="https://x.com/IFG_Goias" target="_blank" rel="noopener noreferrer">
            <img src="/x.png" alt="X" className="social-icon" />
          </a>
          <a href="https://instagram.com/ifg_oficial/" target="_blank" rel="noopener noreferrer">
            <img src="/instagram.png" alt="Instagram" className="social-icon" />
          </a>
        </div>

        <div className="footer-copyright">
          <p>
            <a href="https://ifg.edu.br" target="_blank" rel="noopener noreferrer">
              www.ifg.edu.br
            </a>
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;