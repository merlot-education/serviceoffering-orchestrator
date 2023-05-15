package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.DataExchangeCount;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataDeliveryCredentialSubject extends ServiceOfferingCredentialSubject {
    // inherited from merlot:MerlotServiceOfferingDataDelivery

    @JsonProperty("merlot:exchangeCountOption")
    private DataExchangeCount exchangeCountOption;


}
