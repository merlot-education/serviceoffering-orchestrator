package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Runtime {
    private int runtimeCount;
    private String runtimeMeasurement; // TODO enum instead?
    private boolean runtimeUnlimited;
}
