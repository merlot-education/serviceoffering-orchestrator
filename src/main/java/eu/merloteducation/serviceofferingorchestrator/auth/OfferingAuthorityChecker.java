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

package eu.merloteducation.serviceofferingorchestrator.auth;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import eu.merloteducation.authorizationlibrary.authorization.AuthorityChecker;

import java.util.Set;

@Component("offeringAuthorityChecker")
public class OfferingAuthorityChecker {

    private final AuthorityChecker authorityChecker;

    private final ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    public OfferingAuthorityChecker(@Autowired AuthorityChecker authorityChecker,
                                    @Autowired ServiceOfferingExtensionRepository serviceOfferingExtensionRepository) {
        this.authorityChecker = authorityChecker;
        this.serviceOfferingExtensionRepository = serviceOfferingExtensionRepository;
    }

    /**
     * Given the current authentication and the id of a service offering, check if it is readable by the requesting
     * party.
     *
     * @param authentication current authentication
     * @param offeringId     id of the offering to request
     * @return can access the requested offering
     */
    public boolean canAccessOffering(Authentication authentication, String offeringId) {
        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(offeringId).orElse(null);
        return isOfferingPublic(extension) || isOfferingIssuer(authentication, extension);
    }

    private boolean isOfferingPublic(ServiceOfferingExtension extension) {
        if (extension != null) {
            return extension.getState() == ServiceOfferingState.RELEASED;
        }
        return false;
    }

    private boolean isOfferingIssuer(Authentication authentication, ServiceOfferingExtension extension) {
        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);

        if (extension != null) {
            return representedOrgaIds.contains(extension.getIssuer());
        }
        return false;
    }

    /**
     * Given the current authentication and an offering id, check whether the offering was issued by one
     * of the represented roles.
     *
     * @param authentication current authentication
     * @param offeringId     id of the offering to request
     * @return offering was issued by represented role
     */
    public boolean isOfferingIssuer(Authentication authentication, String offeringId) {
        return isOfferingIssuer(authentication,
                serviceOfferingExtensionRepository.findById(offeringId).orElse(null));
    }

    public boolean representsProviderParticipant(Authentication authentication, ServiceOfferingDto dto) {
        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);
        GxServiceOfferingCredentialSubject cs =
                dto.getSelfDescription().findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class);
        return representedOrgaIds.contains(cs.getProvidedBy().getId());
    }
}
