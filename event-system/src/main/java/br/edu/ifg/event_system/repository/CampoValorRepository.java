package br.edu.ifg.event_system.repository;

import br.edu.ifg.event_system.model.CampoValor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CampoValorRepository extends JpaRepository<CampoValor, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM CampoValor cv WHERE cv.campo.id = :campoId")
    void deleteByCampoId(@Param("campoId") Long campoId);

}