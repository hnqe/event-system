package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.dto.CampoValorDTO;
import br.edu.ifg.event_system.dto.InscricaoRequestDTO;
import br.edu.ifg.event_system.exception.InscricaoException;
import br.edu.ifg.event_system.model.*;
import br.edu.ifg.event_system.repository.CampoAdicionalRepository;
import br.edu.ifg.event_system.repository.InscricaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final CampoAdicionalRepository campoAdicionalRepository;

    public InscricaoService(InscricaoRepository inscricaoRepository,
                            CampoAdicionalRepository campoAdicionalRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.campoAdicionalRepository = campoAdicionalRepository;
    }

    @Transactional
    public Inscricao inscreverUsuarioEmEvento(User user, Evento evento) {
        return inscreverUsuarioEmEvento(user, evento, null);
    }

    @Transactional
    public Inscricao inscreverUsuarioEmEvento(User user, Evento evento, List<CampoValorDTO> camposValores) {
        if (evento.getDataLimiteInscricao() != null) {
            if (LocalDateTime.now().isAfter(evento.getDataLimiteInscricao())) {
                throw new InscricaoException("As inscrições para este evento já foram encerradas!");
            }
        }

        Optional<Inscricao> existente = inscricaoRepository.findByUserIdAndEventoId(user.getId(), evento.getId());
        if (existente.isPresent() && "ATIVA".equalsIgnoreCase(existente.get().getStatus())) {
            throw new InscricaoException("Você já está inscrito neste evento!");
        }

        if (evento.getVagas() != null) {
            long inscritosNoEvento = inscricaoRepository.countByEventoId(evento.getId());
            if (inscritosNoEvento >= evento.getVagas()) {
                throw new InscricaoException("Não há vagas disponíveis neste evento!");
            }
        }

        Inscricao inscricao;
        if (existente.isPresent()) {
            inscricao = existente.get();
            inscricao.setStatus("ATIVA");
            inscricao.setDataInscricao(LocalDateTime.now());

            inscricao.getCamposValores().clear();
        } else {
            inscricao = new Inscricao(user, evento, LocalDateTime.now(), "ATIVA");
        }

        if (camposValores != null && !camposValores.isEmpty()) {
            processarCamposValores(inscricao, evento, camposValores);
        }

        return inscricaoRepository.save(inscricao);
    }

    private void processarCamposValores(Inscricao inscricao, Evento evento, List<CampoValorDTO> camposValores) {
        List<CampoAdicional> camposEvento = evento.getCamposAdicionais();

        Map<Long, CampoAdicional> camposMap = new HashMap<>();
        for (CampoAdicional campo : camposEvento) {
            camposMap.put(campo.getId(), campo);
        }

        List<CampoAdicional> camposObrigatorios = camposEvento.stream()
                .filter(c -> Boolean.TRUE.equals(c.getObrigatorio()))
                .toList();

        Set<Long> camposObrigatoriosPreenchidos = camposValores.stream()
                .map(CampoValorDTO::getCampoId)
                .filter(id -> camposMap.containsKey(id) && Boolean.TRUE.equals(camposMap.get(id).getObrigatorio()))
                .collect(Collectors.toSet());

        if (camposObrigatorios.size() > camposObrigatoriosPreenchidos.size()) {
            List<String> camposFaltantes = camposObrigatorios.stream()
                    .filter(c -> !camposObrigatoriosPreenchidos.contains(c.getId()))
                    .map(CampoAdicional::getNome)
                    .collect(Collectors.toList());

            throw new InscricaoException("Os seguintes campos obrigatórios não foram preenchidos: " +
                    String.join(", ", camposFaltantes));
        }

        for (CampoValorDTO valorDTO : camposValores) {
            CampoAdicional campo = camposMap.get(valorDTO.getCampoId());
            if (campo != null) {
                CampoValor campoValor = new CampoValor();
                campoValor.setCampo(campo);
                campoValor.setValor(valorDTO.getValor());
                campoValor.setInscricao(inscricao);
                inscricao.getCamposValores().add(campoValor);
            }
        }
    }

    @Transactional
    public Inscricao processarInscricao(User user, InscricaoRequestDTO request, Evento evento) {
        return inscreverUsuarioEmEvento(user, evento, request.getCamposValores());
    }

    @Transactional
    public void cancelarInscricao(Long inscricaoId) {
        Inscricao insc = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new InscricaoException("Inscrição não encontrada!"));
        insc.setStatus("CANCELADA");
        inscricaoRepository.save(insc);
    }

    public List<Inscricao> listarInscricoesDoUsuario(Long userId) {
        return inscricaoRepository.findByUserId(userId);
    }

    public List<Inscricao> listarInscricoesDoEvento(Long eventoId) {
        return inscricaoRepository.findByEventoId(eventoId);
    }

    public Inscricao buscarPorId(Long inscricaoId) {
        return inscricaoRepository.findById(inscricaoId).orElse(null);
    }

}