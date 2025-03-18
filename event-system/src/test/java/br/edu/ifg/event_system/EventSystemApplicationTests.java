package br.edu.ifg.event_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EventSystemApplicationTests {

	@Test
	void contextLoads() {
		// This test method is intentionally empty because it only verifies that
		// the Spring application context loads successfully without any errors.
		// The test passes if the application context starts correctly and fails
		// if there are any issues with beans, configurations, or dependencies.
	}

}
