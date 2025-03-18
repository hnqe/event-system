package br.edu.ifg.event_system.dto;

import br.edu.ifg.event_system.model.CampoValor;
import br.edu.ifg.event_system.model.Inscricao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InscricaoResponseDTO {
    private Long id;
    private Long userId;
    private String nomeUsuario;
    private Long eventoId;
    private String tituloEvento;
    private LocalDateTime dataInscricao;
    private String status;
    private List<CampoValorResponseDTO> camposValores = new ArrayList<>();

    public InscricaoResponseDTO() {
    }

    public InscricaoResponseDTO(Inscricao inscricao) {
        this.id = inscricao.getId();
        this.userId = inscricao.getUser().getId();
        this.nomeUsuario = inscricao.getUser().getNomeCompleto();
        this.eventoId = inscricao.getEvento().getId();
        this.tituloEvento = inscricao.getEvento().getTitulo();
        this.dataInscricao = inscricao.getDataInscricao();
        this.status = inscricao.getStatus();

        if (inscricao.getCamposValores() != null) {
            this.camposValores = inscricao.getCamposValores().stream()
                    .map(CampoValorResponseDTO::new)
                    .toList();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public String getTituloEvento() {
        return tituloEvento;
    }

    public void setTituloEvento(String tituloEvento) {
        this.tituloEvento = tituloEvento;
    }

    public LocalDateTime getDataInscricao() {
        return dataInscricao;
    }

    public void setDataInscricao(LocalDateTime dataInscricao) {
        this.dataInscricao = dataInscricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CampoValorResponseDTO> getCamposValores() {
        return camposValores;
    }

    public void setCamposValores(List<CampoValorResponseDTO> camposValores) {
        this.camposValores = camposValores;
    }

}