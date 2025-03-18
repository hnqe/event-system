package br.edu.ifg.event_system.dto;

import jakarta.validation.constraints.NotBlank;

public class AdicionarDepartamentoRequestDTO {

    @NotBlank
    private Long departamentoId;

    public AdicionarDepartamentoRequestDTO(){
    }

    public AdicionarDepartamentoRequestDTO(Long departamentoId) {
        this.departamentoId = departamentoId;
    }

    public Long getDepartamentoId() {
        return departamentoId;
    }

    public void setDepartamentoId(@NotBlank Long departamentoId) {
        this.departamentoId = departamentoId;
    }

}