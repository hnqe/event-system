package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.RoleRepository;
import br.edu.ifg.event_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role roleUser;
    private Role roleAdminCampus;
    private Role roleAdminDepartamento;
    private Campus campus;
    private Departamento departamento;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@ifg.edu.br");
        user.setPassword("encoded_password");
        user.setNomeCompleto("Test User");
        user.setRoles(new ArrayList<>());
        user.setCampusQueAdministro(new ArrayList<>());
        user.setDepartamentosQueAdministro(new ArrayList<>());

        roleUser = new Role();
        roleUser.setId(1L);
        roleUser.setName("USER");

        roleAdminCampus = new Role();
        roleAdminCampus.setId(2L);
        roleAdminCampus.setName("ADMIN_CAMPUS");

        roleAdminDepartamento = new Role();
        roleAdminDepartamento.setId(3L);
        roleAdminDepartamento.setName("ADMIN_DEPARTAMENTO");

        campus = new Campus();
        campus.setId(1L);
        campus.setNome("Campus Teste");

        departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNome("Departamento Teste");
        departamento.setCampus(campus);

        pageable = PageRequest.of(0, 10);

        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
    }

    @Test
    void listarPaginado_SemTermoBusca_DeveChamarFindAll() {
        List<User> users = List.of(user);
        Page<User> page = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.listarPaginado(pageable, null);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).searchUsers(anyString(), any(Pageable.class));
    }

    @Test
    void listarPaginado_ComTermoBusca_DeveChamarSearchUsers() {
        String searchTerm = "test";
        List<User> users = List.of(user);
        Page<User> page = new PageImpl<>(users, pageable, users.size());
        when(userRepository.searchUsers(searchTerm, pageable)).thenReturn(page);

        Page<User> result = userService.listarPaginado(pageable, searchTerm);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchUsers(searchTerm, pageable);
        verify(userRepository, never()).findAll(pageable);
    }

    @Test
    void listarPaginado_ComTermoBuscaVazio_DeveChamarFindAll() {
        List<User> users = List.of(user);
        Page<User> page = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.listarPaginado(pageable, "   ");

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).searchUsers(anyString(), any(Pageable.class));
    }

    @Test
    void registrarUsuario_ComDadosValidos_DeveCriarUsuario() {
        when(userRepository.findByUsername("test@ifg.edu.br")).thenReturn(null);
        when(roleRepository.findByName("USER")).thenReturn(roleUser);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.registrarUsuario("Test User", "test@ifg.edu.br", "password123");

        assertNotNull(result);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("test@ifg.edu.br", savedUser.getUsername());
        assertEquals("Test User", savedUser.getNomeCompleto());
        assertEquals(1, savedUser.getRoles().size());
        assertEquals("USER", savedUser.getRoles().get(0).getName());
    }

    @Test
    void registrarUsuario_ComUsernameExistente_DeveLancarExcecao() {
        when(userRepository.findByUsername("test@ifg.edu.br")).thenReturn(user);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registrarUsuario("Test User", "test@ifg.edu.br", "password123");
        });

        assertEquals("E-mail já cadastrado!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registrarUsuario_ComSenhaCurta_DeveLancarExcecao() {
        when(userRepository.findByUsername("test@ifg.edu.br")).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registrarUsuario("Test User", "test@ifg.edu.br", "12345");
        });

        assertEquals("A senha deve ter pelo menos 6 caracteres!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void criarUsuario_ComRolesExistentes_DeveCriarUsuarioComRoles() {
        List<String> roles = List.of("USER", "ADMIN_CAMPUS");
        when(roleRepository.findByName("USER")).thenReturn(roleUser);
        when(roleRepository.findByName("ADMIN_CAMPUS")).thenReturn(roleAdminCampus);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.criarUsuario("test@ifg.edu.br", "password123", "Test User", roles);

        assertNotNull(result);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("test@ifg.edu.br", savedUser.getUsername());
        assertEquals("Test User", savedUser.getNomeCompleto());
        assertEquals(2, savedUser.getRoles().size());
    }

    @Test
    void criarUsuario_ComRoleNova_DeveCriarRoleEUsuario() {
        List<String> roles = List.of("NEW_ROLE");
        when(roleRepository.findByName("NEW_ROLE")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(user);

        Role novaRole = new Role();
        novaRole.setName("NEW_ROLE");
        when(roleRepository.save(any(Role.class))).thenReturn(novaRole);

        User result = userService.criarUsuario("test@ifg.edu.br", "password123", "Test User", roles);

        assertNotNull(result);
        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void atualizarRolesDoUsuario_DeveAtualizarRoles() {
        user.getRoles().add(roleUser);
        List<String> novasRoles = List.of("ADMIN_CAMPUS");

        when(roleRepository.findByName("ADMIN_CAMPUS")).thenReturn(roleAdminCampus);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.atualizarRolesDoUsuario(user, novasRoles);

        verify(userRepository).save(user);
        assertEquals(1, user.getRoles().size());
        assertEquals("ADMIN_CAMPUS", user.getRoles().get(0).getName());
    }

    @Test
    void adicionarCampusAoUsuario_ComUsuarioSemRoleCampus_DeveAdicionarRoleECampus() {
        when(roleRepository.findByName("ADMIN_CAMPUS")).thenReturn(roleAdminCampus);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.adicionarCampusAoUsuario(user, campus);

        verify(userRepository).save(user);
        assertTrue(user.getRoles().contains(roleAdminCampus));
        assertTrue(user.getCampusQueAdministro().contains(campus));
    }

    @Test
    void adicionarCampusAoUsuario_ComUsuarioJaComRoleCampus_DeveApenasAdicionarCampus() {
        user.getRoles().add(roleAdminCampus);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.adicionarCampusAoUsuario(user, campus);

        verify(userRepository).save(user);
        assertEquals(1, user.getRoles().size()); // Não deve adicionar role duplicada
        assertTrue(user.getCampusQueAdministro().contains(campus));
    }

    @Test
    void adicionarCampusAoUsuario_CampusJaAdicionado_NaoDeveAdicionarDuplicado() {
        user.getRoles().add(roleAdminCampus);
        user.getCampusQueAdministro().add(campus);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.adicionarCampusAoUsuario(user, campus);

        verify(userRepository).save(user);
        assertEquals(1, user.getCampusQueAdministro().size()); // Não deve adicionar campus duplicado
    }

    @Test
    void removerCampusDoUsuario_ComUnicoCampus_DeveRemoverCampusERole() {
        user.getRoles().add(roleAdminCampus);
        user.getCampusQueAdministro().add(campus);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.removerCampusDoUsuario(user, campus);

        verify(userRepository).save(user);
        assertTrue(user.getCampusQueAdministro().isEmpty());
        assertFalse(user.getRoles().contains(roleAdminCampus));
    }

    @Test
    void removerCampusDoUsuario_ComMultiplosCampus_DeveRemoverApenasCampus() {
        Campus outroCampus = new Campus();
        outroCampus.setId(2L);

        user.getRoles().add(roleAdminCampus);
        user.getCampusQueAdministro().add(campus);
        user.getCampusQueAdministro().add(outroCampus);

        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.removerCampusDoUsuario(user, campus);

        verify(userRepository).save(user);
        assertEquals(1, user.getCampusQueAdministro().size());
        assertTrue(user.getCampusQueAdministro().contains(outroCampus));
        assertTrue(user.getRoles().contains(roleAdminCampus));
    }

    @Test
    void adicionarDepartamentoAoUsuario_ComUsuarioSemRoleDepartamento_DeveAdicionarRoleEDepartamento() {
        when(roleRepository.findByName("ADMIN_DEPARTAMENTO")).thenReturn(roleAdminDepartamento);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.adicionarDepartamentoAoUsuario(user, departamento);

        verify(userRepository).save(user);
        assertTrue(user.getRoles().contains(roleAdminDepartamento));
        assertTrue(user.getDepartamentosQueAdministro().contains(departamento));
    }

    @Test
    void adicionarDepartamentoAoUsuario_ComUsuarioJaComRoleDepartamento_DeveApenasAdicionarDepartamento() {
        user.getRoles().add(roleAdminDepartamento);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.adicionarDepartamentoAoUsuario(user, departamento);

        verify(userRepository).save(user);
        assertEquals(1, user.getRoles().size()); // Não deve adicionar role duplicada
        assertTrue(user.getDepartamentosQueAdministro().contains(departamento));
    }

    @Test
    void adicionarDepartamentoAoUsuario_DepartamentoJaAdicionado_NaoDeveAdicionarDuplicado() {
        user.getRoles().add(roleAdminDepartamento);
        user.getDepartamentosQueAdministro().add(departamento);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.adicionarDepartamentoAoUsuario(user, departamento);

        verify(userRepository).save(user);
        assertEquals(1, user.getDepartamentosQueAdministro().size()); // Não deve adicionar departamento duplicado
    }

    @Test
    void removerDepartamentoDoUsuario_ComUnicoDepartamento_DeveRemoverDepartamentoERole() {
        user.getRoles().add(roleAdminDepartamento);
        user.getDepartamentosQueAdministro().add(departamento);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.removerDepartamentoDoUsuario(user, departamento);

        verify(userRepository).save(user);
        assertTrue(user.getDepartamentosQueAdministro().isEmpty());
        assertFalse(user.getRoles().contains(roleAdminDepartamento));
    }

    @Test
    void removerDepartamentoDoUsuario_ComMultiplosDepartamentos_DeveRemoverApenasDepartamento() {
        Departamento outroDepartamento = new Departamento();
        outroDepartamento.setId(2L);

        user.getRoles().add(roleAdminDepartamento);
        user.getDepartamentosQueAdministro().add(departamento);
        user.getDepartamentosQueAdministro().add(outroDepartamento);

        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.removerDepartamentoDoUsuario(user, departamento);

        verify(userRepository).save(user);
        assertEquals(1, user.getDepartamentosQueAdministro().size());
        assertTrue(user.getDepartamentosQueAdministro().contains(outroDepartamento));
        assertTrue(user.getRoles().contains(roleAdminDepartamento));
    }

    @Test
    void buscarPorId_ComIdExistente_DeveRetornarUsuario() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.buscarPorId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void buscarPorId_ComIdInexistente_DeveRetornarNull() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        User result = userService.buscarPorId(999L);

        assertNull(result);
    }

    @Test
    void buscarPorUsername_ComUsernameExistente_DeveRetornarUsuario() {
        when(userRepository.findByUsername("test@ifg.edu.br")).thenReturn(user);

        User result = userService.buscarPorUsername("test@ifg.edu.br");

        assertNotNull(result);
        assertEquals("test@ifg.edu.br", result.getUsername());
    }

    @Test
    void buscarPorUsername_ComUsernameInexistente_DeveRetornarNull() {
        when(userRepository.findByUsername("nonexistent@ifg.edu.br")).thenReturn(null);

        User result = userService.buscarPorUsername("nonexistent@ifg.edu.br");

        assertNull(result);
    }

}