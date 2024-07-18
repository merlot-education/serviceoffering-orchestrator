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

package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceOfferingStateTest {

    @Test
    void transitionFromInDraft() {
        ServiceOfferingState state = ServiceOfferingState.IN_DRAFT;

        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.DELETED));

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }

    @Test
    void transitionFromReleased() {
        ServiceOfferingState state = ServiceOfferingState.RELEASED;

        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }

    @Test
    void transitionFromRevoked() {
        ServiceOfferingState state = ServiceOfferingState.REVOKED;

        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
        assertTrue(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
    }

    @Test
    void transitionFromDeleted() {
        ServiceOfferingState state = ServiceOfferingState.DELETED;

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }

    @Test
    void transitionFromArchived() {
        ServiceOfferingState state = ServiceOfferingState.DELETED;

        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.DELETED));
        assertFalse(state.checkTransitionAllowed(ServiceOfferingState.ARCHIVED));
    }
}
