package eu.merloteducation.serviceofferingorchestrator.models.dto;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingDto {
    private OfferingMetaDto metadata;
    private ProviderDetailsDto providerDetails;
    private SelfDescription selfDescription;
}
