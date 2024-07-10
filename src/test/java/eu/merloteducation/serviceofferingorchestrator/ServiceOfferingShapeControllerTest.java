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
import eu.merloteducation.modelslib.api.serviceoffering.OfferingMetaDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingBasicDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.auth.OfferingAuthorityChecker;
import eu.merloteducation.serviceofferingorchestrator.controller.ServiceOfferingShapeController;
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

import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ServiceOfferingShapeController.class, WebSecurityConfig.class,
        OfferingAuthorityChecker.class})
@Import({ AuthorityChecker.class, ActiveRoleHeaderHandlerInterceptor.class, JwtAuthConverter.class, InterceptorConfig.class,
        MerlotSecurityConfig.class})
@AutoConfigureMockMvc()
class ServiceOfferingShapeControllerTest {

    @MockBean
    private GxfsWizardApiService gxfsWizardApiService;

    @MockBean
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @MockBean
    private UserInfoOpaqueTokenIntrospector userInfoOpaqueTokenIntrospector;

    @MockBean
    private JwtAuthConverter jwtAuthConverter;

    @Autowired
    private MockMvc mvc;

    private String getParticipantId(int num) {
        String merlotDomain = "test.eu";
        return "did:web:"+ merlotDomain + ":participant:orga-" + num;
    }

    @BeforeEach
    public void setUp() {
        lenient().when(gxfsWizardApiService.getShapeByName(any(), any())).thenReturn("shape");
    }


    @Test
    void getGxOfferingShapeUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/gx/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMerlotOfferingShapeUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMerlotSaasOfferingShapeUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering/saas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMerlotDataDeliveryOfferingShapeUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering/datadelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMerlotCoopOfferingShapeUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering/coopcontract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGxOfferingShapeAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/gx/serviceoffering")
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
    void getMerlotOfferingShapeAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering")
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
    void getMerlotSaasOfferingShapeAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering/saas")
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
    void getMerlotDataDeliveryOfferingShapeAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering/datadelivery")
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
    void getMerlotCoopOfferingShapeAllowed() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlot/serviceoffering/coopcontract")
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
