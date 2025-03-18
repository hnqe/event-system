package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.AdicionarDepartamentoRequestDTO;
import br.edu.ifg.event_system.dto.UserRequestDTO;
import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.DepartamentoService;
import br.edu.ifg.event_system.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;
    private final DepartamentoService departamentoService;
    private final CampusService campusService;

    public UserController(UserService userService,
                          DepartamentoService departamentoService,
                          CampusService campusService) {
        this.userService = userService;
        this.departamentoService = departamentoService;
        this.campusService = campusService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS')")
    public ResponseEntity<Page<User>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.listarPaginado(pageable, search));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_GERAL')")
    public ResponseEntity<User> criar(@RequestBody UserRequestDTO userRequestDTO) {
        User user = userService.criarUsuario(
                userRequestDTO.getUsername(),
                userRequestDTO.getPassword(),
                userRequestDTO.getNomeCompleto(),
                userRequestDTO.getRoles()
        );
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN_GERAL')")
    public ResponseEntity<String> atualizarRoles(@PathVariable Long userId,
                                                 @RequestBody List<String> rolesNovas) {
        User user = userService.buscarPorId(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        userService.atualizarRolesDoUsuario(user, rolesNovas);
        return ResponseEntity.ok("Roles atualizadas com sucesso!");
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL')")
    @PostMapping("/{userId}/campus/{campusId}")
    public ResponseEntity<Object> adicionarCampusAoUsuario(@PathVariable Long userId,
                                                           @PathVariable Long campusId) {
        ResponseEntity<Object> validacao = checarUsuarioECampus(userId, campusId);
        if (!(validacao.getBody() instanceof UserCampusData data)) {
            return validacao;
        }

        userService.adicionarCampusAoUsuario(data.user(), data.campus());
        return ResponseEntity.ok("Usuário agora é admin deste campus!");
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL')")
    @DeleteMapping("/{userId}/campus/{campusId}")
    public ResponseEntity<Object> removerCampusDoUsuario(@PathVariable Long userId,
                                                         @PathVariable Long campusId) {
        ResponseEntity<Object> validacao = checarUsuarioECampus(userId, campusId);
        if (!(validacao.getBody() instanceof UserCampusData data)) {
            return validacao;
        }

        userService.removerCampusDoUsuario(data.user(), data.campus());
        return ResponseEntity.ok("Campus removido com sucesso!");
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS')")
    @PostMapping("/{userId}/departamentos")
    public ResponseEntity<String> adicionarDepartamentoAoUsuario(@PathVariable Long userId,
                                                                 @RequestBody AdicionarDepartamentoRequestDTO request) {
        User user = userService.buscarPorId(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Departamento departamento = departamentoService.buscarPorId(request.getDepartamentoId());
        if (departamento == null) {
            return ResponseEntity.badRequest().body("Departamento inexistente.");
        }

        ResponseEntity<String> erroPermissao = checarPermissaoAdminCampusDepartamento(departamento);
        if (erroPermissao != null) {
            return erroPermissao;
        }

        userService.adicionarDepartamentoAoUsuario(user, departamento);
        return ResponseEntity.ok("Usuário agora é admin deste departamento.");
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS')")
    @DeleteMapping("/{userId}/departamentos/{departamentoId}")
    public ResponseEntity<String> removerDepartamentoDoUsuario(@PathVariable Long userId,
                                                               @PathVariable Long departamentoId) {
        User user = userService.buscarPorId(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Departamento departamento = departamentoService.buscarPorId(departamentoId);
        if (departamento == null) {
            return ResponseEntity.badRequest().body("Departamento inválido.");
        }

        ResponseEntity<String> erroPermissao = checarPermissaoAdminCampusDepartamento(departamento);
        if (erroPermissao != null) {
            return erroPermissao;
        }

        userService.removerDepartamentoDoUsuario(user, departamento);
        return ResponseEntity.ok("Departamento removido com sucesso!");
    }

    private ResponseEntity<Object> checarUsuarioECampus(Long userId, Long campusId) {
        User user = userService.buscarPorId(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Campus campus = campusService.buscarPorId(campusId);
        if (campus == null) {
            return ResponseEntity.badRequest().body("Campus inválido.");
        }

        return ResponseEntity.ok(new UserCampusData(user, campus));
    }

    private ResponseEntity<String> checarPermissaoAdminCampusDepartamento(Departamento departamento) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = userService.buscarPorUsername(auth.getName());

        boolean isAdminGeral = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_GERAL"));

        if (isAdminGeral) {
            return null;
        }

        Campus campusDoDepto = departamento.getCampus();
        boolean gerenciaEsteCampus = usuarioLogado.getCampusQueAdministro().stream()
                .anyMatch(c -> c.getId().equals(campusDoDepto.getId()));
        if (!gerenciaEsteCampus) {
            return ResponseEntity.status(403).body("Você não gerencia o campus deste departamento.");
        }

        return null;
    }

    private record UserCampusData(User user, Campus campus) {

    }

}