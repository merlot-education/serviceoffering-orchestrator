package eu.merloteducation.serviceofferingorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.*;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.*;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.Runtime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.CooperationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.DataDeliveryCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.SaaSCredentialSubject;
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
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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
    private ServiceOfferingExtension dateDeliveryOffering;
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

    private String createCatalogItem(String id, String issuer, String sdHash, String type) {
        String contentSaas = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\"" + getParticipantId(10) + "\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingSaaS\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\""+ getParticipantId(10) +"\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\""+ getParticipantId(10) +"\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:runtimeOption\": { \"@type\": \"merlot:Runtime\",\"merlot:runtimeCount\": {\"@value\": \"0\",\"@type\": \"xsd:number\"},\"merlot:runtimeMeasurement\": {\"@value\": \"unlimited\",\"@type\": \"xsd:string\"}},\"merlot:merlotTermsAndConditionsAccepted\":true,\"merlot:userCountOption\": { \"@type\": \"merlot:AllowedUserCount\",\"merlot:userCountUpTo\": {\"@value\": \"0\",\"@type\": \"xsd:number\"}}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
        String contentCooperation = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\""+ getParticipantId(10) +"\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingCooperation\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\""+ getParticipantId(10) +"\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\""+ getParticipantId(10) +"\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:runtimeOption\": { \"@type\": \"merlot:Runtime\",\"merlot:runtimeCount\": {\"@value\": \"0\",\"@type\": \"xsd:number\"},\"merlot:runtimeMeasurement\": {\"@value\": \"unlimited\",\"@type\": \"xsd:string\"}},\"merlot:merlotTermsAndConditionsAccepted\":true},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
        String contentDataDelivery = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\""+ getParticipantId(10) +"\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingDataDelivery\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\""+ getParticipantId(10) +"\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\""+ getParticipantId(10) +"\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:dataAccessType\":{\"@type\":\"xsd:string\",\"@value\":\"Download\"},\"merlot:dataTransferType\":{\"@type\":\"xsd:string\",\"@value\":\"Push\"},\"merlot:runtimeOption\": { \"@type\": \"merlot:Runtime\",\"merlot:runtimeCount\": {\"@value\": \"0\",\"@type\": \"xsd:number\"},\"merlot:runtimeMeasurement\": {\"@value\": \"unlimited\",\"@type\": \"xsd:string\"}},\"merlot:merlotTermsAndConditionsAccepted\":true,\"merlot:exchangeCountOption\": { \"@type\": \"merlot:DataExchangeCount\",\"merlot:exchangeCountUpTo\": {\"@value\": \"0\",\"@type\": \"xsd:number\"}}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
        String catalogItem = """
                        {
                            "meta": {
                                "expirationTime": null,
                                "content": "${content}",
                                "validators": [
                                    "did:web:compliance.lab.gaia-x.eu"
                                ],
                                "subjectId": "${id}",
                                "sdHash": "${sdHash}",
                                "id": "${id}",
                                "status": "active",
                                "issuer": "${issuer}",
                                "validatorDids": [
                                    "did:web:compliance.lab.gaia-x.eu"
                                ],
                                "uploadDatetime": "2023-05-24T13:32:22.712661Z",
                                "statusDatetime": "2023-05-24T13:32:22.712662Z"
                            },
                            "content": "${content}"
                        }
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("issuer", issuer);
        params.put("sdHash", sdHash);
        if (type.equals("saas")) {
            params.put("content", StringSubstitutor.replace(contentSaas, params, "${", "}"));
        } else if (type.equals("dataDelivery")) {
            params.put("content", StringSubstitutor.replace(contentDataDelivery, params, "${", "}"));
        } else if (type.equals("coop")) {
            params.put("content", StringSubstitutor.replace(contentCooperation, params, "${", "}"));
        }
        return StringSubstitutor.replace(catalogItem, params, "${", "}");
    }

    private String createCatalogResponse(List<String> catalogItems) {
        StringBuilder offeringQueryResponseBuilder = new StringBuilder(String.format("""
                {
                    "totalCount": %d,
                    "items": [
                    """, catalogItems.size()));
        for (int i = 0; i < catalogItems.size(); i++) {
            offeringQueryResponseBuilder.append(catalogItems.get(i));
            if (i != catalogItems.size() - 1) {
                offeringQueryResponseBuilder.append(",");
            }
        }
        offeringQueryResponseBuilder.append("""
                    ]
                }
                """);
        return offeringQueryResponseBuilder.toString();
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
        saasOffering.setId("ServiceOffering:exists");
        serviceOfferingExtensionRepository.save(saasOffering);

        dateDeliveryOffering = new ServiceOfferingExtension();
        dateDeliveryOffering.setIssuer(getParticipantId(10));
        dateDeliveryOffering.setCurrentSdHash("12345");
        dateDeliveryOffering.setId("ServiceOffering:exists2");
        dateDeliveryOffering.release();
        serviceOfferingExtensionRepository.save(dateDeliveryOffering);

        cooperationOffering = new ServiceOfferingExtension();
        cooperationOffering.setIssuer(getParticipantId(10));
        cooperationOffering.setCurrentSdHash("123456");
        cooperationOffering.setId("ServiceOffering:exists3");
        cooperationOffering.release();
        serviceOfferingExtensionRepository.save(cooperationOffering);

        List<String> catalogItems = new ArrayList<>();
        //catalogItems.add(createCatalogItem(saasOffering.getId(), saasOffering.getIssuer(), saasOffering.getCurrentSdHash(), "saas"));
        catalogItems.add(createCatalogItem(dateDeliveryOffering.getId(), dateDeliveryOffering.getIssuer(), dateDeliveryOffering.getCurrentSdHash(), "dataDelivery"));
        catalogItems.add(createCatalogItem(cooperationOffering.getId(), cooperationOffering.getIssuer(), cooperationOffering.getCurrentSdHash(), "coop"));

        String offeringQueryResponse = createCatalogResponse(catalogItems);

        List<String> catalogSingleItemSaas = new ArrayList<>();
        catalogSingleItemSaas.add(createCatalogItem(saasOffering.getId(), saasOffering.getIssuer(), saasOffering.getCurrentSdHash(), "saas"));
        String offeringQueryResponseSingleSaas = createCatalogResponse(catalogSingleItemSaas);

        List<String> catalogSingleItemDataDelivery = new ArrayList<>();
        catalogSingleItemDataDelivery.add(createCatalogItem(dateDeliveryOffering.getId(), dateDeliveryOffering.getIssuer(), dateDeliveryOffering.getCurrentSdHash(), "dataDelivery"));
        String offeringQueryResponseSingleDataDelivery = createCatalogResponse(catalogSingleItemDataDelivery);

        List<String> catalogSingleItemCooperation = new ArrayList<>();
        catalogSingleItemCooperation.add(createCatalogItem(cooperationOffering.getId(), cooperationOffering.getIssuer(), cooperationOffering.getCurrentSdHash(), "coop"));
        String offeringQueryResponseSingleCooperation = createCatalogResponse(catalogSingleItemCooperation);

        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponseObj =
                mapper.readValue(unescapeJson(offeringQueryResponse), new TypeReference<>(){});
        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponseSingleSaasObj =
                mapper.readValue(unescapeJson(offeringQueryResponseSingleSaas), new TypeReference<>(){});
        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponseSingleDataDeliveryObj =
                mapper.readValue(unescapeJson(offeringQueryResponseSingleDataDelivery), new TypeReference<>(){});

        GXFSCatalogListResponse<SelfDescriptionItem> offeringQueryResponseSingleCooperationObj =
                mapper.readValue(unescapeJson(offeringQueryResponseSingleCooperation), new TypeReference<>(){});

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(any()))
                .thenReturn(offeringQueryResponseObj);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(any(), any()))
                .thenReturn(offeringQueryResponseObj);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(any()))
                .thenReturn(offeringQueryResponseObj);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(any(), any()))
                .thenReturn(offeringQueryResponseObj);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(eq(new String[]{saasOffering.getId()}), any()))
                .thenReturn(offeringQueryResponseSingleSaasObj);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(eq(new String[]{saasOffering.getCurrentSdHash()})))
                .thenReturn(offeringQueryResponseSingleSaasObj);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(eq(new String[]{dateDeliveryOffering.getId()}), any()))
                .thenReturn(offeringQueryResponseSingleDataDeliveryObj);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(eq(new String[]{dateDeliveryOffering.getCurrentSdHash()})))
                .thenReturn(offeringQueryResponseSingleDataDeliveryObj);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(eq(new String[]{cooperationOffering.getId()}), any()))
                .thenReturn(offeringQueryResponseSingleCooperationObj);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByHashes(eq(new String[]{cooperationOffering.getCurrentSdHash()})))
                .thenReturn(offeringQueryResponseSingleCooperationObj);

        String mockOfferingCreatedResponse = """
                {"sdHash":"4321","id":"ServiceOffering:new","status":"active","issuer":"did:web:test.eu:participant:orga-10","validatorDids":["did:web:compliance.lab.gaia-x.eu"],"uploadDatetime":"2023-05-24T13:32:22.712661Z","statusDatetime":"2023-05-24T13:32:22.712662Z"}
                """;
        SelfDescriptionMeta meta = objectMapper.readValue(unescapeJson(mockOfferingCreatedResponse), new TypeReference<>(){});
        // for participant endpoint return a dummy list of one item
        lenient().when(gxfsCatalogService.addServiceOffering(any(), any(), any()))
                .thenReturn(meta);

        lenient().when(gxfsCatalogService.getParticipantLegalNameByUri(eq("MerlotOrganization"), any())).thenReturn(new GXFSCatalogListResponse<>());

        MerlotParticipantDto organizationDetails = getValidMerlotParticipantDto();
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any()))
                .thenReturn(organizationDetails);

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        MerlotParticipantDto merlotDetails = new MerlotParticipantDto();
        merlotDetails.setSelfDescription(new SelfDescription());
        merlotDetails.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        MerlotOrganizationCredentialSubject credentialSubjectDetails = new MerlotOrganizationCredentialSubject();
        merlotDetails.getSelfDescription().getVerifiableCredential().setCredentialSubject(credentialSubjectDetails);
        credentialSubjectDetails.setId(getParticipantId(1234));
        credentialSubjectDetails.setLegalName("Organization");
        TermsAndConditions merlotTnc = new TermsAndConditions();
        merlotTnc.setContent("https://merlot-education.eu");
        merlotTnc.setHash("hash12345");
        credentialSubjectDetails.setTermsAndConditions(merlotTnc);
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(eq("did:web:" + MERLOT_DOMAIN + ":participant:df15587a-0760-32b5-9c42-bb7be66e8076"), any()))
                .thenReturn(merlotDetails);

        MerlotParticipantDto organizationDetails2 = new MerlotParticipantDto();
        organizationDetails2.setSelfDescription(new SelfDescription());
        organizationDetails2.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        MerlotOrganizationCredentialSubject credentialSubject2 = new MerlotOrganizationCredentialSubject();
        organizationDetails2.getSelfDescription().getVerifiableCredential().setCredentialSubject(credentialSubject2);
        credentialSubject2.setId(getParticipantId(1234));
        credentialSubject2.setLegalName("Organization");
        TermsAndConditions emptyOrgaTnC = new TermsAndConditions();
        emptyOrgaTnC.setContent("");
        emptyOrgaTnC.setHash("");
        credentialSubject2.setTermsAndConditions(emptyOrgaTnC);
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(eq("did:web:" + MERLOT_DOMAIN + ":participant:no-tnc"), any()))
                .thenReturn(organizationDetails2);


    }

    private MerlotParticipantDto getValidMerlotParticipantDto() {

        MerlotParticipantDto organizationDetails = new MerlotParticipantDto();
        organizationDetails.setSelfDescription(new SelfDescription());
        organizationDetails.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        organizationDetails.getSelfDescription().getVerifiableCredential().setCredentialSubject(credentialSubject);
        credentialSubject.setId(getParticipantId(1234));
        credentialSubject.setLegalName("Organization");
        TermsAndConditions orgaTnC = new TermsAndConditions();
        orgaTnC.setContent("http://example.com");
        orgaTnC.setHash("hash1234");
        credentialSubject.setTermsAndConditions(orgaTnC);
        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrganisationSignerConfigDto(new OrganisationSignerConfigDto("private key", "verification method"));
        organizationDetails.setMetadata(metaDto);
        return organizationDetails;
    }

    private SaaSCredentialSubject createValidSaasCredentialSubject() {
        SaaSCredentialSubject credentialSubject = new SaaSCredentialSubject();
        credentialSubject.setId("ServiceOffering:TBR");
        credentialSubject.setContext(new HashMap<>());
        credentialSubject.setOfferedBy(new NodeKindIRITypeId(getParticipantId(10)));
        credentialSubject.setName("Test Offering");

        List<TermsAndConditions> tncList = new ArrayList<>();
        TermsAndConditions tnc = new TermsAndConditions();
        tnc.setContent("http://myexample.com");
        tnc.setHash("1234");
        tnc.setType("gax-trust-framework:TermsAndConditions");
        TermsAndConditions providerTnc = new TermsAndConditions();
        providerTnc.setContent("http://example.com");
        providerTnc.setHash("hash1234");
        providerTnc.setType("gax-trust-framework:TermsAndConditions");
        TermsAndConditions merlotTnc = new TermsAndConditions();
        merlotTnc.setContent("https://merlot-education.eu");
        merlotTnc.setHash("hash12345");
        merlotTnc.setType("gax-trust-framework:TermsAndConditions");
        tncList.add(tnc);
        tncList.add(providerTnc);
        tncList.add(merlotTnc);
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

        List<AllowedUserCount> userCountOptions = new ArrayList<>();
        AllowedUserCount userCountUnlimted = new AllowedUserCount();
        userCountUnlimted.setUserCountUpTo(0);
        userCountOptions.add(userCountUnlimted);
        credentialSubject.setUserCountOptions(userCountOptions);

        credentialSubject.setMerlotTermsAndConditionsAccepted(true);
        return credentialSubject;
    }

    @Test
    void addNewValidServiceOffering() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();

        SelfDescriptionMeta response = serviceOfferingsService.addServiceOffering(credentialSubject, getActiveRoleStringForParticipantId(10));
        assertNotNull(response.getId());
    }

    @Test
    void addNewValidServiceOfferingButNoValidSignerConfig() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId(saasOffering.getId());

        MerlotParticipantDto organizationDetails = getValidMerlotParticipantDto();
        String expectedExceptionMessage = "Service offering cannot be saved: Missing private key and/or verification method.";

        // private key and verification method are null
        organizationDetails.getMetadata().setOrganisationSignerConfigDto(new OrganisationSignerConfigDto());

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        ResponseStatusException exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());

        assertEquals(expectedExceptionMessage, exception.getReason());

        // verification method is blank
        organizationDetails.getMetadata().setOrganisationSignerConfigDto(new OrganisationSignerConfigDto("private key", ""));

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals(expectedExceptionMessage, exception.getReason());

        // private key is null
        organizationDetails.getMetadata().setOrganisationSignerConfigDto(new OrganisationSignerConfigDto(null, "verification method"));

        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
            .thenReturn(organizationDetails);

        exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals(expectedExceptionMessage, exception.getReason());
    }

    @Test
    void addNewValidServiceOfferingButNoProviderTnC() {
        String didWeb = "did:web:" + MERLOT_DOMAIN + ":participant:no-tnc";

        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(didWeb));
        credentialSubject.setOfferedBy(new NodeKindIRITypeId(didWeb));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @Transactional
    void addNewValidServiceOfferingWithoutTncInCredentialSubject() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setProvidedBy(new NodeKindIRITypeId(getParticipantId(1234)));
        credentialSubject.setOfferedBy(new NodeKindIRITypeId(getParticipantId(1234)));
        credentialSubject.setTermsAndConditions(null);

        SelfDescriptionMeta response = serviceOfferingsService.addServiceOffering(credentialSubject, "");
        assertNotNull(response.getId());
        // TODO assert that TnC are actually set (landing in mock catalog currently which discards it)
    }

    @Test
    void updateExistingWithValidServiceOffering() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId(saasOffering.getId());

        SelfDescriptionMeta response = serviceOfferingsService.addServiceOffering(credentialSubject, "");
        assertNotNull(response.getId());

    }

    @Test
    void updateExistingWithValidServiceOfferingFail() throws Exception {
        doThrow(getWebClientResponseException()).when(gxfsCatalogService)
                .deleteSelfDescriptionByHash(saasOffering.getCurrentSdHash());

        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId(saasOffering.getId());

        ResponseStatusException exception =
            assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Service offering could not be updated.", exception.getReason());

    }

    @Test
    void updateExistingWithInvalidServiceOfferingDifferentIssuer() {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId("ServiceOffering:exists");
        credentialSubject.setOfferedBy(new NodeKindIRITypeId(getParticipantId(20)));

        assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
    }

    @Test
    void updateExistingWithInvalidServiceOfferingNotInDraft() {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId("ServiceOffering:exists2");

        assertThrows(ResponseStatusException.class, () -> serviceOfferingsService.addServiceOffering(credentialSubject, ""));
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

        assertInstanceOf(SaaSCredentialSubject.class, model.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject());
        SaaSCredentialSubject credentialSubject = (SaaSCredentialSubject) model.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject();
        assertEquals("merlot:MerlotServiceOfferingSaaS",credentialSubject.getType());
        assertEquals(saasOffering.getId(), credentialSubject.getId());
        assertEquals(saasOffering.getState().name(), model.getMetadata().getState());
        assertEquals(saasOffering.getIssuer(), credentialSubject.getOfferedBy().getId());
        assertEquals(saasOffering.getCurrentSdHash(), model.getMetadata().getHash());

        assertNull(credentialSubject.getHardwareRequirements());
        assertEquals(0, credentialSubject.getUserCountOptions().get(0).getUserCountUpTo());
        assertNull(model.getMetadata().getSignedBy());
    }

    @Test
    void getServiceOfferingDetailsDataDeliveryExistent() throws Exception {
        ServiceOfferingDto model = serviceOfferingsService.getServiceOfferingById(dateDeliveryOffering.getId());
        assertNotNull(model);
        assertInstanceOf(DataDeliveryCredentialSubject.class, model.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject());
        DataDeliveryCredentialSubject credentialSubject = (DataDeliveryCredentialSubject) model.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject();
        assertEquals("merlot:MerlotServiceOfferingDataDelivery", credentialSubject.getType());

        assertEquals("Download", credentialSubject.getDataAccessType());
        assertEquals("Push", credentialSubject.getDataTransferType());
        assertEquals(0, credentialSubject.getExchangeCountOptions().get(0).getExchangeCountUpTo());
        assertNull(model.getMetadata().getSignedBy());
    }

    @Test
    void getServiceOfferingDetailsCooperationExistent() throws Exception {
        GXFSCatalogListResponse<GXFSQueryLegalNameItem> legalNameItems = new GXFSCatalogListResponse<>();
        GXFSQueryLegalNameItem legalNameItem = new GXFSQueryLegalNameItem();
        legalNameItem.setLegalName("Some Orga");
        legalNameItems.setItems(List.of(legalNameItem));
        legalNameItems.setTotalCount(1);

        lenient().when(gxfsCatalogService.getParticipantLegalNameByUri(eq("MerlotOrganization"), eq("did:web:compliance.lab.gaia-x.eu")))
            .thenReturn(legalNameItems);

        ServiceOfferingDto model = serviceOfferingsService.getServiceOfferingById(cooperationOffering.getId());
        assertNotNull(model);
        assertInstanceOf(CooperationCredentialSubject.class, model.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject());
        CooperationCredentialSubject credentialSubject = (CooperationCredentialSubject) model.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject();
        assertEquals("merlot:MerlotServiceOfferingCooperation", credentialSubject.getType());
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
