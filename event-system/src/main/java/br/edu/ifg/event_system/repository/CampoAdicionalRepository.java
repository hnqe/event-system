package br.edu.ifg.event_system.repository;

import br.edu.ifg.event_system.model.CampoAdicional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampoAdicionalRepository extends JpaRepository<CampoAdicional, Long> {

    List<CampoAdicional> findByEventoId(Long eventoId);

}