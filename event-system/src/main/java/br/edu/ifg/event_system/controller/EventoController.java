package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.EventoRequestDTO;
import br.edu.ifg.event_system.model.Evento;
import br.edu.ifg.event_system.model.Inscricao;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.CampoAdicionalRepository;
import br.edu.ifg.event_system.repository.CampoValorRepository;
import br.edu.ifg.event_system.service.*;
import br.edu.ifg.event_system.util.EventoUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final CampusService campusService;
    private final DepartamentoService departamentoService;
    private final UserService userService;
    private final InscricaoService inscricaoService;
    private final CampoAdicionalRepository campoAdicionalRepository;
    private final CampoValorRepository campoValorRepository;

    public EventoController(EventoService eventoService,
                            CampusService campusService,
                            DepartamentoService departamentoService,
                            UserService userService,
                            InscricaoService inscricaoService,
                            CampoAdicionalRepository campoAdicionalRepository,
                            CampoValorRepository campoValorRepository) {
        this.eventoService = eventoService;
        this.campusService = campusService;
        this.departamentoService = departamentoService;
        this.userService = userService;
        this.inscricaoService = inscricaoService;
        this.campoAdicionalRepository = campoAdicionalRepository;
        this.campoValorRepository = campoValorRepository;
    }

    @GetMapping
    public ResponseEntity<List<Evento>> listarOuFiltrar(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) Long departamentoId
    ) {
        if (campusId == null && departamentoId == null) {
            return ResponseEntity.ok(eventoService.listarTodos());
        }
        if (campusId != null && departamentoId == null) {
            return ResponseEntity.ok(eventoService.listarPorCampus(campusId));
        }
        if (campusId == null) {
            return ResponseEntity.ok(eventoService.listarPorDepartamento(departamentoId));
        }
        return ResponseEntity.ok(eventoService.listarPorCampusEDepartamento(campusId, departamentoId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Evento>> pesquisarEventos(@RequestParam String texto) {
        List<Evento> resultados = eventoService.buscarPorTituloOuDescricao(texto);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> buscar(@PathVariable Long id) {
        Evento e = eventoService.buscarPorId(id);
        if (e == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(e);
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping("/proximos-que-gerencio")
    public ResponseEntity<List<Evento>> listarEventosFuturosQueGerencio() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = userService.buscarPorUsername(auth.getName());

        List<Evento> eventosFuturos = eventoService.listarEventosFuturos();
        List<Evento> gerenciaveis = filtrarEventosPorPermissao(eventosFuturos, usuarioLogado);

        if (gerenciaveis.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(gerenciaveis);
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping("/todos-que-gerencio")
    public ResponseEntity<List<Evento>> listarTodosEventosQueGerencio() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = userService.buscarPorUsername(auth.getName());

        List<Evento> todosEventos = eventoService.listarTodos();
        List<Evento> gerenciaveis = filtrarEventosPorPermissao(todosEventos, usuarioLogado);

        if (gerenciaveis.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(gerenciaveis);
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @PostMapping
    public ResponseEntity<Object> criar(@RequestBody EventoRequestDTO request) {
        ResponseEntity<?> validacaoResult = EventoUtils.validarDadosIniciais(
                request, campusService, departamentoService, userService
        );

        if (validacaoResult.getStatusCode() != HttpStatus.OK) {
            return ResponseEntity.status(validacaoResult.getStatusCode())
                    .body(validacaoResult.getBody());
        }

        EventoUtils.EventoValidationData dataOk = (EventoUtils.EventoValidationData) validacaoResult.getBody();
        Evento novoEvento = new Evento();
        assert dataOk != null;

        ResponseEntity<?> persistResult = EventoUtils.persistirEvento(
                novoEvento, request, dataOk, eventoService, campoAdicionalRepository);

        return ResponseEntity.status(persistResult.getStatusCode())
                .body(persistResult.getBody());
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @PutMapping("/{id}/encerrar")
    public ResponseEntity<Object> encerrarEvento(@PathVariable Long id) {
        ResponseEntity<Object> check = checkEventoPermissao(id);
        if (!(check.getBody() instanceof EventoData data)) {
            return check;
        }

        Evento evento = data.evento();

        LocalDateTime agora = LocalDateTime.now();
        if (evento.getDataFim() == null || evento.getDataFim().isAfter(agora)) {
            evento.setDataFim(agora);
        }

        evento.setStatus(Evento.EventoStatus.ENCERRADO);

        eventoService.criarOuAtualizar(evento);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @PutMapping("/{id}")
    public ResponseEntity<Object> atualizar(@PathVariable Long id, @RequestBody EventoRequestDTO request) {
        ResponseEntity<Object> checkResult = checkEventoPermissao(id);
        if (!(checkResult.getBody() instanceof EventoData data)) {
            return checkResult;
        }

        Evento eventoExistente = data.evento();

        ResponseEntity<?> validationResult = EventoUtils.validarDadosIniciais(
                request, campusService, departamentoService, userService);
        if (validationResult.getStatusCode() != HttpStatus.OK) {
            return ResponseEntity.status(validationResult.getStatusCode())
                    .body(validationResult.getBody());
        }

        EventoUtils.EventoValidationData validationData =
                (EventoUtils.EventoValidationData) validationResult.getBody();
        assert validationData != null;

        ResponseEntity<?> persistResult = EventoUtils.persistirEvento(
                eventoExistente, request, validationData, eventoService,
                campoAdicionalRepository, campoValorRepository);

        return ResponseEntity.status(persistResult.getStatusCode())
                .body(persistResult.getBody());
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletar(@PathVariable Long id) {
        ResponseEntity<Object> check = checkEventoPermissao(id);
        if (!(check.getBody() instanceof EventoData data)) {
            return check;
        }

        eventoService.deletar(data.evento().getId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping("/{eventoId}/inscritos")
    public ResponseEntity<Object> listarInscritos(@PathVariable Long eventoId) {
        ResponseEntity<Object> check = checkEventoPermissao(eventoId);
        if (!(check.getBody() instanceof EventoData)) {
            return check;
        }

        List<Inscricao> lista = inscricaoService.listarInscricoesDoEvento(eventoId);
        return ResponseEntity.ok(lista);
    }

    private ResponseEntity<Object> checkEventoPermissao(Long eventoId) {
        Evento evento = eventoService.buscarPorId(eventoId);
        if (evento == null) {
            return ResponseEntity.notFound().build();
        }

        User usuarioLogado = getUsuarioLogado();
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não encontrado");
        }

        ResponseEntity<?> permissao = EventoUtils.checarPermissaoEvento(usuarioLogado, evento);

        if (!permissao.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(permissao.getStatusCode())
                    .body(permissao.getBody());
        }

        return ResponseEntity.ok(new EventoData(usuarioLogado, evento));
    }

    private User getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userService.buscarPorUsername(auth.getName());
    }

    private List<Evento> filtrarEventosPorPermissao(List<Evento> eventos, User usuario) {
        return eventos.stream()
                .filter(evt -> {
                    ResponseEntity<?> permissao = EventoUtils.checarPermissaoEvento(usuario, evt);
                    return permissao.getStatusCode().is2xxSuccessful();
                })
                .toList();
    }

    private record EventoData(User user, Evento evento) {}

}