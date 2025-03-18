package br.edu.ifg.event_system.dto;

public class CampoValorDTO {
    private Long campoId;
    private String valor;

    public CampoValorDTO() {
        // This constructor is intentionally empty because it's required by
        // JSON/XML serialization frameworks (Jackson, JAXB) for object deserialization.
        // Framework uses reflection to create an instance and then populate fields.
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