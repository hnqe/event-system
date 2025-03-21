package br.edu.ifg.event_system.config;

import br.edu.ifg.event_system.model.Role;
import br.edu.ifg.event_system.repository.UserRepository;
import br.edu.ifg.event_system.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String EVENTOS_API_PATH = "/api/eventos";
    private static final String ROLE_ADMIN_GERAL = "ADMIN_GERAL";
    private static final String ROLE_ADMIN_CAMPUS = "ADMIN_CAMPUS";
    private static final String ROLE_ADMIN_DEPARTAMENTO = "ADMIN_DEPARTAMENTO";

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public SecurityConfig(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            var user = userRepository.findByUsername(username);
            if (user == null) {
                return new org.springframework.security.core.userdetails.User(
                        "dummy_user",
                        "{noop}invalid_password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
            }

            var authorities = user.getRoles().stream()
                    .map(Role::getName)
                    .map(roleName -> "ROLE_" + roleName)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    authorities
            );
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authenticationProvider())
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService, userRepository);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/api/auth/**")
                            .permitAll()
                            .requestMatchers("/")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, EVENTOS_API_PATH + "/**")
                            .permitAll()
                            .requestMatchers("/api/inscricoes/**")
                            .authenticated()
                            .requestMatchers(HttpMethod.PUT, EVENTOS_API_PATH + "/{id}/encerrar")
                            .hasAnyRole(ROLE_ADMIN_GERAL, ROLE_ADMIN_CAMPUS, ROLE_ADMIN_DEPARTAMENTO)
                            .requestMatchers(HttpMethod.POST, EVENTOS_API_PATH)
                            .hasAnyRole(ROLE_ADMIN_GERAL, ROLE_ADMIN_CAMPUS, ROLE_ADMIN_DEPARTAMENTO)
                            .requestMatchers(HttpMethod.PUT, EVENTOS_API_PATH + "/{id}")
                            .hasAnyRole(ROLE_ADMIN_GERAL, ROLE_ADMIN_CAMPUS, ROLE_ADMIN_DEPARTAMENTO)
                            .requestMatchers(HttpMethod.DELETE, EVENTOS_API_PATH + "/{id}")
                            .hasAnyRole(ROLE_ADMIN_GERAL, ROLE_ADMIN_CAMPUS, ROLE_ADMIN_DEPARTAMENTO)
                            .requestMatchers(EVENTOS_API_PATH + "/**")
                            .authenticated()
                            .requestMatchers("/admin/**")
                            .hasAnyRole(ROLE_ADMIN_GERAL, ROLE_ADMIN_CAMPUS, ROLE_ADMIN_DEPARTAMENTO)
                            .anyRequest()
                                .authenticated();
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}