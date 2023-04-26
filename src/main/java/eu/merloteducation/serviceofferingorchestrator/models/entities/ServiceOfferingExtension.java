package eu.merloteducation.serviceofferingorchestrator.models.entities;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ServiceOfferingExtension {
    @Id
    private String sdHash;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private ServiceOfferingState state;

    private String associatedContractId;

    public ServiceOfferingExtension() {
        this.state = ServiceOfferingState.IN_DRAFT;
        this.associatedContractId = "";
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
                || (state == ServiceOfferingState.REVOKED && associatedContractId.equals(""))) {
            state = ServiceOfferingState.DELETED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to deleted or contract is associated", state.name()));
        }
    }

    public void archive() throws IllegalStateException {
        if (state == ServiceOfferingState.REVOKED && !associatedContractId.equals("")) {
            state = ServiceOfferingState.ARCHIVED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to archived or no contract is associated", state.name()));
        }
    }

    public void inDraft() throws IllegalStateException {
        if (state == ServiceOfferingState.REVOKED && associatedContractId.equals("")) {
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
