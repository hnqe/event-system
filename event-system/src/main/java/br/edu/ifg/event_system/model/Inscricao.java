package br.edu.ifg.event_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inscricao",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_inscricao_user_evento",
                        columnNames = {"user_id", "evento_id"})
        }
)
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(nullable = false)
    private LocalDateTime dataInscricao;

    @Column(nullable = false, length = 20)
    private String status;

    @OneToMany(mappedBy = "inscricao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampoValor> camposValores = new ArrayList<>();

    public Inscricao() {
    }

    public Inscricao(User user, Evento evento, LocalDateTime dataInscricao, String status) {
        this.user = user;
        this.evento = evento;
        this.dataInscricao = dataInscricao;
        this.status = status;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public Evento getEvento() {
        return evento;
    }
    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public LocalDateTime getDataInscricao() {
        return dataInscricao;
    }
    public void setDataInscricao(LocalDateTime dataInscricao) {
        this.dataInscricao = dataInscricao;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public List<CampoValor> getCamposValores() {
        return camposValores;
    }
    public void setCamposValores(List<CampoValor> camposValores) {
        this.camposValores = camposValores;
    }

    public void addCampoValor(CampoValor campoValor) {
        camposValores.add(campoValor);
        campoValor.setInscricao(this);
    }
    public void removeCampoValor(CampoValor campoValor) {
        camposValores.remove(campoValor);
        campoValor.setInscricao(null);
    }

}