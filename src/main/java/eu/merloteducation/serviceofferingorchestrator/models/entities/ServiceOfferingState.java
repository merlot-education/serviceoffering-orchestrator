package eu.merloteducation.serviceofferingorchestrator.models.entities;

import lombok.Getter;

@Getter
public enum ServiceOfferingState {
    IN_DRAFT(10, 1, 10),
    RELEASED(40, 2, 4),
    REVOKED(60, 4, 27),
    DELETED(70, 8, 0),
    ARCHIVED(80, 16, 0);

    private final int numVal;
    private final int stateBit;
    private final int allowedStatesBits;
    ServiceOfferingState(int numVal, int stateBit, int allowedStatesBits) {
        this.numVal = numVal;
        this.stateBit = stateBit;
        this.allowedStatesBits = allowedStatesBits;
    }

    public boolean checkTransitionAllowed(ServiceOfferingState end) {
        return (allowedStatesBits & end.getStateBit()) != 0;
    }

}
