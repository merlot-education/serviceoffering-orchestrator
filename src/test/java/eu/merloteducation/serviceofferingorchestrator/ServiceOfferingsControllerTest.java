package eu.merloteducation.serviceofferingorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.authorizationlibrary.authorization.*;
import eu.merloteducation.authorizationlibrary.config.InterceptorConfig;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.*;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.DataExchangeCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.Runtime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.CooperationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.DataDeliveryCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.SaaSCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsWizardApiService;
import eu.merloteducation.serviceofferingorchestrator.auth.OfferingAuthorityChecker;
import eu.merloteducation.modelslib.api.serviceoffering.OfferingMetaDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingBasicDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.controller.ServiceOfferingsController;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.security.WebSecurityConfig;
import eu.merloteducation.serviceofferingorchestrator.service.ServiceOfferingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.net.URI;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ServiceOfferingsController.class, WebSecurityConfig.class,
        OfferingAuthorityChecker.class})
@Import({ AuthorityChecker.class, ActiveRoleHeaderHandlerInterceptor.class, JwtAuthConverter.class, InterceptorConfig.class})
@AutoConfigureMockMvc()
class ServiceOfferingsControllerTest {

    @MockBean
    private ServiceOfferingsService serviceOfferingsService;

    @MockBean
    private GxfsWizardApiService gxfsWizardApiService;

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

    private String getParticipantId(int num) {
        String DID_DOMAIN = "test.eu";
        return "did:web:"+ DID_DOMAIN + ":participant:orga-" + num;
    }

