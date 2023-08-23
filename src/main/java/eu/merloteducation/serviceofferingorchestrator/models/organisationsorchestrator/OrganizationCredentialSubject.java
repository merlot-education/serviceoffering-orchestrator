package eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationCredentialSubject {
    @JsonProperty("@id")
    private String id;
    @JsonProperty("gax-trust-framework:legalName")
    private StringTypeValue legalName;
}