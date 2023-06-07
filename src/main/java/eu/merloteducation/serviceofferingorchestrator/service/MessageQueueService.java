package eu.merloteducation.serviceofferingorchestrator.service;

import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.models.messagequeue.ContractTemplateCreated;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageQueueService {

    @RabbitListener(queues = MessageQueueConfig.CONTRACT_CREATED_KEY)
	private void listen(ContractTemplateCreated contractTemplateCreated) {
		System.out.println("Message read from serviceoffering.queue : "
                + contractTemplateCreated.getContractId() + " " + contractTemplateCreated.getServiceOfferingId());
	}
}
