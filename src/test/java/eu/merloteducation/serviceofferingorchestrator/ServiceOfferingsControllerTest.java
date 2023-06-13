package eu.merloteducation.serviceofferingorchestrator;

import eu.merloteducation.serviceofferingorchestrator.config.MessageQueueConfig;
import eu.merloteducation.serviceofferingorchestrator.controller.ServiceOfferingsController;
import eu.merloteducation.serviceofferingorchestrator.service.MessageQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(ServiceOfferingsController.class)
public class ServiceOfferingsControllerTest {

    @MockBean
    MessageQueueService messageQueueService;

    @MockBean
    MessageQueueConfig messageQueueConfig;

    @BeforeEach
    public void setUp() throws Exception {
        String mockOffering = """
                {
                  "@context": {
                    "merlot": "http://w3id.org/gaia-x/merlot#",
                    "dct": "http://purl.org/dc/terms/",
                    "gax-trust-framework": "http://w3id.org/gaia-x/gax-trust-framework#",
                    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                    "sh": "http://www.w3.org/ns/shacl#",
                    "xsd": "http://www.w3.org/2001/XMLSchema#",
                    "gax-validation": "http://w3id.org/gaia-x/validation#",
                    "skos": "http://www.w3.org/2004/02/skos/core#",
                    "dcat": "http://www.w3.org/ns/dcat#",
                    "gax-core": "http://w3id.org/gaia-x/core#"
                  },
                  "@id": "ServiceOffering:TBR",
                  "@type": "merlot:MerlotServiceOfferingSaaS",
                  "gax-trust-framework:name": {
                    "@value": "Test",
                    "@type": "xsd:string"
                  },
                  "gax-core:offeredBy": {
                    "@id": "Participant:30"
                  },
                  "gax-trust-framework:providedBy": {
                    "@id": "Participant:30"
                  },
                  "merlot:creationDate": {
                    "@value": "24.05.2023, 15:30",
                    "@type": "xsd:string"
                  },
                  "merlot:dataAccessType": {
                    "@value": "Download",
                    "@type": "xsd:string"
                  },
                  "gax-trust-framework:dataAccountExport": {
                    "@type": "gax-trust-framework:DataAccountExport",
                    "gax-trust-framework:requestType": {
                      "@value": "dummyValue",
                      "@type": "xsd:string"
                    },
                    "gax-trust-framework:accessType": {
                      "@value": "dummyValue",
                      "@type": "xsd:string"
                    },
                    "gax-trust-framework:formatType": {
                      "@value": "dummyValue",
                      "@type": "xsd:string"
                    }
                  },
                  "gax-trust-framework:policy": {
                    "@value": "dummyPolicy",
                    "@type": "xsd:string"
                  },
                  "merlot:runtimeOption": {
                    "@type": "merlot:Runtime",
                    "merlot:runtimeUnlimited": true
                  },
                  "gax-trust-framework:termsAndConditions": {
                    "@type": "gax-trust-framework:TermsAndConditions",
                    "gax-trust-framework:content": {
                      "@value": "Test",
                      "@type": "xsd:anyURI"
                    },
                    "gax-trust-framework:hash": {
                      "@value": "Test",
                      "@type": "xsd:string"
                    }
                  },
                  "merlot:userCountOption": {
                    "@type": "merlot:AllowedUserCount",
                    "merlot:userCountUnlimited": true
                  },
                  "merlot:merlotTermsAndConditionsAccepted": true
                }
                """;
    }
}
