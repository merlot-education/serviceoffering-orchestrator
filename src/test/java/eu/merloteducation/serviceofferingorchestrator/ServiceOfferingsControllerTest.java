package eu.merloteducation.serviceofferingorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.authorizationlibrary.authorization.AuthorityChecker;
import eu.merloteducation.authorizationlibrary.authorization.JwtAuthConverter;
import eu.merloteducation.authorizationlibrary.authorization.JwtAuthConverterProperties;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.serviceofferingorchestrator.auth.OfferingAuthorityChecker;
import eu.merloteducation.serviceofferingorchestrator.controller.ServiceOfferingsController;
import eu.merloteducation.serviceofferingorchestrator.models.dto.OfferingMetaDto;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingBasicDto;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.*;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.CooperationCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.security.WebSecurityConfig;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSWizardRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ServiceOfferingsController.class, WebSecurityConfig.class,
        OfferingAuthorityChecker.class})
@Import({ AuthorityChecker.class, JwtAuthConverter.class})
@AutoConfigureMockMvc()
class ServiceOfferingsControllerTest {

    @MockBean
    private GXFSCatalogRestService gxfsCatalogRestService;

    @MockBean
    private GXFSWizardRestService gxfsWizardRestService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @MockBean
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @Autowired
    private MockMvc mvc;

