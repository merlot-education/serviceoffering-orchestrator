package eu.merloteducation.serviceofferingorchestrator.repositories;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ServiceOfferingExtensionRepository extends CrudRepository<ServiceOfferingExtension, String> {
    List<ServiceOfferingExtension> findAllByState(ServiceOfferingState state);

    List<ServiceOfferingExtension> findAllByIssuer(String issuer);

}
