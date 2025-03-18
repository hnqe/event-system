package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.DepartamentoRequestDTO;
import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.DepartamentoService;
import br.edu.ifg.event_system.service.UserService;
import br.edu.ifg.event_system.util.DepartamentoUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departamentos")
public class DepartamentoController {

    private final DepartamentoService departamentoService;
    private final CampusService campusService;
    private final UserService userService;

    public DepartamentoController(DepartamentoService departamentoService,
                                  CampusService campusService,
                                  UserService userService) {
        this.departamentoService = departamentoService;
        this.campusService = campusService;
        this.userService = userService;
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping
    public ResponseEntity<List<Departamento>> listar(@RequestParam(required = false) Long campusId) {
        if (campusId != null) {
            List<Departamento> departamentos = departamentoService.listarPorCampus(campusId);
            return ResponseEntity.ok(departamentos);
        }
        return ResponseEntity.ok(departamentoService.listarTodos());
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL', 'ADMIN_CAMPUS', 'ADMIN_DEPARTAMENTO')")
    @GetMapping("/gerenciados")
    public ResponseEntity<List<Departamento>> listarDepartamentosGerenciados() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = userService.buscarPorUsername(auth.getName());

        if (usuarioLogado == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdminGeral = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_GERAL"));
        if (isAdminGeral) {
            return ResponseEntity.ok(departamentoService.listarTodos());
        }

        boolean isAdminCampus = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_CAMPUS"));
        if (isAdminCampus) {
            List<Long> campusIds = usuarioLogado.getCampusQueAdministro().stream()
                    .map(Campus::getId)
                    .toList();

            List<Departamento> departamentosCampus = campusIds.stream()
                    .flatMap(id -> departamentoService.listarPorCampus(id).stream())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(departamentosCampus);
        }

        return ResponseEntity.ok(usuarioLogado.getDepartamentosQueAdministro());
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping("/{id}/campus")
    public ResponseEntity<Campus> getCampusDoDepartamento(@PathVariable Long id) {
        Departamento departamento = departamentoService.buscarPorId(id);
        if (departamento == null) {
            return ResponseEntity.notFound().build();
        }

        Campus campus = departamento.getCampus();
        if (campus == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(campus);
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping("/{id}")
    public ResponseEntity<Departamento> buscar(@PathVariable Long id) {
        Departamento d = departamentoService.buscarPorId(id);
        if (d == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(d);
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS')")
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody DepartamentoRequestDTO request) {
        ResponseEntity<?> validacao = DepartamentoUtils.validarCampusEPermissao(
                request, campusService, userService);
        if (!(validacao.getBody() instanceof DepartamentoUtils.DepartamentoValidationData dataOk)) {
            return validacao;
        }

        Departamento dep = new Departamento();
        dep.setNome(request.getNome());
        dep.setCampus(dataOk.getCampus());

        Departamento salvo = departamentoService.criarOuAtualizar(dep);
        return ResponseEntity.ok(salvo);
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody DepartamentoRequestDTO request) {
        Departamento existente = departamentoService.buscarPorId(id);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<?> validacao = DepartamentoUtils.validarCampusEPermissao(
                request, campusService, userService);
        if (!(validacao.getBody() instanceof DepartamentoUtils.DepartamentoValidationData dataOk)) {
            return validacao;
        }

        if (existente.getCampus() != null) {
            existente.getCampus().getDepartamentos().remove(existente);
        }

        existente.setNome(request.getNome());
        existente.setCampus(dataOk.getCampus());

        dataOk.getCampus().getDepartamentos().add(existente);

        Departamento salvo = departamentoService.criarOuAtualizar(existente);
        return ResponseEntity.ok(salvo);
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        Departamento existente = departamentoService.buscarPorId(id);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        User usuarioLogado = userService.buscarPorUsername(
                org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication().getName()
        );
        boolean isAdminGeral = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_GERAL"));

        if (!isAdminGeral) {
            boolean gerenciaEsteCampus = usuarioLogado.getCampusQueAdministro().stream()
                    .anyMatch(c -> c.getId().equals(existente.getCampus().getId()));
            if (!gerenciaEsteCampus) {
                return ResponseEntity.status(403).body("Você não gerencia este campus.");
            }
        }

        departamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}