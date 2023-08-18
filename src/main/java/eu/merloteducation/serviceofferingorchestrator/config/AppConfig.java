package eu.merloteducation.serviceofferingorchestrator.config;

import eu.merloteducation.serviceofferingorchestrator.service.OrganizationOrchestratorClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {
    @Value("${organizations-orchestrator.base-uri}")
    private String organizationsOrchestratorBaseUri;

    @Bean
    public OrganizationOrchestratorClient organizationOrchestratorClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(organizationsOrchestratorBaseUri)
                .build();
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .build();
        return httpServiceProxyFactory.createClient(OrganizationOrchestratorClient.class);
    }
}
