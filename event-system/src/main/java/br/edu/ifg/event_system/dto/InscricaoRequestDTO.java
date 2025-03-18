package br.edu.ifg.event_system.dto;

import java.util.ArrayList;
import java.util.List;

public class InscricaoRequestDTO {
    private Long eventoId;
    private List<CampoValorDTO> camposValores = new ArrayList<>();

    /**
     * Default no-args constructor.
     *
     * This constructor is intentionally empty because:
     * 1. It's required by Java Bean specification and JSON deserialization frameworks
     *    (e.g., Jackson) to create an instance before populating its properties
     * 2. The eventoId field is initialized with its default value (null)
     * 3. The camposValores field is already initialized with a new ArrayList<>() at
     *    declaration to ensure it's never null, simplifying usage of this DTO
     * 4. This DTO is designed to receive data from HTTP requests and will be populated
     *    by the framework automatically during request processing
     */
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