package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceOfferingExtensionTest {

    private ServiceOfferingExtension getInDraftExtension() {
        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        assertEquals(ServiceOfferingState.IN_DRAFT, extension.getState());
        return extension;
    }

    private ServiceOfferingExtension getReleasedExtension() {
        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.release();
        assertEquals(ServiceOfferingState.RELEASED, extension.getState());
        return extension;
    }

    private ServiceOfferingExtension getRevokedExtension() {
        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.release();
        extension.revoke();
        assertEquals(ServiceOfferingState.REVOKED, extension.getState());
        return extension;
    }

    private ServiceOfferingExtension getDeletedExtension() {
        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.delete();
        assertEquals(ServiceOfferingState.DELETED, extension.getState());
        return extension;
    }

    private ServiceOfferingExtension getArchivedExtension() {
        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.release();
        extension.revoke();
        extension.getAssociatedContractIds().add("12345");
        extension.delete();
        assertEquals(ServiceOfferingState.ARCHIVED, extension.getState());
        return extension;
    }

    @Test
    public void checkFromInDraftTransitions() {
        ServiceOfferingExtension extension = getInDraftExtension();

        assertThrows(IllegalStateException.class, extension::inDraft);
        assertThrows(IllegalStateException.class, extension::revoke);

        assertDoesNotThrow(extension::release);
        assertEquals(ServiceOfferingState.RELEASED, extension.getState());

        extension = getInDraftExtension(); // reset to in_draft

        assertDoesNotThrow(extension::delete);
        assertEquals(ServiceOfferingState.DELETED, extension.getState());
    }

    @Test
    public void checkFromReleasedTransitions() {
        ServiceOfferingExtension extension = getReleasedExtension();

        assertThrows(IllegalStateException.class, extension::inDraft);
        assertThrows(IllegalStateException.class, extension::release);
        assertThrows(IllegalStateException.class, extension::delete);

        assertDoesNotThrow(extension::revoke);
        assertEquals(ServiceOfferingState.REVOKED, extension.getState());
    }

    @Test
    public void checkFromRevokedTransitions() {
        ServiceOfferingExtension extension = getRevokedExtension();

        // without associated contracts
        assertThrows(IllegalStateException.class, extension::revoke);

        extension = getRevokedExtension();
        assertDoesNotThrow(extension::inDraft);
        assertEquals(ServiceOfferingState.IN_DRAFT, extension.getState());

        extension = getRevokedExtension();
        assertDoesNotThrow(extension::release);
        assertEquals(ServiceOfferingState.RELEASED, extension.getState());

        extension = getRevokedExtension();
        assertDoesNotThrow(extension::delete);
        assertEquals(ServiceOfferingState.DELETED, extension.getState());

        // with associated contracts
        extension = getRevokedExtension();
        extension.getAssociatedContractIds().add("12345");

        assertThrows(IllegalStateException.class, extension::revoke);
        assertThrows(IllegalStateException.class, extension::inDraft);

        extension = getRevokedExtension();
        extension.getAssociatedContractIds().add("12345");
        assertDoesNotThrow(extension::release);
        assertEquals(ServiceOfferingState.RELEASED, extension.getState());

        extension = getRevokedExtension();
        extension.getAssociatedContractIds().add("12345");
        assertDoesNotThrow(extension::delete);
        assertEquals(ServiceOfferingState.ARCHIVED, extension.getState());
    }

    @Test
    public void checkFromDeletedTransitions() {
        ServiceOfferingExtension extension = getDeletedExtension();

        assertThrows(IllegalStateException.class, extension::inDraft);
        assertThrows(IllegalStateException.class, extension::release);
        assertThrows(IllegalStateException.class, extension::revoke);
        assertThrows(IllegalStateException.class, extension::delete);
    }

    @Test
    public void checkFromArchivedTransitions() {
        ServiceOfferingExtension extension = getArchivedExtension();

        assertThrows(IllegalStateException.class, extension::inDraft);
        assertThrows(IllegalStateException.class, extension::release);
        assertThrows(IllegalStateException.class, extension::revoke);
        assertThrows(IllegalStateException.class, extension::delete);
    }
}
