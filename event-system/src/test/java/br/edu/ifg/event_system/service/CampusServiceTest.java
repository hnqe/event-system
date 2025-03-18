package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.repository.CampusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;

    @InjectMocks
    private CampusService campusService;

    @Test
    void buscarPorId_ExistingId_ReturnsCampus() {
        Long id = 1L;
        Campus expectedCampus = new Campus();
        expectedCampus.setId(id);
        expectedCampus.setNome("Campus Teste");

        when(campusRepository.findById(id)).thenReturn(Optional.of(expectedCampus));

        Campus result = campusService.buscarPorId(id);

        assertEquals(expectedCampus.getId(), result.getId());
        assertEquals(expectedCampus.getNome(), result.getNome());
    }

}