package br.edu.ifg.event_system.util;

import br.edu.ifg.event_system.dto.CampoAdicionalDTO;
import br.edu.ifg.event_system.dto.EventoRequestDTO;
import br.edu.ifg.event_system.model.*;
import br.edu.ifg.event_system.repository.CampoAdicionalRepository;
import br.edu.ifg.event_system.repository.CampoValorRepository;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.DepartamentoService;
import br.edu.ifg.event_system.service.EventoService;
import br.edu.ifg.event_system.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EventoUtils {

    public static class EventoValidationData {
        private Campus campus;
        private Departamento departamento;
        private User usuarioLogado;

        public Campus getCampus() {
            return campus;
        }
        public void setCampus(Campus campus) {
            this.campus = campus;
        }

        public Departamento getDepartamento() {
            return departamento;
        }
        public void setDepartamento(Departamento departamento) {
            this.departamento = departamento;
        }

        public User getUsuarioLogado() {
            return usuarioLogado;
        }
        public void setUsuarioLogado(User usuarioLogado) {
            this.usuarioLogado = usuarioLogado;
        }
    }

    public static ResponseEntity<?> validarDadosIniciais(EventoRequestDTO request,
                                                         CampusService campusService,
                                                         DepartamentoService departamentoService,
                                                         UserService userService)
    {
        Campus campus = campusService.buscarPorId(request.getCampusId());
        Departamento departamento = departamentoService.buscarPorId(request.getDepartamentoId());
        if (campus == null || departamento == null) {
            return ResponseEntity.badRequest().body("Campus ou Departamento inválido.");
        }

        if (!departamento.getCampus().getId().equals(campus.getId())) {
            return ResponseEntity.badRequest()
                    .body("Departamento não pertence ao Campus informado.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nenhum usuário logado.");
        }
        User usuarioLogado = userService.buscarPorUsername(auth.getName());
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não encontrado.");
        }

        if (!isAdminGeral(usuarioLogado)) {
            boolean isAdminCampus = usuarioLogado.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ADMIN_CAMPUS"));

            if (isAdminCampus) {
                boolean gerenciaCampus = usuarioLogado.getCampusQueAdministro().stream()
                        .anyMatch(c -> c.getId().equals(campus.getId()));
                if (!gerenciaCampus) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Você não gerencia o campus deste departamento.");
                }
            } else {
                boolean gerenciaDepto = usuarioLogado.getDepartamentosQueAdministro().stream()
                        .anyMatch(d -> d.getId().equals(departamento.getId()));
                if (!gerenciaDepto) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Você não gerencia este departamento.");
                }
            }
        }

        LocalDateTime dtInicio = request.getDataInicio();
        LocalDateTime dtFim = request.getDataFim();
        if (dtInicio != null && dtFim != null) {
            if (dtFim.isBefore(dtInicio)) {
                return ResponseEntity.badRequest().body("Data fim não pode ser anterior à data início.");
            }
        }

        EventoValidationData result = new EventoValidationData();
        result.setCampus(campus);
        result.setDepartamento(departamento);
        result.setUsuarioLogado(usuarioLogado);
        return ResponseEntity.ok(result);
    }

    @Transactional
    public static ResponseEntity<?> persistirEvento(Evento evento,
                                                    EventoRequestDTO request,
                                                    EventoValidationData data,
                                                    EventoService eventoService,
                                                    CampoAdicionalRepository campoAdicionalRepository)
    {
        return persistirEvento(evento, request, data, eventoService, campoAdicionalRepository, null);
    }

    @Transactional
    public static ResponseEntity<?> persistirEvento(Evento evento,
                                                    EventoRequestDTO request,
                                                    EventoValidationData data,
                                                    EventoService eventoService,
                                                    CampoAdicionalRepository campoAdicionalRepository,
                                                    CampoValorRepository campoValorRepository)
    {
        evento.setTitulo(request.getTitulo());
        evento.setDataInicio(request.getDataInicio());
        evento.setDataFim(request.getDataFim());
        evento.setCampus(data.getCampus());
        evento.setDepartamento(data.getDepartamento());
        evento.setLocal(request.getLocal());
        evento.setDescricao(request.getDescricao());
        evento.setDataLimiteInscricao(request.getDataLimiteInscricao());
        evento.setVagas(request.getVagas());
        evento.setEstudanteIfg(request.getEstudanteIfg());

        Evento salvo = eventoService.criarOuAtualizar(evento);

        // Processamento de campos adicionais preservando dados
        if (evento.getId() != null) {
            // Se o evento já existe, precisamos atualizar os campos adicionais
            processarCamposAdicionais(salvo, request, campoAdicionalRepository, campoValorRepository);
        } else {
            // Criar campos adicionais para novos eventos
            if (request.getCamposAdicionais() != null && !request.getCamposAdicionais().isEmpty()) {
                List<CampoAdicional> novosCampos = new ArrayList<>();

                for (CampoAdicionalDTO campoDTO : request.getCamposAdicionais()) {
                    CampoAdicional campo = new CampoAdicional();
                    campo.setNome(campoDTO.getNome());
                    campo.setTipo(campoDTO.getTipo());
                    campo.setDescricao(campoDTO.getDescricao());
                    campo.setObrigatorio(campoDTO.getObrigatorio());
                    campo.setOpcoes(campoDTO.getOpcoes());
                    campo.setEvento(salvo);

                    novosCampos.add(campo);
                }

                campoAdicionalRepository.saveAll(novosCampos);
            }
        }

        return ResponseEntity.ok(eventoService.buscarPorId(salvo.getId()));
    }

    /**
     * Processa a atualização dos campos adicionais de um evento, preservando os dados existentes.
     */
    private static void processarCamposAdicionais(Evento evento,
                                                  EventoRequestDTO request,
                                                  CampoAdicionalRepository campoAdicionalRepository,
                                                  CampoValorRepository campoValorRepository) {
        // Obter campos atuais do evento
        List<CampoAdicional> camposAtuais = campoAdicionalRepository.findByEventoId(evento.getId());

        // Mapear campos existentes por nome para facilitar busca
        Map<String, CampoAdicional> mapaDeNomesAtuais = camposAtuais.stream()
                .collect(Collectors.toMap(
                        CampoAdicional::getNome,
                        campo -> campo,
                        // Em caso de nomes duplicados (não deveria acontecer), manter o primeiro
                        (campo1, campo2) -> campo1
                ));

        // Lista de campos que serão mantidos (atualizados ou novos)
        List<CampoAdicional> camposProcessados = new ArrayList<>();

        // Lista de IDs de campos que serão removidos
        Set<Long> idsARemover = new HashSet<>();

        // Mapear todos os IDs existentes para identificar quais foram removidos
        Set<Long> todosIdsAtuais = camposAtuais.stream()
                .map(CampoAdicional::getId)
                .collect(Collectors.toSet());

        // Processar os campos da requisição
        if (request.getCamposAdicionais() != null) {
            for (CampoAdicionalDTO campoDTO : request.getCamposAdicionais()) {
                CampoAdicional campoProcessado;

                // Verificar se o campo já existe (pelo nome)
                CampoAdicional campoExistente = mapaDeNomesAtuais.get(campoDTO.getNome());

                if (campoExistente != null) {
                    // Atualizar campo existente
                    campoExistente.setTipo(campoDTO.getTipo());
                    campoExistente.setDescricao(campoDTO.getDescricao());
                    campoExistente.setObrigatorio(campoDTO.getObrigatorio());
                    campoExistente.setOpcoes(campoDTO.getOpcoes());

                    campoProcessado = campoExistente;

                    // Remover o ID da lista de IDs a serem removidos
                    if (campoExistente.getId() != null) {
                        todosIdsAtuais.remove(campoExistente.getId());
                    }
                } else {
                    // Criar um novo campo
                    CampoAdicional novoCampo = new CampoAdicional();
                    novoCampo.setNome(campoDTO.getNome());
                    novoCampo.setTipo(campoDTO.getTipo());
                    novoCampo.setDescricao(campoDTO.getDescricao());
                    novoCampo.setObrigatorio(campoDTO.getObrigatorio());
                    novoCampo.setOpcoes(campoDTO.getOpcoes());
                    novoCampo.setEvento(evento);

                    campoProcessado = novoCampo;
                }

                camposProcessados.add(campoProcessado);
            }
        }

        idsARemover.addAll(todosIdsAtuais);

        if (campoValorRepository != null && !idsARemover.isEmpty()) {
            for (Long id : idsARemover) {
                try {
                    campoValorRepository.deleteByCampoId(id);
                } catch (Exception e) {
                    // Log do erro e continuar
                    System.err.println("Erro ao remover valores do campo ID " + id + ": " + e.getMessage());
                }
            }
        }

        if (!idsARemover.isEmpty()) {
            campoAdicionalRepository.deleteAllById(idsARemover);
        }

        campoAdicionalRepository.saveAll(camposProcessados);
    }

    public static ResponseEntity<?> checarPermissaoEvento(User usuarioLogado, Evento evento) {
        boolean isAdminGeral = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_GERAL"));
        if (isAdminGeral) {
            return ResponseEntity.ok().build();
        }

        boolean isAdminCampus = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_CAMPUS"));
        if (isAdminCampus) {
            boolean gerenciaCampus = usuarioLogado.getCampusQueAdministro().stream()
                    .anyMatch(c -> c.getId().equals(evento.getCampus().getId()));
            if (!gerenciaCampus) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Você não gerencia o campus deste evento.");
            }
            return ResponseEntity.ok().build();
        }

        boolean isAdminDepartamento = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_DEPARTAMENTO"));
        if (isAdminDepartamento) {
            boolean gerenciaDepartamento = usuarioLogado.getDepartamentosQueAdministro().stream()
                    .anyMatch(d -> d.getId().equals(evento.getDepartamento().getId()));
            if (!gerenciaDepartamento) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Você não gerencia o departamento deste evento.");
            }
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Você não tem permissão para este evento.");
    }

    private static boolean isAdminGeral(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_GERAL"));
    }

}