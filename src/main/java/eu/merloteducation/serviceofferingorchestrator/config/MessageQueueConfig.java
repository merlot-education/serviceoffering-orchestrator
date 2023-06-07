package eu.merloteducation.serviceofferingorchestrator.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueueConfig {
    @Bean
    DirectExchange orchestratorExchange() {
        return new DirectExchange("orchestrator.exchange");
    }

    @Bean
    Binding createdContractBinding(Queue serviceofferingQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(serviceofferingQueue).to(orchestratorExchange).with("created.contract");
    }

    @Bean
    public Queue serviceofferingQueue() {
        return new Queue("serviceoffering.queue", false);
    }

}
