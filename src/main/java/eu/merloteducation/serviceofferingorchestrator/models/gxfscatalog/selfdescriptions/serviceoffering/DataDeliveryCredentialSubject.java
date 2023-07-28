package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.DataExchangeCount;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataDeliveryCredentialSubject extends ServiceOfferingCredentialSubject {
    // inherited from merlot:MerlotServiceOfferingDataDelivery

    @NotNull
    @JsonProperty("merlot:dataAccessType")
    private StringTypeValue dataAccessType;

    @NotNull
    @JsonProperty("merlot:dataTransferType")
    private StringTypeValue dataTransferType;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("merlot:exchangeCountOption")
    private List<DataExchangeCount> exchangeCountOptions;

    @JsonProperty("merlot:exchangeCountUnlimited")
    private boolean exchangeCountUnlimited;
}
