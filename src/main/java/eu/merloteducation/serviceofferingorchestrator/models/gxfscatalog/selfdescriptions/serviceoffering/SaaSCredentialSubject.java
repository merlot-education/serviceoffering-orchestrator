package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaaSCredentialSubject extends ServiceOfferingCredentialSubject {
    // inherited from merlot:MerlotServiceOfferingSaaS

    // mandatory
    @JsonProperty("merlot:hardwareRequirements")
    private StringTypeValue hardwareRequirements;
}
