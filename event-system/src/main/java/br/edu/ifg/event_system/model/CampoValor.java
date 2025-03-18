package br.edu.ifg.event_system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "campo_valor")
public class CampoValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscricao_id", nullable = false)
    @JsonIgnore
    private Inscricao inscricao;

    @ManyToOne
    @JoinColumn(name = "campo_id", nullable = false)
    private CampoAdicional campo;

    @Column(length = 1000)
    private String valor;

    public CampoValor() {
        // This constructor is intentionally empty because JPA specification
        // requires entities to have a no-args constructor. The persistence
        // provider uses this to create instances via reflection.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inscricao getInscricao() {
        return inscricao;
    }

    public void setInscricao(Inscricao inscricao) {
        this.inscricao = inscricao;
    }

    public CampoAdicional getCampo() {
        return campo;
    }

    public void setCampo(CampoAdicional campo) {
        this.campo = campo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

}