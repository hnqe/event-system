package br.edu.ifg.event_system.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    private PostgreSQLContainer<?> postgreSQLContainer;

    @BeforeEach
    void setUp() {
        postgreSQLContainer = new PostgreSQLContainer<>("postgres:14-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");

        postgreSQLContainer.start();
        System.out.println("🚀 Testcontainers PostgreSQL iniciado na URL: " + postgreSQLContainer.getJdbcUrl());
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> tempContainer = new PostgreSQLContainer<>("postgres:14-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");

        tempContainer.start();

        registry.add("spring.datasource.url", tempContainer::getJdbcUrl);
        registry.add("spring.datasource.username", tempContainer::getUsername);
        registry.add("spring.datasource.password", tempContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @AfterEach
    void tearDown() {
        if (postgreSQLContainer != null) {
            System.out.println("🛑 Finalizando Testcontainers PostgreSQL...");
            postgreSQLContainer.stop();
        }
    }

}