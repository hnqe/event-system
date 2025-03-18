package br.edu.ifg.event_system.config;

import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.model.User;
import br.edu.ifg.event_system.repository.UserRepository;
import br.edu.ifg.event_system.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter jwtAuthFilter;
    private User testUser;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(jwtService, userRepository);

        testUser = new User();
        testUser.setUsername("test@ifg.edu.br");
        testUser.setPassword("encoded_password");

        Role adminRole = new Role();
        adminRole.setName("ADMIN_GERAL");

        Role userRole = new Role();
        userRole.setName("USER");

        testUser.setRoles(List.of(adminRole, userRole));

        securityContext = new SecurityContextImpl();
        SecurityContextHolder.setContext(securityContext);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic dXNlcjpwYXNzd29yZA=="})
    void doFilterInternal_WithInvalidAuthHeader_ShouldContinueFilterChainWithoutAuthentication(String authHeader) throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authHeader);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldContinueFilterChainWithoutAuthentication() throws ServletException, IOException {
        String invalidToken = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtService.validateToken(invalidToken)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken(invalidToken);
        verify(jwtService, never()).getUsernameFromToken(anyString());
    }

    @Test
    void doFilterInternal_WithValidTokenButUserNotFound_ShouldContinueFilterChainWithoutAuthentication() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.getUsernameFromToken(validToken)).thenReturn("nonexistent@ifg.edu.br");
        when(userRepository.findByUsername("nonexistent@ifg.edu.br")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken(validToken);
        verify(jwtService).getUsernameFromToken(validToken);
        verify(userRepository).findByUsername("nonexistent@ifg.edu.br");
    }

    @Test
    void doFilterInternal_WithValidTokenAndExistingUser_ShouldAuthenticateAndContinueFilterChain() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.getUsernameFromToken(validToken)).thenReturn("test@ifg.edu.br");
        when(userRepository.findByUsername("test@ifg.edu.br")).thenReturn(testUser);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, authentication);
        assertEquals("test@ifg.edu.br", authentication.getName());
        assertEquals(2, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN_GERAL")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken(validToken);
        verify(jwtService).getUsernameFromToken(validToken);
        verify(userRepository).findByUsername("test@ifg.edu.br");
    }

    @Test
    void doFilterInternal_WithVeryShortBearerToken_ShouldHandleGracefully() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken("");
    }

    @Test
    void securityContext_ShouldBeProperlyPopulated() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.getUsernameFromToken(validToken)).thenReturn("test@ifg.edu.br");
        when(userRepository.findByUsername("test@ifg.edu.br")).thenReturn(testUser);

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);

        SecurityContext mockContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(mockContext);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(mockContext).setAuthentication(authCaptor.capture());
        Authentication capturedAuth = authCaptor.getValue();

        assertEquals("test@ifg.edu.br", capturedAuth.getName());
        assertEquals(2, capturedAuth.getAuthorities().size());
        assertTrue(capturedAuth.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN_GERAL")));
        assertTrue(capturedAuth.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_USER")));

        verify(filterChain).doFilter(request, response);
    }

}