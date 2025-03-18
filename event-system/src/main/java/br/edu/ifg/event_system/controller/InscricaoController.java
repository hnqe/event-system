package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.InscricaoRequestDTO;
import br.edu.ifg.event_system.dto.InscricaoResponseDTO;
import br.edu.ifg.event_system.exception.InscricaoException;
import br.edu.ifg.event_system.model.Evento;
import br.edu.ifg.event_system.model.Inscricao;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.EventoService;
import br.edu.ifg.event_system.service.InscricaoService;
import br.edu.ifg.event_system.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inscricoes")
public class InscricaoController {

    private static final String ERRO_USUARIO_NAO_LOGADO = "Usuário não logado.";

    private final InscricaoService inscricaoService;
    private final UserService userService;
    private final EventoService eventoService;

    public InscricaoController(InscricaoService inscricaoService,
                               UserService userService,
                               EventoService eventoService) {
        this.inscricaoService = inscricaoService;
        this.userService = userService;
        this.eventoService = eventoService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/minhas")
    public ResponseEntity<Object> listarMinhasInscricoes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User userLogado = userService.buscarPorUsername(auth.getName());
        if (userLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ERRO_USUARIO_NAO_LOGADO);
        }

        List<Inscricao> minhasInscricoes = inscricaoService.listarInscricoesDoUsuario(userLogado.getId());
        List<InscricaoResponseDTO> response = minhasInscricoes.stream()
                .map(InscricaoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/inscrever")
    public ResponseEntity<Object> inscreverNoEvento(@RequestParam("eventoId") Long eventoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User userLogado = userService.buscarPorUsername(auth.getName());
        if (userLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ERRO_USUARIO_NAO_LOGADO);
        }

        Evento evento = eventoService.buscarPorId(eventoId);
        if (evento == null) {
            return ResponseEntity.badRequest()
                    .body("Evento inexistente.");
        }

        try {
            Inscricao inscricao = inscricaoService.inscreverUsuarioEmEvento(userLogado, evento);
            return ResponseEntity.ok(new InscricaoResponseDTO(inscricao));
        } catch (InscricaoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao inscrever: " + e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/inscrever-completo")
    public ResponseEntity<Object> inscreverCompletoNoEvento(@RequestBody InscricaoRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User userLogado = userService.buscarPorUsername(auth.getName());
        if (userLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ERRO_USUARIO_NAO_LOGADO);
        }

        Evento evento = eventoService.buscarPorId(request.getEventoId());
        if (evento == null) {
            return ResponseEntity.badRequest()
                    .body("Evento inexistente.");
        }

        try {
            Inscricao inscricao = inscricaoService.processarInscricao(userLogado, request, evento);
            return ResponseEntity.ok(new InscricaoResponseDTO(inscricao));
        } catch (InscricaoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao inscrever: " + e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{inscricaoId}")
    public ResponseEntity<String> cancelarInscricao(@PathVariable Long inscricaoId) {
        Inscricao insc = inscricaoService.buscarPorId(inscricaoId);
        if (insc == null) {
            return ResponseEntity.notFound().build();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User userLogado = userService.buscarPorUsername(auth.getName());

        boolean isAdminGeral = userLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_GERAL"));
        if (!isAdminGeral && !insc.getUser().getId().equals(userLogado.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não pode cancelar a inscrição de outro usuário!");
        }

        try {
            inscricaoService.cancelarInscricao(insc.getId());
            return ResponseEntity.ok("Inscrição cancelada (status CANCELADA) com sucesso!");
        } catch (InscricaoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cancelar inscrição: " + e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/evento/{eventoId}/campos")
    public ResponseEntity<Object> listarCamposDoEvento(@PathVariable Long eventoId) {
        Evento evento = eventoService.buscarPorId(eventoId);
        if (evento == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(evento.getCamposAdicionais());
    }

}