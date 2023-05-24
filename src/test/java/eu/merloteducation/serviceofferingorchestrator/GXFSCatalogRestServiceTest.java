package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.DataAccountExport;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.NodeKindIRITypeId;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.TermsAndConditions;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSSignerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
public class GXFSCatalogRestServiceTest {

    @Mock
    private RestTemplate restTemplate;

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

    @Mock
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

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

        lenient().when(serviceOfferingExtensionRepository.existsById("ServiceOffering:exists")).thenReturn(true);

        ServiceOfferingExtension extension = new ServiceOfferingExtension();
        extension.setIssuer("Participant:10");
        extension.setCurrentSdHash("1234");
        extension.setId("ServiceOffering:exists");
        lenient().when(serviceOfferingExtensionRepository.findById("ServiceOffering:exists")).thenReturn(Optional.of(extension));

        ServiceOfferingExtension extension2 = new ServiceOfferingExtension();
        extension2.setIssuer("Participant:10");
        extension2.setCurrentSdHash("12345");
        extension2.setId("ServiceOffering:exists2");
        extension2.release();
        lenient().when(serviceOfferingExtensionRepository.findById("ServiceOffering:exists2")).thenReturn(Optional.of(extension2));



        lenient().when(restTemplate.postForObject(eq(keycloakTokenUri), any(), eq(String.class)))
                .thenReturn("{\"access_token\": \"1234\", \"refresh_token\": \"5678\"}");

        lenient().when(restTemplate.postForObject(eq(keycloakLogoutUri), any(), eq(String.class)))
                .thenReturn("");

