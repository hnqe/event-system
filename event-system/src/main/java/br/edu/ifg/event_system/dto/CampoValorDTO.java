package br.edu.ifg.event_system.dto;

public class CampoValorDTO {
    private Long campoId;
    private String valor;

    /**
     * Default no-args constructor.
     *
     * This constructor is intentionally empty because:
     * 1. It's required for frameworks like Jackson to deserialize JSON into this object
     * 2. Fields are already initialized with their default values (null)
     * 3. The object is expected to be populated after creation via setter methods
     */
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