package br.edu.ifg.event_system.repository;

import br.edu.ifg.event_system.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByCampusId(Long campusId);
    List<Evento> findByDepartamentoId(Long departamentoId);
    List<Evento> findByCampusIdAndDepartamentoId(Long campusId, Long departamentoId);

    @Query("SELECT e FROM Evento e " +
            "WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "   OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Evento> searchByTituloOrDescricao(@Param("texto") String texto);

    @Query("SELECT e FROM Evento e WHERE e.dataFim IS NULL OR e.dataFim > :agora")
    List<Evento> findEventosFuturos(@Param("agora") LocalDateTime agora);

}