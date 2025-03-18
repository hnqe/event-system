package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.CampusRepository;
import br.edu.ifg.event_system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CampusService {

    private final CampusRepository campusRepository;
    private final UserRepository userRepository;

    public CampusService(CampusRepository campusRepository, UserRepository userRepository) {
        this.campusRepository = campusRepository;
        this.userRepository = userRepository;
    }

    public Campus criarOuAtualizar(Campus campus) {
        return campusRepository.save(campus);
    }

    public List<Campus> listarTodos() {
        return campusRepository.findAll();
    }

    public Campus buscarPorId(Long id) {
        return campusRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deletar(Long id) {
        Campus campus = buscarPorId(id);
        if (campus == null) {
            return;
        }

        List<User> usuariosComCampus = userRepository.findAll().stream()
                .filter(user -> user.getCampusQueAdministro() != null &&
                        user.getCampusQueAdministro().stream()
                                .anyMatch(c -> c.getId().equals(id)))
                .toList();

        for (User user : usuariosComCampus) {
            user.getCampusQueAdministro().removeIf(c -> c.getId().equals(id));

            if (user.getCampusQueAdministro().isEmpty()) {
                user.getRoles().removeIf(r -> r.getName().equals("ADMIN_CAMPUS"));
            }

            userRepository.save(user);
        }

        campusRepository.deleteById(id);
    }

}