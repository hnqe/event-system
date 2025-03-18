package br.edu.ifg.event_system.util;

import br.edu.ifg.event_system.dto.DepartamentoRequestDTO;
import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class DepartamentoUtils {

    public static class DepartamentoValidationData {
        private Campus campus;
        private User usuarioLogado;

        public Campus getCampus() {
            return campus;
        }
        public void setCampus(Campus campus) {
            this.campus = campus;
        }
        public User getUsuarioLogado() {
            return usuarioLogado;
        }
        public void setUsuarioLogado(User usuarioLogado) {
            this.usuarioLogado = usuarioLogado;
        }

    }

    public static ResponseEntity<?> validarCampusEPermissao(DepartamentoRequestDTO request,
                                                            CampusService campusService,
                                                            UserService userService) {
        Campus campus = campusService.buscarPorId(request.getCampusId());
        if (campus == null) {
            return ResponseEntity.badRequest().body("Campus inválido ou não encontrado.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = userService.buscarPorUsername(auth.getName());

        boolean isAdminGeral = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_GERAL"));
        if (isAdminGeral) {
            DepartamentoValidationData data = new DepartamentoValidationData();
            data.setCampus(campus);
            data.setUsuarioLogado(usuarioLogado);
            return ResponseEntity.ok(data);
        }

        boolean isAdminCampus = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_CAMPUS"));
        if (isAdminCampus) {
            boolean gerenciaEsteCampus = usuarioLogado.getCampusQueAdministro().stream()
                    .anyMatch(c -> c.getId().equals(campus.getId()));
            if (!gerenciaEsteCampus) {
                return ResponseEntity.status(403).body("Você não gerencia este campus.");
            }
            DepartamentoValidationData data = new DepartamentoValidationData();
            data.setCampus(campus);
            data.setUsuarioLogado(usuarioLogado);
            return ResponseEntity.ok(data);
        }

        boolean isAdminDepartamento = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN_DEPARTAMENTO"));
        if (isAdminDepartamento) {
            boolean gerenciaDepartamentoNesteCampus = usuarioLogado.getDepartamentosQueAdministro().stream()
                    .anyMatch(d -> d.getCampus() != null && d.getCampus().getId().equals(campus.getId()));

            if (!gerenciaDepartamentoNesteCampus) {
                return ResponseEntity.status(403).body("Você não administra departamentos neste campus.");
            }

            DepartamentoValidationData data = new DepartamentoValidationData();
            data.setCampus(campus);
            data.setUsuarioLogado(usuarioLogado);
            return ResponseEntity.ok(data);
        }

        return ResponseEntity.status(403).body("Você não tem permissão para esta operação.");
    }

}