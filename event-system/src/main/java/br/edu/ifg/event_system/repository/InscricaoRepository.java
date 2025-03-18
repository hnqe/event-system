package br.edu.ifg.event_system.repository;

import br.edu.ifg.event_system.model.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    List<Inscricao> findByUserId(Long userId);

    List<Inscricao> findByEventoId(Long eventoId);

    Optional<Inscricao> findByUserIdAndEventoId(Long userId, Long eventoId);

    long countByEventoId(Long eventoId);

}
