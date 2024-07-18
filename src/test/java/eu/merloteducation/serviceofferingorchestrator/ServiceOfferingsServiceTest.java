/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.serviceofferingorchestrator;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.gxfscataloglibrary.models.credentials.CastableCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.*;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxDataAccountExport;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxSOTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.DataExchangeCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.OfferingRuntime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.ParticipantTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.modelslib.api.organization.OrganisationSignerConfigDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingBasicDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.mappers.ServiceOfferingMapper;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.service.*;
import info.weboftrust.ldsignatures.LdProof;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.*;

import static eu.merloteducation.serviceofferingorchestrator.SelfDescriptionDemoData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
class ServiceOfferingsServiceTest {

    @MockBean
    private OrganizationOrchestratorClient organizationOrchestratorClient;

    @MockBean
    MessageQueueService messageQueueService;

    @Mock
    MessageQueueConfig messageQueueConfig;

    @MockBean
    private GxfsCatalogService gxfsCatalogService;

    @Autowired
    ServiceOfferingMapper serviceOfferingMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @InjectMocks
    private ServiceOfferingsService serviceOfferingsService;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    private ServiceOfferingExtension saasOffering;
    private ServiceOfferingExtension dataDeliveryOffering;
    private ServiceOfferingExtension cooperationOffering;

    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;
    private final String MERLOT_DOMAIN = "test.eu";

    private String getParticipantId(int num) {
        return "did:web:"+ MERLOT_DOMAIN + ":participant:orga-" + num;
    }

    private String getActiveRoleStringForParticipantId(int num) {
        return "OrgLegRep_did:web:"+ MERLOT_DOMAIN + ":participant:orga-" + num;
    }

    private SelfDescriptionItem createCatalogItem(String id, String issuer, String sdHash, String type, String providedBy) throws JsonProcessingException {
        SelfDescriptionItem item = new SelfDescriptionItem();
        SelfDescriptionMeta meta = new SelfDescriptionMeta();
        item.setMeta(meta);
        meta.setSubjectId(id);
        GxServiceOfferingCredentialSubject gxCs = getGxServiceOfferingCs(id, "Some Offering", providedBy);
        MerlotServiceOfferingCredentialSubject merlotCs = getMerlotServiceOfferingCs(id);
        PojoCredentialSubject merlotSpecificCs = switch (type) {
            case "coop" -> getMerlotCoopContractServiceOfferingCs(id);
            case "dataDelivery" -> getMerlotDataDeliveryServiceOfferingCs(id, "Push");
            case "saas" -> getMerlotSaasServiceOfferingCs(id);
            default -> null;
        };
        ExtendedVerifiablePresentation vp = createVpFromCsList(List.of(gxCs, merlotCs, merlotSpecificCs), "did:web:someorga");
        LdProof proof = new LdProof();
        proof.setJsonObjectKeyValue("verificationMethod", issuer);
        vp.setJsonObjectKeyValue("proof", proof.getJsonObject());
        meta.setContent(vp);
        meta.setSdHash(sdHash);
        meta.setId(id);
        meta.setStatus("active");
        meta.setIssuer(issuer);
        meta.setValidatorDids(List.of("did:web:compliance.lab.gaia-x.eu"));
        meta.setUploadDatetime("2023-05-24T13:32:22.712661Z");
        meta.setStatusDatetime("2023-05-24T13:32:22.712662Z");

        return item;
    }

    private GXFSCatalogListResponse<SelfDescriptionItem> createCatalogResponse(List<SelfDescriptionItem> catalogItems) {
        GXFSCatalogListResponse<SelfDescriptionItem> response = new GXFSCatalogListResponse<>();
        response.setTotalCount(catalogItems.size());
        response.setItems(catalogItems);
        return response;
    }

