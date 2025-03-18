package br.edu.ifg.event_system.util;

import br.edu.ifg.event_system.dto.DepartamentoRequestDTO;
import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartamentoUtilsTest {

    @Mock
    private CampusService campusService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private DepartamentoRequestDTO requestDTO;
    private Campus campus;
    private User user;

    @BeforeEach
    void setUp() {
        requestDTO = new DepartamentoRequestDTO();
        requestDTO.setCampusId(1L);

        campus = new Campus();
        campus.setId(1L);
        campus.setNome("Campus Teste");

        user = new User();
        user.setId(1L);
        user.setUsername("test_user");
    }

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<DepartamentoUtils> constructor = DepartamentoUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("Esta é uma classe utilitária e não pode ser instanciada.", exception.getCause().getMessage());
    }

    @Test
    void testValidarCampusEPermissao_CampusNaoEncontrado() {
        when(campusService.buscarPorId(1L)).thenReturn(null);

        ResponseEntity<Object> response = DepartamentoUtils.validarCampusEPermissao(requestDTO, campusService, userService);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Campus inválido ou não encontrado.", response.getBody());
        verify(campusService).buscarPorId(1L);
        verifyNoInteractions(userService);
    }

    @Test
    void testValidarCampusEPermissao_AdminGeral() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test_user");
        when(userService.buscarPorUsername("test_user")).thenReturn(user);

        List<Role> roles = new ArrayList<>();
        Role adminGeralRole = new Role();
        adminGeralRole.setName("ADMIN_GERAL");
        roles.add(adminGeralRole);
        user.setRoles(roles);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            ResponseEntity<Object> response = DepartamentoUtils.validarCampusEPermissao(requestDTO, campusService, userService);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(DepartamentoUtils.DepartamentoValidationData.class, response.getBody());

            DepartamentoUtils.DepartamentoValidationData data = (DepartamentoUtils.DepartamentoValidationData) response.getBody();
            assertEquals(campus, data.getCampus());
            assertEquals(user, data.getUsuarioLogado());
        }
    }

    @Test
    void testValidarCampusEPermissao_AdminCampus_GerenciaCampus() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test_user");
        when(userService.buscarPorUsername("test_user")).thenReturn(user);

        List<Role> roles = new ArrayList<>();
        Role adminCampusRole = new Role();
        adminCampusRole.setName("ADMIN_CAMPUS");
        roles.add(adminCampusRole);
        user.setRoles(roles);

        List<Campus> campusQueAdministro = new ArrayList<>();
        campusQueAdministro.add(campus);
        user.setCampusQueAdministro(campusQueAdministro);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            ResponseEntity<Object> response = DepartamentoUtils.validarCampusEPermissao(requestDTO, campusService, userService);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(DepartamentoUtils.DepartamentoValidationData.class, response.getBody());

            DepartamentoUtils.DepartamentoValidationData data = (DepartamentoUtils.DepartamentoValidationData) response.getBody();
            assertEquals(campus, data.getCampus());
            assertEquals(user, data.getUsuarioLogado());
        }
    }

    @Test
    void testValidarCampusEPermissao_AdminCampus_NaoGerenciaCampus() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test_user");
        when(userService.buscarPorUsername("test_user")).thenReturn(user);

        List<Role> roles = new ArrayList<>();
        Role adminCampusRole = new Role();
        adminCampusRole.setName("ADMIN_CAMPUS");
        roles.add(adminCampusRole);
        user.setRoles(roles);

        Campus outroCampus = new Campus();
        outroCampus.setId(2L);
        List<Campus> campusQueAdministro = new ArrayList<>();
        campusQueAdministro.add(outroCampus);
        user.setCampusQueAdministro(campusQueAdministro);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            ResponseEntity<Object> response = DepartamentoUtils.validarCampusEPermissao(requestDTO, campusService, userService);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não gerencia este campus.", response.getBody());
        }
    }

    @Test
    void testValidarCampusEPermissao_AdminDepartamento_GerenciaDepartamentoNoCampus() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test_user");
        when(userService.buscarPorUsername("test_user")).thenReturn(user);

        List<Role> roles = new ArrayList<>();
        Role adminDepartamentoRole = new Role();
        adminDepartamentoRole.setName("ADMIN_DEPARTAMENTO");
        roles.add(adminDepartamentoRole);
        user.setRoles(roles);

        Departamento departamento = new Departamento();
        departamento.setCampus(campus);
        List<Departamento> departamentosQueAdministro = new ArrayList<>();
        departamentosQueAdministro.add(departamento);
        user.setDepartamentosQueAdministro(departamentosQueAdministro);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            ResponseEntity<Object> response = DepartamentoUtils.validarCampusEPermissao(requestDTO, campusService, userService);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(DepartamentoUtils.DepartamentoValidationData.class, response.getBody());

            DepartamentoUtils.DepartamentoValidationData data = (DepartamentoUtils.DepartamentoValidationData) response.getBody();
            assertEquals(campus, data.getCampus());
            assertEquals(user, data.getUsuarioLogado());
        }
    }

    @Test
    void testValidarCampusEPermissao_AdminDepartamento_NaoGerenciaDepartamentoNoCampus() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test_user");
        when(userService.buscarPorUsername("test_user")).thenReturn(user);

        List<Role> roles = new ArrayList<>();
        Role adminDepartamentoRole = new Role();
        adminDepartamentoRole.setName("ADMIN_DEPARTAMENTO");
        roles.add(adminDepartamentoRole);
        user.setRoles(roles);

        Campus outroCampus = new Campus();
        outroCampus.setId(2L);

        Departamento departamento = new Departamento();
        departamento.setCampus(outroCampus);

        List<Departamento> departamentosQueAdministro = new ArrayList<>();
        departamentosQueAdministro.add(departamento);
        user.setDepartamentosQueAdministro(departamentosQueAdministro);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            ResponseEntity<Object> response = DepartamentoUtils.validarCampusEPermissao(requestDTO, campusService, userService);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não administra departamentos neste campus.", response.getBody());
        }
    }

    @Test
    void testValidarCampusEPermissao_SemPermissao() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test_user");
        when(userService.buscarPorUsername("test_user")).thenReturn(user);

        List<Role> roles = new ArrayList<>();
        Role userRole = new Role();
        userRole.setName("USER");
        roles.add(userRole);
        user.setRoles(roles);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            ResponseEntity<Object> response = DepartamentoUtils.validarCampusEPermissao(requestDTO, campusService, userService);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não tem permissão para esta operação.", response.getBody());
        }
    }

    @Test
    void testDepartamentoValidationData_GettersSetters() {
        DepartamentoUtils.DepartamentoValidationData data = new DepartamentoUtils.DepartamentoValidationData();

        assertNull(data.getCampus());
        assertNull(data.getUsuarioLogado());

        data.setCampus(campus);
        data.setUsuarioLogado(user);

        assertEquals(campus, data.getCampus());
        assertEquals(user, data.getUsuarioLogado());
    }

}