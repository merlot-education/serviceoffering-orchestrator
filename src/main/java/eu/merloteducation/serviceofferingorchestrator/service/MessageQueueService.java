package eu.merloteducation.serviceofferingorchestrator.service;

import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.messagequeue.ContractTemplateUpdated;
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
	private void contractCreatedListener(ContractTemplateUpdated contractTemplateUpdated) {
		logger.info("Contract created message: {}", contractTemplateUpdated);

		ServiceOfferingExtension extension = serviceOfferingExtensionRepository
				.findById(contractTemplateUpdated.getServiceOfferingId()).orElse(null);

		if (extension != null) {
			extension.addAssociatedContract(contractTemplateUpdated.getContractId());
		} else {
			logger.error("No Service Offering with ID {} was found, hence associated contracts are not updated.",
					contractTemplateUpdated.getServiceOfferingId());
			return;
		}

		serviceOfferingExtensionRepository.save(extension);
	}

	@RabbitListener(queues = MessageQueueConfig.CONTRACT_PURGED_QUEUE)
	private void contractPurgedListener(ContractTemplateUpdated contractTemplateUpdated) {
		logger.info("Contract deleted message: {}", contractTemplateUpdated);

		ServiceOfferingExtension extension = serviceOfferingExtensionRepository
				.findById(contractTemplateUpdated.getServiceOfferingId()).orElse(null);

		if (extension != null) {
			if (extension.getAssociatedContractIds().contains(contractTemplateUpdated.getContractId())) {
				extension.getAssociatedContractIds().remove(contractTemplateUpdated.getContractId());
			} else {
				logger.error("No Contract with ID {} was found in service offering {}.",
						contractTemplateUpdated.getContractId(), contractTemplateUpdated.getServiceOfferingId());
				return;
			}
		} else {
			logger.error("No Service Offering with ID {} was found, hence associated contracts are not updated.",
					contractTemplateUpdated.getServiceOfferingId());
			return;
		}

		serviceOfferingExtensionRepository.save(extension);
	}
}
