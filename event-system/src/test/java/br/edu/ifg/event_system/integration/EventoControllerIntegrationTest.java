package br.edu.ifg.event_system.integration;

import br.edu.ifg.event_system.dto.EventoRequestDTO;
import br.edu.ifg.event_system.model.Campus;
import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.CampusRepository;
import br.edu.ifg.event_system.repository.DepartamentoRepository;
import br.edu.ifg.event_system.repository.RoleRepository;
import br.edu.ifg.event_system.repository.UserRepository;
import br.edu.ifg.event_system.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class EventoControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User adminUser;
    private Campus campus;
    private Departamento departamento;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        departamentoRepository.deleteAll();
        campusRepository.deleteAll();

        Role adminRole = new Role();
        adminRole.setName("ADMIN_GERAL");
        roleRepository.save(adminRole);

        adminUser = new User();
        adminUser.setUsername("admin@ifg.edu.br");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setNomeCompleto("Administrador");
        adminUser.setRoles(List.of(adminRole));
        userRepository.save(adminUser);

        campus = new Campus();
        campus.setNome("Campus Teste");
        campusRepository.save(campus);

        departamento = new Departamento();
        departamento.setNome("Departamento Teste");
        departamento.setCampus(campus);
        departamentoRepository.save(departamento);

        adminToken = jwtService.generateToken(adminUser.getUsername(),
                List.of("ADMIN_GERAL"));
    }

    @Test
    void listarEventos_DeveRetornarListaVazia_QuandoNaoHaEventos() throws Exception {
        mockMvc.perform(get("/api/eventos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void criarEvento_DeveRetornarEvento_QuandoAutenticadoComoAdmin() throws Exception {
        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setTitulo("Evento de Teste");
        dto.setDescricao("Descrição do evento de teste");
        dto.setCampusId(campus.getId());
        dto.setDepartamentoId(departamento.getId());
        dto.setDataInicio(LocalDateTime.now().plusDays(1));
        dto.setDataFim(LocalDateTime.now().plusDays(2));
        dto.setVagas(100);
        dto.setLocal("Sala de reuniões 101");
        dto.setEstudanteIfg(true);

        mockMvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Evento de Teste")))
                .andExpect(jsonPath("$.campus.id", is(campus.getId().intValue())))
                .andExpect(jsonPath("$.departamento.id", is(departamento.getId().intValue())));
    }

    @Test
    void criarEvento_DeveRetornarForbidden_QuandoNaoAutenticado() throws Exception {
        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setTitulo("Evento de Teste");
        dto.setCampusId(campus.getId());
        dto.setDepartamentoId(departamento.getId());
        dto.setDataInicio(LocalDateTime.now().plusDays(1));
        dto.setLocal("Sala de reuniões 101");

        mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarEvento_DeveRetornarNotFound_QuandoEventoNaoExiste() throws Exception {
        mockMvc.perform(get("/api/eventos/999"))
                .andExpect(status().isNotFound());
    }

}