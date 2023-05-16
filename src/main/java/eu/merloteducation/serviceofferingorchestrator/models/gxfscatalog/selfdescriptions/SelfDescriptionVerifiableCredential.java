package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SelfDescriptionVerifiableCredential<T extends ServiceOfferingCredentialSubject> {
    private T credentialSubject;
}
