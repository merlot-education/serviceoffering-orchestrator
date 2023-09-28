package eu.merloteducation.serviceofferingorchestrator.auth;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("offeringAuthorityChecker")
public class OfferingAuthorityChecker {

    private static final String PARTICIPANT = "Participant:";

    @Autowired
    private AuthorityChecker authorityChecker;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    public boolean canAccessOffering(Authentication authentication, String offeringId) {
        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(offeringId).orElse(null);

        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);

        if (extension != null) {
            return representedOrgaIds.contains(extension.getIssuer().replace(PARTICIPANT, ""));
        }

        return false;
    }
}
