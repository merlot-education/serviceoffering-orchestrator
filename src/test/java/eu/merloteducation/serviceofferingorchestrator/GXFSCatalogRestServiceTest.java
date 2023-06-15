package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.*;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.DataDeliveryServiceOfferingDetailedModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.SaasServiceOfferingDetailedModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingBasicModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSSignerService;
import eu.merloteducation.serviceofferingorchestrator.service.MessageQueueService;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
class GXFSCatalogRestServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @MockBean
    MessageQueueService messageQueueService;

    @Mock
    MessageQueueConfig messageQueueConfig;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;

    @Value("${keycloak.logout-uri}")
    private String keycloakLogoutUri;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.authorization-grant-type}")
    private String grantType;

    @Value("${keycloak.gxfscatalog-user}")
    private String keycloakGXFScatalogUser;

    @Value("${keycloak.gxfscatalog-pass}")
    private String keycloakGXFScatalogPass;

    @Value("${gxfscatalog.selfdescriptions-uri}")
    private String gxfscatalogSelfdescriptionsUri;
    @InjectMocks
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    private ServiceOfferingExtension extension1;
    private ServiceOfferingExtension extension2;

    private String createCatalogItem(String id, String issuer, String sdHash, String type) {
        String contentSaas = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\"Participant:10\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingSaaS\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\"Participant:10\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\"Participant:10\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:runtimeOption\":[{\"merlot:runtimeUnlimited\":true}],\"merlot:merlotTermsAndConditionsAccepted\":true,\"merlot:userCountOption\":[{\"merlot:userCountUnlimited\":true}]},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
        String contentDataDelivery = "{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"http://example.edu/verifiablePresentation/self-description1\",\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],\"@id\":\"https://www.example.org/ServiceOffering.json\",\"@type\":[\"VerifiableCredential\"],\"issuer\":\"Participant:10\",\"issuanceDate\":\"2022-10-19T18:48:09Z\",\"credentialSubject\":{\"@id\":\"${id}\",\"@type\":\"merlot:MerlotServiceOfferingDataDelivery\",\"@context\":{\"merlot\":\"http://w3id.org/gaia-x/merlot#\",\"dct\":\"http://purl.org/dc/terms/\",\"gax-trust-framework\":\"http://w3id.org/gaia-x/gax-trust-framework#\",\"rdf\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\"sh\":\"http://www.w3.org/ns/shacl#\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"gax-validation\":\"http://w3id.org/gaia-x/validation#\",\"skos\":\"http://www.w3.org/2004/02/skos/core#\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"gax-core\":\"http://w3id.org/gaia-x/core#\"},\"gax-core:offeredBy\":{\"@id\":\"Participant:10\"},\"gax-trust-framework:name\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"gax-trust-framework:termsAndConditions\":[{\"gax-trust-framework:content\":{\"@type\":\"xsd:anyURI\",\"@value\":\"Test\"},\"gax-trust-framework:hash\":{\"@type\":\"xsd:string\",\"@value\":\"Test\"},\"@type\":\"gax-trust-framework:TermsAndConditions\"}],\"gax-trust-framework:policy\":[{\"@type\":\"xsd:string\",\"@value\":\"dummyPolicy\"}],\"gax-trust-framework:dataAccountExport\":[{\"gax-trust-framework:formatType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:accessType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"gax-trust-framework:requestType\":{\"@type\":\"xsd:string\",\"@value\":\"dummyValue\"},\"@type\":\"gax-trust-framework:DataAccountExport\"}],\"gax-trust-framework:providedBy\":{\"@id\":\"Participant:10\"},\"merlot:creationDate\":{\"@type\":\"xsd:string\",\"@value\":\"2023-05-24T13:30:12.382871745Z\"},\"merlot:dataAccessType\":{\"@type\":\"xsd:string\",\"@value\":\"Download\"},\"merlot:dataTransferType\":{\"@type\":\"xsd:string\",\"@value\":\"Push\"},\"merlot:runtimeOption\":[{\"merlot:runtimeUnlimited\":true}],\"merlot:merlotTermsAndConditionsAccepted\":true,\"merlot:exchangeCountOption\":[{\"merlot:exchangeCountUnlimited\":true}]},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\"}},\"proof\":{\"type\":\"JsonWebSignature2020\",\"created\":\"2023-05-24T13:32:22Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:compliance.lab.gaia-x.eu\",\"jws\":\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\"}}";
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
        params.put("content", type.equals("saas")
                ? StringSubstitutor.replace(contentSaas, params, "${", "}")
                : StringSubstitutor.replace(contentDataDelivery, params, "${", "}"));
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

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakTokenUri", keycloakTokenUri);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakLogoutUri", keycloakLogoutUri);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "clientId", clientId);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "clientSecret", clientSecret);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "grantType", grantType);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakGXFScatalogUser", keycloakGXFScatalogUser);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakGXFScatalogPass", keycloakGXFScatalogPass);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "gxfscatalogSelfdescriptionsUri", gxfscatalogSelfdescriptionsUri);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "gxfsSignerService", new GXFSSignerService());
        ReflectionTestUtils.setField(gxfsCatalogRestService, "serviceOfferingExtensionRepository", serviceOfferingExtensionRepository);

        extension1 = new ServiceOfferingExtension();
        extension1.setIssuer("Participant:10");
        extension1.setCurrentSdHash("1234");
        extension1.setId("ServiceOffering:exists");
        serviceOfferingExtensionRepository.save(extension1);

        extension2 = new ServiceOfferingExtension();
        extension2.setIssuer("Participant:10");
        extension2.setCurrentSdHash("12345");
        extension2.setId("ServiceOffering:exists2");
        extension2.release();
        serviceOfferingExtensionRepository.save(extension2);


        lenient().when(restTemplate.postForObject(eq(keycloakTokenUri), any(), eq(String.class)))
                .thenReturn("{\"access_token\": \"1234\", \"refresh_token\": \"5678\"}");

        lenient().when(restTemplate.postForObject(eq(keycloakLogoutUri), any(), eq(String.class)))
                .thenReturn("");

        lenient().when(restTemplate.exchange(any(URI.class),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        List<String> catalogItems = new ArrayList<>();
        catalogItems.add(createCatalogItem(extension1.getId(), extension1.getIssuer(), extension1.getCurrentSdHash(), "saas"));
        catalogItems.add(createCatalogItem(extension2.getId(), extension2.getIssuer(), extension2.getCurrentSdHash(), "dataDelivery"));

        String offeringQueryResponse = createCatalogResponse(catalogItems);

        List<String> catalogSingleItemSaas = new ArrayList<>();
        catalogSingleItemSaas.add(createCatalogItem(extension1.getId(), extension1.getIssuer(), extension1.getCurrentSdHash(), "saas"));
        String offeringQueryResponseSingleSaas = createCatalogResponse(catalogSingleItemSaas);

        List<String> catalogSingleItemDataDelivery = new ArrayList<>();
        catalogSingleItemDataDelivery.add(createCatalogItem(extension2.getId(), extension2.getIssuer(), extension2.getCurrentSdHash(), "dataDelivery"));
        String offeringQueryResponseSingleDataDelivery = createCatalogResponse(catalogSingleItemDataDelivery);

        lenient().when(restTemplate.exchange(eq(gxfscatalogSelfdescriptionsUri + "/" + extension1.getCurrentSdHash()),
                        eq(HttpMethod.DELETE), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(offeringQueryResponseSingleSaas, HttpStatus.OK));


        lenient().when(restTemplate.exchange(startsWith(gxfscatalogSelfdescriptionsUri + "?"),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(offeringQueryResponse, HttpStatus.OK));

        lenient().when(restTemplate.exchange(eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&ids=" + extension1.getId()),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(offeringQueryResponseSingleSaas, HttpStatus.OK));

        lenient().when(restTemplate.exchange(eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&ids=" + extension2.getId()),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(offeringQueryResponseSingleDataDelivery, HttpStatus.OK));


        String mockOfferingCreatedResponse = """
                {"sdHash":"4321","id":"ServiceOffering:new","status":"active","issuer":"Participant:10","validatorDids":["did:web:compliance.lab.gaia-x.eu"],"uploadDatetime":"2023-05-24T13:32:22.712661Z","statusDatetime":"2023-05-24T13:32:22.712662Z"}
                """;
        // for participant endpoint return a dummy list of one item
        lenient().when(restTemplate.exchange(eq(gxfscatalogSelfdescriptionsUri),
                        eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockOfferingCreatedResponse, HttpStatus.OK));

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

        List<Runtime> runtimes = new ArrayList<>();
        Runtime runtime = new Runtime();
        runtime.setRuntimeUnlimited(true);
        runtimes.add(runtime);
        credentialSubject.setRuntimes(runtimes);

        List<AllowedUserCount> userCounts = new ArrayList<>();
        AllowedUserCount userCount = new AllowedUserCount();
        userCount.setUserCountUnlimited(true);
        userCounts.add(userCount);
        credentialSubject.setUserCountOption(userCounts);

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
    void addNewInvalidServiceOfferingTncNotAccepted() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setMerlotTermsAndConditionsAccepted(false);

        assertThrows(ResponseStatusException.class, () -> gxfsCatalogRestService.addServiceOffering(credentialSubject));
    }

    @Test
    void updateExistingWithValidServiceOffering() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId(extension1.getId());

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
    void getAllPublicOfferings() throws Exception {
        Page<ServiceOfferingBasicModel> offerings = gxfsCatalogRestService
                .getAllPublicServiceOfferings(
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    void getOrganizationOfferingsNoState() throws Exception {
        Page<ServiceOfferingBasicModel> offerings = gxfsCatalogRestService
                .getOrganizationServiceOfferings("10", null,
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    void getOrganizationOfferingsByState() throws Exception {
        Page<ServiceOfferingBasicModel> offerings = gxfsCatalogRestService
                .getOrganizationServiceOfferings("10", ServiceOfferingState.IN_DRAFT,
                        PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(offerings.getNumberOfElements() > 0 && offerings.getNumberOfElements() <= 9);
    }

    @Test
    @Transactional
    void transitionServiceOfferingValid() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(extension1.getIssuer().replace("Participant:", ""));
        gxfsCatalogRestService.transitionServiceOfferingExtension(extension1.getId(),
                ServiceOfferingState.RELEASED, representedOrgaIds);
        ServiceOfferingExtension result = serviceOfferingExtensionRepository.findById(extension1.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.RELEASED, result.getState());

        gxfsCatalogRestService.transitionServiceOfferingExtension(extension1.getId(),
                ServiceOfferingState.REVOKED, representedOrgaIds);
        result = serviceOfferingExtensionRepository.findById(extension1.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.REVOKED, result.getState());

        gxfsCatalogRestService.transitionServiceOfferingExtension(extension1.getId(),
                ServiceOfferingState.DELETED, representedOrgaIds);
        result = serviceOfferingExtensionRepository.findById(extension1.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(ServiceOfferingState.DELETED, result.getState());
    }

    @Test
    @Transactional
    void transitionServiceOfferingNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(extension1.getIssuer().replace("Participant:", ""));
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
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.transitionServiceOfferingExtension(extension1.getId(),
                        ServiceOfferingState.RELEASED, representedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @Transactional
    void transitionServiceOfferingInvalid() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(extension1.getIssuer().replace("Participant:", ""));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.transitionServiceOfferingExtension(extension1.getId(),
                        ServiceOfferingState.REVOKED, representedOrgaIds));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }

    @Test
    void getServiceOfferingDetailsSaasExistent() throws Exception {
        ServiceOfferingDetailedModel model = gxfsCatalogRestService.getServiceOfferingById(extension1.getId());
        assertNotNull(model);
        assertInstanceOf(SaasServiceOfferingDetailedModel.class, model);
        assertEquals("merlot:MerlotServiceOfferingSaaS", model.getType());
        assertEquals(extension1.getId(), model.getId());
        assertEquals(extension1.getState().name(), model.getMerlotState());
        assertEquals(extension1.getIssuer(), model.getOfferedBy());
        assertEquals(extension1.getCurrentSdHash(), model.getSdHash());

        SaasServiceOfferingDetailedModel saasModel = (SaasServiceOfferingDetailedModel) model;
        assertNull(saasModel.getHardwareRequirements());
        List<eu.merloteducation.serviceofferingorchestrator.models.orchestrator.AllowedUserCount> userCounts = new ArrayList<>();
        eu.merloteducation.serviceofferingorchestrator.models.orchestrator.AllowedUserCount allowedUserCount =
                new eu.merloteducation.serviceofferingorchestrator.models.orchestrator.AllowedUserCount();
        allowedUserCount.setUserCountUnlimited(true);
        userCounts.add(allowedUserCount);
        assertEquals(userCounts, saasModel.getUserCountOption());
    }

    @Test
    void getServiceOfferingDetailsDataDeliveryExistent() throws Exception {
        ServiceOfferingDetailedModel model = gxfsCatalogRestService.getServiceOfferingById(extension2.getId());
        assertNotNull(model);
        assertInstanceOf(DataDeliveryServiceOfferingDetailedModel.class, model);
        assertEquals("merlot:MerlotServiceOfferingDataDelivery", model.getType());

        DataDeliveryServiceOfferingDetailedModel dataDeliveryModel = (DataDeliveryServiceOfferingDetailedModel) model;
        assertEquals("Download", dataDeliveryModel.getDataAccessType());
        assertEquals("Push", dataDeliveryModel.getDataTransferType());
        List<eu.merloteducation.serviceofferingorchestrator.models.orchestrator.DataExchangeCount> exchangeCounts = new ArrayList<>();
        eu.merloteducation.serviceofferingorchestrator.models.orchestrator.DataExchangeCount exchangeCount =
                new eu.merloteducation.serviceofferingorchestrator.models.orchestrator.DataExchangeCount();
        exchangeCount.setExchangeCountUnlimited(true);
        exchangeCounts.add(exchangeCount);
        assertEquals(exchangeCounts, dataDeliveryModel.getExchangeCountOption());
    }

    @Test
    void getServiceOfferingDetailsNonExistent() throws Exception {

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gxfsCatalogRestService.getServiceOfferingById("garbage"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

}