    private GxLegalParticipantCredentialSubject getGxParticipantCs(String id) {
        GxLegalParticipantCredentialSubject cs = new GxLegalParticipantCredentialSubject();
        cs.setId(id);
        cs.setName("Organization");
        GxVcard address = new GxVcard();
        address.setCountryCode("DE");
        address.setCountrySubdivisionCode("DE-BE");
        address.setStreetAddress("Some Street 3");
        address.setLocality("Berlin");
        address.setPostalCode("12345");
        cs.setHeadquarterAddress(address);
        cs.setLegalAddress(address);
        cs.setLegalRegistrationNumber(List.of(new NodeKindIRITypeId(id + "-regId")));
        return cs;
    }

    private GxLegalRegistrationNumberCredentialSubject getGxRegistrationNumberCs(String id) {
        GxLegalRegistrationNumberCredentialSubject cs = new GxLegalRegistrationNumberCredentialSubject();
        cs.setId(id  + "-regId");
        cs.setLeiCode("894500MQZ65CN32S9A66");
        return cs;
    }

    private MerlotLegalParticipantCredentialSubject getMerlotParticipantCs(String id, String tncUrl, String tncHash) {
        MerlotLegalParticipantCredentialSubject cs = new MerlotLegalParticipantCredentialSubject();
        cs.setId(id);
        cs.setLegalForm("LLC");
        cs.setLegalName("Organization");
        ParticipantTermsAndConditions tnc = new ParticipantTermsAndConditions();
        tnc.setUrl(tncUrl);
        tnc.setHash(tncHash);
        cs.setTermsAndConditions(tnc);
        return cs;
    }

    private MerlotParticipantDto createMerlotParticipantDto(String id, String tncUrl, String tncHash) throws JsonProcessingException {
        MerlotParticipantDto merlotDetails = new MerlotParticipantDto();
        merlotDetails.setId(id);
        merlotDetails.setSelfDescription(createVpFromCsList(
                List.of(
                        getGxParticipantCs(id),
                        getGxRegistrationNumberCs(id),
                        getMerlotParticipantCs(id, tncUrl, tncHash)
                ),
                "did:web:someorga"
        ));
        return merlotDetails;
    }

    private String unescapeJson(String jsonString) {
        jsonString = StringEscapeUtils.unescapeJson(jsonString);
        if (jsonString != null)
            jsonString = jsonString.replace("\"{", "{").replace("}\"", "}");
        return jsonString;
    }

