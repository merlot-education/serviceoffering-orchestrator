package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.AllowedUserCount;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaaSCredentialSubject extends ServiceOfferingCredentialSubject {
    // inherited from merlot:MerlotServiceOfferingSaaS

    @NotNull
    @JsonProperty("merlot:hardwareRequirements")
    private StringTypeValue hardwareRequirements;

    @JsonProperty("merlot:userCountOption")
    private AllowedUserCount userCountOption;
}
