package br.edu.ifg.event_system.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private LocalDateTime dataInicio;

    private LocalDateTime dataFim;

    @Column(nullable = false)
    private String local;

    @Column(length = 5000)
    private String descricao;

    private LocalDateTime dataLimiteInscricao;

    private Integer vagas;

    private Boolean estudanteIfg;

    @Enumerated(EnumType.STRING)
    private EventoStatus status = EventoStatus.ATIVO;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampoAdicional> camposAdicionais = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @ManyToOne
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    public Evento() {
    }

    public Evento(String titulo, LocalDateTime dataInicio, LocalDateTime dataFim,
                  Campus campus, Departamento departamento) {
        this.titulo = titulo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.campus = campus;
        this.departamento = departamento;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public Integer getVagas() {
        return vagas;
    }

    public void setVagas(Integer vagas) {
        this.vagas = vagas;
    }

    public Boolean getEstudanteIfg() {
        return estudanteIfg;
    }

    public void setEstudanteIfg(Boolean estudanteIfg) {
        this.estudanteIfg = estudanteIfg;
    }

    public LocalDateTime getDataLimiteInscricao() {
        return dataLimiteInscricao;
    }

    public void setDataLimiteInscricao(LocalDateTime dataLimiteInscricao) {
        this.dataLimiteInscricao = dataLimiteInscricao;
    }

    public enum EventoStatus {
        ATIVO,
        ENCERRADO
    }

    public EventoStatus getStatus() {
        return status;
    }

    public void setStatus(EventoStatus status) {
        this.status = status;
    }

    public List<CampoAdicional> getCamposAdicionais() {
        return camposAdicionais;
    }

    public void setCamposAdicionais(List<CampoAdicional> camposAdicionais) {
        this.camposAdicionais = camposAdicionais;
    }

}