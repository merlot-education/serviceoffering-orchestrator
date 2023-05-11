package eu.merloteducation.serviceofferingorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingBasicModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSSignerService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

    @GetMapping("/organization/{orgaId}")
    public List<ServiceOfferingBasicModel> getOrganizationServiceOfferings(Principal principal,
                                                               @PathVariable(value = "orgaId") String orgaId,
                                                               HttpServletResponse response) throws Exception {
        // TODO make sure the user is authenticated and has this role
        return gxfsCatalogRestService.getOrganizationServiceOfferings(orgaId);
    }

    @PostMapping("/serviceoffering")
    public SelfDescriptionsCreateResponse addServiceOffering(Principal principal, HttpServletResponse response,
                                     @Valid @RequestBody ServiceOfferingCredentialSubject credentialSubject) throws Exception {

        // TODO check if the user is allowed to create an offering for the claimed organization
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    @GetMapping("/serviceoffering/inDraft/{soId}")
    public void inDraftServiceOffering(Principal principal,
                                                               @PathVariable(value = "soId") String serviceofferingId,
                                                               HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.IN_DRAFT);
    }

    @GetMapping("/serviceoffering/release/{soId}")
    public void releaseServiceOffering(Principal principal,
                                                               @PathVariable(value = "soId") String serviceofferingId,
                                                               HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.RELEASED);
    }

    @GetMapping("/serviceoffering/revoke/{soId}")
    public void revokeServiceOffering(Principal principal,
                                                              @PathVariable(value = "soId") String serviceofferingId,
                                                              HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.REVOKED);
    }

    @GetMapping("/serviceoffering/delete/{soId}")
    public void deleteServiceOffering(Principal principal,
                                                              @PathVariable(value = "soId") String serviceofferingId,
                                                              HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.DELETED);
    }

}
