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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EventoUtils {

    private static final String ROLE_ADMIN_GERAL = "ADMIN_GERAL";
    private static final String ROLE_ADMIN_CAMPUS = "ADMIN_CAMPUS";
    private static final String ROLE_ADMIN_DEPARTAMENTO = "ADMIN_DEPARTAMENTO";

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

    public static ResponseEntity<Object> validarDadosIniciais(EventoRequestDTO request,
                                                              CampusService campusService,
                                                              DepartamentoService departamentoService,
                                                              UserService userService)
    {
        ResponseEntity<Object> validacaoCampusDepto = validarCampusEDepartamento(
                request, campusService, departamentoService);
        if (validacaoCampusDepto.getStatusCode() != HttpStatus.OK) {
            return validacaoCampusDepto;
        }

        Campus campus = campusService.buscarPorId(request.getCampusId());
        Departamento departamento = departamentoService.buscarPorId(request.getDepartamentoId());

        ResponseEntity<Object> validacaoUsuario = obterUsuarioLogado(userService);
        if (validacaoUsuario.getStatusCode() != HttpStatus.OK) {
            return validacaoUsuario;
        }
        User usuarioLogado = (User) validacaoUsuario.getBody();

        ResponseEntity<Object> permissao = verificarPermissoesCampusEDepartamento(
                usuarioLogado, campus, departamento);
        if (permissao.getStatusCode() != HttpStatus.OK) {
            return permissao;
        }

        ResponseEntity<Object> validacaoDatas = validarDatas(request);
        if (validacaoDatas.getStatusCode() != HttpStatus.OK) {
            return validacaoDatas;
        }

        EventoValidationData result = new EventoValidationData();
        result.setCampus(campus);
        result.setDepartamento(departamento);
        result.setUsuarioLogado(usuarioLogado);
        return ResponseEntity.ok(result);
    }

    private static ResponseEntity<Object> validarCampusEDepartamento(
            EventoRequestDTO request,
            CampusService campusService,
            DepartamentoService departamentoService) {

        Campus campus = campusService.buscarPorId(request.getCampusId());
        Departamento departamento = departamentoService.buscarPorId(request.getDepartamentoId());

        if (campus == null || departamento == null) {
            return ResponseEntity.badRequest().body("Campus ou Departamento inválido.");
        }

        if (!departamento.getCampus().getId().equals(campus.getId())) {
            return ResponseEntity.badRequest()
                    .body("Departamento não pertence ao Campus informado.");
        }

        return ResponseEntity.ok().build();
    }

    private static ResponseEntity<Object> obterUsuarioLogado(UserService userService) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nenhum usuário logado.");
        }

        User usuarioLogado = userService.buscarPorUsername(auth.getName());
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não encontrado.");
        }

        return ResponseEntity.ok(usuarioLogado);
    }

    private static ResponseEntity<Object> verificarPermissoesCampusEDepartamento(
            User usuario, Campus campus, Departamento departamento) {

        if (isAdminGeral(usuario)) {
            return ResponseEntity.ok().build();
        }

        boolean isAdminCampus = usuario.getRoles().stream()
                .anyMatch(r -> r.getName().equals(ROLE_ADMIN_CAMPUS));

        if (isAdminCampus) {
            boolean gerenciaCampus = usuario.getCampusQueAdministro().stream()
                    .anyMatch(c -> c.getId().equals(campus.getId()));
            if (!gerenciaCampus) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Você não gerencia o campus deste departamento.");
            }
            return ResponseEntity.ok().build();
        }

        boolean gerenciaDepto = usuario.getDepartamentosQueAdministro().stream()
                .anyMatch(d -> d.getId().equals(departamento.getId()));
        if (!gerenciaDepto) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não gerencia este departamento.");
        }

        return ResponseEntity.ok().build();
    }

    private static ResponseEntity<Object> validarDatas(EventoRequestDTO request) {
        LocalDateTime dtInicio = request.getDataInicio();
        LocalDateTime dtFim = request.getDataFim();

        if (dtInicio != null && dtFim != null && dtFim.isBefore(dtInicio)) {
            return ResponseEntity.badRequest().body("Data fim não pode ser anterior à data início.");
        }

        return ResponseEntity.ok().build();
    }

    public static ResponseEntity<Object> persistirEvento(Evento evento,
                                                         EventoRequestDTO request,
                                                         EventoValidationData data,
                                                         EventoService eventoService,
                                                         CampoAdicionalRepository campoAdicionalRepository)
    {
        return persistirEvento(evento, request, data, eventoService, campoAdicionalRepository, null);
    }

    public static ResponseEntity<Object> persistirEvento(Evento evento,
                                                         EventoRequestDTO request,
                                                         EventoValidationData data,
                                                         EventoService eventoService,
                                                         CampoAdicionalRepository campoAdicionalRepository,
                                                         CampoValorRepository campoValorRepository)
    {
        preencherDadosEvento(evento, request, data);

        Evento salvo = eventoService.criarOuAtualizar(evento);

        if (evento.getId() != null) {
            processarCamposAdicionais(salvo, request, campoAdicionalRepository, campoValorRepository);
        } else {
            criarCamposAdicionais(salvo, request, campoAdicionalRepository);
        }

        return ResponseEntity.ok(eventoService.buscarPorId(salvo.getId()));
    }

    private static void preencherDadosEvento(Evento evento, EventoRequestDTO request, EventoValidationData data) {
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
    }

    private static void criarCamposAdicionais(Evento evento, EventoRequestDTO request,
                                              CampoAdicionalRepository campoAdicionalRepository) {
        if (request.getCamposAdicionais() == null || request.getCamposAdicionais().isEmpty()) {
            return;
        }

        List<CampoAdicional> novosCampos = new ArrayList<>();

        for (CampoAdicionalDTO campoDTO : request.getCamposAdicionais()) {
            CampoAdicional campo = new CampoAdicional();
            preencherDadosCampoAdicional(campo, campoDTO, evento);
            novosCampos.add(campo);
        }

        campoAdicionalRepository.saveAll(novosCampos);
    }

    private static void preencherDadosCampoAdicional(CampoAdicional campo, CampoAdicionalDTO dto, Evento evento) {
        campo.setNome(dto.getNome());
        campo.setTipo(dto.getTipo());
        campo.setDescricao(dto.getDescricao());
        campo.setObrigatorio(dto.getObrigatorio());
        campo.setOpcoes(dto.getOpcoes());
        campo.setEvento(evento);
    }

    /**
     * Processa a atualização dos campos adicionais de um evento, preservando os dados existentes.
     */
    private static void processarCamposAdicionais(Evento evento,
                                                  EventoRequestDTO request,
                                                  CampoAdicionalRepository campoAdicionalRepository,
                                                  CampoValorRepository campoValorRepository) {
        List<CampoAdicional> camposAtuais = campoAdicionalRepository.findByEventoId(evento.getId());

        Map<String, CampoAdicional> mapaDeNomesAtuais = criarMapaDeCamposAtuais(camposAtuais);

        List<CampoAdicional> camposProcessados = new ArrayList<>();

        Set<Long> idsARemover = processarCamposRequisicao(
                request, evento, mapaDeNomesAtuais, camposAtuais, camposProcessados);

        removerValoresDeCampos(idsARemover, campoValorRepository);

        if (!idsARemover.isEmpty()) {
            campoAdicionalRepository.deleteAllById(idsARemover);
        }

        campoAdicionalRepository.saveAll(camposProcessados);
    }

    private static Map<String, CampoAdicional> criarMapaDeCamposAtuais(List<CampoAdicional> camposAtuais) {
        return camposAtuais.stream()
                .collect(Collectors.toMap(
                        CampoAdicional::getNome,
                        campo -> campo,
                        (campo1, campo2) -> campo1
                ));
    }

    private static Set<Long> processarCamposRequisicao(
            EventoRequestDTO request,
            Evento evento,
            Map<String, CampoAdicional> mapaDeNomesAtuais,
            List<CampoAdicional> camposAtuais,
            List<CampoAdicional> camposProcessados) {

        Set<Long> todosIdsAtuais = camposAtuais.stream()
                .map(CampoAdicional::getId)
                .collect(Collectors.toSet());

        if (request.getCamposAdicionais() != null) {
            for (CampoAdicionalDTO campoDTO : request.getCamposAdicionais()) {
                processarCampoDTO(campoDTO, evento, mapaDeNomesAtuais, todosIdsAtuais, camposProcessados);
            }
        }

        return todosIdsAtuais;
    }

    private static void processarCampoDTO(
            CampoAdicionalDTO campoDTO,
            Evento evento,
            Map<String, CampoAdicional> mapaDeNomesAtuais,
            Set<Long> todosIdsAtuais,
            List<CampoAdicional> camposProcessados) {

        CampoAdicional campoExistente = mapaDeNomesAtuais.get(campoDTO.getNome());
        CampoAdicional campoProcessado;

        if (campoExistente != null) {
            preencherDadosCampoAdicional(campoExistente, campoDTO, evento);
            campoProcessado = campoExistente;

            if (campoExistente.getId() != null) {
                todosIdsAtuais.remove(campoExistente.getId());
            }
        } else {
            CampoAdicional novoCampo = new CampoAdicional();
            preencherDadosCampoAdicional(novoCampo, campoDTO, evento);
            campoProcessado = novoCampo;
        }

        camposProcessados.add(campoProcessado);
    }

    private static void removerValoresDeCampos(Set<Long> idsARemover, CampoValorRepository campoValorRepository) {
        if (campoValorRepository == null || idsARemover.isEmpty()) {
            return;
        }

        for (Long id : idsARemover) {
            try {
                campoValorRepository.deleteByCampoId(id);
            } catch (Exception e) {
                System.err.println("Erro ao remover valores do campo ID " + id + ": " + e.getMessage());
            }
        }
    }

    public static ResponseEntity<Object> checarPermissaoEvento(User usuarioLogado, Evento evento) {
        if (isAdminGeral(usuarioLogado)) {
            return ResponseEntity.ok().build();
        }

        ResponseEntity<Object> permissaoCampus = checarPermissaoCampus(usuarioLogado, evento.getCampus());
        if (permissaoCampus.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok().build();
        }

        ResponseEntity<Object> permissaoDepartamento = checarPermissaoDepartamento(usuarioLogado, evento.getDepartamento());
        if (permissaoDepartamento.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Você não tem permissão para este evento.");
    }

    private static ResponseEntity<Object> checarPermissaoCampus(User usuarioLogado, Campus campus) {
        boolean isAdminCampus = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals(ROLE_ADMIN_CAMPUS));

        if (!isAdminCampus) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean gerenciaCampus = usuarioLogado.getCampusQueAdministro().stream()
                .anyMatch(c -> c.getId().equals(campus.getId()));

        if (!gerenciaCampus) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não gerencia o campus deste evento.");
        }

        return ResponseEntity.ok().build();
    }

    private static ResponseEntity<Object> checarPermissaoDepartamento(User usuarioLogado, Departamento departamento) {
        boolean isAdminDepartamento = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals(ROLE_ADMIN_DEPARTAMENTO));

        if (!isAdminDepartamento) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean gerenciaDepartamento = usuarioLogado.getDepartamentosQueAdministro().stream()
                .anyMatch(d -> d.getId().equals(departamento.getId()));

        if (!gerenciaDepartamento) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não gerencia o departamento deste evento.");
        }

        return ResponseEntity.ok().build();
    }

    private static boolean isAdminGeral(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(ROLE_ADMIN_GERAL));
    }

}