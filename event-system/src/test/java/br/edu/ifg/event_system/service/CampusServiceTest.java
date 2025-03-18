package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.CampusRepository;
import br.edu.ifg.event_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CampusService campusService;

    private Campus campus;
    private User user;
    private Role adminCampusRole;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(1L);
        campus.setNome("Campus Teste");

        adminCampusRole = new Role();
        adminCampusRole.setId(1L);
        adminCampusRole.setName("ADMIN_CAMPUS");

        user = new User();
        user.setId(1L);
        user.setUsername("test@ifg.edu.br");
        user.setRoles(new ArrayList<>());
        user.setCampusQueAdministro(new ArrayList<>());
    }

    @Test
    void criarOuAtualizar_DeveSalvarERetornarCampus() {
        when(campusRepository.save(any(Campus.class))).thenReturn(campus);

        Campus resultado = campusService.criarOuAtualizar(campus);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Campus Teste", resultado.getNome());
        verify(campusRepository).save(campus);
    }

    @Test
    void listarTodos_DeveRetornarListaDeCampus() {
        List<Campus> campi = Collections.singletonList(campus);
        when(campusRepository.findAll()).thenReturn(campi);

        List<Campus> resultado = campusService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Campus Teste", resultado.get(0).getNome());
        verify(campusRepository).findAll();
    }

    @Test
    void buscarPorId_ComIdExistente_DeveRetornarCampus() {
        when(campusRepository.findById(1L)).thenReturn(Optional.of(campus));

        Campus resultado = campusService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Campus Teste", resultado.getNome());
        verify(campusRepository).findById(1L);
    }

    @Test
    void buscarPorId_ComIdInexistente_DeveRetornarNull() {
        when(campusRepository.findById(999L)).thenReturn(Optional.empty());

        Campus resultado = campusService.buscarPorId(999L);

        assertNull(resultado);
        verify(campusRepository).findById(999L);
    }

    @Test
    void deletar_ComIdInexistente_NaoDeveFazerNada() {
        when(campusRepository.findById(999L)).thenReturn(Optional.empty());

        campusService.deletar(999L);

        verify(campusRepository).findById(999L);
        verify(campusRepository, never()).deleteById(anyLong());
        verify(userRepository, never()).findAll();
    }

    @Test
    void deletar_ComIdExistenteSemUsuariosAdministrando_DeveDeletarCampus() {
        when(campusRepository.findById(1L)).thenReturn(Optional.of(campus));
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        campusService.deletar(1L);

        verify(campusRepository).findById(1L);
        verify(userRepository).findAll();
        verify(campusRepository).deleteById(1L);
    }

    @Test
    void deletar_ComIdExistenteEUsuarioAdministrandoVariosOutrosCampus_DeveRemoverCampusDoUsuario() {
        when(campusRepository.findById(1L)).thenReturn(Optional.of(campus));

        Campus outroCampus = new Campus();
        outroCampus.setId(2L);
        outroCampus.setNome("Outro Campus");

        user.getRoles().add(adminCampusRole);
        user.getCampusQueAdministro().add(campus);
        user.getCampusQueAdministro().add(outroCampus);

        when(userRepository.findAll()).thenReturn(List.of(user));

        campusService.deletar(1L);

        verify(campusRepository).findById(1L);
        verify(userRepository).findAll();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User usuarioSalvo = userCaptor.getValue();

        assertEquals(1, usuarioSalvo.getCampusQueAdministro().size());
        assertEquals(2L, usuarioSalvo.getCampusQueAdministro().get(0).getId());
        assertTrue(usuarioSalvo.getRoles().contains(adminCampusRole));

        verify(campusRepository).deleteById(1L);
    }

    @Test
    void deletar_ComIdExistenteEUsuarioAdministrandoSomenteEsteCampus_DeveRemoverCampusERoleDoUsuario() {
        when(campusRepository.findById(1L)).thenReturn(Optional.of(campus));

        user.getRoles().add(adminCampusRole);
        user.getCampusQueAdministro().add(campus);

        when(userRepository.findAll()).thenReturn(List.of(user));

        campusService.deletar(1L);

        verify(campusRepository).findById(1L);
        verify(userRepository).findAll();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User usuarioSalvo = userCaptor.getValue();

        assertTrue(usuarioSalvo.getCampusQueAdministro().isEmpty());
        assertFalse(usuarioSalvo.getRoles().contains(adminCampusRole));

        verify(campusRepository).deleteById(1L);
    }

    @Test
    void deletar_ComMultiplosUsuariosAdministrando_DeveAtualizarTodosUsuarios() {
        when(campusRepository.findById(1L)).thenReturn(Optional.of(campus));

        User usuario1 = new User();
        usuario1.setId(1L);
        usuario1.setUsername("user1@ifg.edu.br");
        usuario1.setRoles(new ArrayList<>(List.of(adminCampusRole)));
        usuario1.setCampusQueAdministro(new ArrayList<>(List.of(campus)));

        Campus outroCampus = new Campus();
        outroCampus.setId(2L);
        outroCampus.setNome("Outro Campus");

        User usuario2 = new User();
        usuario2.setId(2L);
        usuario2.setUsername("user2@ifg.edu.br");
        usuario2.setRoles(new ArrayList<>(List.of(adminCampusRole)));
        usuario2.setCampusQueAdministro(new ArrayList<>(Arrays.asList(campus, outroCampus)));

        when(userRepository.findAll()).thenReturn(Arrays.asList(usuario1, usuario2));

        campusService.deletar(1L);

        verify(campusRepository).findById(1L);
        verify(userRepository).findAll();
        verify(userRepository, times(2)).save(any(User.class));
        verify(campusRepository).deleteById(1L);
    }

}