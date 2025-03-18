package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Evento;
import br.edu.ifg.event_system.repository.EventoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private EventoService eventoService;

    private Evento evento;

    @BeforeEach
    void setUp() {
        evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Evento Teste");
        evento.setDescricao("Descrição do Evento Teste");
        evento.setDataInicio(LocalDateTime.now().plusDays(1));
        evento.setDataFim(LocalDateTime.now().plusDays(2));

        ReflectionTestUtils.setField(eventoService, "entityManager", entityManager);
    }

    @Test
    void criarOuAtualizar_DeveSalvarERetornarEvento() {
        when(eventoRepository.save(any(Evento.class))).thenReturn(evento);

        Evento resultado = eventoService.criarOuAtualizar(evento);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Evento Teste", resultado.getTitulo());
        verify(eventoRepository).save(evento);
    }

    @Test
    void listarTodos_DeveRetornarListaDeEventos() {
        List<Evento> eventos = Collections.singletonList(evento);
        when(eventoRepository.findAll()).thenReturn(eventos);

        List<Evento> resultado = eventoService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Evento Teste", resultado.get(0).getTitulo());
        verify(eventoRepository).findAll();
    }

    @Test
    void buscarPorId_ComIdExistente_DeveRetornarEvento() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        Evento resultado = eventoService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Evento Teste", resultado.getTitulo());
        verify(eventoRepository).findById(1L);
    }

    @Test
    void buscarPorId_ComIdInexistente_DeveRetornarNull() {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        Evento resultado = eventoService.buscarPorId(999L);

        assertNull(resultado);
        verify(eventoRepository).findById(999L);
    }

    @Test
    void buscarPorTituloOuDescricao_DeveRetornarEventosEncontrados() {
        List<Evento> eventos = Collections.singletonList(evento);
        String termo = "Teste";
        when(eventoRepository.searchByTituloOrDescricao(termo)).thenReturn(eventos);

        List<Evento> resultado = eventoService.buscarPorTituloOuDescricao(termo);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Evento Teste", resultado.get(0).getTitulo());
        verify(eventoRepository).searchByTituloOrDescricao(termo);
    }

    @Test
    void deletar_ComIdExistente_DeveDeletarEvento() {
        when(eventoRepository.existsById(1L)).thenReturn(true);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);

        eventoService.deletar(1L);

        verify(eventoRepository).existsById(1L);
        verify(entityManager, times(4)).createNativeQuery(anyString());
        verify(query, times(4)).setParameter(anyInt(), any());
        verify(query, times(4)).executeUpdate();
    }

    @Test
    void deletar_ComIdInexistente_NaoDeveFazerNada() {
        when(eventoRepository.existsById(999L)).thenReturn(false);

        eventoService.deletar(999L);

        verify(eventoRepository).existsById(999L);
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void listarEventosFuturos_DeveRetornarEventosFuturos() {
        List<Evento> eventos = Collections.singletonList(evento);
        when(eventoRepository.findEventosFuturos(any(LocalDateTime.class))).thenReturn(eventos);

        List<Evento> resultado = eventoService.listarEventosFuturos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Evento Teste", resultado.get(0).getTitulo());
        verify(eventoRepository).findEventosFuturos(any(LocalDateTime.class));
    }

    @Test
    void listarPorCampus_DeveRetornarEventosDoCampus() {
        List<Evento> eventos = Collections.singletonList(evento);
        when(eventoRepository.findByCampusId(1L)).thenReturn(eventos);

        List<Evento> resultado = eventoService.listarPorCampus(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Evento Teste", resultado.get(0).getTitulo());
        verify(eventoRepository).findByCampusId(1L);
    }

    @Test
    void listarPorDepartamento_DeveRetornarEventosDoDepartamento() {
        List<Evento> eventos = Collections.singletonList(evento);
        when(eventoRepository.findByDepartamentoId(1L)).thenReturn(eventos);

        List<Evento> resultado = eventoService.listarPorDepartamento(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Evento Teste", resultado.get(0).getTitulo());
        verify(eventoRepository).findByDepartamentoId(1L);
    }

    @Test
    void listarPorCampusEDepartamento_DeveRetornarEventosDoCampusEDepartamento() {
        List<Evento> eventos = Collections.singletonList(evento);
        when(eventoRepository.findByCampusIdAndDepartamentoId(1L, 1L)).thenReturn(eventos);

        List<Evento> resultado = eventoService.listarPorCampusEDepartamento(1L, 1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Evento Teste", resultado.get(0).getTitulo());
        verify(eventoRepository).findByCampusIdAndDepartamentoId(1L, 1L);
    }

}