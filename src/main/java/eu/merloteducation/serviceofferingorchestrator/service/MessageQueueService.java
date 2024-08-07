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

package eu.merloteducation.serviceofferingorchestrator.service;

import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.modelslib.queue.ContractTemplateUpdated;
import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MessageQueueService {

    private final ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;
    private final ServiceOfferingsService serviceOfferingsService;

    public MessageQueueService(@Autowired ServiceOfferingExtensionRepository serviceOfferingExtensionRepository,
                               @Autowired ServiceOfferingsService serviceOfferingsService) {
        this.serviceOfferingExtensionRepository = serviceOfferingExtensionRepository;
        this.serviceOfferingsService = serviceOfferingsService;
    }

    /**
     * Listen for the event that a contract was created on the message bus.
     * In that case, update the corresponding offering to be linked to the contract.
     *
     * @param contractTemplateUpdated contract created event details
     */
    @RabbitListener(queues = MessageQueueConfig.CONTRACT_CREATED_QUEUE)
    public void contractCreatedListener(ContractTemplateUpdated contractTemplateUpdated) {
        log.info("Contract created message: {}", contractTemplateUpdated);

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository
                .findById(contractTemplateUpdated.getServiceOfferingId()).orElse(null);

        if (extension != null) {
            extension.addAssociatedContract(contractTemplateUpdated.getContractId());
        } else {
            log.error("No Service Offering with ID {} was found, hence associated contracts are not updated.",
                    contractTemplateUpdated.getServiceOfferingId());
            return;
        }

        serviceOfferingExtensionRepository.save(extension);
    }

    /**
     * Listen for the event that a contract was purged on the message bus.
     * In that case, update the corresponding offering to be no longer linked to the contract.
     *
     * @param contractTemplateUpdated contract purged event details
     */
    @RabbitListener(queues = MessageQueueConfig.CONTRACT_PURGED_QUEUE)
    public void contractPurgedListener(ContractTemplateUpdated contractTemplateUpdated) {
        log.info("Contract deleted message: {}", contractTemplateUpdated);

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository
                .findById(contractTemplateUpdated.getServiceOfferingId()).orElse(null);

        if (extension != null) {
            if (extension.getAssociatedContractIds().contains(contractTemplateUpdated.getContractId())) {
                extension.getAssociatedContractIds().remove(contractTemplateUpdated.getContractId());
            } else {
                log.error("No Contract with ID {} was found in service offering {}.",
                        contractTemplateUpdated.getContractId(), contractTemplateUpdated.getServiceOfferingId());
                return;
            }
        } else {
            log.error("No Service Offering with ID {} was found, hence associated contracts are not updated.",
                    contractTemplateUpdated.getServiceOfferingId());
            return;
        }

        serviceOfferingExtensionRepository.save(extension);
    }

    /**
     * Listen for request of offering details on the message bus.
     *
     * @param offeringId id of the offering
     */
    @RabbitListener(queues = MessageQueueConfig.OFFERING_REQUEST_QUEUE)
    public ServiceOfferingDto offeringDetailsRequestListener(String offeringId) {
        log.info("Offering request message: offering ID {}", offeringId);
        return serviceOfferingsService.getServiceOfferingById(offeringId);
    }

    /**
     * Listen for the event that an organization's membership has been revoked on the message bus.
     * In that case, revoke the offerings associated with that organization.
     *
     * @param orgaId id of the organization whose membership has been revoked
     */
    @RabbitListener(queues = MessageQueueConfig.ORGANIZATION_REVOKED_QUEUE)
    public void organizationRevokedListener(String orgaId) {
        log.info("Organization revoked message: organization ID {}", orgaId);

        List<ServiceOfferingExtension> releasedOfferingsByRevokedOrga = serviceOfferingExtensionRepository.findAllByIssuerAndState(
            orgaId, ServiceOfferingState.RELEASED);

        if (!releasedOfferingsByRevokedOrga.isEmpty()) {
            log.info("Revoking released service offerings of organization with ID {}", orgaId);
            releasedOfferingsByRevokedOrga.forEach(extension -> {
                extension.revoke();
                serviceOfferingExtensionRepository.save(extension);
            });
        } else {
            log.info("No released service offerings of organization with ID {} found to delete", orgaId);
        }
    }
}
