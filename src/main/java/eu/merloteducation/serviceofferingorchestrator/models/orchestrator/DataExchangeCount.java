package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataExchangeCount {
    private int exchangeCountUpTo;
    private boolean exchangeCountUnlimited;
}
