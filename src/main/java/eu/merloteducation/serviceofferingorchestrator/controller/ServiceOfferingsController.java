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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/serviceofferings")
public class ServiceOfferingsController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;


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

        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return gxfsCatalogRestService.getOrganizationServiceOfferings(orgaId);
    }

    @PostMapping("/serviceoffering")
    public SelfDescriptionsCreateResponse addServiceOffering(Principal principal, HttpServletResponse response,
                                     @Valid @RequestBody ServiceOfferingCredentialSubject credentialSubject) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(credentialSubject.getOfferedBy().getId().replace("Participant:", ""))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    @GetMapping("/serviceoffering/inDraft/{soId}")
    public void inDraftServiceOffering(Principal principal,
                                                               @PathVariable(value = "soId") String serviceofferingId,
                                                               HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.IN_DRAFT, getRepresentedOrgaIds(principal));
    }

    @GetMapping("/serviceoffering/release/{soId}")
    public void releaseServiceOffering(Principal principal,
                                                               @PathVariable(value = "soId") String serviceofferingId,
                                                               HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.RELEASED, getRepresentedOrgaIds(principal));
    }

    @GetMapping("/serviceoffering/revoke/{soId}")
    public void revokeServiceOffering(Principal principal,
                                                              @PathVariable(value = "soId") String serviceofferingId,
                                                              HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.REVOKED, getRepresentedOrgaIds(principal));
    }

    @GetMapping("/serviceoffering/delete/{soId}")
    public void deleteServiceOffering(Principal principal,
                                                              @PathVariable(value = "soId") String serviceofferingId,
                                                              HttpServletResponse response) throws Exception {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, ServiceOfferingState.DELETED, getRepresentedOrgaIds(principal));
    }

    @GetMapping("/shapes/getAvailableShapesCategorized")
    public String getAvailableShapes    (Principal principal,
                                                               @PathVariable(value = "soId") String serviceofferingId,
                                                               HttpServletResponse response) throws Exception {
        // TODO pass through wizard api
        return "";
    }

    @GetMapping("/shapes/getJSON")
    public String getShapeJson    (Principal principal,
                                        @RequestParam String name,
                                         HttpServletResponse response) throws Exception {
        // TODO pass through wizard api
        return "";
    }

}
