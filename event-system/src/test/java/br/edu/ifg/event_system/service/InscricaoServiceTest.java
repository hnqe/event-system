package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.dto.CampoValorDTO;
import br.edu.ifg.event_system.dto.InscricaoRequestDTO;
import br.edu.ifg.event_system.exception.InscricaoException;
import br.edu.ifg.event_system.model.CampoAdicional;
import br.edu.ifg.event_system.model.Evento;
import br.edu.ifg.event_system.model.Inscricao;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.InscricaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceTest {

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private ApplicationContext applicationContext;

    @Spy
    @InjectMocks
    private InscricaoService inscricaoService;

    private User user;
    private Evento evento;
    private Inscricao inscricao;
    private List<CampoValorDTO> camposValoresDTO;
    private List<CampoAdicional> camposAdicionais;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@ifg.edu.br");
        user.setNomeCompleto("Usuário Teste");

        evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Evento Teste");
        evento.setVagas(100);
        evento.setDataLimiteInscricao(LocalDateTime.now().plusDays(7));

        camposAdicionais = new ArrayList<>();
        CampoAdicional campoObrigatorio = new CampoAdicional();
        campoObrigatorio.setId(1L);
        campoObrigatorio.setNome("Campo Obrigatório");
        campoObrigatorio.setObrigatorio(true);
        campoObrigatorio.setTipo("TEXT");
        campoObrigatorio.setEvento(evento);

        CampoAdicional campoOpcional = new CampoAdicional();
        campoOpcional.setId(2L);
        campoOpcional.setNome("Campo Opcional");
        campoOpcional.setObrigatorio(false);
        campoOpcional.setTipo("TEXT");
        campoOpcional.setEvento(evento);

        camposAdicionais.add(campoObrigatorio);
        camposAdicionais.add(campoOpcional);
        evento.setCamposAdicionais(camposAdicionais);

        camposValoresDTO = new ArrayList<>();
        CampoValorDTO campoValorDTO = new CampoValorDTO();
        campoValorDTO.setCampoId(1L);
        campoValorDTO.setValor("Valor do campo obrigatório");
        camposValoresDTO.add(campoValorDTO);

        inscricao = new Inscricao();
        inscricao.setId(1L);
        inscricao.setUser(user);
        inscricao.setEvento(evento);
        inscricao.setDataInscricao(LocalDateTime.now());
        inscricao.setStatus("ATIVA");
        inscricao.setCamposValores(new ArrayList<>());

        lenient().when(applicationContext.getBean(InscricaoService.class)).thenReturn(inscricaoService);
    }

    @Test
    void inscreverUsuarioEmEvento_DeveCriarInscricaoSemCamposValores() {
        when(inscricaoRepository.findByUserIdAndEventoId(1L, 1L)).thenReturn(Optional.empty());
        when(inscricaoRepository.countByEventoId(1L)).thenReturn(0L);
        when(inscricaoRepository.save(any(Inscricao.class))).thenReturn(inscricao);

        Inscricao result = inscricaoService.inscreverUsuarioEmEvento(user, evento);

        assertNotNull(result);
        assertEquals("ATIVA", result.getStatus());
        assertEquals(user, result.getUser());
        assertEquals(evento, result.getEvento());

        verify(inscricaoRepository).findByUserIdAndEventoId(1L, 1L);
        verify(inscricaoRepository).countByEventoId(1L);
        verify(inscricaoRepository).save(any(Inscricao.class));
    }

    @Test
    void inscreverUsuarioEmEvento_DeveCriarInscricaoComCamposValores() {
        when(inscricaoRepository.findByUserIdAndEventoId(1L, 1L)).thenReturn(Optional.empty());
        when(inscricaoRepository.countByEventoId(1L)).thenReturn(0L);
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> {
            Inscricao insc = invocation.getArgument(0);
            insc.setId(1L);
            return insc;
        });

        Inscricao result = inscricaoService.inscreverUsuarioEmEvento(user, evento, camposValoresDTO);

        assertNotNull(result);
        assertEquals("ATIVA", result.getStatus());
        assertEquals(user, result.getUser());
        assertEquals(evento, result.getEvento());
        assertEquals(1, result.getCamposValores().size());

        verify(inscricaoRepository).findByUserIdAndEventoId(1L, 1L);
        verify(inscricaoRepository).countByEventoId(1L);
        verify(inscricaoRepository).save(any(Inscricao.class));
    }

    @Test
    void inscreverUsuarioEmEvento_DeveReativarInscricaoCancelada() {
        Inscricao inscricaoCancelada = new Inscricao();
        inscricaoCancelada.setId(1L);
        inscricaoCancelada.setUser(user);
        inscricaoCancelada.setEvento(evento);
        inscricaoCancelada.setStatus("CANCELADA");
        inscricaoCancelada.setCamposValores(new ArrayList<>());

        when(inscricaoRepository.findByUserIdAndEventoId(1L, 1L)).thenReturn(Optional.of(inscricaoCancelada));
        when(inscricaoRepository.countByEventoId(1L)).thenReturn(0L);
        when(inscricaoRepository.save(any(Inscricao.class))).thenReturn(inscricaoCancelada);

        Inscricao result = inscricaoService.inscreverUsuarioEmEvento(user, evento, camposValoresDTO);

        assertNotNull(result);
        assertEquals("ATIVA", result.getStatus());
        assertEquals(1, result.getCamposValores().size());

        verify(inscricaoRepository).save(any(Inscricao.class));
    }

    @Test
    void inscreverUsuarioEmEvento_DeveLancarExcecaoQuandoJaInscrito() {
        Inscricao inscricaoAtiva = new Inscricao();
        inscricaoAtiva.setId(1L);
        inscricaoAtiva.setUser(user);
        inscricaoAtiva.setEvento(evento);
        inscricaoAtiva.setStatus("ATIVA");

        when(inscricaoRepository.findByUserIdAndEventoId(1L, 1L)).thenReturn(Optional.of(inscricaoAtiva));

        InscricaoException exception = assertThrows(InscricaoException.class, () -> {
            inscricaoService.inscreverUsuarioEmEvento(user, evento);
        });

        assertEquals("Você já está inscrito neste evento!", exception.getMessage());
        verify(inscricaoRepository, never()).save(any(Inscricao.class));
    }

    @Test
    void inscreverUsuarioEmEvento_DeveLancarExcecaoQuandoAposDataLimite() {
        evento.setDataLimiteInscricao(LocalDateTime.now().minusDays(1));

        InscricaoException exception = assertThrows(InscricaoException.class, () -> {
            inscricaoService.inscreverUsuarioEmEvento(user, evento);
        });

        assertEquals("As inscrições para este evento já foram encerradas!", exception.getMessage());
        verify(inscricaoRepository, never()).findByUserIdAndEventoId(anyLong(), anyLong());
        verify(inscricaoRepository, never()).save(any(Inscricao.class));
    }

    @Test
    void inscreverUsuarioEmEvento_DeveLancarExcecaoQuandoSemVagas() {
        when(inscricaoRepository.findByUserIdAndEventoId(1L, 1L)).thenReturn(Optional.empty());
        when(inscricaoRepository.countByEventoId(1L)).thenReturn(100L);

        InscricaoException exception = assertThrows(InscricaoException.class, () -> {
            inscricaoService.inscreverUsuarioEmEvento(user, evento);
        });

        assertEquals("Não há vagas disponíveis neste evento!", exception.getMessage());
        verify(inscricaoRepository).findByUserIdAndEventoId(1L, 1L);
        verify(inscricaoRepository).countByEventoId(1L);
        verify(inscricaoRepository, never()).save(any(Inscricao.class));
    }

    @Test
    void inscreverUsuarioEmEvento_DeveLancarExcecaoQuandoFaltaCampoObrigatorio() {
        InscricaoService realService = new InscricaoService(inscricaoRepository, applicationContext);

        when(inscricaoRepository.findByUserIdAndEventoId(1L, 1L)).thenReturn(Optional.empty());
        when(inscricaoRepository.countByEventoId(1L)).thenReturn(0L);
        lenient().when(applicationContext.getBean(InscricaoService.class)).thenReturn(realService);

        List<CampoValorDTO> camposIncompletos = new ArrayList<>();
        CampoValorDTO campoOpcionalDTO = new CampoValorDTO();
        campoOpcionalDTO.setCampoId(2L);
        campoOpcionalDTO.setValor("Valor do campo opcional");
        camposIncompletos.add(campoOpcionalDTO);

        InscricaoException exception = assertThrows(InscricaoException.class, () -> {
            realService.inscreverUsuarioEmEvento(user, evento, camposIncompletos);
        });

        assertTrue(exception.getMessage().contains("Os seguintes campos obrigatórios não foram preenchidos"));
        assertTrue(exception.getMessage().contains("Campo Obrigatório"));
    }

    @Test
    void processarInscricao_DeveDelegarParaInscreverUsuario() {
        InscricaoRequestDTO requestDTO = new InscricaoRequestDTO();
        requestDTO.setCamposValores(camposValoresDTO);

        doReturn(inscricao).when(inscricaoService).inscreverUsuarioEmEvento(user, evento, camposValoresDTO);

        Inscricao result = inscricaoService.processarInscricao(user, requestDTO, evento);

        assertNotNull(result);
        assertEquals(inscricao, result);
        verify(inscricaoService).inscreverUsuarioEmEvento(user, evento, camposValoresDTO);
    }

    @Test
    void cancelarInscricao_DeveAtualizarStatusParaCancelada() {
        when(inscricaoRepository.findById(1L)).thenReturn(Optional.of(inscricao));

        inscricaoService.cancelarInscricao(1L);

        assertEquals("CANCELADA", inscricao.getStatus());
        verify(inscricaoRepository).save(inscricao);
    }

    @Test
    void cancelarInscricao_DeveLancarExcecaoQuandoNaoEncontrada() {
        when(inscricaoRepository.findById(999L)).thenReturn(Optional.empty());

        InscricaoException exception = assertThrows(InscricaoException.class, () -> {
            inscricaoService.cancelarInscricao(999L);
        });

        assertEquals("Inscrição não encontrada!", exception.getMessage());
        verify(inscricaoRepository, never()).save(any(Inscricao.class));
    }

    @Test
    void listarInscricoesDoUsuario_DeveRetornarListaDeInscricoes() {
        List<Inscricao> inscricoes = List.of(inscricao);
        when(inscricaoRepository.findByUserId(1L)).thenReturn(inscricoes);

        List<Inscricao> result = inscricaoService.listarInscricoesDoUsuario(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(inscricao, result.get(0));
        verify(inscricaoRepository).findByUserId(1L);
    }

    @Test
    void listarInscricoesDoEvento_DeveRetornarListaDeInscricoes() {
        List<Inscricao> inscricoes = List.of(inscricao);
        when(inscricaoRepository.findByEventoId(1L)).thenReturn(inscricoes);

        List<Inscricao> result = inscricaoService.listarInscricoesDoEvento(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(inscricao, result.get(0));
        verify(inscricaoRepository).findByEventoId(1L);
    }

    @Test
    void buscarPorId_DeveRetornarInscricao() {
        when(inscricaoRepository.findById(1L)).thenReturn(Optional.of(inscricao));

        Inscricao result = inscricaoService.buscarPorId(1L);

        assertNotNull(result);
        assertEquals(inscricao, result);
        verify(inscricaoRepository).findById(1L);
    }

    @Test
    void buscarPorId_DeveRetornarNullQuandoNaoEncontrada() {
        when(inscricaoRepository.findById(999L)).thenReturn(Optional.empty());

        Inscricao result = inscricaoService.buscarPorId(999L);

        assertNull(result);
        verify(inscricaoRepository).findById(999L);
    }

}