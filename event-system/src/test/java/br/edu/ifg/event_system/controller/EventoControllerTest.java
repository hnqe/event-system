package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.EventoRequestDTO;
import br.edu.ifg.event_system.model.*;
import br.edu.ifg.event_system.repository.CampoAdicionalRepository;
import br.edu.ifg.event_system.repository.CampoValorRepository;
import br.edu.ifg.event_system.service.*;
import br.edu.ifg.event_system.util.EventoUtils;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static br.edu.ifg.event_system.model.Evento.EventoStatus.ATIVO;
import static br.edu.ifg.event_system.model.Evento.EventoStatus.ENCERRADO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoControllerTest {

    @InjectMocks
    private EventoController eventoController;

    @Mock
    private EventoService eventoService;
    @Mock
    private CampusService campusService;
    @Mock
    private DepartamentoService departamentoService;
    @Mock
    private UserService userService;
    @Mock
    private InscricaoService inscricaoService;
    @Mock
    private CampoAdicionalRepository campoAdicionalRepository;
    @Mock
    private CampoValorRepository campoValorRepository;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private User userLogado;
    private User adminGeral;
    private Campus campus;
    private Departamento departamento;
    private Evento evento1;
    private Evento evento2;
    private List<Evento> listaEventos;
    private Inscricao inscricao;
    private List<Inscricao> listaInscricoes;

    @BeforeEach
    void setUp() {
        userLogado = new User();
        userLogado.setId(10L);
        userLogado.setUsername("usuario@ifg.edu.br");
        userLogado.setNomeCompleto("Usuario");
        userLogado.setRoles(new ArrayList<>());

        Role roleAdminGeral = new Role();
        roleAdminGeral.setId(1L);
        roleAdminGeral.setName("ADMIN_GERAL");

        adminGeral = new User();
        adminGeral.setId(11L);
        adminGeral.setUsername("admin@ifg.edu.br");
        adminGeral.setNomeCompleto("Admin Geral");
        adminGeral.setRoles(List.of(roleAdminGeral));

        campus = new Campus();
        campus.setId(1L);
        campus.setNome("Campus Teste");

        departamento = new Departamento();
        departamento.setId(2L);
        departamento.setNome("Departamento Teste");
        departamento.setCampus(campus);

        evento1 = new Evento();
        evento1.setId(100L);
        evento1.setTitulo("Evento 1");
        evento1.setCampus(campus);
        evento1.setDepartamento(departamento);
        evento1.setStatus(ATIVO);

        evento2 = new Evento();
        evento2.setId(200L);
        evento2.setTitulo("Evento 2");
        evento2.setCampus(campus);
        evento2.setDepartamento(departamento);
        evento2.setStatus(ATIVO);

        listaEventos = new ArrayList<>();
        listaEventos.add(evento1);
        listaEventos.add(evento2);

        inscricao = new Inscricao();
        inscricao.setId(500L);
        inscricao.setEvento(evento1);
        inscricao.setUser(userLogado);
        inscricao.setStatus("CONFIRMADA");

        listaInscricoes = new ArrayList<>();
        listaInscricoes.add(inscricao);

        SecurityContextHolder.setContext(securityContext);

        // Set up common mocks for all authentication-requiring methods
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void listarOuFiltrar_SemFiltros_DeveRetornarTodosEventos() {
        when(eventoService.listarTodos()).thenReturn(listaEventos);

        ResponseEntity<List<Evento>> response = eventoController.listarOuFiltrar(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(eventoService).listarTodos();
    }

    @Test
    void listarOuFiltrar_ComCampusId_DeveRetornarEventosPorCampus() {
        when(eventoService.listarPorCampus(1L)).thenReturn(listaEventos);

        ResponseEntity<List<Evento>> response = eventoController.listarOuFiltrar(1L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(eventoService).listarPorCampus(1L);
    }

    @Test
    void listarOuFiltrar_ComDepartamentoId_DeveRetornarEventosPorDepartamento() {
        when(eventoService.listarPorDepartamento(2L)).thenReturn(listaEventos);

        ResponseEntity<List<Evento>> response = eventoController.listarOuFiltrar(null, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(eventoService).listarPorDepartamento(2L);
    }

    @Test
    void listarOuFiltrar_ComCampusIdEDepartamentoId_DeveRetornarEventos() {
        when(eventoService.listarPorCampusEDepartamento(1L, 2L)).thenReturn(listaEventos);

        ResponseEntity<List<Evento>> response = eventoController.listarOuFiltrar(1L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(eventoService).listarPorCampusEDepartamento(1L, 2L);
    }

    @Test
    void pesquisarEventos_DeveRetornarResultados() {
        String textoPesquisa = "algo";
        when(eventoService.buscarPorTituloOuDescricao(textoPesquisa)).thenReturn(listaEventos);

        ResponseEntity<List<Evento>> response = eventoController.pesquisarEventos(textoPesquisa);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(eventoService).buscarPorTituloOuDescricao(textoPesquisa);
    }

    @Test
    void buscar_QuandoEventoExiste_DeveRetornarEvento() {
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);

        ResponseEntity<Evento> response = eventoController.buscar(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(evento1, response.getBody());
        verify(eventoService).buscarPorId(100L);
    }

    @Test
    void buscar_QuandoEventoNaoExiste_DeveRetornarNotFound() {
        when(eventoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Evento> response = eventoController.buscar(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(eventoService).buscarPorId(999L);
    }

    @Test
    void listarEventosFuturosQueGerencio_ComoAdminGeral_DeveRetornarLista() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.listarEventosFuturos()).thenReturn(listaEventos);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            ResponseEntity<List<Evento>> response = eventoController.listarEventosFuturosQueGerencio();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(2, Objects.requireNonNull(response.getBody()).size());
            verify(eventoService).listarEventosFuturos();
        }
    }

    @Test
    void listarEventosFuturosQueGerencio_QuandoNaoHaEventosGerenciaveis_DeveRetornarNoContent() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.listarEventosFuturos()).thenReturn(new ArrayList<>());

        ResponseEntity<List<Evento>> response = eventoController.listarEventosFuturosQueGerencio();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void listarTodosEventosQueGerencio_ComoAdminGeral_DeveRetornarLista() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.listarTodos()).thenReturn(listaEventos);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            ResponseEntity<List<Evento>> response = eventoController.listarTodosEventosQueGerencio();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(2, Objects.requireNonNull(response.getBody()).size());
            verify(eventoService).listarTodos();
        }
    }

    @Test
    void listarTodosEventosQueGerencio_QuandoNaoHaEventosGerenciaveis_DeveRetornarNoContent() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.listarTodos()).thenReturn(new ArrayList<>());

        ResponseEntity<List<Evento>> response = eventoController.listarTodosEventosQueGerencio();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void criar_EventoValido_DeveCriarERetornarOk() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);

        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setCampusId(1L);
        dto.setDepartamentoId(2L);
        dto.setTitulo("Novo Evento");
        dto.setDataInicio(LocalDateTime.now().plusDays(1));
        dto.setDataFim(LocalDateTime.now().plusDays(2));

        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(2L)).thenReturn(departamento);

        Evento eventoSalvo = new Evento();
        eventoSalvo.setId(300L);
        eventoSalvo.setTitulo("Novo Evento");

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            EventoUtils.EventoValidationData validData = mock(EventoUtils.EventoValidationData.class);
            when(validData.getCampus()).thenReturn(campus);
            when(validData.getDepartamento()).thenReturn(departamento);
            when(validData.getUsuarioLogado()).thenReturn(adminGeral);

            mockedEventoUtils.when(() -> EventoUtils.validarDadosIniciais(any(), any(), any(), any()))
                    .thenReturn(ResponseEntity.ok(validData));

            mockedEventoUtils.when(() -> EventoUtils.persistirEvento(any(), any(), any(), any(), any()))
                    .thenReturn(ResponseEntity.ok(eventoSalvo));

            ResponseEntity<Object> response = eventoController.criar(dto);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(eventoSalvo, response.getBody());
        }
    }

    @Test
    void criar_QuandoValidacaoFalhar_DeveRetornarErro() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);

        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setCampusId(999L);
        dto.setDepartamentoId(2L);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.validarDadosIniciais(any(), any(), any(), any()))
                    .thenReturn(ResponseEntity.badRequest().body("Campus não encontrado"));

            ResponseEntity<Object> response = eventoController.criar(dto);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(eventoService, never()).criarOuAtualizar(any(Evento.class));
        }
    }

    @Test
    void encerrarEvento_QuandoPossuiPermissao_DeveEncerrarComSucesso() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            ResponseEntity<Object> response = eventoController.encerrarEvento(100L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(eventoService).criarOuAtualizar(argThat(evt ->
                    evt.getStatus() == ENCERRADO && evt.getDataFim() != null
            ));
        }
    }

    @Test
    void encerrarEvento_QuandoEventoNaoExiste_DeveRetornarNotFound() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Object> response = eventoController.encerrarEvento(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(eventoService, never()).criarOuAtualizar(any(Evento.class));
    }

    @Test
    void encerrarEvento_QuandoSemPermissao_DeveRetornar403() {
        when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
        when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(userLogado);
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado"));

            ResponseEntity<Object> response = eventoController.encerrarEvento(100L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            verify(eventoService, never()).criarOuAtualizar(any(Evento.class));
        }
    }

    @Test
    void atualizar_QuandoValido_DeveAtualizarComSucesso() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);
        when(campusService.buscarPorId(1L)).thenReturn(campus);
        when(departamentoService.buscarPorId(2L)).thenReturn(departamento);

        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setCampusId(1L);
        dto.setDepartamentoId(2L);
        dto.setTitulo("Evento Atualizado");
        dto.setDataInicio(LocalDateTime.now().plusDays(1));
        dto.setDataFim(LocalDateTime.now().plusDays(2));

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            EventoUtils.EventoValidationData validData = mock(EventoUtils.EventoValidationData.class);
            when(validData.getCampus()).thenReturn(campus);
            when(validData.getDepartamento()).thenReturn(departamento);
            when(validData.getUsuarioLogado()).thenReturn(adminGeral);

            mockedEventoUtils.when(() -> EventoUtils.validarDadosIniciais(any(), any(), any(), any()))
                    .thenReturn(ResponseEntity.ok(validData));

            mockedEventoUtils.when(() -> EventoUtils.persistirEvento(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ResponseEntity.ok(evento1));

            ResponseEntity<Object> response = eventoController.atualizar(100L, dto);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    void atualizar_QuandoEventoNaoEncontrado_DeveRetornarNotFound() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(999L)).thenReturn(null);

        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setCampusId(1L);
        dto.setDepartamentoId(2L);

        ResponseEntity<Object> response = eventoController.atualizar(999L, dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(eventoService, never()).criarOuAtualizar(any(Evento.class));
    }

    @Test
    void atualizar_QuandoDadosInvalidos_DeveRetornarErro() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);

        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setCampusId(999L);
        dto.setDepartamentoId(2L);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            mockedEventoUtils.when(() -> EventoUtils.validarDadosIniciais(any(), any(), any(), any()))
                    .thenReturn(ResponseEntity.badRequest().body("Campus não encontrado"));

            ResponseEntity<Object> response = eventoController.atualizar(100L, dto);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(eventoService, never()).criarOuAtualizar(any(Evento.class));
        }
    }

    @Test
    void deletar_QuandoPossuiPermissao_DeveDeletarComSucesso() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            ResponseEntity<Object> response = eventoController.deletar(100L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            verify(eventoService).deletar(100L);
        }
    }

    @Test
    void deletar_QuandoEventoNaoEncontrado_DeveRetornarNotFound() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Object> response = eventoController.deletar(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(eventoService, never()).deletar(anyLong());
    }

    @Test
    void listarInscritos_QuandoPossuiPermissao_DeveRetornarListaInscritos() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);
        when(inscricaoService.listarInscricoesDoEvento(100L)).thenReturn(listaInscricoes);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            ResponseEntity<Object> response = eventoController.listarInscritos(100L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(List.class, response.getBody());
            @SuppressWarnings("unchecked")
            List<Inscricao> inscricoesResp = (List<Inscricao>) response.getBody();
            assertEquals(1, inscricoesResp.size());
            verify(inscricaoService).listarInscricoesDoEvento(100L);
        }
    }

    @Test
    void listarInscritos_QuandoEventoNaoExiste_DeveRetornarNotFound() {
        when(authentication.getName()).thenReturn("admin@ifg.edu.br");
        when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
        when(eventoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Object> response = eventoController.listarInscritos(999L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(inscricaoService, never()).listarInscricoesDoEvento(anyLong());
    }

    @Test
    void listarInscritos_QuandoSemPermissao_DeveRetornar403() {
        when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
        when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(userLogado);
        when(eventoService.buscarPorId(100L)).thenReturn(evento1);

        try (MockedStatic<EventoUtils> mockedEventoUtils = Mockito.mockStatic(EventoUtils.class)) {
            mockedEventoUtils.when(() -> EventoUtils.checarPermissaoEvento(any(), any()))
                    .thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado"));

            ResponseEntity<Object> response = eventoController.listarInscritos(100L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            verify(inscricaoService, never()).listarInscricoesDoEvento(anyLong());
        }
    }

}