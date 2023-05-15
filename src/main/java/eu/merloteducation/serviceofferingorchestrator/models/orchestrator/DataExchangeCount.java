package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.NumberTypeValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataExchangeCount {
    private int exchangeCountUpTo;
    private boolean exchangeCountUnlimited;
}
