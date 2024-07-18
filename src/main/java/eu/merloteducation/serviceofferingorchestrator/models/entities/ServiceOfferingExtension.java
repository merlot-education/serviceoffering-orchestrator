/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.serviceofferingorchestrator.models.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class ServiceOfferingExtension {
    @Id
    private String id;

    private String currentSdHash;

    private String issuer; // duplicated from gxfs catalog to allow local querying

    private OffsetDateTime creationDate; // duplicated from gxfs catalog to allow local paging

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private ServiceOfferingState state;

    @Setter(AccessLevel.NONE)
    private List<String> associatedContractIds;

    public ServiceOfferingExtension() {
        this.state = ServiceOfferingState.IN_DRAFT;
        this.associatedContractIds = new ArrayList<>();
        this.creationDate = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void release() throws IllegalStateException {
        if (state.checkTransitionAllowed(ServiceOfferingState.RELEASED)) {
            state = ServiceOfferingState.RELEASED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to released", state.name()));
        }
    }

    public void delete() throws IllegalStateException {
        if (state.checkTransitionAllowed(ServiceOfferingState.DELETED) && associatedContractIds.isEmpty()) {
            state = ServiceOfferingState.DELETED;
        } else if (state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED) && !associatedContractIds.isEmpty()) {
            state = ServiceOfferingState.ARCHIVED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to deleted/archived", state.name()));
        }
    }

    public void inDraft() throws IllegalStateException {
        if (state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT) && associatedContractIds.isEmpty()) {
            state = ServiceOfferingState.IN_DRAFT;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to in draft", state.name()));
        }
    }

    public void revoke() throws IllegalStateException {
        if (state.checkTransitionAllowed(ServiceOfferingState.REVOKED)) {
            state = ServiceOfferingState.REVOKED;
        } else {
            throw new IllegalStateException(String.format("Cannot transition from state %s to revoked", state.name()));
        }
    }

    public void addAssociatedContract(String contractId) {
        this.associatedContractIds.add(contractId);
    }

}
