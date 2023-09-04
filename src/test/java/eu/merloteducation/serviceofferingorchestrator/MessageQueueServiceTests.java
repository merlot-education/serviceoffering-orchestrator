package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.messagequeue.ContractTemplateUpdated;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.MessageQueueService;
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

import java.util.NoSuchElementException;
import java.util.Optional;

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
    GXFSCatalogRestService gxfsCatalogRestService;

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(messageQueueService, "serviceOfferingExtensionRepository", serviceOfferingExtensionRepository);
        ReflectionTestUtils.setField(messageQueueService, "gxfsCatalogRestService", gxfsCatalogRestService);
        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.setId("1234");
        serviceOfferingExtensionRepository.save(extension);
        when(gxfsCatalogRestService.getServiceOfferingById(any())).thenThrow(NoSuchElementException.class);
        doReturn(new ServiceOfferingDto()).when(gxfsCatalogRestService).getServiceOfferingById("1234");
    }

    @Transactional
    @Test
    void contractCreatedForExistingOffering() {
        ContractTemplateUpdated contractTemplateUpdated = new ContractTemplateUpdated();
        contractTemplateUpdated.setContractId("contract");
        contractTemplateUpdated.setServiceOfferingId("1234");
        messageQueueService.contractCreatedListener(contractTemplateUpdated);

        ServiceOfferingExtension offering = serviceOfferingExtensionRepository.findById(contractTemplateUpdated
                .getServiceOfferingId()).orElse(null);
        assertNotNull(offering);
        assertThat(offering.getAssociatedContractIds()).contains(contractTemplateUpdated.getContractId());
    }

    @Transactional
    @Test
    void contractPurgedForExistingOffering() {
        ContractTemplateUpdated contractTemplateUpdated = new ContractTemplateUpdated();
        contractTemplateUpdated.setContractId("contract");
        contractTemplateUpdated.setServiceOfferingId("1234");
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

}
