package br.edu.ifg.event_system.repository;

import br.edu.ifg.event_system.model.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, Long> {
}