package br.edu.ifg.event_system.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private String username;
    private List<String> roles;
    private static final String SECRET_KEY = "MINHA_CHAVE_SECRETA_GRANDE_O_SUFICIENTE_PARA_256BITS";

    @BeforeEach
    void setUp() {
        username = "test@ifg.edu.br";
        roles = List.of("USER", "ADMIN_CAMPUS");
    }

    @Test
    void testGenerateToken() {
        String token = jwtService.generateToken(username, roles);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(username, claims.getSubject());
        assertEquals(roles, claims.get("roles"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        assertTrue(claims.getExpiration().after(new Date()));

        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(TimeUnit.HOURS.toMillis(2), diff, 100); // Tolerância de 100ms
    }

    @Test
    void testGetUsernameFromToken() {
        String token = jwtService.generateToken(username, roles);

        String extractedUsername = jwtService.getUsernameFromToken(token);

        assertEquals(username, extractedUsername);
    }

    @Test
    void testValidateToken_WithValidToken() {
        String token = jwtService.generateToken(username, roles);

        boolean isValid = jwtService.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void testValidateToken_WithInvalidFormat() {
        String malformedToken = "this.is.not.a.valid.jwt.token";

        boolean isValid = jwtService.validateToken(malformedToken);

        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithTamperedToken() {
        String originalToken = jwtService.generateToken(username, roles);
        String tamperedToken = originalToken.substring(0, originalToken.lastIndexOf('.') + 1) + "tampered";

        boolean isValid = jwtService.validateToken(tamperedToken);

        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithExpiredToken() {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

        Date pastDate = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3));
        Date expiredDate = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

        String expiredToken = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(pastDate)
                .setExpiration(expiredDate)
                .signWith(key)
                .compact();

        boolean isValid = jwtService.validateToken(expiredToken);

        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithNull() {
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void testValidateToken_WithEmptyToken() {
        assertFalse(jwtService.validateToken(""));
    }

}