    @BeforeEach
    public void setUp() throws JsonProcessingException, CredentialSignatureException, CredentialPresentationException {
        ObjectMapper mapper = new ObjectMapper();
        ReflectionTestUtils.setField(serviceOfferingsService, "serviceOfferingMapper", serviceOfferingMapper);
        ReflectionTestUtils.setField(serviceOfferingsService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(serviceOfferingsService, "serviceOfferingExtensionRepository", serviceOfferingExtensionRepository);
        ReflectionTestUtils.setField(serviceOfferingsService, "gxfsCatalogService", gxfsCatalogService);
        ReflectionTestUtils.setField(serviceOfferingsService, "organizationOrchestratorClient", organizationOrchestratorClient);
        ReflectionTestUtils.setField(serviceOfferingsService, "merlotDomain", MERLOT_DOMAIN);

        saasOffering = new ServiceOfferingExtension();
        saasOffering.setIssuer(getParticipantId(10));
        saasOffering.setCurrentSdHash("1234");
        saasOffering.setId("urn:uuid:exists");
        serviceOfferingExtensionRepository.save(saasOffering);

        dataDeliveryOffering = new ServiceOfferingExtension();
        dataDeliveryOffering.setIssuer(getParticipantId(10));
        dataDeliveryOffering.setCurrentSdHash("12345");
        dataDeliveryOffering.setId("urn:uuid:exists2");
        dataDeliveryOffering.release();
        serviceOfferingExtensionRepository.save(dataDeliveryOffering);

        cooperationOffering = new ServiceOfferingExtension();
        cooperationOffering.setIssuer(getParticipantId(10));
        cooperationOffering.setCurrentSdHash("123456");
        cooperationOffering.setId("urn:uuid:exists3");
        cooperationOffering.release();
        serviceOfferingExtensionRepository.save(cooperationOffering);

        List<SelfDescriptionItem> catalogItems = new ArrayList<>();
        //catalogItems.add(createCatalogItem(saasOffering.getId(), saasOffering.getIssuer(), saasOffering.getCurrentSdHash(), "saas"));
        catalogItems.add(createCatalogItem(dataDeliveryOffering.getId(), dataDeliveryOffering.getIssuer(), dataDeliveryOffering.getCurrentSdHash(), "dataDelivery", getParticipantId(10)));
        catalogItems.add(createCatalogItem(cooperationOffering.getId(), cooperationOffering.getIssuer(), cooperationOffering.getCurrentSdHash(), "coop", getParticipantId(10)));

        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponse = createCatalogResponse(catalogItems);

        List<SelfDescriptionItem> catalogSingleItemSaas = new ArrayList<>();
        catalogSingleItemSaas.add(createCatalogItem(saasOffering.getId(), saasOffering.getIssuer(), saasOffering.getCurrentSdHash(), "saas", getParticipantId(10)));
        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponseSingleSaas = createCatalogResponse(catalogSingleItemSaas);

        List<SelfDescriptionItem> catalogSingleItemDataDelivery = new ArrayList<>();
        catalogSingleItemDataDelivery.add(createCatalogItem(dataDeliveryOffering.getId(), dataDeliveryOffering.getIssuer(), dataDeliveryOffering.getCurrentSdHash(), "dataDelivery", getParticipantId(10)));
        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponseSingleDataDelivery = createCatalogResponse(catalogSingleItemDataDelivery);

        List<SelfDescriptionItem> catalogSingleItemCooperation = new ArrayList<>();
        catalogSingleItemCooperation.add(createCatalogItem(cooperationOffering.getId(), cooperationOffering.getIssuer(), cooperationOffering.getCurrentSdHash(), "coop", getParticipantId(10)));
        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponseSingleCooperation = createCatalogResponse(catalogSingleItemCooperation);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(any()))
                .thenReturn(offeringQueryResponse);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(any(), any()))
                .thenReturn(offeringQueryResponse);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(any()))
                .thenReturn(offeringQueryResponse);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(any(), any()))
                .thenReturn(offeringQueryResponse);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(aryEq(new String[]{saasOffering.getId()})))
                .thenReturn(offeringQueryResponseSingleSaas);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(aryEq(new String[]{saasOffering.getId()}), any()))
                .thenReturn(offeringQueryResponseSingleSaas);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(aryEq(new String[]{saasOffering.getCurrentSdHash()})))
                .thenReturn(offeringQueryResponseSingleSaas);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(aryEq(new String[]{saasOffering.getCurrentSdHash()}), any()))
                .thenReturn(offeringQueryResponseSingleSaas);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(aryEq(new String[]{cooperationOffering.getId()})))
                .thenReturn(offeringQueryResponseSingleCooperation);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(aryEq(new String[]{cooperationOffering.getId()}), any()))
                .thenReturn(offeringQueryResponseSingleCooperation);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(aryEq(new String[]{cooperationOffering.getCurrentSdHash()})))
                .thenReturn(offeringQueryResponseSingleCooperation);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(aryEq(new String[]{cooperationOffering.getCurrentSdHash()}), any()))
                .thenReturn(offeringQueryResponseSingleCooperation);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(aryEq(new String[]{dataDeliveryOffering.getId()})))
                .thenReturn(offeringQueryResponseSingleDataDelivery);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(aryEq(new String[]{dataDeliveryOffering.getId()}), any()))
                .thenReturn(offeringQueryResponseSingleDataDelivery);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(aryEq(new String[]{dataDeliveryOffering.getCurrentSdHash()})))
                .thenReturn(offeringQueryResponseSingleDataDelivery);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(aryEq(new String[]{dataDeliveryOffering.getCurrentSdHash()}), any()))
                .thenReturn(offeringQueryResponseSingleDataDelivery);
        

        SelfDescriptionMeta offeringCreatedResponse = createOfferingCreatedResponse();

        lenient().when(gxfsCatalogService.addServiceOffering(any(), any(), any()))
                .thenReturn(offeringCreatedResponse);
        lenient().when(gxfsCatalogService.addServiceOffering(any(), any()))
                .thenReturn(offeringCreatedResponse);

        GXFSCatalogListResponse<GXFSQueryLegalNameItem> legalNameItems = new GXFSCatalogListResponse<>();
        GXFSQueryLegalNameItem legalNameItem = new GXFSQueryLegalNameItem();
        legalNameItem.setLegalName("Some Orga");
        legalNameItems.setItems(List.of(legalNameItem));
        legalNameItems.setTotalCount(1);

        lenient().when(gxfsCatalogService.getParticipantLegalNameByUri(eq(MerlotLegalParticipantCredentialSubject.TYPE_CLASS), any()))
                .thenReturn(legalNameItems);

        MerlotParticipantDto organizationDetails = getValidMerlotParticipantDto();
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any()))
                .thenReturn(organizationDetails);

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        MerlotParticipantDto merlotDetails = createMerlotParticipantDto(
                getParticipantId(1234),
                "https://merlot-education.eu",
                "hash12345"
        );
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(eq("did:web:" + MERLOT_DOMAIN + ":participant:df15587a-0760-32b5-9c42-bb7be66e8076"), any()))
                .thenReturn(merlotDetails);

        MerlotParticipantDto organizationDetails2 = createMerlotParticipantDto(
                getParticipantId(1234),
                "",
                ""
        );
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(eq("did:web:" + MERLOT_DOMAIN + ":participant:no-tnc"), any()))
                .thenReturn(organizationDetails2);


    }

    private SelfDescriptionMeta createOfferingCreatedResponse() {
        SelfDescriptionMeta offeringCreatedResponse = new SelfDescriptionMeta();
        offeringCreatedResponse.setSdHash("4321");
        offeringCreatedResponse.setId("urn:uuid:1234");
        offeringCreatedResponse.setStatus("active");
        offeringCreatedResponse.setIssuer("did:web:test.eu:participant:orga-10");
        offeringCreatedResponse.setValidatorDids(List.of("did:web:compliance.lab.gaia-x.eu"));
        offeringCreatedResponse.setUploadDatetime("2023-05-24T13:32:22.712661Z");
        offeringCreatedResponse.setStatusDatetime("2023-05-24T13:32:22.712662Z");
        return offeringCreatedResponse;
    }

    private MerlotParticipantDto getValidMerlotParticipantDto() throws JsonProcessingException {

        MerlotParticipantDto dto = createMerlotParticipantDto(
                getParticipantId(1234),
                "http://example.com",
                "hash1234");
        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrganisationSignerConfigDto(new OrganisationSignerConfigDto("private key", "merlot verification method", "verification method"));
        dto.setMetadata(metaDto);
        return dto;
    }

    private ServiceOfferingDto createValidSaasOffering(String id, String providedBy) throws JsonProcessingException {
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(
                createVpFromCsList(
                        List.of(
                                getGxServiceOfferingCs(id, "Some offering", providedBy),
                                getMerlotServiceOfferingCs(id),
                                getMerlotSaasServiceOfferingCs(id)
                        ),
                        "did:web:someorga"
                )
        );
        return dto;
    }

    @Test
    void addNewValidServiceOffering() throws Exception {
        ServiceOfferingDto credentialSubject = createValidSaasOffering("urn:uuid:TBR", getParticipantId(10));

        SelfDescriptionMeta response = serviceOfferingsService.addServiceOffering(credentialSubject, getActiveRoleStringForParticipantId(10));
        assertNotNull(response.getId());
    }

    @Test
    void addNewValidServiceOfferingButNoValidSignerConfig() throws Exception {
        ServiceOfferingDto credentialSubject = createValidSaasOffering(saasOffering.getId(), getParticipantId(10));

        MerlotParticipantDto organizationDetails = getValidMerlotParticipantDto();
        String expectedExceptionMessage = "Service offering cannot be saved: Missing private key and/or verification method.";

        // private key, verification method and merlot verification method are null
        organizationDetails.getMetadata().setOrganisationSignerConfigDto(new OrganisationSignerConfigDto());

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        ResponseStatusException exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());

        assertEquals(expectedExceptionMessage, exception.getReason());

        // verification method and merlot verification method are blank
        organizationDetails.getMetadata().setOrganisationSignerConfigDto(new OrganisationSignerConfigDto("private key", "", ""));

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals(expectedExceptionMessage, exception.getReason());

        // private key is null and merlot verification method is blank
        organizationDetails.getMetadata().setOrganisationSignerConfigDto(new OrganisationSignerConfigDto(null, "verification method", ""));

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals(expectedExceptionMessage, exception.getReason());

        // private key and verification method are blank
        organizationDetails.getMetadata().setOrganisationSignerConfigDto(new OrganisationSignerConfigDto("", "", "merlot verification method"));

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals(expectedExceptionMessage, exception.getReason());
    }

    @Test
    void addNewValidServiceOfferingButNoProviderTnC() throws JsonProcessingException {
        String didWeb = "did:web:" + MERLOT_DOMAIN + ":participant:no-tnc";

        ServiceOfferingDto credentialSubject = createValidSaasOffering("urn:uuid:TBR", didWeb);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void updateExistingWithValidServiceOffering() throws Exception {
        ServiceOfferingDto credentialSubject = createValidSaasOffering(saasOffering.getId(), getParticipantId(10));

        SelfDescriptionMeta response = serviceOfferingsService.updateServiceOffering(credentialSubject, saasOffering.getId(), "");
        assertNotNull(response.getId());

    }

    @Test
    void updateExistingWithValidServiceOfferingFail() throws Exception {
        doThrow(getWebClientResponseException()).when(gxfsCatalogService)
                .deleteSelfDescriptionByHash(saasOffering.getCurrentSdHash());

        String id = saasOffering.getId();

        ServiceOfferingDto credentialSubject = createValidSaasOffering(id, getParticipantId(10));

        ResponseStatusException exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.updateServiceOffering(credentialSubject, id, ""));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Service offering could not be updated.", exception.getReason());

    }

    @Test
    void updateExistingWithInvalidServiceOfferingDifferentIssuer() throws JsonProcessingException {
        ServiceOfferingDto credentialSubject = createValidSaasOffering("urn:uuid:exists", getParticipantId(20));

        assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.updateServiceOffering(credentialSubject, "urn:uuid:exists", ""));
    }

    @Test
    void updateExistingWithInvalidServiceOfferingNotInDraft() throws JsonProcessingException {
        ServiceOfferingDto credentialSubject = createValidSaasOffering("urn:uuid:exists2", getParticipantId(10));

        assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.updateServiceOffering(credentialSubject, "urn:uuid:exists2", ""));
    }

    @Test
    @Transactional
    void regenerateExistingServiceOfferingValid() throws Exception {
        String offeringId = saasOffering.getId();
        serviceOfferingsService.transitionServiceOfferingExtension(offeringId,
                ServiceOfferingState.RELEASED);

        SelfDescriptionMeta response = serviceOfferingsService.regenerateOffering(offeringId, "");
        assertNotNull(response.getId());
    }

    @Test
    @Transactional
    void regenerateExistingServiceOfferingWrongState() {
        String offeringId = saasOffering.getId();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceOfferingsService.regenerateOffering(offeringId, ""));
        assertEquals(HttpStatus.PRECONDITION_FAILED, exception.getStatusCode());
    }

    @Test
    @Transactional
    void regenerateExistingServiceOfferingNonExistent() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceOfferingsService.regenerateOffering("garbage", ""));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getAllPublicOfferings() throws Exception {
        Page<ServiceOfferingBasicDto> offerings = serviceOfferingsService
                .getAllPublicServiceOfferings(
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    void getAllPublicOfferingsFail(){
        doThrow(getWebClientResponseException()).when(gxfsCatalogService).getSelfDescriptionsByHashes(any());

        PageRequest request = PageRequest.of(0, 9, Sort.by("creationDate").descending());
        assertThrows(ResponseStatusException.class, () -> serviceOfferingsService
            .getAllPublicServiceOfferings(request));
    }

    @Test
    void getOrganizationOfferingsNoState() throws Exception {
        Page<ServiceOfferingBasicDto> offerings = serviceOfferingsService
                .getOrganizationServiceOfferings(getParticipantId(10), null,
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    void getOrganizationOfferingsByState() throws Exception {
        Page<ServiceOfferingBasicDto> offerings = serviceOfferingsService
                .getOrganizationServiceOfferings(getParticipantId(10), ServiceOfferingState.IN_DRAFT,
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    void getOrganizationOfferingsByStateFail() {
        doThrow(getWebClientResponseException()).when(gxfsCatalogService).getSelfDescriptionsByHashes(any(), any());

        PageRequest request = PageRequest.of(0, 9, Sort.by("creationDate").descending());
        String participantId = getParticipantId(10);
        assertThrows(ResponseStatusException.class, () -> serviceOfferingsService
            .getOrganizationServiceOfferings(participantId, ServiceOfferingState.IN_DRAFT, request));
    }

    @Test
    @Transactional
    void transitionServiceOfferingValid() {
        serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.RELEASED);
        ServiceOfferingExtension result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.RELEASED, result.getState());

        serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.REVOKED);
        result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.REVOKED, result.getState());

        serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.DELETED);
        result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.DELETED, result.getState());

        serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.PURGED);
        result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNull(result);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // handle transactions manually
    void transitionServiceOfferingDeletedFail() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        doThrow(getWebClientResponseException()).when(gxfsCatalogService).revokeSelfDescriptionByHash(any());

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                    ServiceOfferingState.RELEASED);
                serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                    ServiceOfferingState.REVOKED);
            }
        });

        Exception thrownEx = null;
        try {
            transactionTemplate.execute(status -> {
                serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                    ServiceOfferingState.DELETED);

                return "foo";
            });
        } catch (Exception ex) {
            thrownEx = ex;
        }

        assertNotNull(thrownEx);
        assertEquals(thrownEx.getClass(), ResponseStatusException.class);

        // extension should be in REVOKED state as transition to DELETED state was unsuccessful
        ServiceOfferingExtension result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.REVOKED, result.getState());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // handle transactions manually
    void transitionServiceOfferingPurgedFail() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        doThrow(getWebClientResponseException()).when(gxfsCatalogService)
                .deleteSelfDescriptionByHash(saasOffering.getCurrentSdHash());

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                    ServiceOfferingState.RELEASED);
                serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                    ServiceOfferingState.REVOKED);
                serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                    ServiceOfferingState.DELETED);
            }
        });

        Exception thrownEx = null;

        try {
            transactionTemplate.execute(status -> {

                serviceOfferingsService.transitionServiceOfferingExtension(saasOffering.getId(),
                    ServiceOfferingState.PURGED);

                return "foo";
            });
        } catch (Exception ex) {
            thrownEx = ex;
        }

        assertNotNull(thrownEx);
        assertEquals(thrownEx.getClass(), ResponseStatusException.class);

        // extension should still exist and be in DELETED state as purging was unsuccessful
        ServiceOfferingExtension result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.DELETED, result.getState());
    }

    @Test
    @Transactional
    void transitionServiceOfferingNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasOffering.getIssuer());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceOfferingsService.transitionServiceOfferingExtension("garbage",
                        ServiceOfferingState.RELEASED));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @Transactional
    void transitionServiceOfferingInvalid() {
        String offeringId = saasOffering.getId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceOfferingsService.transitionServiceOfferingExtension(offeringId,
                        ServiceOfferingState.REVOKED));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }

    @Test
    void getServiceOfferingDetailsSaasExistent() throws Exception {
        ServiceOfferingDto model = serviceOfferingsService.getServiceOfferingById(saasOffering.getId());
        assertNotNull(model);

        GxServiceOfferingCredentialSubject gxCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class);
        MerlotServiceOfferingCredentialSubject merlotCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotServiceOfferingCredentialSubject.class);
        MerlotSaasServiceOfferingCredentialSubject merlotSpecificCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotSaasServiceOfferingCredentialSubject.class);

        assertNotNull(gxCs);
        assertNotNull(merlotCs);
        assertNotNull(merlotSpecificCs);
        assertEquals(saasOffering.getId(), gxCs.getId());
        assertEquals(saasOffering.getState().name(), model.getMetadata().getState());
        assertEquals(saasOffering.getIssuer(), gxCs.getProvidedBy().getId());
        assertEquals(saasOffering.getCurrentSdHash(), model.getMetadata().getHash());

        assertEquals("1.21 Gigawatts", merlotSpecificCs.getHardwareRequirements());
        assertEquals(0, merlotSpecificCs.getUserCountOptions().get(0).getUserCountUpTo());
        assertNotNull(model.getMetadata().getSignedBy());
    }

    @Test
    void getServiceOfferingDetailsDataDeliveryExistent() throws Exception {
        ServiceOfferingDto model = serviceOfferingsService.getServiceOfferingById(dataDeliveryOffering.getId());
        assertNotNull(model);

        GxServiceOfferingCredentialSubject gxCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class);
        MerlotServiceOfferingCredentialSubject merlotCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotServiceOfferingCredentialSubject.class);
        MerlotDataDeliveryServiceOfferingCredentialSubject merlotSpecificCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotDataDeliveryServiceOfferingCredentialSubject.class);

        assertNotNull(gxCs);
        assertNotNull(merlotCs);
        assertNotNull(merlotSpecificCs);

        assertEquals("Download", merlotSpecificCs.getDataAccessType());
        assertEquals("Push", merlotSpecificCs.getDataTransferType());
        assertEquals(0, merlotSpecificCs.getExchangeCountOptions().get(0).getExchangeCountUpTo());
        assertNotNull(model.getMetadata().getSignedBy());
    }

    @Test
    void getServiceOfferingDetailsCooperationExistent() throws Exception {

        ServiceOfferingDto model = serviceOfferingsService.getServiceOfferingById(cooperationOffering.getId());
        assertNotNull(model);

        GxServiceOfferingCredentialSubject gxCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class);
        MerlotServiceOfferingCredentialSubject merlotCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotServiceOfferingCredentialSubject.class);
        MerlotCoopContractServiceOfferingCredentialSubject merlotSpecificCs = model.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotCoopContractServiceOfferingCredentialSubject.class);

        assertNotNull(gxCs);
        assertNotNull(merlotCs);
        assertNotNull(merlotSpecificCs);

        assertEquals("Some Orga", model.getMetadata().getSignedBy());
    }

    @Test
    void getServiceOfferingDetailsNonExistent() {
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> serviceOfferingsService.getServiceOfferingById("garbage"));
    }

    @Test
    void getServiceOfferingDetailsFail() {
        doThrow(getWebClientResponseException()).when(gxfsCatalogService).getSelfDescriptionsByIds(any(), any());

        String id = cooperationOffering.getId();
        assertThrows(ResponseStatusException.class,
            () -> serviceOfferingsService.getServiceOfferingById(id));
    }

    private WebClientResponseException getWebClientResponseException(){
        byte[] byteArray = {123, 34, 99, 111, 100, 101, 34, 58, 34, 110, 111, 116, 95, 102, 111, 117, 110, 100, 95, 101,
            114, 114, 111, 114, 34, 44, 34, 109, 101, 115, 115, 97, 103, 101, 34, 58, 34, 80, 97, 114,
            116, 105, 99, 105, 112, 97, 110, 116, 32, 110, 111, 116, 32, 102, 111, 117, 110, 100, 58,
            32, 80, 97, 114, 116, 105, 99, 105, 112, 97, 110, 116, 58, 49, 50, 51, 52, 49, 51, 52, 50,
            51, 52, 50, 49, 34, 125};
        return new WebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "garbage", null, byteArray, null);
    }

}
