package eu.merloteducation.serviceofferingorchestrator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingBasicModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSSignerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/serviceofferings")
public class ServiceOfferingsController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private GXFSSignerService gxfsSignerService;


    @GetMapping("")
    public List<ServiceOfferingBasicModel> getAllPublicServiceOfferings(Principal principal,
                                                                        HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getAllPublicServiceOfferings();
    }

    @GetMapping("/serviceoffering/{soId}")
    public ServiceOfferingDetailedModel getServiceOfferingById(Principal principal,
                                                                  @PathVariable(value = "soId") String serviceofferingId,
                                                                  HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getServiceOfferingById(serviceofferingId);
    }

    @PostMapping("/serviceoffering")
    public SelfDescriptionsCreateResponse addServiceOffering(Principal principal, HttpServletResponse response,
                                     @RequestBody ServiceOfferingCredentialSubject credentialSubject) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String credentialSubjectJson = mapper.writeValueAsString(credentialSubject);

        String vp = """
                {
                    "@context": ["https://www.w3.org/2018/credentials/v1"],
                    "@id": "http://example.edu/verifiablePresentation/self-description1",
                    "type": ["VerifiablePresentation"],
                    "verifiableCredential": {
                        "@context": ["https://www.w3.org/2018/credentials/v1"],
                        "@id": "https://www.example.org/ServiceOffering.json",
                        "@type": ["VerifiableCredential"],
                        "issuer": \"""" + credentialSubject.getOfferedBy().getId() + """
                        ",
                        "issuanceDate": "2022-10-19T18:48:09Z",
                        "credentialSubject":\s""" + credentialSubjectJson + """
                    }
                }
                """;

        String signedVp = gxfsSignerService.signVerifiablePresentation(vp);

        SelfDescriptionsCreateResponse catalogResponse = gxfsCatalogRestService.addServiceOffering(signedVp);

        // TODO update internal database with state machine

        return catalogResponse;
    }

}
