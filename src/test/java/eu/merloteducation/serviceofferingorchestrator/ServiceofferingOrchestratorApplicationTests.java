package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.service.KeycloakAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ServiceofferingOrchestratorApplicationTests {

	@MockBean
	private KeycloakAuthService keycloakAuthService;

	@Test
	void contextLoads() {
		// no explicit assertions necessary, just be sure we reach this point
		assertTrue(true);
	}

}
