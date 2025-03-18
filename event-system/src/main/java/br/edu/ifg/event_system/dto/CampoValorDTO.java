package br.edu.ifg.event_system.dto;

public class CampoValorDTO {
    private Long campoId;
    private String valor;

    public CampoValorDTO() {
    }

    public Long getCampoId() {
        return campoId;
    }

    public void setCampoId(Long campoId) {
        this.campoId = campoId;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}