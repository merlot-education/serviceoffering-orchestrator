package eu.merloteducation.serviceofferingorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface OrganizationOrchestratorClient {
    @GetExchange("/organization/{orgaId}")
    MerlotParticipantDto getOrganizationDetails(@PathVariable String orgaId);
}
