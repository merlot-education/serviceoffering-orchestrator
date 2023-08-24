package eu.merloteducation.serviceofferingorchestrator.service;

import eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator.OrganizationDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface OrganizationOrchestratorClient {
    @GetExchange("/organization/{orgaId}")
    OrganizationDetails getOrganizationDetails(@PathVariable String orgaId);
}
