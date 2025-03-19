package br.edu.ifg.event_system.integration;

import br.edu.ifg.event_system.dto.LoginRequestDTO;
import br.edu.ifg.event_system.dto.RegisterRequestDTO;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.RoleRepository;
import br.edu.ifg.event_system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        User testUser = new User();
        testUser.setUsername("existing@ifg.edu.br");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNomeCompleto("Usuário Existente");
        testUser.setRoles(java.util.List.of(userRole));
        userRepository.save(testUser);
    }

    @Test
    void register_DeveRegistrarNovoUsuario_QuandoDadosValidos() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("novo@ifg.edu.br");
        dto.setPassword("senha123");
        dto.setNomeCompleto("Novo Usuário");

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("novo@ifg.edu.br")))
                .andExpect(jsonPath("$.nomeCompleto", is("Novo Usuário")))
                .andExpect(jsonPath("$.password").doesNotExist());

        User savedUser = userRepository.findByUsername("novo@ifg.edu.br");
        assertNotNull(savedUser);
        assertEquals("Novo Usuário", savedUser.getNomeCompleto());
        assertTrue(passwordEncoder.matches("senha123", savedUser.getPassword()));
    }

    @Test
    void register_DeveFalhar_QuandoUsernameJaExiste() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("existing@ifg.edu.br"); // Username já existe
        dto.setPassword("senha123");
        dto.setNomeCompleto("Tentativa Duplicada");

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        assertEquals(1, userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals("existing@ifg.edu.br"))
                .count());
    }

    @Test
    void login_DeveRetornarToken_QuandoCredenciaisValidas() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("existing@ifg.edu.br");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_DeveFalhar_QuandoCredenciaisInvalidas() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("existing@ifg.edu.br");
        dto.setPassword("senhaErrada");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_DeveRetornarUsuarioLogado_QuandoAutenticado() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("existing@ifg.edu.br");
        dto.setPassword("password123");

        String result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(result).get("token").asText();

        mockMvc.perform(get("/api/auth/current-user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("existing@ifg.edu.br")))
                .andExpect(jsonPath("$.nomeCompleto", is("Usuário Existente")));
    }

    @Test
    void getCurrentUser_DeveFalhar_QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isUnauthorized());
    }

}