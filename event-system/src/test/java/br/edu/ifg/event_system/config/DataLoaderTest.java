package br.edu.ifg.event_system.config;

import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataLoaderTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private DataLoader dataLoader;

    @BeforeEach
    void setUp() {
        // No additional setup needed as we're using Mockito annotations
    }

    @Test
    void run_WhenAdminUserDoesNotExist_ShouldCreateAdminUser() {
        when(userService.buscarPorUsername("admin@ifg.br")).thenReturn(null);

        dataLoader.run();

        verify(userService).buscarPorUsername("admin@ifg.br");
        verify(userService).criarUsuario(
                "admin@ifg.br",
                "123456",
                "Administrador Geral",
                List.of("ADMIN_GERAL")
        );
    }

    @Test
    void run_WhenAdminUserExists_ShouldNotCreateAdminUser() {
        User existingAdmin = new User();
        existingAdmin.setUsername("admin@ifg.br");
        existingAdmin.setNomeCompleto("Administrador Geral");

        when(userService.buscarPorUsername("admin@ifg.br")).thenReturn(existingAdmin);

        dataLoader.run();

        verify(userService).buscarPorUsername("admin@ifg.br");
        verify(userService, never()).criarUsuario(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void run_WithCommandLineArguments_ShouldWorkTheSame() {
        when(userService.buscarPorUsername("admin@ifg.br")).thenReturn(null);

        dataLoader.run("--server.port=8080", "--spring.profiles.active=test");

        verify(userService).buscarPorUsername("admin@ifg.br");
        verify(userService).criarUsuario(
                "admin@ifg.br",
                "123456",
                "Administrador Geral",
                List.of("ADMIN_GERAL")
        );
    }

}