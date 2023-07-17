package eu.merloteducation.serviceofferingorchestrator.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueueConfig {

    public static final String ORCHESTRATOR_EXCHANGE = "orchestrator.exchange";
    public static final String CONTRACT_CREATED_KEY = "created.contract";
    public static final String CONTRACT_PURGED_KEY = "purged.contract";
    public static final String CONTRACT_CREATED_QUEUE = "serviceoffering.create.contract.queue";
    public static final String CONTRACT_PURGED_QUEUE = "serviceoffering.purge.contract.queue";
    @Bean
    DirectExchange orchestratorExchange() {
        return new DirectExchange(ORCHESTRATOR_EXCHANGE);
    }

    @Bean
    Binding createdContractBinding(Queue contractCreatedQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(contractCreatedQueue).to(orchestratorExchange).with(CONTRACT_CREATED_KEY);
    }

    @Bean
    Binding purgedContractBinding(Queue contractPurgedQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(contractPurgedQueue).to(orchestratorExchange).with(CONTRACT_PURGED_KEY);
    }

    @Bean
    public Queue contractCreatedQueue() {
        return new Queue(CONTRACT_CREATED_QUEUE, false);
    }

    @Bean
    public Queue contractPurgedQueue() {
        return new Queue(CONTRACT_PURGED_QUEUE, false);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

}
