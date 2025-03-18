package br.edu.ifg.event_system.dto;

public class DepartamentoRequestDTO {
    private String nome;
    private Long campusId;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getCampusId() {
        return campusId;
    }

    public void setCampusId(Long campusId) {
        this.campusId = campusId;
    }

}
