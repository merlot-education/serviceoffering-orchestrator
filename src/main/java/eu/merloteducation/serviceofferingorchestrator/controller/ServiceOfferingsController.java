package eu.merloteducation.serviceofferingorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingBasicModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSSignerService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSWizardRestService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@CrossOrigin
@RequestMapping("/")
public class ServiceOfferingsController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private GXFSWizardRestService gxfsWizardRestService;

    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }


    // TODO refactor to library
    private Set<String> getMerlotRoles(Principal principal) {
        // get roles from the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Set<String> getRepresentedOrgaIds(Principal principal) {
        Set<String> roles = getMerlotRoles(principal);
        // extract all orgaIds from the OrgRep and OrgLegRep Roles
        return roles
                .stream()
                .filter(s -> s.startsWith("ROLE_OrgRep_") || s.startsWith("ROLE_OrgLegRep_"))
                .map(s -> s.replace("ROLE_OrgRep_", "").replace("ROLE_OrgLegRep_", ""))
                .collect(Collectors.toSet());
    }


    @GetMapping("")
    public Page<ServiceOfferingBasicModel> getAllPublicServiceOfferings(@RequestParam("page") int page,
                                                                        @RequestParam("size") int size,
                                                                        Principal principal,
                                                                        HttpServletResponse response) throws Exception {

        Page<ServiceOfferingBasicModel> resultPage = gxfsCatalogRestService.getAllPublicServiceOfferings(PageRequest.of(page, size));
        if (page > resultPage.getTotalPages()) {
            throw new ResponseStatusException(NOT_FOUND, "Requested page exceeds available entries.");
        }

        return resultPage;
    }

    @GetMapping("/organization/{orgaId}")
    public Page<ServiceOfferingBasicModel> getOrganizationServiceOfferings(@RequestParam("page") int page,
                                                                           @RequestParam("size") int size,
                                                                           @RequestParam(name = "state", required = false) ServiceOfferingState state,
                                                                            Principal principal,
                                                                           @PathVariable(value = "orgaId") String orgaId,
                                                                           HttpServletResponse response) throws Exception {

        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        Page<ServiceOfferingBasicModel> resultPage = gxfsCatalogRestService.getOrganizationServiceOfferings(orgaId, state, PageRequest.of(page, size));
        if (page > resultPage.getTotalPages()) {
            throw new ResponseStatusException(NOT_FOUND, "Requested page exceeds available entries.");
        }

        return resultPage;
    }

    @GetMapping("/serviceoffering/{soId}")
    public ServiceOfferingDetailedModel getServiceOfferingById(Principal principal,
                                                                  @PathVariable(value = "soId") String serviceofferingId,
                                                                  HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getServiceOfferingById(serviceofferingId);
    }

    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
    public SelfDescriptionsCreateResponse addServiceOfferingSaas(Principal principal, HttpServletResponse response,
                                                             @Valid @RequestBody SaaSCredentialSubject credentialSubject) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(credentialSubject.getOfferedBy().getId().replace("Participant:", ""))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
    public SelfDescriptionsCreateResponse addServiceOfferingDataDelivery(Principal principal, HttpServletResponse response,
                                                                 @Valid @RequestBody DataDeliveryCredentialSubject credentialSubject) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(credentialSubject.getOfferedBy().getId().replace("Participant:", ""))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    @PatchMapping("/serviceoffering/status/{soId}/{status}")
    public void patchStatusServiceOffering(Principal principal,
                                                               @PathVariable(value = "soId") String serviceofferingId,
                                                               @PathVariable(value = "status") ServiceOfferingState status,
                                                               HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, status, getRepresentedOrgaIds(principal));
    }

    @GetMapping("/shapes/getAvailableShapesCategorized")
    public Map<String, List<String>> getAvailableShapes(Principal principal,
                                                            HttpServletResponse response) throws Exception {
        return gxfsWizardRestService.getServiceOfferingShapes();
    }

    @GetMapping("/shapes/getJSON")
    public String getShapeJson(Principal principal,
                                        @RequestParam String name,
                                         HttpServletResponse response) throws Exception {
        return gxfsWizardRestService.getShape(name);
    }

}