    private String objectAsJsonString(final Object obj) {
        try {
            return JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @BeforeEach
    public void setUp() throws Exception {

        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.setIssuer("Participant:10");
        lenient().when(serviceOfferingExtensionRepository.findById("notreleased")).thenReturn(Optional.of(extension));

        ServiceOfferingExtension extension2 = new ServiceOfferingExtension();
        extension2.release();
        extension2.setIssuer("Participant:10");
        lenient().when(serviceOfferingExtensionRepository.findById("released")).thenReturn(Optional.of(extension2));

        ServiceOfferingDto serviceOfferingNotReleasedDto = new ServiceOfferingDto();
        serviceOfferingNotReleasedDto.setMetadata(new OfferingMetaDto());
        serviceOfferingNotReleasedDto.getMetadata().setState("IN_DRAFT");
        serviceOfferingNotReleasedDto.setSelfDescription(new SelfDescription());
        serviceOfferingNotReleasedDto.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        serviceOfferingNotReleasedDto.getSelfDescription().getVerifiableCredential().setIssuer("Participant:10");

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
                .getServiceOfferingById(eq("notreleased"))).thenReturn(serviceOfferingNotReleasedDto);

        lenient().when(gxfsCatalogRestService
                .getServiceOfferingById(eq("garbage"))).thenThrow(NoSuchElementException.class);

        lenient().when(gxfsCatalogRestService
                .addServiceOffering(any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(gxfsCatalogRestService
                .addServiceOffering(any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(gxfsCatalogRestService
                .regenerateOffering(any())).thenReturn(selfDescriptionsCreateResponse);

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
    void getOrganizationOfferingsForbidden() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOfferingByIdUnauthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/serviceoffering/released")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOfferingByIdPublicNotReleased() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/serviceoffering/notreleased")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getOfferingByIdCreatorNotReleased() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/serviceoffering/notreleased")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOfferingByIdPublicReleased() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/serviceoffering/released")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOfferingByIdCreatorReleased() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/serviceoffering/released")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOfferingByIdNonExistent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/serviceoffering/garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    private void setValidCredentialSubjectFields(ServiceOfferingCredentialSubject credentialSubject) {
        credentialSubject.setId("ServiceOffering:TBR");
        credentialSubject.setContext(new HashMap<>());
        credentialSubject.setOfferedBy(new NodeKindIRITypeId("Participant:10"));
        credentialSubject.setName(new StringTypeValue("Test Offering"));

        List<TermsAndConditions> tncList = new ArrayList<>();
        TermsAndConditions tnc = new TermsAndConditions();
        tnc.setContent(new StringTypeValue("http://example.com"));
        tnc.setHash(new StringTypeValue("1234"));
        tnc.setType("gax-trust-framework:TermsAndConditions");
        tncList.add(tnc);
        credentialSubject.setTermsAndConditions(tncList);

        List<StringTypeValue> policies = new ArrayList<>();
        policies.add(new StringTypeValue("Policy"));
        credentialSubject.setPolicy(policies);

        List<DataAccountExport> exports = new ArrayList<>();
        DataAccountExport export = new DataAccountExport();
        export.setFormatType(new StringTypeValue("dummyValue"));
        export.setAccessType(new StringTypeValue("dummyValue"));
        export.setRequestType(new StringTypeValue("dummyValue"));
        exports.add(export);
        credentialSubject.setDataAccountExport(exports);

        credentialSubject.setProvidedBy(new NodeKindIRITypeId("Participant:10"));
        credentialSubject.setCreationDate(new StringTypeValue("1234"));

        List<eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime> runtimeOptions = new ArrayList<>();
        eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime runtimeUnlimited = new Runtime();
        runtimeUnlimited.setRuntimeCount(new NumberTypeValue(0));
        runtimeUnlimited.setRuntimeMeasurement(new StringTypeValue("unlimited"));
        runtimeOptions.add(runtimeUnlimited);
        credentialSubject.setRuntimeOptions(runtimeOptions);

        credentialSubject.setMerlotTermsAndConditionsAccepted(true);
    }

    private void setValidSaasCredentialSubjectFields(SaaSCredentialSubject credentialSubject) {
        credentialSubject.setType("merlot:MerlotServiceOfferingSaaS");
        List<AllowedUserCount> userCountOptions = new ArrayList<>();
        AllowedUserCount userCountUnlimted = new AllowedUserCount();
        userCountUnlimted.setUserCountUpTo(new NumberTypeValue(0));
        userCountOptions.add(userCountUnlimted);
        credentialSubject.setUserCountOptions(userCountOptions);
    }

    private void setValidDataDeliveryCredentialSubjectFields(DataDeliveryCredentialSubject credentialSubject) {
        credentialSubject.setType("merlot:MerlotServiceOfferingDataDelivery");
        List<DataExchangeCount> exchangeCountOptions = new ArrayList<>();
        DataExchangeCount exchangeCount = new DataExchangeCount();
        exchangeCount.setExchangeCountUpTo(new NumberTypeValue(5));
        exchangeCountOptions.add(exchangeCount);
        credentialSubject.setExchangeCountOptions(exchangeCountOptions);
        credentialSubject.setDataAccessType(new StringTypeValue("Download"));
        credentialSubject.setDataTransferType(new StringTypeValue("Pull"));
    }

    private void setValidCooperationCredentialSubjectFields(CooperationCredentialSubject credentialSubject) {
        credentialSubject.setType("merlot:MerlotServiceOfferingCooperation");
    }

    private SaaSCredentialSubject createValidSaasCredentialSubject() {
        SaaSCredentialSubject credentialSubject = new SaaSCredentialSubject();
        setValidCredentialSubjectFields(credentialSubject);
        setValidSaasCredentialSubjectFields(credentialSubject);
        return credentialSubject;
    }

    private DataDeliveryCredentialSubject createValidDataDeliveryCredentialSubject() {
        DataDeliveryCredentialSubject credentialSubject = new DataDeliveryCredentialSubject();
        setValidCredentialSubjectFields(credentialSubject);
        setValidDataDeliveryCredentialSubjectFields(credentialSubject);
        return credentialSubject;
    }

    private CooperationCredentialSubject createValidCooperationCredentialSubject() {
        CooperationCredentialSubject credentialSubject = new CooperationCredentialSubject();
        setValidCredentialSubjectFields(credentialSubject);
        setValidCooperationCredentialSubjectFields(credentialSubject);
        return credentialSubject;
    }

    @Test
    void addSaasOfferingUnauthenticated() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addSaasOfferingForbidden() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId("Participant:10"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addSaasOfferingAllowed() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId("Participant:10"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void addDataDeliveryOfferingUnauthenticated() throws Exception {
        DataDeliveryCredentialSubject credentialSubject = createValidDataDeliveryCredentialSubject();
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addDataDeliveryOfferingForbidden() throws Exception {
        DataDeliveryCredentialSubject credentialSubject = createValidDataDeliveryCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId("Participant:10"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addDataDeliveryOfferingAllowed() throws Exception {
        DataDeliveryCredentialSubject credentialSubject = createValidDataDeliveryCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId("Participant:10"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void addCooperationOfferingUnauthenticated() throws Exception {
        CooperationCredentialSubject credentialSubject = createValidCooperationCredentialSubject();
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingCooperation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCooperationOfferingForbidden() throws Exception {
        CooperationCredentialSubject credentialSubject = createValidCooperationCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId("Participant:10"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingCooperation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addCooperationOfferingAllowed() throws Exception {
        CooperationCredentialSubject credentialSubject = createValidCooperationCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId("Participant:10"));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingCooperation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void regenerateOfferingUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/regenerate/1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regenerateOfferingAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/regenerate/released")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shiftOfferingUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .patch("/serviceoffering/status/released/REVOKED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shiftOfferingAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .patch("/serviceoffering/status/released/REVOKED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableShapesUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/getAvailableShapesCategorized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAvailableShapesAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/getAvailableShapesCategorized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getShapeJsonUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/getJSON?name=shape.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getShapeJsonAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/getJSON?name=shape.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }


}
