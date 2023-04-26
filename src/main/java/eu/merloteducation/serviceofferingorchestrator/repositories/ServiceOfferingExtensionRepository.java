package eu.merloteducation.serviceofferingorchestrator.repositories;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import org.springframework.data.repository.CrudRepository;

public interface ServiceOfferingExtensionRepository extends CrudRepository<ServiceOfferingExtension, String> {
}
