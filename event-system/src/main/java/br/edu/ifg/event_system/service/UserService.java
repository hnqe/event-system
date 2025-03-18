package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.RoleRepository;
import br.edu.ifg.event_system.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private static final String ROLE_ADMIN_DEPARTAMENTO = "ADMIN_DEPARTAMENTO";
    private static final String ROLE_ADMIN_CAMPUS = "ADMIN_CAMPUS";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> listarPaginado(Pageable pageable, String search) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.searchUsers(search, pageable);
    }

    public User registrarUsuario(String nomeCompleto, String username, String senha) {
        username = username.toLowerCase();

        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("E-mail já cadastrado!");
        }

        if (senha.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres!");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(senha));
        newUser.setNomeCompleto(nomeCompleto);

        Role userRole = roleRepository.findByName("USER");
        if (userRole == null) {
            userRole = new Role("USER");
            roleRepository.save(userRole);
        }
        newUser.setRoles(List.of(userRole));

        return userRepository.save(newUser);
    }

    public User criarUsuario(String username, String senha, String nomeCompleto, List<String> rolesStr) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(senha));
        user.setNomeCompleto(nomeCompleto);

        atribuirRoles(user, rolesStr);
        return user;
    }

    public void atualizarRolesDoUsuario(User user, List<String> rolesStr) {
        user.getRoles().clear();
        atribuirRoles(user, rolesStr);
    }

    public void adicionarCampusAoUsuario(User user, Campus campus) {
        adicionarRoleSeNecessario(user, ROLE_ADMIN_CAMPUS);

        if (!user.getCampusQueAdministro().contains(campus)) {
            user.getCampusQueAdministro().add(campus);
        }

        userRepository.save(user);
    }

    public void removerCampusDoUsuario(User user, Campus campus) {
        user.getCampusQueAdministro().remove(campus);
        if (user.getCampusQueAdministro().isEmpty()) {
            user.getRoles().removeIf(r -> r.getName().equals(ROLE_ADMIN_CAMPUS));
        }
        userRepository.save(user);
    }

    public void adicionarDepartamentoAoUsuario(User user, Departamento departamento) {
        adicionarRoleSeNecessario(user, ROLE_ADMIN_DEPARTAMENTO);

        if (!user.getDepartamentosQueAdministro().contains(departamento)) {
            user.getDepartamentosQueAdministro().add(departamento);
        }

        userRepository.save(user);
    }

    public void removerDepartamentoDoUsuario(User user, Departamento departamento) {
        user.getDepartamentosQueAdministro().remove(departamento);
        if (user.getDepartamentosQueAdministro().isEmpty()) {
            user.getRoles().removeIf(r -> r.getName().equals(ROLE_ADMIN_DEPARTAMENTO));
        }
        userRepository.save(user);
    }

    public User buscarPorId(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User buscarPorUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private void atribuirRoles(User user, List<String> rolesStr) {
        for (String roleName : rolesStr) {
            Role role = roleRepository.findByName(roleName);
            if (role == null) {
                role = new Role(roleName);
                roleRepository.save(role);
            }
            user.getRoles().add(role);
        }
        userRepository.save(user);
    }

    private void adicionarRoleSeNecessario(User user, String roleName) {
        boolean jaTemRole = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(roleName));

        if (!jaTemRole) {
            Role role = roleRepository.findByName(roleName);
            if (role == null) {
                role = new Role(roleName);
                roleRepository.save(role);
            }
            user.getRoles().add(role);
        }
    }

}