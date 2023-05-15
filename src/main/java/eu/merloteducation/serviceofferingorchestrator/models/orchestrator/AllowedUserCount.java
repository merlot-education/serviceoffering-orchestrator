package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllowedUserCount {
    private int userCountUpTo;
    private boolean userCountUnlimited;
}
