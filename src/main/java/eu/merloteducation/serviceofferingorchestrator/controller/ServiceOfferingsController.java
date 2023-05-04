package eu.merloteducation.serviceofferingorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
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


        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

}
