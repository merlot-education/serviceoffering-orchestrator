package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.controller.ServiceOfferingsController;
import eu.merloteducation.serviceofferingorchestrator.models.dto.OfferingMetaDto;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingBasicDto;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.security.JwtAuthConverter;
import eu.merloteducation.serviceofferingorchestrator.security.JwtAuthConverterProperties;
import eu.merloteducation.serviceofferingorchestrator.security.WebSecurityConfig;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSWizardRestService;
import eu.merloteducation.serviceofferingorchestrator.service.KeycloakAuthService;
import eu.merloteducation.serviceofferingorchestrator.service.MessageQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ServiceOfferingsController.class, WebSecurityConfig.class})
@AutoConfigureMockMvc()
public class ServiceOfferingsControllerTest {

    @MockBean
    private GXFSCatalogRestService gxfsCatalogRestService;

    @MockBean
    private GXFSWizardRestService gxfsWizardRestService;

    @MockBean
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @Autowired
    private MockMvc mvc;


    @BeforeEach
    public void setUp() throws Exception {

        ServiceOfferingDto serviceOfferingDto = new ServiceOfferingDto();
        serviceOfferingDto.setMetadata(new OfferingMetaDto());
        serviceOfferingDto.getMetadata().setState("RELEASED");
        serviceOfferingDto.setSelfDescription(new SelfDescription());
        serviceOfferingDto.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        serviceOfferingDto.getSelfDescription().getVerifiableCredential().setIssuer("Participant:10");
        ServiceOfferingBasicDto serviceOfferingBasicDto = new ServiceOfferingBasicDto();
        SelfDescriptionsCreateResponse selfDescriptionsCreateResponse = new SelfDescriptionsCreateResponse();

        lenient().when(gxfsCatalogRestService
                .getAllPublicServiceOfferings(any())).thenReturn(new PageImpl<>(List.of(serviceOfferingBasicDto)));

        lenient().when(gxfsCatalogRestService
                .getOrganizationServiceOfferings(any(), any(), any())).thenReturn(new PageImpl<>(List.of(serviceOfferingBasicDto)));

        lenient().when(gxfsCatalogRestService
                .getServiceOfferingById(any())).thenReturn(serviceOfferingDto);

        lenient().when(gxfsCatalogRestService
                .addServiceOffering(any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(gxfsCatalogRestService
                .addServiceOffering(any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(gxfsCatalogRestService
                .regenerateOffering(any(), any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(gxfsWizardRestService.getServiceOfferingShapes()).thenReturn(new HashMap<>());

        lenient().when(gxfsWizardRestService.getShape(any())).thenReturn("shape");
    }

    @Test
    void getAllPublicOfferingsUnauthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOrganizationOfferingsUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrganizationOfferingsAuthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOfferingByIdAuthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/serviceoffering/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }


}
