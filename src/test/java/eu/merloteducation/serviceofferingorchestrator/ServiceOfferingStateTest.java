package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceOfferingStateTest {

    @Test
    public void transitionFromInDraft() {
        ServiceOfferingState state = ServiceOfferingState.IN_DRAFT;

        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.DELETED));

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }

    @Test
    public void transitionFromReleased() {
        ServiceOfferingState state = ServiceOfferingState.RELEASED;

        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }

    @Test
    public void transitionFromRevoked() {
        ServiceOfferingState state = ServiceOfferingState.REVOKED;

        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
    }

    @Test
    public void transitionFromDeleted() {
        ServiceOfferingState state = ServiceOfferingState.DELETED;

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }

    @Test
    public void transitionFromArchived() {
        ServiceOfferingState state = ServiceOfferingState.DELETED;

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }
}
