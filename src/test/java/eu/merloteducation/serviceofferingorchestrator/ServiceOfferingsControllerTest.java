/*
 *  Copyright 2023-2024 Dataport AöR
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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.authorizationlibrary.authorization.*;
import eu.merloteducation.authorizationlibrary.config.InterceptorConfig;
import eu.merloteducation.authorizationlibrary.config.MerlotSecurityConfig;
import eu.merloteducation.gxfscataloglibrary.models.credentials.CastableCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxDataAccountExport;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxSOTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.DataExchangeCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.OfferingRuntime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
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

import static eu.merloteducation.serviceofferingorchestrator.SelfDescriptionDemoData.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ServiceOfferingsController.class, WebSecurityConfig.class,
        OfferingAuthorityChecker.class})
@Import({ AuthorityChecker.class, ActiveRoleHeaderHandlerInterceptor.class, JwtAuthConverter.class, InterceptorConfig.class,
        MerlotSecurityConfig.class})
@AutoConfigureMockMvc()
class ServiceOfferingsControllerTest {

    @MockBean
    private ServiceOfferingsService serviceOfferingsService;

    @MockBean
    private UserInfoOpaqueTokenIntrospector userInfoOpaqueTokenIntrospector;

    @MockBean
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @Autowired
    private MockMvc mvc;

    private final String saasId = "urn:uuid:somesaasid";
    private final String dataDeliveryId = "urn:uuid:somedatadeliveryid";
    private final String coopId = "urn:uuid:somecoopid";

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
        String merlotDomain = "test.eu";
        return "did:web:"+ merlotDomain + ":participant:orga-" + num;
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
        serviceOfferingNotReleasedDto.setSelfDescription(createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs("urn:uuid:notreleased", "Some Offering", getParticipantId(10)),
                        getMerlotServiceOfferingCs("urn:uuid:notreleased"),
                        getMerlotSaasServiceOfferingCs("urn:uuid:notreleased")
                ), getParticipantId(10)
        ));

        ServiceOfferingDto serviceOfferingDto = new ServiceOfferingDto();
        serviceOfferingDto.setMetadata(new OfferingMetaDto());
        serviceOfferingDto.getMetadata().setState("RELEASED");
        serviceOfferingDto.setSelfDescription(createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs("urn:uuid:released", "Some Offering", getParticipantId(10)),
                        getMerlotServiceOfferingCs("urn:uuid:released"),
                        getMerlotSaasServiceOfferingCs("urn:uuid:released")
                ), getParticipantId(10)
        ));

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

        lenient().when(serviceOfferingsService.addServiceOffering(any(), any())).thenReturn(selfDescriptionsCreateResponse);

        lenient().when(serviceOfferingsService
                .updateServiceOffering(any(), eq(saasId), any())).thenReturn(new SelfDescriptionMeta());
        lenient().when(serviceOfferingsService
                .updateServiceOffering(any(), eq(dataDeliveryId), any())).thenReturn(new SelfDescriptionMeta());
        lenient().when(serviceOfferingsService
                .updateServiceOffering(any(), eq(coopId), any())).thenReturn(new SelfDescriptionMeta());

        lenient().when(serviceOfferingsService
                .regenerateOffering(any(), any())).thenReturn(selfDescriptionsCreateResponse);
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    private ExtendedVerifiablePresentation createValidSaasVp(String id, String providedBy) throws JsonProcessingException {
        return createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(id, "Some Offering", providedBy),
                        getMerlotServiceOfferingCs(id),
                        getMerlotSaasServiceOfferingCs(id)
                ), getParticipantId(10)
        );
    }

    private ExtendedVerifiablePresentation createValidDataDeliveryVp(String id, String providedBy) throws JsonProcessingException {
        return createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(id, "Some Offering", providedBy),
                        getMerlotServiceOfferingCs(id),
                        getMerlotDataDeliveryServiceOfferingCs(id, "Push")
                ), getParticipantId(10)
        );
    }

    private ExtendedVerifiablePresentation createValidCooperationVp(String id, String providedBy) throws JsonProcessingException {
        return createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(id, "Some Offering", providedBy),
                        getMerlotServiceOfferingCs(id),
                        getMerlotCoopContractServiceOfferingCs(id)
                ), getParticipantId(10)
        );
    }

    @Test
    void addSaasOfferingUnauthenticated() throws Exception {
        ExtendedVerifiablePresentation vp = createValidSaasVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addSaasOfferingForbidden() throws Exception {
        ExtendedVerifiablePresentation vp = createValidSaasVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addSaasOfferingAllowed() throws Exception {
        ExtendedVerifiablePresentation vp = createValidSaasVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void updateSaasOfferingUnauthenticated() throws Exception {
        ExtendedVerifiablePresentation vp = createValidSaasVp(saasId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + saasId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSaasOfferingForbidden() throws Exception {
        ExtendedVerifiablePresentation vp = createValidSaasVp(saasId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + saasId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSaasOfferingAllowed() throws Exception {
        ExtendedVerifiablePresentation vp = createValidSaasVp(saasId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + saasId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void addDataDeliveryOfferingUnauthenticated() throws Exception {
        ExtendedVerifiablePresentation vp = createValidDataDeliveryVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addDataDeliveryOfferingForbidden() throws Exception {
        ExtendedVerifiablePresentation vp = createValidDataDeliveryVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addDataDeliveryOfferingAllowed() throws Exception {
        ExtendedVerifiablePresentation vp = createValidDataDeliveryVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void updateDataDeliveryOfferingUnauthenticated() throws Exception {
        ExtendedVerifiablePresentation vp = createValidDataDeliveryVp(dataDeliveryId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + dataDeliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateDataDeliveryOfferingForbidden() throws Exception {
        ExtendedVerifiablePresentation vp = createValidDataDeliveryVp(dataDeliveryId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + dataDeliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void updateDataDeliveryOfferingAllowed() throws Exception {
        ExtendedVerifiablePresentation vp = createValidDataDeliveryVp(dataDeliveryId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + dataDeliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void addCooperationOfferingUnauthenticated() throws Exception {
        ExtendedVerifiablePresentation vp = createValidCooperationVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCooperationOfferingForbidden() throws Exception {
        ExtendedVerifiablePresentation vp = createValidCooperationVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addCooperationOfferingAllowed() throws Exception {
        ExtendedVerifiablePresentation vp = createValidCooperationVp("urn:uuid:TBR", getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .post("/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void updateCooperationOfferingUnauthenticated() throws Exception {
        ExtendedVerifiablePresentation vp = createValidCooperationVp(coopId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + coopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCooperationOfferingForbidden() throws Exception {
        ExtendedVerifiablePresentation vp = createValidCooperationVp(coopId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + coopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(20))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCooperationOfferingAllowed() throws Exception {
        ExtendedVerifiablePresentation vp = createValidCooperationVp(coopId, getParticipantId(10));
        ServiceOfferingDto dto = new ServiceOfferingDto();
        dto.setSelfDescription(vp);
        mvc.perform(MockMvcRequestBuilders
                        .put("/serviceoffering/" + coopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(dto))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
