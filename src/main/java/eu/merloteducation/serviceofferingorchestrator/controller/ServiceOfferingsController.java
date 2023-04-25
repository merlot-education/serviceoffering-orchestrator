package eu.merloteducation.serviceofferingorchestrator.controller;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingModel;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
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
    public List<ServiceOfferingModel> getAllPublicServiceOfferings(Principal principal,
                                                                   HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getAllPublicServiceOfferings();
    }

}
