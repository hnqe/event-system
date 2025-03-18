package br.edu.ifg.event_system.config;

import br.edu.ifg.event_system.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserService userService;

    public DataLoader(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        var admin = userService.buscarPorUsername("admin@ifg.br");
        if (admin == null) {
            userService.criarUsuario(
                    "admin@ifg.br",
                    "123456",
                    "Administrador Geral",
                    List.of("ADMIN_GERAL")
            );
        }
    }

}