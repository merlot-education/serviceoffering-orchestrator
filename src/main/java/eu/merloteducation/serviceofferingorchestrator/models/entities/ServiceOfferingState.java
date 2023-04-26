package eu.merloteducation.serviceofferingorchestrator.models.entities;

public enum ServiceOfferingState {
    IN_DRAFT(10),
    RELEASED(40),
    REVOKED(60),
    DELETED(70),
    ARCHIVED(80);

    private final int numVal;
    ServiceOfferingState(int numVal) {
        this.numVal = numVal;
    }

    public int getNumVal() {
        return numVal;
    }

}
