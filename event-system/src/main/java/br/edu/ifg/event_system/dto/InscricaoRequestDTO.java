package br.edu.ifg.event_system.dto;

import java.util.ArrayList;
import java.util.List;

public class InscricaoRequestDTO {
    private Long eventoId;
    private List<CampoValorDTO> camposValores = new ArrayList<>();

    public InscricaoRequestDTO() {
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public List<CampoValorDTO> getCamposValores() {
        return camposValores;
    }

    public void setCamposValores(List<CampoValorDTO> camposValores) {
        this.camposValores = camposValores;
    }

}