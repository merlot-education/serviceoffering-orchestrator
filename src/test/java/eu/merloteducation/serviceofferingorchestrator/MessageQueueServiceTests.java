package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.modelslib.queue.ContractTemplateUpdated;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.service.MessageQueueService;
import eu.merloteducation.serviceofferingorchestrator.service.ServiceOfferingsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageQueueServiceTests {

    @Autowired
    MessageQueueService messageQueueService;

    @Autowired
    ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @Mock
    ServiceOfferingsService serviceOfferingsService;

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(messageQueueService, "serviceOfferingExtensionRepository", serviceOfferingExtensionRepository);
        ReflectionTestUtils.setField(messageQueueService, "serviceOfferingsService", serviceOfferingsService);
        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.setId("1234");
        serviceOfferingExtensionRepository.save(extension);
        when(serviceOfferingsService.getServiceOfferingById(any())).thenThrow(NoSuchElementException.class);
        doReturn(new ServiceOfferingDto()).when(serviceOfferingsService).getServiceOfferingById("1234");
    }

    @Transactional
    @Test
    void contractCreatedForExistingOffering() {
        ContractTemplateUpdated contractTemplateUpdated = new ContractTemplateUpdated("contract", "1234");
        messageQueueService.contractCreatedListener(contractTemplateUpdated);

        ServiceOfferingExtension offering = serviceOfferingExtensionRepository.findById(contractTemplateUpdated
                .getServiceOfferingId()).orElse(null);
        assertNotNull(offering);
        assertThat(offering.getAssociatedContractIds()).contains(contractTemplateUpdated.getContractId());
    }

    @Transactional
    @Test
    void contractPurgedForExistingOffering() {
        ContractTemplateUpdated contractTemplateUpdated = new ContractTemplateUpdated("contract", "1234");
        ServiceOfferingExtension offering = serviceOfferingExtensionRepository.findById(contractTemplateUpdated
                .getServiceOfferingId()).orElse(null);
        offering.addAssociatedContract(contractTemplateUpdated.getContractId());
        serviceOfferingExtensionRepository.save(offering);

        messageQueueService.contractPurgedListener(contractTemplateUpdated);

        offering = serviceOfferingExtensionRepository.findById(contractTemplateUpdated
                .getServiceOfferingId()).orElse(null);
        assertNotNull(offering);
        assertThat(offering.getAssociatedContractIds()).doesNotContain(contractTemplateUpdated.getContractId());
    }

    @Test
    void offeringDetailsRequestExisting() throws Exception {
        ServiceOfferingDto offeringDto = messageQueueService.offeringDetailsRequestListener("1234");
        assertNotNull(offeringDto);
    }

    @Transactional
    @Test
    void organizationRevokedDeleteReleasedOfferings() {

        ServiceOfferingExtension initialExtension = serviceOfferingExtensionRepository.findById("1234").orElse(null);
        assertNotNull(initialExtension);
        assertThat(initialExtension.getState()).isEqualTo(ServiceOfferingState.IN_DRAFT);

        initialExtension.setIssuer("issuer");
        initialExtension.release();
        serviceOfferingExtensionRepository.save(initialExtension);

        ServiceOfferingExtension updatedExtension = serviceOfferingExtensionRepository.findById("1234").orElse(null);
        assertNotNull(updatedExtension);
        assertThat(updatedExtension.getState()).isEqualTo(ServiceOfferingState.RELEASED);

        messageQueueService.organizationRevokedListener("issuer");

        ServiceOfferingExtension extensionAfterOrganizationRevoked = serviceOfferingExtensionRepository.findById("1234").orElse(null);
        assertNotNull(extensionAfterOrganizationRevoked);
        assertThat(extensionAfterOrganizationRevoked.getState()).isEqualTo(ServiceOfferingState.REVOKED);
    }

}
