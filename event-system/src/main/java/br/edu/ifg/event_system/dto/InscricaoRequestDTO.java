package br.edu.ifg.event_system.dto;

import java.util.ArrayList;
import java.util.List;

public class InscricaoRequestDTO {
    private Long eventoId;
    private List<CampoValorDTO> camposValores = new ArrayList<>();

    public InscricaoRequestDTO() {
        // This constructor is intentionally empty because it's required by
        // Java Bean specification and JSON frameworks. The camposValores field
        // is already initialized with a new ArrayList<>() at declaration.
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