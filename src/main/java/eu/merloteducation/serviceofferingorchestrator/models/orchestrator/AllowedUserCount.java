package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class AllowedUserCount {
    private int userCountUpTo;
    private boolean userCountUnlimited;
}
