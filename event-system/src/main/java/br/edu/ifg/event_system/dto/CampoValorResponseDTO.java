package br.edu.ifg.event_system.dto;

import br.edu.ifg.event_system.model.CampoValor;

public class CampoValorResponseDTO {
    private Long id;
    private Long campoId;
    private String nomeCampo;
    private String tipoCampo;
    private String valor;

    public CampoValorResponseDTO() {
    }

    public CampoValorResponseDTO(CampoValor campoValor) {
        this.id = campoValor.getId();
        this.campoId = campoValor.getCampo().getId();
        this.nomeCampo = campoValor.getCampo().getNome();
        this.tipoCampo = campoValor.getCampo().getTipo();
        this.valor = campoValor.getValor();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampoId() {
        return campoId;
    }

    public void setCampoId(Long campoId) {
        this.campoId = campoId;
    }

    public String getNomeCampo() {
        return nomeCampo;
    }

    public void setNomeCampo(String nomeCampo) {
        this.nomeCampo = nomeCampo;
    }

    public String getTipoCampo() {
        return tipoCampo;
    }

    public void setTipoCampo(String tipoCampo) {
        this.tipoCampo = tipoCampo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

}