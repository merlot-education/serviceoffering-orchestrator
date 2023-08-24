package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.service.KeycloakAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ServiceofferingOrchestratorApplicationTests {

	@MockBean
	private KeycloakAuthService keycloakAuthService;

	@Test
	void contextLoads() {
	}

}
