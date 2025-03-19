package br.edu.ifg.event_system.integration;

import br.edu.ifg.event_system.model.*;
import br.edu.ifg.event_system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class RepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    private Campus campus;
    private Departamento departamento;
    private User user;
    private Evento evento;

    @BeforeEach
    @Override
    void setUp() {
        inscricaoRepository.deleteAll();
        eventoRepository.deleteAll();
        userRepository.deleteAll();
        departamentoRepository.deleteAll();
        campusRepository.deleteAll();

        campus = new Campus();
        campus.setNome("Campus Goiânia");
        campus.setDepartamentos(new ArrayList<>());
        campusRepository.save(campus);

        departamento = new Departamento();
        departamento.setNome("Departamento de Informática");
        departamento.setCampus(campus);
        campus.getDepartamentos().add(departamento);
        departamentoRepository.save(departamento);
        campusRepository.save(campus);

        user = new User();
        user.setUsername("aluno@ifg.edu.br");
        user.setPassword("encoded_password");
        user.setNomeCompleto("Aluno Teste");
        userRepository.save(user);

        evento = new Evento();
        evento.setTitulo("Evento Teste");
        evento.setDescricao("Descrição do evento");
        evento.setLocal("Auditório Principal");
        evento.setDataInicio(LocalDateTime.now().plusDays(1));
        evento.setDataFim(LocalDateTime.now().plusDays(2));
        evento.setCampus(campus);
        evento.setDepartamento(departamento);
        evento.setStatus(Evento.EventoStatus.ATIVO);
        evento.setVagas(100);
        eventoRepository.save(evento);
    }

    @Test
    void eventoRepository_DeveSalvarERecuperarEvento() {
        Optional<Evento> found = eventoRepository.findById(evento.getId());

        assertTrue(found.isPresent());
        assertEquals("Evento Teste", found.get().getTitulo());
        assertEquals("Auditório Principal", found.get().getLocal());
        assertEquals(campus.getId(), found.get().getCampus().getId());
        assertEquals(departamento.getId(), found.get().getDepartamento().getId());
    }

    @Test
    void eventoRepository_DeveBuscarEventosPorCampus() {
        List<Evento> eventos = eventoRepository.findByCampusId(campus.getId());
        assertFalse(eventos.isEmpty());
        assertEquals(1, eventos.size());
        assertEquals("Evento Teste", eventos.get(0).getTitulo());
    }

    @Test
    void eventoRepository_DeveBuscarEventosPorDepartamento() {
        List<Evento> eventos = eventoRepository.findByDepartamentoId(departamento.getId());
        assertFalse(eventos.isEmpty());
        assertEquals(1, eventos.size());
        assertEquals("Evento Teste", eventos.get(0).getTitulo());
    }

    @Test
    void inscricaoRepository_DeveSalvarERecuperarInscricao() {
        Inscricao inscricao = new Inscricao();
        inscricao.setEvento(evento);
        inscricao.setUser(user);
        inscricao.setDataInscricao(LocalDateTime.now());
        inscricao.setStatus("CONFIRMADA");
        inscricaoRepository.save(inscricao);

        Optional<Inscricao> found = inscricaoRepository.findById(inscricao.getId());

        assertTrue(found.isPresent());
        assertEquals(evento.getId(), found.get().getEvento().getId());
        assertEquals(user.getId(), found.get().getUser().getId());
        assertEquals("CONFIRMADA", found.get().getStatus());
    }

    @Test
    void inscricaoRepository_DeveBuscarInscricoesPorEvento() {
        Inscricao inscricao = new Inscricao();
        inscricao.setEvento(evento);
        inscricao.setUser(user);
        inscricao.setDataInscricao(LocalDateTime.now());
        inscricao.setStatus("CONFIRMADA");
        inscricaoRepository.save(inscricao);

        List<Inscricao> inscricoes = inscricaoRepository.findByEventoId(evento.getId());

        assertFalse(inscricoes.isEmpty());
        assertEquals(1, inscricoes.size());
        assertEquals(user.getId(), inscricoes.get(0).getUser().getId());
    }

    @Test
    void inscricaoRepository_DeveBuscarInscricoesPorUsuario() {
        Inscricao inscricao = new Inscricao();
        inscricao.setEvento(evento);
        inscricao.setUser(user);
        inscricao.setDataInscricao(LocalDateTime.now());
        inscricao.setStatus("CONFIRMADA");
        inscricaoRepository.save(inscricao);

        List<Inscricao> inscricoes = inscricaoRepository.findByUserId(user.getId());

        assertFalse(inscricoes.isEmpty());
        assertEquals(1, inscricoes.size());
        assertEquals(evento.getId(), inscricoes.get(0).getEvento().getId());
    }

    @Test
    void userRepository_DeveEncontrarPorUsername() {
        User found = userRepository.findByUsername("aluno@ifg.edu.br");
        assertNotNull(found);
        assertEquals("Aluno Teste", found.getNomeCompleto());
    }

    @Test
    void campus_DeveTerRelacionamentoCorretoComDepartamento() {
        Campus savedCampus = campusRepository.findById(campus.getId()).orElseThrow();

        assertNotNull(savedCampus.getDepartamentos());
        assertFalse(savedCampus.getDepartamentos().isEmpty());

        Departamento dept = savedCampus.getDepartamentos().iterator().next();
        assertEquals("Departamento de Informática", dept.getNome());
        assertEquals(campus.getId(), dept.getCampus().getId());
    }
}