    @BeforeEach
    public void setUp() throws Exception {

        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.setIssuer(getParticipantId(10));
        lenient().when(serviceOfferingExtensionRepository.findById("notreleased")).thenReturn(Optional.of(extension));

        ServiceOfferingExtension extension2 = new ServiceOfferingExtension();
        extension2.release();
        extension2.setIssuer(getParticipantId(10));
        lenient().when(serviceOfferingExtensionRepository.findById("released")).thenReturn(Optional.of(extension2));

        ServiceOfferingDto serviceOfferingNotReleasedDto = new ServiceOfferingDto();
        serviceOfferingNotReleasedDto.setMetadata(new OfferingMetaDto());
        serviceOfferingNotReleasedDto.getMetadata().setState("IN_DRAFT");
        serviceOfferingNotReleasedDto.setSelfDescription(new SelfDescription());
        serviceOfferingNotReleasedDto.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        serviceOfferingNotReleasedDto.getSelfDescription().getVerifiableCredential().setIssuer(getParticipantId(10));

        ServiceOfferingDto serviceOfferingDto = new ServiceOfferingDto();
        serviceOfferingDto.setMetadata(new OfferingMetaDto());
        serviceOfferingDto.getMetadata().setState("RELEASED");
        serviceOfferingDto.setSelfDescription(new SelfDescription());
        serviceOfferingDto.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        serviceOfferingDto.getSelfDescription().getVerifiableCredential().setIssuer(getParticipantId(10));
        ServiceOfferingBasicDto serviceOfferingBasicDto = new ServiceOfferingBasicDto();
        serviceOfferingBasicDto.setId("1234");
        serviceOfferingBasicDto.setName("bla");
        SelfDescriptionMeta selfDescriptionsCreateResponse = new SelfDescriptionMeta();

        lenient().when(serviceOfferingsService
                .getAllPublicServiceOfferings(any())).thenReturn(null);

        lenient().when(serviceOfferingsService
                .getOrganizationServiceOfferings(any(), any(), any())).thenReturn(null);

        lenient().when(serviceOfferingsService
                .getServiceOfferingById(any())).thenReturn(serviceOfferingDto);

        lenient().when(serviceOfferingsService
                .getServiceOfferingById(eq("notreleased"))).thenReturn(serviceOfferingNotReleasedDto);

        lenient().when(serviceOfferingsService
                .getServiceOfferingById(eq("garbage"))).thenThrow(NoSuchElementException.class);

        lenient().when(serviceOfferingsService
                .addServiceOffering(any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(serviceOfferingsService
                .addServiceOffering(any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(serviceOfferingsService
                .regenerateOffering(any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(gxfsWizardApiService.getServiceOfferingShapesByEcosystem(eq("merlot"))).thenReturn(Collections.emptyList());

        lenient().when(gxfsWizardApiService.getShapeByName(any())).thenReturn("shape");
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
                        .get(new URI("/organization/" + getParticipantId(10).replace("#", "%23")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrganizationOfferingsForbidden() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get(new URI("/organization/" + getParticipantId(10).replace("#", "%23")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrganizationOfferingsAuthorized() throws Exception {
        System.out.println(getParticipantId(10));
        mvc.perform(MockMvcRequestBuilders
                        .get(new URI("/organization/" + getParticipantId(10).replace("#", "%23")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(20))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(20))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    private void setValidCredentialSubjectFields(MerlotServiceOfferingCredentialSubject credentialSubject) {
        credentialSubject.setId("ServiceOffering:TBR");
        credentialSubject.setContext(new HashMap<>());
        credentialSubject.setOfferedBy(new NodeKindIRITypeId(getParticipantId(10)));
        credentialSubject.setName("Test Offering");

        List<TermsAndConditions> tncList = new ArrayList<>();
        TermsAndConditions tnc = new TermsAndConditions();
        tnc.setContent("http://example.com");
        tnc.setHash("1234");
        tnc.setType("gax-trust-framework:TermsAndConditions");
        tncList.add(tnc);
        credentialSubject.setTermsAndConditions(tncList);

        List<String> policies = new ArrayList<>();
        policies.add("Policy");
        credentialSubject.setPolicy(policies);

        List<DataAccountExport> exports = new ArrayList<>();
        DataAccountExport export = new DataAccountExport();
        export.setFormatType("dummyValue");
        export.setAccessType("dummyValue");
        export.setRequestType("dummyValue");
        exports.add(export);
        credentialSubject.setDataAccountExport(exports);

        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(10)));
        credentialSubject.setCreationDate("1234");

        List<Runtime> runtimeOptions = new ArrayList<>();
        Runtime runtimeUnlimited = new Runtime();
        runtimeUnlimited.setRuntimeCount(0);
        runtimeUnlimited.setRuntimeMeasurement("unlimited");
        runtimeOptions.add(runtimeUnlimited);
        credentialSubject.setRuntimeOptions(runtimeOptions);

        credentialSubject.setMerlotTermsAndConditionsAccepted(true);
    }

    private void setValidSaasCredentialSubjectFields(SaaSCredentialSubject credentialSubject) {
        credentialSubject.setType("merlot:MerlotServiceOfferingSaaS");
        List<AllowedUserCount> userCountOptions = new ArrayList<>();
        AllowedUserCount userCountUnlimted = new AllowedUserCount();
        userCountUnlimted.setUserCountUpTo(0);
        userCountOptions.add(userCountUnlimted);
        credentialSubject.setUserCountOptions(userCountOptions);
    }

    private void setValidDataDeliveryCredentialSubjectFields(DataDeliveryCredentialSubject credentialSubject) {
        credentialSubject.setType("merlot:MerlotServiceOfferingDataDelivery");
        List<DataExchangeCount> exchangeCountOptions = new ArrayList<>();
        DataExchangeCount exchangeCount = new DataExchangeCount();
        exchangeCount.setExchangeCountUpTo(5);
        exchangeCountOptions.add(exchangeCount);
        credentialSubject.setExchangeCountOptions(exchangeCountOptions);
        credentialSubject.setDataAccessType("Download");
        credentialSubject.setDataTransferType("Pull");
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
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(10)));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addSaasOfferingAllowed() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(10)));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(10)));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addDataDeliveryOfferingAllowed() throws Exception {
        DataDeliveryCredentialSubject credentialSubject = createValidDataDeliveryCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(10)));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(10)));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingCooperation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addCooperationOfferingAllowed() throws Exception {
        CooperationCredentialSubject credentialSubject = createValidCooperationCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(10)));
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering/merlot:MerlotServiceOfferingCooperation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_" + getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }


}
