package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.CampoValorDTO;
import br.edu.ifg.event_system.dto.InscricaoRequestDTO;
import br.edu.ifg.event_system.dto.InscricaoResponseDTO;
import br.edu.ifg.event_system.exception.InscricaoException;
import br.edu.ifg.event_system.model.*;
import br.edu.ifg.event_system.service.EventoService;
import br.edu.ifg.event_system.service.InscricaoService;
import br.edu.ifg.event_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscricaoControllerTest {

    @Mock
    private InscricaoService inscricaoService;

    @Mock
    private UserService userService;

    @Mock
    private EventoService eventoService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private InscricaoController inscricaoController;

    private User usuarioLogado;
    private User adminGeral;
    private Evento evento;
    private Inscricao inscricao;
    private List<Inscricao> inscricoes;
    private InscricaoRequestDTO inscricaoRequestDTO;
    private List<CampoAdicional> camposAdicionais;

    @BeforeEach
    void setUp() {
        // Configurar usuário comum
        usuarioLogado = new User();
        usuarioLogado.setId(1L);
        usuarioLogado.setUsername("usuario@ifg.edu.br");
        usuarioLogado.setNomeCompleto("Usuário Teste");
        usuarioLogado.setRoles(new ArrayList<>());

        // Configurar admin geral
        Role roleAdminGeral = new Role();
        roleAdminGeral.setId(1L);
        roleAdminGeral.setName("ADMIN_GERAL");

        adminGeral = new User();
        adminGeral.setId(2L);
        adminGeral.setUsername("admin@ifg.edu.br");
        adminGeral.setRoles(new ArrayList<>(List.of(roleAdminGeral)));

        camposAdicionais = new ArrayList<>();
        CampoAdicional campo1 = new CampoAdicional();
        campo1.setId(1L);
        campo1.setNome("campo1");
        campo1.setDescricao("Descrição Campo 1");

        CampoAdicional campo2 = new CampoAdicional();
        campo2.setId(2L);
        campo2.setNome("campo2");
        campo2.setDescricao("Descrição Campo 2");

        camposAdicionais.add(campo1);
        camposAdicionais.add(campo2);

        evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Evento Teste");
        evento.setDescricao("Descrição do Evento Teste");
        evento.setLocal("Local Teste");
        evento.setStatus(Evento.EventoStatus.ATIVO);
        evento.setCamposAdicionais(camposAdicionais);

        inscricao = new Inscricao();
        inscricao.setId(1L);
        inscricao.setUser(usuarioLogado);
        inscricao.setEvento(evento);
        inscricao.setStatus("CONFIRMADA");

        inscricoes = new ArrayList<>();
        inscricoes.add(inscricao);

        inscricaoRequestDTO = new InscricaoRequestDTO();
        inscricaoRequestDTO.setEventoId(1L);
        List<CampoValorDTO> camposValores = new ArrayList<>();

        CampoValorDTO campoValor1 = new CampoValorDTO();
        campoValor1.setCampoId(1L);
        campoValor1.setValor("Resposta 1");

        CampoValorDTO campoValor2 = new CampoValorDTO();
        campoValor2.setCampoId(2L);
        campoValor2.setValor("Resposta 2");

        camposValores.add(campoValor1);
        camposValores.add(campoValor2);
        inscricaoRequestDTO.setCamposValores(camposValores);
    }

    @Test
    void listarMinhasInscricoes_ComUsuarioLogado_DeveRetornarInscricoes() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(inscricaoService.listarInscricoesDoUsuario(1L)).thenReturn(inscricoes);

            ResponseEntity<Object> response = inscricaoController.listarMinhasInscricoes();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(List.class, response.getBody());

            @SuppressWarnings("unchecked")
            List<InscricaoResponseDTO> listaResponse = (List<InscricaoResponseDTO>) response.getBody();
            assertEquals(1, listaResponse.size());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(inscricaoService).listarInscricoesDoUsuario(1L);
        }
    }

    @Test
    void listarMinhasInscricoes_SemUsuarioLogado_DeveRetornarUnauthorized() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(null);

            ResponseEntity<Object> response = inscricaoController.listarMinhasInscricoes();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Usuário não logado.", response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(inscricaoService, never()).listarInscricoesDoUsuario(anyLong());
        }
    }

    @Test
    void inscreverNoEvento_ComUsuarioLogadoEEventoValido_DeveInscreverComSucesso() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(eventoService.buscarPorId(1L)).thenReturn(evento);
            when(inscricaoService.inscreverUsuarioEmEvento(usuarioLogado, evento)).thenReturn(inscricao);

            ResponseEntity<Object> response = inscricaoController.inscreverNoEvento(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(InscricaoResponseDTO.class, response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService).buscarPorId(1L);
            verify(inscricaoService).inscreverUsuarioEmEvento(usuarioLogado, evento);
        }
    }

    @Test
    void inscreverNoEvento_SemUsuarioLogado_DeveRetornarUnauthorized() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(null);

            ResponseEntity<Object> response = inscricaoController.inscreverNoEvento(1L);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Usuário não logado.", response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService, never()).buscarPorId(anyLong());
            verify(inscricaoService, never()).inscreverUsuarioEmEvento(any(), any());
        }
    }

    @Test
    void inscreverNoEvento_ComEventoInexistente_DeveRetornarBadRequest() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(eventoService.buscarPorId(999L)).thenReturn(null);

            ResponseEntity<Object> response = inscricaoController.inscreverNoEvento(999L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Evento inexistente.", response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService).buscarPorId(999L);
            verify(inscricaoService, never()).inscreverUsuarioEmEvento(any(), any());
        }
    }

    @Test
    void inscreverNoEvento_ComInscricaoException_DeveRetornarBadRequest() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(eventoService.buscarPorId(1L)).thenReturn(evento);
            when(inscricaoService.inscreverUsuarioEmEvento(usuarioLogado, evento))
                    .thenThrow(new InscricaoException("Erro de inscrição: evento já encerrado."));

            ResponseEntity<Object> response = inscricaoController.inscreverNoEvento(1L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Erro de inscrição: evento já encerrado.", response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService).buscarPorId(1L);
            verify(inscricaoService).inscreverUsuarioEmEvento(usuarioLogado, evento);
        }
    }

    @Test
    void inscreverNoEvento_ComException_DeveRetornarInternalServerError() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(eventoService.buscarPorId(1L)).thenReturn(evento);
            when(inscricaoService.inscreverUsuarioEmEvento(usuarioLogado, evento))
                    .thenThrow(new RuntimeException("Erro interno"));

            ResponseEntity<Object> response = inscricaoController.inscreverNoEvento(1L);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Erro ao inscrever: Erro interno", response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService).buscarPorId(1L);
            verify(inscricaoService).inscreverUsuarioEmEvento(usuarioLogado, evento);
        }
    }

    @Test
    void inscreverCompletoNoEvento_ComUsuarioLogadoEEventoValido_DeveInscreverComSucesso() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(eventoService.buscarPorId(1L)).thenReturn(evento);
            when(inscricaoService.processarInscricao(eq(usuarioLogado), any(InscricaoRequestDTO.class), eq(evento)))
                    .thenReturn(inscricao);

            ResponseEntity<Object> response = inscricaoController.inscreverCompletoNoEvento(inscricaoRequestDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertInstanceOf(InscricaoResponseDTO.class, response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService).buscarPorId(1L);
            verify(inscricaoService).processarInscricao(eq(usuarioLogado), any(InscricaoRequestDTO.class), eq(evento));
        }
    }

    @Test
    void inscreverCompletoNoEvento_SemUsuarioLogado_DeveRetornarUnauthorized() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(null);

            ResponseEntity<Object> response = inscricaoController.inscreverCompletoNoEvento(inscricaoRequestDTO);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Usuário não logado.", response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService, never()).buscarPorId(anyLong());
            verify(inscricaoService, never()).processarInscricao(any(), any(), any());
        }
    }

    @Test
    void inscreverCompletoNoEvento_ComEventoInexistente_DeveRetornarBadRequest() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(eventoService.buscarPorId(1L)).thenReturn(null);

            ResponseEntity<Object> response = inscricaoController.inscreverCompletoNoEvento(inscricaoRequestDTO);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Evento inexistente.", response.getBody());

            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(eventoService).buscarPorId(1L);
            verify(inscricaoService, never()).processarInscricao(any(), any(), any());
        }
    }

    @Test
    void cancelarInscricao_ComUsuarioDonoInscricao_DeveCancelarComSucesso() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(inscricaoService.buscarPorId(1L)).thenReturn(inscricao);
            doNothing().when(inscricaoService).cancelarInscricao(1L);

            ResponseEntity<String> response = inscricaoController.cancelarInscricao(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Inscrição cancelada (status CANCELADA) com sucesso!", response.getBody());

            verify(inscricaoService).buscarPorId(1L);
            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(inscricaoService).cancelarInscricao(1L);
        }
    }

    @Test
    void cancelarInscricao_ComAdminGeral_DeveCancelarComSucesso() {
        User outroUsuario = new User();
        outroUsuario.setId(3L);
        outroUsuario.setUsername("outro@ifg.edu.br");

        Inscricao inscricaoOutroUsuario = new Inscricao();
        inscricaoOutroUsuario.setId(2L);
        inscricaoOutroUsuario.setUser(outroUsuario);
        inscricaoOutroUsuario.setEvento(evento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin@ifg.edu.br");
            when(userService.buscarPorUsername("admin@ifg.edu.br")).thenReturn(adminGeral);
            when(inscricaoService.buscarPorId(2L)).thenReturn(inscricaoOutroUsuario);
            doNothing().when(inscricaoService).cancelarInscricao(2L);

            ResponseEntity<String> response = inscricaoController.cancelarInscricao(2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Inscrição cancelada (status CANCELADA) com sucesso!", response.getBody());

            verify(inscricaoService).buscarPorId(2L);
            verify(userService).buscarPorUsername("admin@ifg.edu.br");
            verify(inscricaoService).cancelarInscricao(2L);
        }
    }

    @Test
    void cancelarInscricao_ComInscricaoInexistente_DeveRetornarNotFound() {
        when(inscricaoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<String> response = inscricaoController.cancelarInscricao(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(inscricaoService).buscarPorId(999L);
        verify(inscricaoService, never()).cancelarInscricao(anyLong());
    }

    @Test
    void cancelarInscricao_ComUsuarioSemPermissao_DeveRetornarForbidden() {
        User outroUsuario = new User();
        outroUsuario.setId(3L);
        outroUsuario.setUsername("outro@ifg.edu.br");

        Inscricao inscricaoOutroUsuario = new Inscricao();
        inscricaoOutroUsuario.setId(2L);
        inscricaoOutroUsuario.setUser(outroUsuario);
        inscricaoOutroUsuario.setEvento(evento);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(inscricaoService.buscarPorId(2L)).thenReturn(inscricaoOutroUsuario);

            ResponseEntity<String> response = inscricaoController.cancelarInscricao(2L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Você não pode cancelar a inscrição de outro usuário!", response.getBody());

            verify(inscricaoService).buscarPorId(2L);
            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(inscricaoService, never()).cancelarInscricao(anyLong());
        }
    }

    @Test
    void cancelarInscricao_ComInscricaoException_DeveRetornarBadRequest() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("usuario@ifg.edu.br");
            when(userService.buscarPorUsername("usuario@ifg.edu.br")).thenReturn(usuarioLogado);
            when(inscricaoService.buscarPorId(1L)).thenReturn(inscricao);
            doThrow(new InscricaoException("Não é possível cancelar inscrição com status atual."))
                    .when(inscricaoService).cancelarInscricao(1L);

            ResponseEntity<String> response = inscricaoController.cancelarInscricao(1L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Não é possível cancelar inscrição com status atual.", response.getBody());

            verify(inscricaoService).buscarPorId(1L);
            verify(userService).buscarPorUsername("usuario@ifg.edu.br");
            verify(inscricaoService).cancelarInscricao(1L);
        }
    }

    @Test
    void listarCamposDoEvento_ComEventoExistente_DeveRetornarCampos() {
        when(eventoService.buscarPorId(1L)).thenReturn(evento);

        ResponseEntity<Object> response = inscricaoController.listarCamposDoEvento(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        List<CampoAdicional> campos = (List<CampoAdicional>) response.getBody();
        assertEquals(2, campos.size());
        assertEquals("campo1", campos.get(0).getNome());
        assertEquals("campo2", campos.get(1).getNome());

        verify(eventoService).buscarPorId(1L);
    }

    @Test
    void listarCamposDoEvento_ComEventoInexistente_DeveRetornarNotFound() {
        when(eventoService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Object> response = inscricaoController.listarCamposDoEvento(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(eventoService).buscarPorId(999L);
    }

}