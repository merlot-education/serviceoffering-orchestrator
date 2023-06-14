package eu.merloteducation.serviceofferingorchestrator.repositories;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOfferingExtensionRepository extends JpaRepository<ServiceOfferingExtension, String> {
    Page<ServiceOfferingExtension> findAllByState(ServiceOfferingState state, Pageable pageable);

    Page<ServiceOfferingExtension> findAllByIssuer(String issuer, Pageable pageable);

    Page<ServiceOfferingExtension> findAllByIssuerAndState(String issuer, ServiceOfferingState state, Pageable pageable);

}
