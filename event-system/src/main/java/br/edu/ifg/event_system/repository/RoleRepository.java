package br.edu.ifg.event_system.repository;

import br.edu.ifg.event_system.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String name);

}