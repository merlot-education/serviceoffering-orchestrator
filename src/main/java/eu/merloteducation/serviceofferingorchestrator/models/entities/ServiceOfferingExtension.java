package eu.merloteducation.serviceofferingorchestrator.models.entities;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class ServiceOfferingExtension {
    @Id
    private String id;

    private String currentSdHash;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private ServiceOfferingState state;

    private List<String> associatedContractIds;

    public ServiceOfferingExtension() {
        this.state = ServiceOfferingState.IN_DRAFT;
        this.associatedContractIds = new ArrayList<>();
    }

    public ServiceOfferingExtension(String id, String currentSdHash) {
        this();
        this.id = id;
        this.currentSdHash = currentSdHash;
    }

    public void release() throws IllegalStateException {
        if (state == ServiceOfferingState.IN_DRAFT
                || state == ServiceOfferingState.REVOKED) {
            state = ServiceOfferingState.RELEASED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to released", state.name()));
        }
    }

    public void delete() throws IllegalStateException {
        if (state == ServiceOfferingState.IN_DRAFT
                || (state == ServiceOfferingState.REVOKED && associatedContractIds.isEmpty())) {
            state = ServiceOfferingState.DELETED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to deleted or contract is associated", state.name()));
        }
    }

    public void archive() throws IllegalStateException {
        if (state == ServiceOfferingState.REVOKED && !associatedContractIds.isEmpty()) {
            state = ServiceOfferingState.ARCHIVED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to archived or no contract is associated", state.name()));
        }
    }

    public void inDraft() throws IllegalStateException {
        if (state == ServiceOfferingState.REVOKED && associatedContractIds.isEmpty()) {
            state = ServiceOfferingState.IN_DRAFT;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to archived or contract is associated", state.name()));
        }
    }

    public void revoke() throws IllegalStateException {
        if (state == ServiceOfferingState.RELEASED) {
            state = ServiceOfferingState.IN_DRAFT;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to revoked", state.name()));
        }
    }

}
