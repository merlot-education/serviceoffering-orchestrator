package eu.merloteducation.serviceofferingorchestrator;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServiceofferingOrchestratorApplication {
	public static void main(String[] args) {
		SpringApplication.run(ServiceofferingOrchestratorApplication.class, args);
	}

}
