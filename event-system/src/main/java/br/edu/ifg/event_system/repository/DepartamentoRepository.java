package br.edu.ifg.event_system.repository;

import br.edu.ifg.event_system.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    List<Departamento> findByCampusId(Long campusId);
}
