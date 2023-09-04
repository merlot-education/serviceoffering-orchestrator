package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.mappers.ServiceOfferingMapper;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingBasicDto;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.*;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.CooperationCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator.OrganizationCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator.OrganizationDetails;
import eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator.OrganizationSelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator.OrganizationVerifiableCredential;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.service.*;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
class GXFSCatalogRestServiceTest {

    @Mock
    private OrganizationOrchestratorClient organizationOrchestratorClient;

    @MockBean
    MessageQueueService messageQueueService;

    @Mock
    MessageQueueConfig messageQueueConfig;

    @MockBean
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    ServiceOfferingMapper serviceOfferingMapper;

    @Value("${gxfscatalog.selfdescriptions-uri}")
    private String gxfscatalogSelfdescriptionsUri;
    @InjectMocks
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    private ServiceOfferingExtension saasOffering;
    private ServiceOfferingExtension dateDeliveryOffering;
    private ServiceOfferingExtension cooperationOffering;

    private String createCatalogItem(String id, String issuer, String sdHash, String type) {
        String contentSaas = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\"Participant:10\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingSaaS\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\"Participant:10\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\"Participant:10\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:runtimeOption\": { \"@type\": \"merlot:Runtime\",\"merlot:runtimeCount\": {\"@value\": \"0\",\"@type\": \"xsd:number\"},\"merlot:runtimeMeasurement\": {\"@value\": \"unlimited\",\"@type\": \"xsd:string\"}},\"merlot:merlotTermsAndConditionsAccepted\":true,\"merlot:userCountOption\": { \"@type\": \"merlot:AllowedUserCount\",\"merlot:userCountUpTo\": {\"@value\": \"0\",\"@type\": \"xsd:number\"}}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
        String contentCooperation = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\"Participant:10\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingCooperation\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\"Participant:10\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\"Participant:10\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:runtimeOption\": { \"@type\": \"merlot:Runtime\",\"merlot:runtimeCount\": {\"@value\": \"0\",\"@type\": \"xsd:number\"},\"merlot:runtimeMeasurement\": {\"@value\": \"unlimited\",\"@type\": \"xsd:string\"}},\"merlot:merlotTermsAndConditionsAccepted\":true},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
        String contentDataDelivery = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\"Participant:10\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingDataDelivery\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\"Participant:10\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\"Participant:10\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:dataAccessType\":{\"@type\":\"xsd:string\",\"@value\":\"Download\"},\"merlot:dataTransferType\":{\"@type\":\"xsd:string\",\"@value\":\"Push\"},\"merlot:runtimeOption\": { \"@type\": \"merlot:Runtime\",\"merlot:runtimeCount\": {\"@value\": \"0\",\"@type\": \"xsd:number\"},\"merlot:runtimeMeasurement\": {\"@value\": \"unlimited\",\"@type\": \"xsd:string\"}},\"merlot:merlotTermsAndConditionsAccepted\":true,\"merlot:exchangeCountOption\": { \"@type\": \"merlot:DataExchangeCount\",\"merlot:exchangeCountUpTo\": {\"@value\": \"0\",\"@type\": \"xsd:number\"}}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
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
    public void setUp() {
        ReflectionTestUtils.setField(gxfsCatalogRestService, "serviceOfferingMapper", serviceOfferingMapper);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "gxfscatalogSelfdescriptionsUri", gxfscatalogSelfdescriptionsUri);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "gxfsSignerService", new GXFSSignerService());
        ReflectionTestUtils.setField(gxfsCatalogRestService, "serviceOfferingExtensionRepository", serviceOfferingExtensionRepository);

        saasOffering = new ServiceOfferingExtension();
        saasOffering.setIssuer("Participant:10");
        saasOffering.setCurrentSdHash("1234");
        saasOffering.setId("ServiceOffering:exists");
        serviceOfferingExtensionRepository.save(saasOffering);

        dateDeliveryOffering = new ServiceOfferingExtension();
        dateDeliveryOffering.setIssuer("Participant:10");
        dateDeliveryOffering.setCurrentSdHash("12345");
        dateDeliveryOffering.setId("ServiceOffering:exists2");
        dateDeliveryOffering.release();
        serviceOfferingExtensionRepository.save(dateDeliveryOffering);

