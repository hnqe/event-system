package br.edu.ifg.event_system.dto;

import jakarta.validation.constraints.NotBlank;

public class AdicionarDepartamentoRequestDTO {

    @NotBlank
    private Long departamentoId;

    public AdicionarDepartamentoRequestDTO(Long departamentoId) {
        this.departamentoId = departamentoId;
    }

    public Long getDepartamentoId() {
        return departamentoId;
    }

}