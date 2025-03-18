package br.edu.ifg.event_system.util;

import br.edu.ifg.event_system.dto.CampoAdicionalDTO;
import br.edu.ifg.event_system.dto.EventoRequestDTO;
import br.edu.ifg.event_system.model.*;
import br.edu.ifg.event_system.repository.CampoAdicionalRepository;
import br.edu.ifg.event_system.repository.CampoValorRepository;
import br.edu.ifg.event_system.service.CampusService;
import br.edu.ifg.event_system.service.DepartamentoService;
import br.edu.ifg.event_system.service.EventoService;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoUtilsTest {

    @Mock
    private CampusService campusService;

    @Mock
    private DepartamentoService departamentoService;

    @Mock
    private UserService userService;

    @Mock
    private EventoService eventoService;

    @Mock
    private CampoAdicionalRepository campoAdicionalRepository;

    @Mock
    private CampoValorRepository campoValorRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private EventoRequestDTO requestDTO;
    private Campus campus;
    private Departamento departamento;
    private User user;
    private Evento evento;
    private List<CampoAdicionalDTO> camposAdicionaisDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new EventoRequestDTO();
        requestDTO.setCampusId(1L);
        requestDTO.setDepartamentoId(1L);
        requestDTO.setTitulo("Evento Teste");
        requestDTO.setDataInicio(LocalDateTime.now().plusDays(1));
        requestDTO.setDataFim(LocalDateTime.now().plusDays(2));
        requestDTO.setLocal("Local Teste");
        requestDTO.setDescricao("Descrição Teste");
        requestDTO.setDataLimiteInscricao(LocalDateTime.now().plusHours(12));
        requestDTO.setVagas(100);
        requestDTO.setEstudanteIfg(true);

        campus = new Campus();
        campus.setId(1L);
        campus.setNome("Campus Teste");

        departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNome("Departamento Teste");
        departamento.setCampus(campus);

        user = new User();
        user.setId(1L);
        user.setUsername("test_user");
        user.setRoles(new ArrayList<>());
        user.setCampusQueAdministro(new ArrayList<>());
        user.setDepartamentosQueAdministro(new ArrayList<>());

        evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Evento Existente");
        evento.setCampus(campus);
        evento.setDepartamento(departamento);

        camposAdicionaisDTO = new ArrayList<>();
        CampoAdicionalDTO campoDTO = new CampoAdicionalDTO();
        campoDTO.setNome("Campo Teste");
        campoDTO.setTipo("TEXT");
        campoDTO.setDescricao("Descrição do Campo");
        campoDTO.setObrigatorio(true);
        campoDTO.setOpcoes("Opção 1,Opção 2");
        camposAdicionaisDTO.add(campoDTO);

        requestDTO.setCamposAdicionais(camposAdicionaisDTO);
    }

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<EventoUtils> constructor = EventoUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("Esta é uma classe utilitária e não pode ser instanciada.", exception.getCause().getMessage());
    }

    @Test
    void testValidarDadosIniciais_CampusOuDepartamentoInvalido() {
        when(campusService.buscarPorId(1L)).thenReturn(null);

        ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                requestDTO, campusService, departamentoService, userService);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Campus ou Departamento inválido.", response.getBody());
    }

    @Test
    void testValidarDadosIniciais_DepartamentoNaoPertenceAoCampus() {
        Campus outroCampus = new Campus();
        outroCampus.setId(2L);

        Departamento departamentoOutroCampus = new Departamento();
        departamentoOutroCampus.setId(1L);
        departamentoOutroCampus.setCampus(outroCampus);

        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamentoOutroCampus);

        ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                requestDTO, campusService, departamentoService, userService);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Departamento não pertence ao Campus informado.", response.getBody());
    }

    @Test
    void testValidarDadosIniciais_UsuarioNaoLogado() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(null);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Nenhum usuário logado.", response.getBody());
        }
    }

    @Test
    void testValidarDadosIniciais_UsuarioNaoEncontrado() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(null);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Usuário não encontrado.", response.getBody());
        }
    }

    @Test
    void testValidarDadosIniciais_UsuarioSemPermissao() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        Role role = new Role();
        role.setName("USER");
        user.getRoles().add(role);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(user);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não gerencia este departamento.", response.getBody());
        }
    }

    @Test
    void testValidarDadosIniciais_AdminGeral() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        Role role = new Role();
        role.setName("ADMIN_GERAL");
        user.getRoles().add(role);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(user);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(EventoUtils.EventoValidationData.class, response.getBody());

            EventoUtils.EventoValidationData data = (EventoUtils.EventoValidationData) response.getBody();
            assertEquals(campus, data.getCampus());
            assertEquals(departamento, data.getDepartamento());
            assertEquals(user, data.getUsuarioLogado());
        }
    }

    @Test
    void testValidarDadosIniciais_AdminCampus_GerenciaCampus() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        Role role = new Role();
        role.setName("ADMIN_CAMPUS");
        user.getRoles().add(role);
        user.getCampusQueAdministro().add(campus);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(user);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(EventoUtils.EventoValidationData.class, response.getBody());
        }
    }

    @Test
    void testValidarDadosIniciais_AdminCampus_NaoGerenciaCampus() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        Role role = new Role();
        role.setName("ADMIN_CAMPUS");
        user.getRoles().add(role);

        Campus outroCampus = new Campus();
        outroCampus.setId(2L);
        user.getCampusQueAdministro().add(outroCampus);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(user);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não gerencia o campus deste departamento.", response.getBody());
        }
    }

    @Test
    void testValidarDadosIniciais_AdminDepartamento_GerenciaDepartamento() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        Role role = new Role();
        role.setName("ADMIN_DEPARTAMENTO");
        user.getRoles().add(role);
        user.getDepartamentosQueAdministro().add(departamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(user);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(EventoUtils.EventoValidationData.class, response.getBody());
        }
    }

    @Test
    void testValidarDadosIniciais_AdminDepartamento_NaoGerenciaDepartamento() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        Role role = new Role();
        role.setName("ADMIN_DEPARTAMENTO");
        user.getRoles().add(role);

        Departamento outroDepartamento = new Departamento();
        outroDepartamento.setId(2L);
        user.getDepartamentosQueAdministro().add(outroDepartamento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(user);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não gerencia este departamento.", response.getBody());
        }
    }

    @Test
    void testValidarDadosIniciais_DataFimAnteriorDataInicio() {
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(1L)).thenReturn(departamento);

        Role role = new Role();
        role.setName("ADMIN_GERAL");
        user.getRoles().add(role);

        requestDTO.setDataInicio(LocalDateTime.now().plusDays(2));
        requestDTO.setDataFim(LocalDateTime.now().plusDays(1));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test_user");
            when(userService.buscarPorUsername("test_user")).thenReturn(user);

            ResponseEntity<Object> response = EventoUtils.validarDadosIniciais(
                    requestDTO, campusService, departamentoService, userService);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Data fim não pode ser anterior à data início.", response.getBody());
        }
    }

    @Test
    void testPersistirEvento_NovoCriado() {
        EventoUtils.EventoValidationData data = new EventoUtils.EventoValidationData();
        data.setCampus(campus);
        data.setDepartamento(departamento);
        data.setUsuarioLogado(user);

        Evento novoEvento = new Evento();

        when(eventoService.criarOuAtualizar(any(Evento.class))).thenAnswer(invocation -> {
            Evento e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        when(eventoService.buscarPorId(1L)).thenReturn(novoEvento);

        ResponseEntity<Object> response = EventoUtils.persistirEvento(
                novoEvento, requestDTO, data, eventoService, campoAdicionalRepository);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventoService).criarOuAtualizar(novoEvento);
        verify(campoAdicionalRepository).saveAll(anyList());
    }

    @Test
    void testPersistirEvento_Atualizado() {
        EventoUtils.EventoValidationData data = new EventoUtils.EventoValidationData();
        data.setCampus(campus);
        data.setDepartamento(departamento);
        data.setUsuarioLogado(user);

        Evento eventoExistente = new Evento();
        eventoExistente.setId(1L);

        when(eventoService.criarOuAtualizar(any(Evento.class))).thenReturn(eventoExistente);
        when(eventoService.buscarPorId(1L)).thenReturn(eventoExistente);

        List<CampoAdicional> camposAtuais = new ArrayList<>();
        when(campoAdicionalRepository.findByEventoId(1L)).thenReturn(camposAtuais);

        ResponseEntity<Object> response = EventoUtils.persistirEvento(
                eventoExistente, requestDTO, data, eventoService,
                campoAdicionalRepository, campoValorRepository);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventoService).criarOuAtualizar(eventoExistente);
        verify(campoAdicionalRepository).saveAll(anyList());
    }

    @Test
    void testPersistirEvento_AtualizadoComCamposExistentes() {
        EventoUtils.EventoValidationData data = new EventoUtils.EventoValidationData();
        data.setCampus(campus);
        data.setDepartamento(departamento);
        data.setUsuarioLogado(user);

        Evento eventoExistente = new Evento();
        eventoExistente.setId(1L);

        CampoAdicional campoExistente = new CampoAdicional();
        campoExistente.setId(1L);
        campoExistente.setNome("Campo Teste");
        campoExistente.setEvento(eventoExistente);

        List<CampoAdicional> camposAtuais = new ArrayList<>();
        camposAtuais.add(campoExistente);

        when(eventoService.criarOuAtualizar(any(Evento.class))).thenReturn(eventoExistente);
        when(eventoService.buscarPorId(1L)).thenReturn(eventoExistente);
        when(campoAdicionalRepository.findByEventoId(1L)).thenReturn(camposAtuais);

        ResponseEntity<Object> response = EventoUtils.persistirEvento(
                eventoExistente, requestDTO, data, eventoService,
                campoAdicionalRepository, campoValorRepository);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventoService).criarOuAtualizar(eventoExistente);
        verify(campoAdicionalRepository).saveAll(anyList());
    }

    @Test
    void testPersistirEvento_AtualizadoComCampoRemovido() {
        EventoUtils.EventoValidationData data = new EventoUtils.EventoValidationData();
        data.setCampus(campus);
        data.setDepartamento(departamento);
        data.setUsuarioLogado(user);

        Evento eventoExistente = new Evento();
        eventoExistente.setId(1L);

        CampoAdicional campoExistente = new CampoAdicional();
        campoExistente.setId(1L);
        campoExistente.setNome("Campo Diferente");
        campoExistente.setEvento(eventoExistente);

        List<CampoAdicional> camposAtuais = new ArrayList<>();
        camposAtuais.add(campoExistente);

        when(eventoService.criarOuAtualizar(any(Evento.class))).thenReturn(eventoExistente);
        when(eventoService.buscarPorId(1L)).thenReturn(eventoExistente);
        when(campoAdicionalRepository.findByEventoId(1L)).thenReturn(camposAtuais);

        ResponseEntity<Object> response = EventoUtils.persistirEvento(
                eventoExistente, requestDTO, data, eventoService,
                campoAdicionalRepository, campoValorRepository);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventoService).criarOuAtualizar(eventoExistente);
        verify(campoValorRepository).deleteByCampoId(1L);
        verify(campoAdicionalRepository).deleteAllById(anySet());
        verify(campoAdicionalRepository).saveAll(anyList());
    }

    @Test
    void testPersistirEventoSemCamposAdicionais() {
        EventoUtils.EventoValidationData data = new EventoUtils.EventoValidationData();
        data.setCampus(campus);
        data.setDepartamento(departamento);
        data.setUsuarioLogado(user);

        Evento novoEvento = new Evento();
        requestDTO.setCamposAdicionais(null);

        when(eventoService.criarOuAtualizar(any(Evento.class))).thenAnswer(invocation -> {
            Evento e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        when(eventoService.buscarPorId(1L)).thenReturn(novoEvento);

        ResponseEntity<Object> response = EventoUtils.persistirEvento(
                novoEvento, requestDTO, data, eventoService, campoAdicionalRepository);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventoService).criarOuAtualizar(novoEvento);
        verify(campoAdicionalRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testPersistirEventoComErroAoRemoverValores() {
        EventoUtils.EventoValidationData data = new EventoUtils.EventoValidationData();
        data.setCampus(campus);
        data.setDepartamento(departamento);
        data.setUsuarioLogado(user);

        Evento eventoExistente = new Evento();
        eventoExistente.setId(1L);

        CampoAdicional campoExistente = new CampoAdicional();
        campoExistente.setId(1L);
        campoExistente.setNome("Campo Diferente");
        campoExistente.setEvento(eventoExistente);

        List<CampoAdicional> camposAtuais = new ArrayList<>();
        camposAtuais.add(campoExistente);

        when(eventoService.criarOuAtualizar(any(Evento.class))).thenReturn(eventoExistente);
        when(eventoService.buscarPorId(1L)).thenReturn(eventoExistente);
        when(campoAdicionalRepository.findByEventoId(1L)).thenReturn(camposAtuais);
        doThrow(new RuntimeException("Erro ao excluir")).when(campoValorRepository).deleteByCampoId(1L);

        ResponseEntity<Object> response = EventoUtils.persistirEvento(
                eventoExistente, requestDTO, data, eventoService,
                campoAdicionalRepository, campoValorRepository);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventoService).criarOuAtualizar(eventoExistente);
        verify(campoValorRepository).deleteByCampoId(1L);
        verify(campoAdicionalRepository).deleteAllById(anySet());
        verify(campoAdicionalRepository).saveAll(anyList());
    }

    @Test
    void testChecarPermissaoEvento_AdminGeral() {
        Role role = new Role();
        role.setName("ADMIN_GERAL");
        user.getRoles().add(role);

        ResponseEntity<Object> response = EventoUtils.checarPermissaoEvento(user, evento);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testChecarPermissaoEvento_AdminCampus_GerenciaCampus() {
        Role role = new Role();
        role.setName("ADMIN_CAMPUS");
        user.getRoles().add(role);
        user.getCampusQueAdministro().add(campus);

        ResponseEntity<Object> response = EventoUtils.checarPermissaoEvento(user, evento);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testChecarPermissaoEvento_AdminCampus_NaoGerenciaCampus() {
        Role role = new Role();
        role.setName("ADMIN_CAMPUS");
        user.getRoles().add(role);

        Campus outroCampus = new Campus();
        outroCampus.setId(2L);
        user.getCampusQueAdministro().add(outroCampus);

        ResponseEntity<Object> response = EventoUtils.checarPermissaoEvento(user, evento);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Você não tem permissão para este evento.", response.getBody());
    }

    @Test
    void testChecarPermissaoEvento_AdminDepartamento_GerenciaDepartamento() {
        Role role = new Role();
        role.setName("ADMIN_DEPARTAMENTO");
        user.getRoles().add(role);
        user.getDepartamentosQueAdministro().add(departamento);

        ResponseEntity<Object> response = EventoUtils.checarPermissaoEvento(user, evento);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testChecarPermissaoEvento_AdminDepartamento_NaoGerenciaDepartamento() {
        Role role = new Role();
        role.setName("ADMIN_DEPARTAMENTO");
        user.getRoles().add(role);

        Departamento outroDepartamento = new Departamento();
        outroDepartamento.setId(2L);
        user.getDepartamentosQueAdministro().add(outroDepartamento);

        ResponseEntity<Object> response = EventoUtils.checarPermissaoEvento(user, evento);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Você não tem permissão para este evento.", response.getBody());
    }

    @Test
    void testChecarPermissaoEvento_SemPermissao() {
        Role role = new Role();
        role.setName("USER");
        user.getRoles().add(role);

        ResponseEntity<Object> response = EventoUtils.checarPermissaoEvento(user, evento);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Você não tem permissão para este evento.", response.getBody());
    }

    @Test
    void testEventoValidationData_GettersSetters() {
        EventoUtils.EventoValidationData data = new EventoUtils.EventoValidationData();

        assertNull(data.getCampus());
        assertNull(data.getDepartamento());
        assertNull(data.getUsuarioLogado());

        data.setCampus(campus);
        data.setDepartamento(departamento);
        data.setUsuarioLogado(user);

        assertEquals(campus, data.getCampus());
        assertEquals(departamento, data.getDepartamento());
        assertEquals(user, data.getUsuarioLogado());
    }

}