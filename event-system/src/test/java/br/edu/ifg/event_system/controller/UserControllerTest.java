package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.AdicionarDepartamentoRequestDTO;
import br.edu.ifg.event_system.dto.UserRequestDTO;
import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.DepartamentoService;
import br.edu.ifg.event_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private DepartamentoService departamentoService;

    @Mock
    private CampusService campusService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserController userController;

    private User user;
    private Campus campus;
    private Departamento departamento;
    private User adminGeral;
    private User adminCampus;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("user@ifg.edu.br");
        user.setNomeCompleto("Usuário Teste");
        user.setRoles(new ArrayList<>());
        user.setCampusQueAdministro(new ArrayList<>());
        user.setDepartamentosQueAdministro(new ArrayList<>());

        campus = new Campus();
        campus.setId(1L);
        campus.setNome("Campus Teste");

        departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNome("Departamento Teste");
        departamento.setCampus(campus);

        Role roleAdminGeral = new Role();
        roleAdminGeral.setId(1L);
        roleAdminGeral.setName("ADMIN_GERAL");

        Role roleAdminCampus = new Role();
        roleAdminCampus.setId(2L);
        roleAdminCampus.setName("ADMIN_CAMPUS");

        adminGeral = new User();
        adminGeral.setId(2L);
        adminGeral.setUsername("admin_geral@ifg.edu.br");
        adminGeral.setRoles(new ArrayList<>(List.of(roleAdminGeral)));

        adminCampus = new User();
        adminCampus.setId(3L);
        adminCampus.setUsername("admin_campus@ifg.edu.br");
        adminCampus.setRoles(new ArrayList<>(List.of(roleAdminCampus)));
        adminCampus.setCampusQueAdministro(new ArrayList<>(List.of(campus)));

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void listarPaginado_DeveRetornarPaginaDeUsuarios() {
        List<User> users = Collections.singletonList(user);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userService.listarPaginado(any(Pageable.class), eq(null))).thenReturn(userPage);

        ResponseEntity<Page<User>> response = userController.listarPaginado(0, 10, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("user@ifg.edu.br", response.getBody().getContent().get(0).getUsername());
        verify(userService).listarPaginado(any(Pageable.class), eq(null));
    }

    @Test
    void criar_DeveRetornarUsuarioCriado() {
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setUsername("novo@ifg.edu.br");
        userRequestDTO.setPassword("senha123");
        userRequestDTO.setNomeCompleto("Novo Usuário");
        userRequestDTO.setRoles(List.of("USER"));

        when(userService.criarUsuario(
                "novo@ifg.edu.br",
                "senha123",
                "Novo Usuário",
                List.of("USER")
        )).thenReturn(user);

        ResponseEntity<User> response = userController.criar(userRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("user@ifg.edu.br", response.getBody().getUsername());
        verify(userService).criarUsuario(
                "novo@ifg.edu.br",
                "senha123",
                "Novo Usuário",
                List.of("USER")
        );
    }

    @Test
    void atualizarRoles_ComUsuarioExistente_DeveRetornarSucesso() {
        List<String> novasRoles = List.of("USER", "ADMIN_CAMPUS");

        when(userService.buscarPorId(1L)).thenReturn(user);
        doNothing().when(userService).atualizarRolesDoUsuario(user, novasRoles);

        ResponseEntity<String> response = userController.atualizarRoles(1L, novasRoles);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Roles atualizadas com sucesso!", response.getBody());
        verify(userService).buscarPorId(1L);
        verify(userService).atualizarRolesDoUsuario(user, novasRoles);
    }

    @Test
    void atualizarRoles_ComUsuarioInexistente_DeveRetornarNotFound() {
        List<String> novasRoles = List.of("USER", "ADMIN_CAMPUS");

        when(userService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<String> response = userController.atualizarRoles(999L, novasRoles);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).buscarPorId(999L);
        verify(userService, never()).atualizarRolesDoUsuario(any(), any());
    }

    @Test
    void adicionarCampusAoUsuario_ComUsuarioECampusValidos_DeveRetornarSucesso() {
        when(userService.buscarPorId(1L)).thenReturn(user);
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        doNothing().when(userService).adicionarCampusAoUsuario(user, campus);

        ResponseEntity<Object> response = userController.adicionarCampusAoUsuario(1L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Usuário agora é admin deste campus!", response.getBody());
        verify(userService).buscarPorId(1L);
        verify(campusService).buscarPorId(1L);
        verify(userService).adicionarCampusAoUsuario(user, campus);
    }

    @Test
    void adicionarCampusAoUsuario_ComUsuarioInexistente_DeveRetornarNotFound() {
        when(userService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Object> response = userController.adicionarCampusAoUsuario(999L, 1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).buscarPorId(999L);
        verify(campusService, never()).buscarPorId(anyLong());
        verify(userService, never()).adicionarCampusAoUsuario(any(), any());
    }

    @Test
    void adicionarCampusAoUsuario_ComCampusInvalido_DeveRetornarBadRequest() {
        when(userService.buscarPorId(1L)).thenReturn(user);
        when(campusService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Object> response = userController.adicionarCampusAoUsuario(1L, 999L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Campus inválido.", response.getBody());
        verify(userService).buscarPorId(1L);
        verify(campusService).buscarPorId(999L);
        verify(userService, never()).adicionarCampusAoUsuario(any(), any());
    }

    @Test
    void removerCampusDoUsuario_ComUsuarioECampusValidos_DeveRetornarSucesso() {
        when(userService.buscarPorId(1L)).thenReturn(user);
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        doNothing().when(userService).removerCampusDoUsuario(user, campus);

        ResponseEntity<Object> response = userController.removerCampusDoUsuario(1L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Campus removido com sucesso!", response.getBody());
        verify(userService).buscarPorId(1L);
        verify(campusService).buscarPorId(1L);
        verify(userService).removerCampusDoUsuario(user, campus);
    }

    @Test
    void adicionarDepartamentoAoUsuario_ComoAdminGeral_DeveRetornarSucesso() {
        AdicionarDepartamentoRequestDTO requestDTO = new AdicionarDepartamentoRequestDTO();
        requestDTO.setDepartamentoId(1L);

        when(userService.buscarPorId(1L)).thenReturn(user);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin_geral@ifg.edu.br");
            when(userService.buscarPorUsername("admin_geral@ifg.edu.br")).thenReturn(adminGeral);

            doNothing().when(userService).adicionarDepartamentoAoUsuario(user, departamento);

            ResponseEntity<String> response = userController.adicionarDepartamentoAoUsuario(1L, requestDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Usuário agora é admin deste departamento.", response.getBody());
            verify(userService).buscarPorId(1L);
            verify(departamentoService).buscarPorId(1L);
            verify(userService).adicionarDepartamentoAoUsuario(user, departamento);
        }
    }

    @Test
    void adicionarDepartamentoAoUsuario_ComoAdminCampus_DeveRetornarSucesso() {
        AdicionarDepartamentoRequestDTO requestDTO = new AdicionarDepartamentoRequestDTO();
        requestDTO.setDepartamentoId(1L);

        when(userService.buscarPorId(1L)).thenReturn(user);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin_campus@ifg.edu.br");
            when(userService.buscarPorUsername("admin_campus@ifg.edu.br")).thenReturn(adminCampus);

            doNothing().when(userService).adicionarDepartamentoAoUsuario(user, departamento);

            ResponseEntity<String> response = userController.adicionarDepartamentoAoUsuario(1L, requestDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Usuário agora é admin deste departamento.", response.getBody());
            verify(userService).buscarPorId(1L);
            verify(departamentoService).buscarPorId(1L);
            verify(userService).adicionarDepartamentoAoUsuario(user, departamento);
        }
    }

    @Test
    void adicionarDepartamentoAoUsuario_ComUsuarioInexistente_DeveRetornarNotFound() {
        AdicionarDepartamentoRequestDTO requestDTO = new AdicionarDepartamentoRequestDTO();
        requestDTO.setDepartamentoId(1L);

        when(userService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<String> response = userController.adicionarDepartamentoAoUsuario(999L, requestDTO);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).buscarPorId(999L);
        verify(departamentoService, never()).buscarPorId(anyLong());
    }

    @Test
    void adicionarDepartamentoAoUsuario_ComDepartamentoInexistente_DeveRetornarBadRequest() {
        AdicionarDepartamentoRequestDTO requestDTO = new AdicionarDepartamentoRequestDTO();
        requestDTO.setDepartamentoId(999L);

        when(userService.buscarPorId(1L)).thenReturn(user);
        when(departamentoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<String> response = userController.adicionarDepartamentoAoUsuario(1L, requestDTO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Departamento inexistente.", response.getBody());
        verify(userService).buscarPorId(1L);
        verify(departamentoService).buscarPorId(999L);
    }

    @Test
    void adicionarDepartamentoAoUsuario_ComAdminCampusSemPermissao_DeveRetornarForbidden() {
        AdicionarDepartamentoRequestDTO requestDTO = new AdicionarDepartamentoRequestDTO();
        requestDTO.setDepartamentoId(1L);

        Campus outroCampus = new Campus();
        outroCampus.setId(2L);
        outroCampus.setNome("Outro Campus");

        User adminOutroCampus = new User();
        adminOutroCampus.setId(4L);
        adminOutroCampus.setUsername("admin_outro_campus@ifg.edu.br");
        adminOutroCampus.setRoles(new ArrayList<>(adminCampus.getRoles()));
        adminOutroCampus.setCampusQueAdministro(new ArrayList<>(List.of(outroCampus)));

        when(userService.buscarPorId(1L)).thenReturn(user);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin_outro_campus@ifg.edu.br");
            when(userService.buscarPorUsername("admin_outro_campus@ifg.edu.br")).thenReturn(adminOutroCampus);

            ResponseEntity<String> response = userController.adicionarDepartamentoAoUsuario(1L, requestDTO);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não gerencia o campus deste departamento.", response.getBody());
            verify(userService).buscarPorId(1L);
            verify(departamentoService).buscarPorId(1L);
            verify(userService, never()).adicionarDepartamentoAoUsuario(any(), any());
        }
    }

    @Test
    void removerDepartamentoDoUsuario_ComoAdminGeral_DeveRetornarSucesso() {
        when(userService.buscarPorId(1L)).thenReturn(user);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin_geral@ifg.edu.br");
            when(userService.buscarPorUsername("admin_geral@ifg.edu.br")).thenReturn(adminGeral);

            doNothing().when(userService).removerDepartamentoDoUsuario(user, departamento);

            ResponseEntity<String> response = userController.removerDepartamentoDoUsuario(1L, 1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Departamento removido com sucesso!", response.getBody());
            verify(userService).buscarPorId(1L);
            verify(departamentoService).buscarPorId(1L);
            verify(userService).removerDepartamentoDoUsuario(user, departamento);
        }
    }

    @Test
    void removerDepartamentoDoUsuario_ComUsuarioInexistente_DeveRetornarNotFound() {
        when(userService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<String> response = userController.removerDepartamentoDoUsuario(999L, 1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).buscarPorId(999L);
        verify(departamentoService, never()).buscarPorId(anyLong());
    }

    @Test
    void removerDepartamentoDoUsuario_ComDepartamentoInvalido_DeveRetornarBadRequest() {
        when(userService.buscarPorId(1L)).thenReturn(user);
        when(departamentoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<String> response = userController.removerDepartamentoDoUsuario(1L, 999L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Departamento inválido.", response.getBody());
        verify(userService).buscarPorId(1L);
        verify(departamentoService).buscarPorId(999L);
    }

}