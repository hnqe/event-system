package br.edu.ifg.event_system.config;

import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.UserRepository;
import br.edu.ifg.event_system.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private SecurityConfig securityConfig;
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(userRepository, jwtService);
        userDetailsService = securityConfig.userDetailsService();
    }

    @Test
    void loadUserByUsername_WhenUserExists_ReturnsUserDetailsWithCorrectAuthorities() {
        // Arrange
        User user = new User();
        user.setUsername("test@ifg.edu.br");
        user.setPassword("encoded_password");

        Role adminRole = new Role();
        adminRole.setName("ADMIN_GERAL");

        Role userRole = new Role();
        userRole.setName("USER");

        List<Role> roles = new ArrayList<>();
        roles.add(adminRole);
        roles.add(userRole);
        user.setRoles(roles);

        when(userRepository.findByUsername("test@ifg.edu.br")).thenReturn(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@ifg.edu.br");

        assertNotNull(userDetails);
        assertEquals("test@ifg.edu.br", userDetails.getUsername());
        assertEquals("encoded_password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN_GERAL")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertEquals(2, userDetails.getAuthorities().size());

        verify(userRepository).findByUsername("test@ifg.edu.br");
    }

    @Test
    void loadUserByUsername_WhenUserDoesNotExist_ReturnsDummyUser() {
        when(userRepository.findByUsername("nonexistent@ifg.edu.br")).thenReturn(null);

        UserDetails userDetails = userDetailsService.loadUserByUsername("nonexistent@ifg.edu.br");

        assertNotNull(userDetails);
        assertEquals("dummy_user", userDetails.getUsername());
        assertEquals("{noop}invalid_password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertEquals(1, userDetails.getAuthorities().size());

        verify(userRepository).findByUsername("nonexistent@ifg.edu.br");
    }

    @Test
    void loadUserByUsername_WithNullUsername_StillQueriesRepository() {
        when(userRepository.findByUsername(null)).thenReturn(null);

        UserDetails userDetails = userDetailsService.loadUserByUsername(null);

        assertNotNull(userDetails);
        assertEquals("dummy_user", userDetails.getUsername());

        verify(userRepository).findByUsername(null);
    }

}