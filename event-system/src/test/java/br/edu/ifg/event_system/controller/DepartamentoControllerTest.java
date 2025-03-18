package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.DepartamentoRequestDTO;
import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.DepartamentoService;
import br.edu.ifg.event_system.service.UserService;
import br.edu.ifg.event_system.util.DepartamentoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepartamentoControllerTest {

    @InjectMocks
    private DepartamentoController departamentoController;

    @Mock
    private DepartamentoService departamentoService;

    @Mock
    private CampusService campusService;

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User adminGeral;
    private User adminCampus;
    private User adminDepartamento;
    private Campus campus1;
    private Campus campus2;
    private Departamento departamento1;
    private Departamento departamento2;
    private List<Departamento> listaDepartamentos;

    @BeforeEach
    void setUp() {
        Role roleAdminGeral = new Role();
        roleAdminGeral.setId(1L);
        roleAdminGeral.setName("ADMIN_GERAL");

        Role roleAdminCampus = new Role();
        roleAdminCampus.setId(2L);
        roleAdminCampus.setName("ADMIN_CAMPUS");

        Role roleAdminDepartamento = new Role();
        roleAdminDepartamento.setId(3L);
        roleAdminDepartamento.setName("ADMIN_DEPARTAMENTO");

        adminGeral = new User();
        adminGeral.setId(1L);
        adminGeral.setUsername("admin.geral@ifg.edu.br");
        adminGeral.setNomeCompleto("Admin Geral");
        adminGeral.setRoles(List.of(roleAdminGeral));
        adminGeral.setCampusQueAdministro(new ArrayList<>());
        adminGeral.setDepartamentosQueAdministro(new ArrayList<>());

        adminCampus = new User();
        adminCampus.setId(2L);
        adminCampus.setUsername("admin.campus@ifg.edu.br");
        adminCampus.setNomeCompleto("Admin Campus");
        adminCampus.setRoles(List.of(roleAdminCampus));
        adminCampus.setCampusQueAdministro(new ArrayList<>());
        adminCampus.setDepartamentosQueAdministro(new ArrayList<>());

        adminDepartamento = new User();
        adminDepartamento.setId(3L);
        adminDepartamento.setUsername("admin.departamento@ifg.edu.br");
        adminDepartamento.setNomeCompleto("Admin Departamento");
        adminDepartamento.setRoles(List.of(roleAdminDepartamento));
        adminDepartamento.setCampusQueAdministro(new ArrayList<>());
        adminDepartamento.setDepartamentosQueAdministro(new ArrayList<>());

        campus1 = new Campus();
        campus1.setId(1L);
        campus1.setNome("Campus 1");
        campus1.setDepartamentos(new ArrayList<>());

        campus2 = new Campus();
        campus2.setId(2L);
        campus2.setNome("Campus 2");
        campus2.setDepartamentos(new ArrayList<>());

        adminCampus.getCampusQueAdministro().add(campus1);

        departamento1 = new Departamento();
        departamento1.setId(1L);
        departamento1.setNome("Departamento 1");
        departamento1.setCampus(campus1);

        departamento2 = new Departamento();
        departamento2.setId(2L);
        departamento2.setNome("Departamento 2");
        departamento2.setCampus(campus2);

        campus1.getDepartamentos().add(departamento1);
        campus2.getDepartamentos().add(departamento2);

        adminDepartamento.getDepartamentosQueAdministro().add(departamento1);

        listaDepartamentos = new ArrayList<>();
        listaDepartamentos.add(departamento1);
        listaDepartamentos.add(departamento2);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void listar_SemFiltro_DeveRetornarTodosDepartamentos() {
        when(departamentoService.listarTodos()).thenReturn(listaDepartamentos);

        ResponseEntity<List<Departamento>> response = departamentoController.listar(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(departamentoService).listarTodos();
    }

    @Test
    void listar_ComFiltro_DeveRetornarDepartamentosDoCampus() {
        List<Departamento> departamentosCampus1 = List.of(departamento1);
        when(departamentoService.listarPorCampus(1L)).thenReturn(departamentosCampus1);

        ResponseEntity<List<Departamento>> response = departamentoController.listar(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(departamento1, response.getBody().get(0));
        verify(departamentoService).listarPorCampus(1L);
    }

    @Test
    void listarDepartamentosGerenciados_ComoAdminGeral_DeveRetornarTodos() {
        when(authentication.getName()).thenReturn("admin.geral@ifg.edu.br");
        when(userService.buscarPorUsername("admin.geral@ifg.edu.br")).thenReturn(adminGeral);
        when(departamentoService.listarTodos()).thenReturn(listaDepartamentos);

        ResponseEntity<List<Departamento>> response = departamentoController.listarDepartamentosGerenciados();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(departamentoService).listarTodos();
    }

    @Test
    void listarDepartamentosGerenciados_ComoAdminCampus_DeveRetornarDepartamentosDoCampus() {
        when(authentication.getName()).thenReturn("admin.campus@ifg.edu.br");
        when(userService.buscarPorUsername("admin.campus@ifg.edu.br")).thenReturn(adminCampus);
        when(departamentoService.listarPorCampus(1L)).thenReturn(List.of(departamento1));

        ResponseEntity<List<Departamento>> response = departamentoController.listarDepartamentosGerenciados();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(departamento1, response.getBody().get(0));
    }

    @Test
    void listarDepartamentosGerenciados_ComoAdminDepartamento_DeveRetornarDepartamentosGerenciados() {
        when(authentication.getName()).thenReturn("admin.departamento@ifg.edu.br");
        when(userService.buscarPorUsername("admin.departamento@ifg.edu.br")).thenReturn(adminDepartamento);

        ResponseEntity<List<Departamento>> response = departamentoController.listarDepartamentosGerenciados();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(departamento1, response.getBody().get(0));
    }

    @Test
    void listarDepartamentosGerenciados_QuandoUsuarioNaoEncontrado_DeveRetornarNotFound() {
        when(authentication.getName()).thenReturn("nao.existe@ifg.edu.br");
        when(userService.buscarPorUsername("nao.existe@ifg.edu.br")).thenReturn(null);

        ResponseEntity<List<Departamento>> response = departamentoController.listarDepartamentosGerenciados();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getCampusDoDepartamento_QuandoDepartamentoExiste_DeveRetornarCampus() {
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento1);

        ResponseEntity<Campus> response = departamentoController.getCampusDoDepartamento(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(campus1, response.getBody());
    }

    @Test
    void getCampusDoDepartamento_QuandoDepartamentoNaoExiste_DeveRetornarNotFound() {
        when(departamentoService.buscarPorId(99L)).thenReturn(null);

        ResponseEntity<Campus> response = departamentoController.getCampusDoDepartamento(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getCampusDoDepartamento_QuandoDepartamentoSemCampus_DeveRetornarNotFound() {
        Departamento depSemCampus = new Departamento();
        depSemCampus.setId(3L);
        depSemCampus.setNome("Departamento Sem Campus");

        when(departamentoService.buscarPorId(3L)).thenReturn(depSemCampus);

        ResponseEntity<Campus> response = departamentoController.getCampusDoDepartamento(3L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void buscar_QuandoDepartamentoExiste_DeveRetornarDepartamento() {
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento1);

        ResponseEntity<Departamento> response = departamentoController.buscar(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(departamento1, response.getBody());
    }

    @Test
    void buscar_QuandoDepartamentoNaoExiste_DeveRetornarNotFound() {
        when(departamentoService.buscarPorId(99L)).thenReturn(null);

        ResponseEntity<Departamento> response = departamentoController.buscar(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void criar_QuandoDadosValidosEPermissaoOk_DeveCriarDepartamento() {
        DepartamentoRequestDTO dto = new DepartamentoRequestDTO();
        dto.setNome("Novo Departamento");
        dto.setCampusId(1L);

        try (MockedStatic<DepartamentoUtils> mockedUtils = Mockito.mockStatic(DepartamentoUtils.class)) {
            DepartamentoUtils.DepartamentoValidationData validData = mock(DepartamentoUtils.DepartamentoValidationData.class);
            when(validData.getCampus()).thenReturn(campus1);

            mockedUtils.when(() -> DepartamentoUtils.validarCampusEPermissao(any(), any(), any()))
                    .thenReturn(ResponseEntity.ok(validData));

            Departamento novoDep = new Departamento();
            novoDep.setId(3L);
            novoDep.setNome("Novo Departamento");
            novoDep.setCampus(campus1);

            when(departamentoService.criarOuAtualizar(any(Departamento.class))).thenReturn(novoDep);

            ResponseEntity<Object> response = departamentoController.criar(dto);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(novoDep, response.getBody());

            verify(departamentoService).criarOuAtualizar(any(Departamento.class));
        }
    }

    @Test
    void criar_QuandoValidacaoFalha_DeveRetornarErro() {
        DepartamentoRequestDTO dto = new DepartamentoRequestDTO();
        dto.setNome("Novo Departamento");
        dto.setCampusId(99L);

        try (MockedStatic<DepartamentoUtils> mockedUtils = Mockito.mockStatic(DepartamentoUtils.class)) {
            mockedUtils.when(() -> DepartamentoUtils.validarCampusEPermissao(any(), any(), any()))
                    .thenReturn(ResponseEntity.badRequest().body("Campus não encontrado"));

            ResponseEntity<Object> response = departamentoController.criar(dto);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Campus não encontrado", response.getBody());

            verify(departamentoService, never()).criarOuAtualizar(any(Departamento.class));
        }
    }

    @Test
    void atualizar_QuandoDadosValidosEPermissaoOk_DeveAtualizarDepartamento() {
        DepartamentoRequestDTO dto = new DepartamentoRequestDTO();
        dto.setNome("Departamento 1 Atualizado");
        dto.setCampusId(1L);

        when(departamentoService.buscarPorId(1L)).thenReturn(departamento1);

        try (MockedStatic<DepartamentoUtils> mockedUtils = Mockito.mockStatic(DepartamentoUtils.class)) {
            DepartamentoUtils.DepartamentoValidationData validData = mock(DepartamentoUtils.DepartamentoValidationData.class);
            when(validData.getCampus()).thenReturn(campus1);

            mockedUtils.when(() -> DepartamentoUtils.validarCampusEPermissao(any(), any(), any()))
                    .thenReturn(ResponseEntity.ok(validData));

            Departamento depAtualizado = new Departamento();
            depAtualizado.setId(1L);
            depAtualizado.setNome("Departamento 1 Atualizado");
            depAtualizado.setCampus(campus1);

            when(departamentoService.criarOuAtualizar(any(Departamento.class))).thenReturn(depAtualizado);

            ResponseEntity<Object> response = departamentoController.atualizar(1L, dto);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(depAtualizado, response.getBody());

            verify(departamentoService).criarOuAtualizar(any(Departamento.class));
        }
    }

    @Test
    void atualizar_QuandoDepartamentoNaoExiste_DeveRetornarNotFound() {
        DepartamentoRequestDTO dto = new DepartamentoRequestDTO();
        dto.setNome("Departamento Inexistente");
        dto.setCampusId(1L);

        when(departamentoService.buscarPorId(99L)).thenReturn(null);

        ResponseEntity<Object> response = departamentoController.atualizar(99L, dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(departamentoService, never()).criarOuAtualizar(any(Departamento.class));
    }

    @Test
    void atualizar_QuandoValidacaoFalha_DeveRetornarErro() {
        DepartamentoRequestDTO dto = new DepartamentoRequestDTO();
        dto.setNome("Departamento 1 Atualizado");
        dto.setCampusId(99L);

        when(departamentoService.buscarPorId(1L)).thenReturn(departamento1);

        try (MockedStatic<DepartamentoUtils> mockedUtils = Mockito.mockStatic(DepartamentoUtils.class)) {
            mockedUtils.when(() -> DepartamentoUtils.validarCampusEPermissao(any(), any(), any()))
                    .thenReturn(ResponseEntity.badRequest().body("Campus não encontrado"));

            ResponseEntity<Object> response = departamentoController.atualizar(1L, dto);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Campus não encontrado", response.getBody());

            verify(departamentoService, never()).criarOuAtualizar(any(Departamento.class));
        }
    }

    @Test
    void deletar_ComoAdminGeral_DeveDeletarDepartamento() {
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento1);
        when(authentication.getName()).thenReturn("admin.geral@ifg.edu.br");
        when(userService.buscarPorUsername("admin.geral@ifg.edu.br")).thenReturn(adminGeral);

        ResponseEntity<String> response = departamentoController.deletar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(departamentoService).deletar(1L);
    }

    @Test
    void deletar_ComoAdminCampusGerenciandoCampus_DeveDeletarDepartamento() {
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento1);
        when(authentication.getName()).thenReturn("admin.campus@ifg.edu.br");
        when(userService.buscarPorUsername("admin.campus@ifg.edu.br")).thenReturn(adminCampus);

        ResponseEntity<String> response = departamentoController.deletar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(departamentoService).deletar(1L);
    }

    @Test
    void deletar_ComoAdminCampusNaoGerenciandoCampus_DeveRetornarForbidden() {
        when(departamentoService.buscarPorId(2L)).thenReturn(departamento2); // Departamento do campus2
        when(authentication.getName()).thenReturn("admin.campus@ifg.edu.br");
        when(userService.buscarPorUsername("admin.campus@ifg.edu.br")).thenReturn(adminCampus);

        ResponseEntity<String> response = departamentoController.deletar(2L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Você não gerencia este campus.", response.getBody());
        verify(departamentoService, never()).deletar(anyLong());
    }

    @Test
    void deletar_QuandoDepartamentoNaoExiste_DeveRetornarNotFound() {
        when(departamentoService.buscarPorId(99L)).thenReturn(null);

        ResponseEntity<String> response = departamentoController.deletar(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(departamentoService, never()).deletar(anyLong());
    }

}