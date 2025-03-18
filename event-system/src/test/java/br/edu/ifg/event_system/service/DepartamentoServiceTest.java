package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.repository.DepartamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartamentoServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;

    @InjectMocks
    private DepartamentoService departamentoService;

    private Departamento departamento;
    private Campus campus;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(1L);
        campus.setNome("Campus Teste");

        departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNome("Departamento Teste");
        departamento.setCampus(campus);
    }

    @Test
    void listarTodos_DeveRetornarListaDeDepartamentos() {
        List<Departamento> departamentos = Collections.singletonList(departamento);
        when(departamentoRepository.findAll()).thenReturn(departamentos);

        List<Departamento> resultado = departamentoService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Departamento Teste", resultado.get(0).getNome());
        verify(departamentoRepository).findAll();
    }

    @Test
    void listarPorCampus_DeveRetornarDepartamentosDoCampus() {
        List<Departamento> departamentos = Collections.singletonList(departamento);
        when(departamentoRepository.findByCampusId(1L)).thenReturn(departamentos);

        List<Departamento> resultado = departamentoService.listarPorCampus(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Departamento Teste", resultado.get(0).getNome());
        assertEquals(1L, resultado.get(0).getCampus().getId());
        verify(departamentoRepository).findByCampusId(1L);
    }

    @Test
    void criarOuAtualizar_DeveSalvarERetornarDepartamento() {
        when(departamentoRepository.save(any(Departamento.class))).thenReturn(departamento);

        Departamento resultado = departamentoService.criarOuAtualizar(departamento);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Departamento Teste", resultado.getNome());
        verify(departamentoRepository).save(departamento);
    }

    @Test
    void buscarPorId_ComIdExistente_DeveRetornarDepartamento() {
        when(departamentoRepository.findById(1L)).thenReturn(Optional.of(departamento));

        Departamento resultado = departamentoService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Departamento Teste", resultado.getNome());
        verify(departamentoRepository).findById(1L);
    }

    @Test
    void buscarPorId_ComIdInexistente_DeveRetornarNull() {
        when(departamentoRepository.findById(999L)).thenReturn(Optional.empty());

        Departamento resultado = departamentoService.buscarPorId(999L);

        assertNull(resultado);
        verify(departamentoRepository).findById(999L);
    }

    @Test
    void deletar_DeveChamarDeleteById() {
        doNothing().when(departamentoRepository).deleteById(1L);

        departamentoService.deletar(1L);

        verify(departamentoRepository).deleteById(1L);
    }

}