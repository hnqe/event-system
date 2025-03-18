package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Evento;
import br.edu.ifg.event_system.repository.EventoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public Evento criarOuAtualizar(Evento evento) {
        return eventoRepository.save(evento);
    }

    public List<Evento> listarTodos() {
        return eventoRepository.findAll();
    }

    public Evento buscarPorId(Long id) {
        return eventoRepository.findById(id).orElse(null);
    }

    public List<Evento> buscarPorTituloOuDescricao(String texto) {
        return eventoRepository.searchByTituloOrDescricao(texto);
    }

    @Transactional
    public void deletar(Long id) {
        if (!eventoRepository.existsById(id)) {
            return;
        }

        entityManager.createNativeQuery(
                        "DELETE FROM campo_valor WHERE inscricao_id IN " +
                                "(SELECT id FROM inscricao WHERE evento_id = ?)")
                .setParameter(1, id)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "DELETE FROM inscricao WHERE evento_id = ?")
                .setParameter(1, id)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "DELETE FROM campo_adicional WHERE evento_id = ?")
                .setParameter(1, id)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "DELETE FROM evento WHERE id = ?")
                .setParameter(1, id)
                .executeUpdate();
    }

    public List<Evento> listarEventosFuturos() {
        LocalDateTime agora = LocalDateTime.now();
        return eventoRepository.findEventosFuturos(agora);
    }

    public List<Evento> listarPorCampus(Long campusId) {
        return eventoRepository.findByCampusId(campusId);
    }

    public List<Evento> listarPorDepartamento(Long departamentoId) {
        return eventoRepository.findByDepartamentoId(departamentoId);
    }

    public List<Evento> listarPorCampusEDepartamento(Long campusId, Long departamentoId) {
        return eventoRepository.findByCampusIdAndDepartamentoId(campusId, departamentoId);
    }

}