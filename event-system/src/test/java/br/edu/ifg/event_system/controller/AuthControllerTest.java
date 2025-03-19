package br.edu.ifg.event_system.controller;

import br.edu.ifg.event_system.dto.LoginRequestDTO;
import br.edu.ifg.event_system.dto.LoginResponseDTO;
import br.edu.ifg.event_system.dto.RegisterRequestDTO;
import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.service.JwtService;
import br.edu.ifg.event_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private Role userRole;
    private String testToken = "jwt.test.token";

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test@ifg.edu.br");
        testUser.setNomeCompleto("Test User");
        testUser.setRoles(List.of(userRole));

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("test@ifg.edu.br");
        loginRequest.setPassword("password123");

        when(userService.buscarPorUsername("test@ifg.edu.br")).thenReturn(testUser);
        when(jwtService.generateToken(eq("test@ifg.edu.br"), anyList())).thenReturn(testToken);

        ResponseEntity<LoginResponseDTO> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testToken, response.getBody().getToken());

        verify(authenticationManager).authenticate(
                argThat(auth ->
                        auth instanceof UsernamePasswordAuthenticationToken &&
                                "test@ifg.edu.br".equals(auth.getPrincipal()) &&
                                "password123".equals(auth.getCredentials())
                )
        );
        verify(userService).buscarPorUsername("test@ifg.edu.br");
        verify(jwtService).generateToken(eq("test@ifg.edu.br"), anyList());
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("test@ifg.edu.br");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<LoginResponseDTO> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, never()).buscarPorUsername(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyList());
    }

    @Test
    void register_WithValidData_ShouldReturnCreatedUser() {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("newuser@ifg.edu.br");
        registerRequest.setPassword("password123");
        registerRequest.setNomeCompleto("New User");

        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newuser@ifg.edu.br");
        newUser.setNomeCompleto("New User");
        newUser.setRoles(new ArrayList<>());

        when(userService.registrarUsuario("New User", "newuser@ifg.edu.br", "password123"))
                .thenReturn(newUser);

        ResponseEntity<User> response = authController.register(registerRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(newUser, response.getBody());

        verify(userService).registrarUsuario("New User", "newuser@ifg.edu.br", "password123");
    }

    @Test
    void register_WithDuplicateUsername_ShouldReturnBadRequest() {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("existing@ifg.edu.br");
        registerRequest.setPassword("password123");
        registerRequest.setNomeCompleto("Duplicate User");

        when(userService.registrarUsuario("Duplicate User", "existing@ifg.edu.br", "password123"))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        ResponseEntity<User> response = authController.register(registerRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());

        verify(userService).registrarUsuario("Duplicate User", "existing@ifg.edu.br", "password123");
    }

    @Test
    void getCurrentUser_WhenUserExists_ShouldReturnUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@ifg.edu.br");
        when(userService.buscarPorUsername("test@ifg.edu.br")).thenReturn(testUser);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("test@ifg.edu.br");

        ResponseEntity<User> response = authController.getCurrentUser();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUser, response.getBody());

        verify(securityContext).getAuthentication();
        verify(authentication).getName();
        verify(authentication).isAuthenticated();
        verify(authentication).getPrincipal();
        verify(userService).buscarPorUsername("test@ifg.edu.br");
    }

    @Test
    void getCurrentUser_WhenUserDoesNotExist_ShouldReturnNotFound() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("nonexistent@ifg.edu.br");
        when(userService.buscarPorUsername("nonexistent@ifg.edu.br")).thenReturn(null);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("nonexistent@ifg.edu.br"); // Qualquer valor que não seja "anonymousUser"

        ResponseEntity<User> response = authController.getCurrentUser();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(securityContext).getAuthentication();
        verify(authentication).getName();
        verify(authentication).isAuthenticated();
        verify(authentication).getPrincipal();
        verify(userService).buscarPorUsername("nonexistent@ifg.edu.br");
    }

    @Test
    void getCurrentUser_WhenNotAuthenticated_ShouldReturnUnauthorized() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        ResponseEntity<User> response = authController.getCurrentUser();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        verify(securityContext).getAuthentication();
        verify(authentication).isAuthenticated();
        verify(authentication, never()).getName();
        verify(userService, never()).buscarPorUsername(anyString());
    }

    @Test
    void getCurrentUser_WhenAnonymousUser_ShouldReturnUnauthorized() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        ResponseEntity<User> response = authController.getCurrentUser();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        verify(securityContext).getAuthentication();
        verify(authentication).isAuthenticated();
        verify(authentication).getPrincipal();
        verify(authentication, never()).getName();
        verify(userService, never()).buscarPorUsername(anyString());
    }

}