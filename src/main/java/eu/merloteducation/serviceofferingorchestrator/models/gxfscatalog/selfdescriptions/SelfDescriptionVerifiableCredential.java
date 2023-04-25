package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelfDescriptionVerifiableCredential {
    private ServiceOfferingCredentialSubject credentialSubject;
}
