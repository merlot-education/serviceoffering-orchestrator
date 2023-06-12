package eu.merloteducation.serviceofferingorchestrator.service;

import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.messagequeue.ContractTemplateCreated;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageQueueService {

    @Autowired
    ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    private final Logger logger = LoggerFactory.getLogger(MessageQueueService.class);

    @RabbitListener(queues = MessageQueueConfig.CONTRACT_CREATED_QUEUE)
    private void listen(ContractTemplateCreated contractTemplateCreated) {
        logger.info("Contract created message: {}", contractTemplateCreated);

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository
                .findById(contractTemplateCreated.getServiceOfferingId()).orElse(null);

        if (extension != null) {
            extension.addAssociatedContract(contractTemplateCreated.getContractId());
        } else {
            logger.error("No Service Offering with ID {} was found, hence associated contracts are not updated.",
                    contractTemplateCreated.getServiceOfferingId());
            return;
        }

        serviceOfferingExtensionRepository.save(extension);
    }
}
