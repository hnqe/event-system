package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.service.CampusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusControllerTest {

    @Mock
    private CampusService campusService;

    @InjectMocks
    private CampusController campusController;

    private Campus campus1;
    private Campus campus2;
    private List<Campus> listaCampus;

    @BeforeEach
    void setUp() {
        campus1 = new Campus();
        campus1.setId(1L);
        campus1.setNome("Campus Goiânia");
        campus1.setDepartamentos(new ArrayList<>());

        Departamento departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNome("Departamento de Informática");
        departamento.setCampus(campus1);
        campus1.getDepartamentos().add(departamento);

        campus2 = new Campus();
        campus2.setId(2L);
        campus2.setNome("Campus Anápolis");
        campus2.setDepartamentos(new ArrayList<>());

        listaCampus = new ArrayList<>();
        listaCampus.add(campus1);
        listaCampus.add(campus2);
    }

    @Test
    void listar_DeveRetornarTodosCampus() {
        when(campusService.listarTodos()).thenReturn(listaCampus);

        ResponseEntity<List<Campus>> response = campusController.listar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        verify(campusService).listarTodos();
    }

    @Test
    void buscarPorId_QuandoCampusExiste_DeveRetornarCampus() {
        when(campusService.buscarPorId(1L)).thenReturn(campus1);

        ResponseEntity<Campus> response = campusController.buscarPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(campus1, response.getBody());
        verify(campusService).buscarPorId(1L);
    }

    @Test
    void buscarPorId_QuandoCampusNaoExiste_DeveRetornarNotFound() {
        when(campusService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Campus> response = campusController.buscarPorId(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(campusService).buscarPorId(999L);
    }

    @Test
    void criar_DeveRetornarCampusSalvo() {
        Campus novoCampus = new Campus();
        novoCampus.setNome("Campus Novo");

        Campus campusSalvo = new Campus();
        campusSalvo.setId(3L);
        campusSalvo.setNome("Campus Novo");
        campusSalvo.setDepartamentos(new ArrayList<>());

        when(campusService.criarOuAtualizar(any(Campus.class))).thenReturn(campusSalvo);

        ResponseEntity<Campus> response = campusController.criar(novoCampus);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(campusSalvo, response.getBody());
        verify(campusService).criarOuAtualizar(any(Campus.class));
    }

    @Test
    void atualizar_QuandoCampusExiste_DeveRetornarCampusAtualizado() {
        Campus campusAtualizar = new Campus();
        campusAtualizar.setNome("Campus Goiânia Atualizado");

        Campus campusAtualizado = new Campus();
        campusAtualizado.setId(1L);
        campusAtualizado.setNome("Campus Goiânia Atualizado");
        campusAtualizado.setDepartamentos(new ArrayList<>());

        when(campusService.buscarPorId(1L)).thenReturn(campus1);
        when(campusService.criarOuAtualizar(any(Campus.class))).thenReturn(campusAtualizado);

        ResponseEntity<Campus> response = campusController.atualizar(1L, campusAtualizar);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(campusAtualizado, response.getBody());
        verify(campusService).buscarPorId(1L);
        verify(campusService).criarOuAtualizar(any(Campus.class));
    }

    @Test
    void atualizar_QuandoCampusNaoExiste_DeveRetornarNotFound() {
        Campus campusAtualizar = new Campus();
        campusAtualizar.setNome("Campus Inexistente");

        when(campusService.buscarPorId(999L)).thenReturn(null);

        ResponseEntity<Campus> response = campusController.atualizar(999L, campusAtualizar);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(campusService).buscarPorId(999L);
        verify(campusService, never()).criarOuAtualizar(any(Campus.class));
    }

    @Test
    void deletar_QuandoSucesso_DeveRetornarNoContent() {
        ResponseEntity<String> response = campusController.deletar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(campusService).deletar(1L);
    }

    @Test
    void deletar_QuandoDataIntegrityViolation_DeveRetornarBadRequest() {
        doThrow(new DataIntegrityViolationException("Could not execute statement"))
                .when(campusService).deletar(1L);

        ResponseEntity<String> response = campusController.deletar(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Não foi possível remover o campus porque ainda existem outras referências a ele no sistema.",
                response.getBody());
        verify(campusService).deletar(1L);
    }

    @Test
    void deletar_QuandoDataIntegrityViolationComUsuarios_DeveRetornarMensagemEspecifica() {
        doThrow(new DataIntegrityViolationException("Could not execute statement; SQL []; constraint [user_campus]"))
                .when(campusService).deletar(1L);

        ResponseEntity<String> response = campusController.deletar(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Não foi possível remover o campus pois existem usuários vinculados a ele. " +
                        "Remova primeiro as associações de usuários com este campus.",
                response.getBody());
        verify(campusService).deletar(1L);
    }

}