        cooperationOffering = new ServiceOfferingExtension();
        cooperationOffering.setIssuer("Participant:10");
        cooperationOffering.setCurrentSdHash("123456");
        cooperationOffering.setId("ServiceOffering:exists3");
        cooperationOffering.release();
        serviceOfferingExtensionRepository.save(cooperationOffering);

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        any(), any(), any()))
                .thenThrow(HttpClientErrorException.NotFound.class);

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

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.DELETE),
                        eq(gxfscatalogSelfdescriptionsUri + "/" + saasOffering.getCurrentSdHash()), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponseSingleSaas));

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        startsWith(gxfscatalogSelfdescriptionsUri + "?"), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponse));

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&ids=" + saasOffering.getId()), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponseSingleSaas));

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&hashes=" + saasOffering.getCurrentSdHash()), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponseSingleSaas));

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&ids=" + dateDeliveryOffering.getId()), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponseSingleDataDelivery));

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&hashes=" + dateDeliveryOffering.getCurrentSdHash()), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponseSingleDataDelivery));

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&ids=" + cooperationOffering.getId()), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponseSingleCooperation));

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&hashes=" + cooperationOffering.getCurrentSdHash()), any(), any()))
                .thenReturn(unescapeJson(offeringQueryResponseSingleCooperation));

        String mockOfferingCreatedResponse = """
                {"sdHash":"4321","id":"ServiceOffering:new","status":"active","issuer":"Participant:10","validatorDids":["did:web:compliance.lab.gaia-x.eu"],"uploadDatetime":"2023-05-24T13:32:22.712661Z","statusDatetime":"2023-05-24T13:32:22.712662Z"}
                """;
        // for participant endpoint return a dummy list of one item
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.POST),
                        startsWith(gxfscatalogSelfdescriptionsUri), any(), any()))
                .thenReturn(unescapeJson(mockOfferingCreatedResponse));

        OrganizationDetails organizationDetails = new OrganizationDetails();
        organizationDetails.setSelfDescription(new OrganizationSelfDescription());
        organizationDetails.getSelfDescription().setVerifiableCredential(new OrganizationVerifiableCredential());
        organizationDetails.getSelfDescription().getVerifiableCredential().setCredentialSubject(new OrganizationCredentialSubject());
        organizationDetails.getSelfDescription().getVerifiableCredential().getCredentialSubject().setId("Participant:1234");
        organizationDetails.getSelfDescription().getVerifiableCredential().getCredentialSubject().setLegalName(new StringTypeValue("Organization"));
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any()))
                .thenReturn(organizationDetails);

    }

    private SaaSCredentialSubject createValidSaasCredentialSubject() {
        SaaSCredentialSubject credentialSubject = new SaaSCredentialSubject();
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

        List<Runtime> runtimeOptions = new ArrayList<>();
        Runtime runtimeUnlimited = new Runtime();
        runtimeUnlimited.setRuntimeCount(new NumberTypeValue(0));
        runtimeUnlimited.setRuntimeMeasurement(new StringTypeValue("unlimited"));
        runtimeOptions.add(runtimeUnlimited);
        credentialSubject.setRuntimeOptions(runtimeOptions);

        List<AllowedUserCount> userCountOptions = new ArrayList<>();
        AllowedUserCount userCountUnlimted = new AllowedUserCount();
        userCountUnlimted.setUserCountUpTo(new NumberTypeValue(0));
        userCountOptions.add(userCountUnlimted);
        credentialSubject.setUserCountOptions(userCountOptions);

        credentialSubject.setMerlotTermsAndConditionsAccepted(true);
        return credentialSubject;
    }

    @Test
    void addNewValidServiceOffering() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();

        SelfDescriptionsCreateResponse response = gxfsCatalogRestService.addServiceOffering(credentialSubject);
        assertNotNull(response.getId());
    }

    @Test
    void updateExistingWithValidServiceOffering() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId(saasOffering.getId());

        SelfDescriptionsCreateResponse response = gxfsCatalogRestService.addServiceOffering(credentialSubject);
        assertNotNull(response.getId());
    }

    @Test
    void updateExistingWithInvalidServiceOfferingDifferentIssuer() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId("ServiceOffering:exists");
        credentialSubject.setOfferedBy(new NodeKindIRITypeId("Participant:20"));

        assertThrows(ResponseStatusException.class, () -> gxfsCatalogRestService.addServiceOffering(credentialSubject));
    }

    @Test
    void updateExistingWithInvalidServiceOfferingNotInDraft() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId("ServiceOffering:exists2");

        assertThrows(ResponseStatusException.class, () -> gxfsCatalogRestService.addServiceOffering(credentialSubject));
    }

    @Test
    @Transactional
    void regenerateExistingServiceOfferingValid() throws Exception {
        String offeringId = saasOffering.getId();
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasOffering.getIssuer().replace("Participant:", ""));
        gxfsCatalogRestService.transitionServiceOfferingExtension(offeringId,
                ServiceOfferingState.RELEASED, representedOrgaIds);

        SelfDescriptionsCreateResponse response = gxfsCatalogRestService.regenerateOffering(offeringId, representedOrgaIds);
        assertNotNull(response.getId());
    }

    @Test
    @Transactional
    void regenerateExistingServiceOfferingWrongState() throws Exception {
        String offeringId = saasOffering.getId();
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasOffering.getIssuer().replace("Participant:", ""));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.regenerateOffering(offeringId, representedOrgaIds));
        assertEquals(HttpStatus.PRECONDITION_FAILED, exception.getStatusCode());
    }

    @Test
    @Transactional
    void regenerateExistingServiceOfferingWrongIssuer() throws Exception {
        String offeringId = saasOffering.getId();
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasOffering.getIssuer().replace("Participant:", ""));
        gxfsCatalogRestService.transitionServiceOfferingExtension(offeringId,
                ServiceOfferingState.RELEASED, representedOrgaIds);

        Set<String> invalidRepresentedOrgaIds = new HashSet<>();
        invalidRepresentedOrgaIds.add("garbage");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.regenerateOffering(offeringId, invalidRepresentedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @Transactional
    void regenerateExistingServiceOfferingNonExistent() throws Exception {
        Set<String> representedOrgaIds = new HashSet<>();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.regenerateOffering("garbage", representedOrgaIds));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getAllPublicOfferings() throws Exception {
        Page<ServiceOfferingBasicDto> offerings = gxfsCatalogRestService
                .getAllPublicServiceOfferings(
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    void getOrganizationOfferingsNoState() throws Exception {
        Page<ServiceOfferingBasicDto> offerings = gxfsCatalogRestService
                .getOrganizationServiceOfferings("10", null,
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    void getOrganizationOfferingsByState() throws Exception {
        Page<ServiceOfferingBasicDto> offerings = gxfsCatalogRestService
                .getOrganizationServiceOfferings("10", ServiceOfferingState.IN_DRAFT,
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    @Transactional
    void transitionServiceOfferingValid() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasOffering.getIssuer().replace("Participant:", ""));
        gxfsCatalogRestService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.RELEASED, representedOrgaIds);
        ServiceOfferingExtension result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.RELEASED, result.getState());

        gxfsCatalogRestService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.REVOKED, representedOrgaIds);
        result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.REVOKED, result.getState());

        gxfsCatalogRestService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.DELETED, representedOrgaIds);
        result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.DELETED, result.getState());

        gxfsCatalogRestService.transitionServiceOfferingExtension(saasOffering.getId(),
                ServiceOfferingState.PURGED, representedOrgaIds);
        result = serviceOfferingExtensionRepository.findById(saasOffering.getId()).orElse(null);
        assertNull(result);
    }

    @Test
    @Transactional
    void transitionServiceOfferingNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasOffering.getIssuer().replace("Participant:", ""));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.transitionServiceOfferingExtension("garbage",
                        ServiceOfferingState.RELEASED, representedOrgaIds));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @Transactional
    void transitionServiceOfferingNotAuthorized() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("Participant:99");
        String offeringId = saasOffering.getId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.transitionServiceOfferingExtension(offeringId,
                        ServiceOfferingState.RELEASED, representedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @Transactional
    void transitionServiceOfferingInvalid() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasOffering.getIssuer().replace("Participant:", ""));
        String offeringId = saasOffering.getId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.transitionServiceOfferingExtension(offeringId,
                        ServiceOfferingState.REVOKED, representedOrgaIds));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }

    @Test
    void getServiceOfferingDetailsSaasExistent() throws Exception {
        ServiceOfferingDto model = gxfsCatalogRestService.getServiceOfferingById(saasOffering.getId());
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
        assertEquals(0, credentialSubject.getUserCountOptions().get(0).getUserCountUpTo().getValue());
    }

    @Test
    void getServiceOfferingDetailsDataDeliveryExistent() throws Exception {
        ServiceOfferingDto model = gxfsCatalogRestService.getServiceOfferingById(dateDeliveryOffering.getId());
        assertNotNull(model);
        assertInstanceOf(DataDeliveryCredentialSubject.class, model.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject());
        DataDeliveryCredentialSubject credentialSubject = (DataDeliveryCredentialSubject) model.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject();
        assertEquals("merlot:MerlotServiceOfferingDataDelivery", credentialSubject.getType());

        assertEquals("Download", credentialSubject.getDataAccessType().getValue());
        assertEquals("Push", credentialSubject.getDataTransferType().getValue());
        assertEquals(0, credentialSubject.getExchangeCountOptions().get(0).getExchangeCountUpTo().getValue());
    }

    @Test
    void getServiceOfferingDetailsCooperationExistent() throws Exception {
        ServiceOfferingDto model = gxfsCatalogRestService.getServiceOfferingById(cooperationOffering.getId());
        assertNotNull(model);
        assertInstanceOf(CooperationCredentialSubject.class, model.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject());
        CooperationCredentialSubject credentialSubject = (CooperationCredentialSubject) model.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject();
        assertEquals("merlot:MerlotServiceOfferingCooperation", credentialSubject.getType());
    }

    @Test
    void getServiceOfferingDetailsNonExistent() {
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> gxfsCatalogRestService.getServiceOfferingById("garbage"));
    }

}
