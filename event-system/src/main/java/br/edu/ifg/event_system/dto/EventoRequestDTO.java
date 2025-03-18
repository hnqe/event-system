package br.edu.ifg.event_system.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventoRequestDTO {
    private String titulo;
    private String descricao;
    private String local;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private LocalDateTime dataLimiteInscricao;
    private Long campusId;
    private Long departamentoId;
    private Integer vagas;
    private Boolean estudanteIfg;
    private List<CampoAdicionalDTO> camposAdicionais = new ArrayList<>();

    /**
     * Default no-args constructor.
     *
     * This constructor is intentionally empty because:
     * 1. It's required by JSON/XML serialization frameworks (e.g., Jackson, JAXB)
     * 2. Most fields are initialized with their default values (null)
     * 3. The camposAdicionais field is explicitly initialized with a new ArrayList<>()
     * 4. The object will be populated through setter methods or directly by
     *    deserialization frameworks during HTTP request processing
     */
    public EventoRequestDTO() {
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
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

    public LocalDateTime getDataLimiteInscricao() {
        return dataLimiteInscricao;
    }

    public void setDataLimiteInscricao(LocalDateTime dataLimiteInscricao) {
        this.dataLimiteInscricao = dataLimiteInscricao;
    }

    public Long getCampusId() {
        return campusId;
    }

    public void setCampusId(Long campusId) {
        this.campusId = campusId;
    }

    public Long getDepartamentoId() {
        return departamentoId;
    }

    public void setDepartamentoId(Long departamentoId) {
        this.departamentoId = departamentoId;
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

    public List<CampoAdicionalDTO> getCamposAdicionais() {
        return camposAdicionais;
    }

    public void setCamposAdicionais(List<CampoAdicionalDTO> camposAdicionais) {
        this.camposAdicionais = camposAdicionais;
    }

}