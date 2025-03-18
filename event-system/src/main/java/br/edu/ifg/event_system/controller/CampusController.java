package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.service.CampusService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campus")
public class CampusController {

    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping
    public ResponseEntity<List<Campus>> listar() {
        return ResponseEntity.ok(campusService.listarTodos());
    }

    @PreAuthorize("hasAnyRole('ADMIN_GERAL','ADMIN_CAMPUS','ADMIN_DEPARTAMENTO')")
    @GetMapping("/{id}")
    public ResponseEntity<Campus> buscarPorId(@PathVariable Long id) {
        Campus campus = campusService.buscarPorId(id);
        if (campus == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(campus);
    }

    @PreAuthorize("hasRole('ADMIN_GERAL')")
    @PostMapping
    public ResponseEntity<Campus> criar(@RequestBody Campus campus) {
        Campus salvo = campusService.criarOuAtualizar(campus);
        return ResponseEntity.ok(salvo);
    }

    @PreAuthorize("hasRole('ADMIN_GERAL')")
    @PutMapping("/{id}")
    public ResponseEntity<Campus> atualizar(@PathVariable Long id, @RequestBody Campus campus) {
        Campus existente = campusService.buscarPorId(id);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }
        existente.setNome(campus.getNome());
        Campus salvo = campusService.criarOuAtualizar(existente);
        return ResponseEntity.ok(salvo);
    }

    @PreAuthorize("hasRole('ADMIN_GERAL')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletar(@PathVariable Long id) {
        try {
            campusService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            String mensagem = "Não foi possível remover o campus porque ainda existem outras referências a ele no sistema.";

            if (e.getMessage() != null && e.getMessage().contains("user_campus")) {
                mensagem = "Não foi possível remover o campus pois existem usuários vinculados a ele. " +
                        "Remova primeiro as associações de usuários com este campus.";
            }

            return ResponseEntity.badRequest().body(mensagem);
        }
    }

}