        lenient().when(restTemplate.exchange(any(URI.class),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        String offeringQueryResponse = """
                {
                    "totalCount": 1,
                    "items": [
                        {
                            "meta": {
                                "expirationTime": null,
                                "content": "{\\"@context\\":[\\"https://www.w3.org/2018/credentials/v1\\"],\\"@id\\":\\"http://example.edu/verifiablePresentation/self-description1\\",\\"type\\":[\\"VerifiablePresentation\\"],\\"verifiableCredential\\":{\\"@context\\":[\\"https://www.w3.org/2018/credentials/v1\\"],\\"@id\\":\\"https://www.example.org/ServiceOffering.json\\",\\"@type\\":[\\"VerifiableCredential\\"],\\"issuer\\":\\"Participant:10\\",\\"issuanceDate\\":\\"2022-10-19T18:48:09Z\\",\\"credentialSubject\\":{\\"@id\\":\\"ServiceOffering:exists\\",\\"@type\\":\\"merlot:MerlotServiceOfferingSaaS\\",\\"@context\\":{\\"merlot\\":\\"http://w3id.org/gaia-x/merlot#\\",\\"dct\\":\\"http://purl.org/dc/terms/\\",\\"gax-trust-framework\\":\\"http://w3id.org/gaia-x/gax-trust-framework#\\",\\"rdf\\":\\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\",\\"sh\\":\\"http://www.w3.org/ns/shacl#\\",\\"xsd\\":\\"http://www.w3.org/2001/XMLSchema#\\",\\"gax-validation\\":\\"http://w3id.org/gaia-x/validation#\\",\\"skos\\":\\"http://www.w3.org/2004/02/skos/core#\\",\\"dcat\\":\\"http://www.w3.org/ns/dcat#\\",\\"gax-core\\":\\"http://w3id.org/gaia-x/core#\\"},\\"gax-core:offeredBy\\":{\\"@id\\":\\"Participant:10\\"},\\"gax-trust-framework:name\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"Test\\"},\\"gax-trust-framework:termsAndConditions\\":[{\\"gax-trust-framework:content\\":{\\"@type\\":\\"xsd:anyURI\\",\\"@value\\":\\"Test\\"},\\"gax-trust-framework:hash\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"Test\\"},\\"@type\\":\\"gax-trust-framework:TermsAndConditions\\"}],\\"gax-trust-framework:policy\\":[{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyPolicy\\"}],\\"gax-trust-framework:dataAccountExport\\":[{\\"gax-trust-framework:formatType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyValue\\"},\\"gax-trust-framework:accessType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyValue\\"},\\"gax-trust-framework:requestType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyValue\\"},\\"@type\\":\\"gax-trust-framework:DataAccountExport\\"}],\\"gax-trust-framework:providedBy\\":{\\"@id\\":\\"Participant:10\\"},\\"merlot:creationDate\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"2023-05-24T13:30:12.382871745Z\\"},\\"merlot:dataAccessType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"Download\\"},\\"merlot:runtimeOption\\":[{\\"merlot:runtimeUnlimited\\":true}],\\"merlot:merlotTermsAndConditionsAccepted\\":true,\\"merlot:userCountOption\\":[{\\"merlot:userCountUnlimited\\":true}]},\\"proof\\":{\\"type\\":\\"JsonWebSignature2020\\",\\"created\\":\\"2023-05-24T13:32:22Z\\",\\"proofPurpose\\":\\"assertionMethod\\",\\"verificationMethod\\":\\"did:web:compliance.lab.gaia-x.eu\\",\\"jws\\":\\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\\"}},\\"proof\\":{\\"type\\":\\"JsonWebSignature2020\\",\\"created\\":\\"2023-05-24T13:32:22Z\\",\\"proofPurpose\\":\\"assertionMethod\\",\\"verificationMethod\\":\\"did:web:compliance.lab.gaia-x.eu\\",\\"jws\\":\\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\\"}}",
                                "validators": [
                                    "did:web:compliance.lab.gaia-x.eu"
                                ],
                                "subjectId": "ServiceOffering:exists",
                                "sdHash": "1234",
                                "id": "ServiceOffering:exists",
                                "status": "active",
                                "issuer": "Participant:10",
                                "validatorDids": [
                                    "did:web:compliance.lab.gaia-x.eu"
                                ],
                                "uploadDatetime": "2023-05-24T13:32:22.712661Z",
                                "statusDatetime": "2023-05-24T13:32:22.712662Z"
                            },
                            "content": "{\\"@context\\":[\\"https://www.w3.org/2018/credentials/v1\\"],\\"@id\\":\\"http://example.edu/verifiablePresentation/self-description1\\",\\"type\\":[\\"VerifiablePresentation\\"],\\"verifiableCredential\\":{\\"@context\\":[\\"https://www.w3.org/2018/credentials/v1\\"],\\"@id\\":\\"https://www.example.org/ServiceOffering.json\\",\\"@type\\":[\\"VerifiableCredential\\"],\\"issuer\\":\\"Participant:10\\",\\"issuanceDate\\":\\"2022-10-19T18:48:09Z\\",\\"credentialSubject\\":{\\"@id\\":\\"ServiceOffering:exists\\",\\"@type\\":\\"merlot:MerlotServiceOfferingSaaS\\",\\"@context\\":{\\"merlot\\":\\"http://w3id.org/gaia-x/merlot#\\",\\"dct\\":\\"http://purl.org/dc/terms/\\",\\"gax-trust-framework\\":\\"http://w3id.org/gaia-x/gax-trust-framework#\\",\\"rdf\\":\\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\",\\"sh\\":\\"http://www.w3.org/ns/shacl#\\",\\"xsd\\":\\"http://www.w3.org/2001/XMLSchema#\\",\\"gax-validation\\":\\"http://w3id.org/gaia-x/validation#\\",\\"skos\\":\\"http://www.w3.org/2004/02/skos/core#\\",\\"dcat\\":\\"http://www.w3.org/ns/dcat#\\",\\"gax-core\\":\\"http://w3id.org/gaia-x/core#\\"},\\"gax-core:offeredBy\\":{\\"@id\\":\\"Participant:10\\"},\\"gax-trust-framework:name\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"Test\\"},\\"gax-trust-framework:termsAndConditions\\":[{\\"gax-trust-framework:content\\":{\\"@type\\":\\"xsd:anyURI\\",\\"@value\\":\\"Test\\"},\\"gax-trust-framework:hash\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"Test\\"},\\"@type\\":\\"gax-trust-framework:TermsAndConditions\\"}],\\"gax-trust-framework:policy\\":[{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyPolicy\\"}],\\"gax-trust-framework:dataAccountExport\\":[{\\"gax-trust-framework:formatType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyValue\\"},\\"gax-trust-framework:accessType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyValue\\"},\\"gax-trust-framework:requestType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"dummyValue\\"},\\"@type\\":\\"gax-trust-framework:DataAccountExport\\"}],\\"gax-trust-framework:providedBy\\":{\\"@id\\":\\"Participant:10\\"},\\"merlot:creationDate\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"2023-05-24T13:30:12.382871745Z\\"},\\"merlot:dataAccessType\\":{\\"@type\\":\\"xsd:string\\",\\"@value\\":\\"Download\\"},\\"merlot:runtimeOption\\":[{\\"merlot:runtimeUnlimited\\":true}],\\"merlot:merlotTermsAndConditionsAccepted\\":true,\\"merlot:userCountOption\\":[{\\"merlot:userCountUnlimited\\":true}]},\\"proof\\":{\\"type\\":\\"JsonWebSignature2020\\",\\"created\\":\\"2023-05-24T13:32:22Z\\",\\"proofPurpose\\":\\"assertionMethod\\",\\"verificationMethod\\":\\"did:web:compliance.lab.gaia-x.eu\\",\\"jws\\":\\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..j1bhdECZ4y2IQfyPLZYKRJzxk0K3mNwbBQ_Lxc0PA5v_2DG_nhdW9Nck2k-Q2g_WjY8ypIgpm-4ooFkIGfflWegs9gV4i8OhbBV9qKP-wplGgVUcBZ-gSW5_xjbvfrFMre1JiZNHa4cKXDFC68MAEUb7lxUufbg4yk5JO1qgwPKu49OtL9DaJjGe1IlENj2-MCR1PbmQ0Ygpu4LapojonX0NdfJfBPufr_g_iaaSAS9y35Evjek2Bie_YMqymARXkGQSlJGhFHd8HzZfletnAA8ZUYAgxxPAgJZCpWZRCqi59bmxAxkJVV0DfX0hUQZnDwDPbuxLLVKHbcJaVrhbu9M8x-KLgJPmfLOc1XoX-fa71hSpvQaXz-a3j3ycjgrQ6kiExK0IMpLOZ4J6fUEGaguufhpOtM_Q6sc28uhfQ8Obav4xktNz4vrOsWxQJkd9nEvmMZN-xLswiSQvy-kLwosjvZ9CnIElRz7-ge_pAToPa6748GmBEFUqNSskg0Saz-vR8B23yi67KdmjTXToLj-_KPiUd7IJESLvrzSFwEVwlTguaPQ0jQJ64BBx_mKG5pIAKTAfBol4aOzyFgJ8Wf0Bz3d9oANks5ESJE7jdJIu8xR3UW3eqgxsoQPw__ArxC6v1xnBWXueUewXGbHS1UfgfRobCX5e9bRc0mCrIUQ\\"}},\\"proof\\":{\\"type\\":\\"JsonWebSignature2020\\",\\"created\\":\\"2023-05-24T13:32:22Z\\",\\"proofPurpose\\":\\"assertionMethod\\",\\"verificationMethod\\":\\"did:web:compliance.lab.gaia-x.eu\\",\\"jws\\":\\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..c-Cha-QPu0cao8jeAnNdDxamY97qlpwAb0jGkEGGyTTPWLjH0j38KOWSb6dZEdsPeVjT8k5GYc_tc5HDe-c3Oe0ur79IVSAfR_RIUk1ozrMCGJTWimXs_Vo_EHmiRfdhj1S1MWOKmECTG7jDWAru4odPB-zMb1Oh0v7WI3eD1neKdNaCSsqrSmEDgv7ep63d-iLsh-7czzj8fqZZktHfVSzaD_Ml-6Um7zU-W2LC01WftqFTMIBOEkpj-ypZNMdroeBuJPp2jJMi1HW0QRgNriwsqKC9xpalkx9IcF-Xj5AItfWYJwnXv0K4mzNWCjor21h48TDwBL7N0qrRb9h3BFux9FbfNpOIXbG4oxtUtaHMEOB6_4S3usLE80PgogP_v7_ImZ4Zfe_43I9Lku3ePqUIMbl5mF7UeIt0jARSJwNdchqPoqC0nnOTt89SG9VsMqtIHZ0m-A0NR-hAOnHdkEalFeULL9xrZ6oZ5e3aKg5rDbyPBwf__f3Ip8l3--BX92C-b-MuNFEKzBEpRax4iVSdkCRx-ZLQZa9Z2LPBFOrQYo05txZBzrBWEBoRH9WYB8pxix-rrYzo2PNaoYDw9v7q4_JG0nx18XFzZBvNxqPgZyLyH76CebEI7qxfxvtta1NPWw2QFuJc3RiFQbAAvQzRbegDLYELfmVro_CQ2Jo\\"}}"
                        }
                    ]
                }
                """;

        lenient().when(restTemplate.exchange(eq(gxfscatalogSelfdescriptionsUri + "/1234"),
                        eq(HttpMethod.DELETE), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(offeringQueryResponse, HttpStatus.OK));

        lenient().when(restTemplate.exchange(eq(gxfscatalogSelfdescriptionsUri + "?withContent=true&ids=ServiceOffering:exists"),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(offeringQueryResponse, HttpStatus.OK));


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
        credentialSubject.setType("merlot:MerlotServiceOfferingSaaS");
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
        credentialSubject.setDataAccessType(new StringTypeValue("Download"));

        List<Runtime> runtimes = new ArrayList<>();
        Runtime runtime = new Runtime();
        runtime.setRuntimeUnlimited(true);
        runtimes.add(runtime);
        credentialSubject.setRuntimes(runtimes);
        credentialSubject.setMerlotTermsAndConditionsAccepted(true);
        return credentialSubject;
    }

    @Test
    public void addNewValidServiceOffering() throws Exception {

        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();

        SelfDescriptionsCreateResponse response = gxfsCatalogRestService.addServiceOffering(credentialSubject);
        assertNotNull(response.getId());


    }

    @Test
    public void addNewInvalidServiceOfferingTncNotAccepted() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setMerlotTermsAndConditionsAccepted(false);

        assertThrows(ResponseStatusException.class , () -> gxfsCatalogRestService.addServiceOffering(credentialSubject));
    }

    @Test
    public void updateExistingWithValidServiceOffering() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId("ServiceOffering:exists");

        SelfDescriptionsCreateResponse response = gxfsCatalogRestService.addServiceOffering(credentialSubject);
        assertNotNull(response.getId());
    }

    @Test
    public void updateExistingWithInvalidServiceOfferingDifferentIssuer() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId("ServiceOffering:exists");
        credentialSubject.setOfferedBy(new NodeKindIRITypeId("Participant:20"));

        assertThrows(ResponseStatusException.class , () -> gxfsCatalogRestService.addServiceOffering(credentialSubject));
    }

    @Test
    public void updateExistingWithInvalidServiceOfferingNotInDraft() throws Exception {
        SaaSCredentialSubject credentialSubject = createValidSaasCredentialSubject();
        credentialSubject.setId("ServiceOffering:exists2");

        assertThrows(ResponseStatusException.class , () -> gxfsCatalogRestService.addServiceOffering(credentialSubject));
    }
}
