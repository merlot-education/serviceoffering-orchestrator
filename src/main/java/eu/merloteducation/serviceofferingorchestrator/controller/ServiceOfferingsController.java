package eu.merloteducation.serviceofferingorchestrator.controller;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.CooperationCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingBasicModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSWizardRestService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/")
public class ServiceOfferingsController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private GXFSWizardRestService gxfsWizardRestService;

    /**
     * Health endpoint.
     */
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

    /**
     * GET request for getting a page of all public service offerings.
     *
     * @param page page number
     * @param size number of items
     * @return page of offerings
     * @throws Exception exception during offering fetching
     */
    @GetMapping("")
    public Page<ServiceOfferingBasicModel> getAllPublicServiceOfferings(@RequestParam(value = "page", defaultValue = "0") int page,
                                                                        @RequestParam(value = "size", defaultValue = "9") int size) throws Exception {

        Page<ServiceOfferingBasicModel> resultPage = gxfsCatalogRestService
                .getAllPublicServiceOfferings(
                        PageRequest.of(page, size, Sort.by("creationDate").descending()));
        if (page > resultPage.getTotalPages()) {
            throw new ResponseStatusException(NOT_FOUND, "Requested page exceeds available entries.");
        }

        return resultPage;
    }

    /**
     * GET request for getting a page of all offerings of an organization.
     * Optionally also filter by a specific offering state.
     *
     * @param page      page number
     * @param size      number of items
     * @param state     optional offering state filter
     * @param principal user auth info
     * @param orgaId    organization to fetch the offerings for
     * @return page of organization offerings
     * @throws Exception exception during offering fetching
     */
    @GetMapping("/organization/{orgaId}")
    public Page<ServiceOfferingBasicModel> getOrganizationServiceOfferings(@RequestParam("page") int page,
                                                                           @RequestParam("size") int size,
                                                                           @RequestParam(name = "state", required = false) ServiceOfferingState state,
                                                                           @PathVariable(value = "orgaId") String orgaId,
                                                                           Principal principal) throws Exception {

        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Page<ServiceOfferingBasicModel> resultPage = gxfsCatalogRestService
                .getOrganizationServiceOfferings(
                        orgaId, state, PageRequest.of(page, size, Sort.by("creationDate").descending()));
        if (page > resultPage.getTotalPages()) {
            throw new ResponseStatusException(NOT_FOUND, "Requested page exceeds available entries.");
        }

        return resultPage;
    }

    /**
     * GET request for accessing details to a specific offering.
     *
     * @param principal         user auth info
     * @param serviceofferingId id of the offering to fetch data about
     * @return details to the offering
     * @throws Exception exception during offering fetching
     */
    @GetMapping("/serviceoffering/{soId}")
    public ServiceOfferingDetailedModel getServiceOfferingById(Principal principal,
                                                               @PathVariable(value = "soId") String serviceofferingId) throws Exception {
        return gxfsCatalogRestService.getServiceOfferingById(serviceofferingId, getRepresentedOrgaIds(principal));
    }

    /**
     * POST request for publishing a Software as a Service offering.
     *
     * @param principal         user auth info
     * @param credentialSubject SaaS self description
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
    public SelfDescriptionsCreateResponse addServiceOfferingSaas(Principal principal,
                                                                 @Valid @RequestBody SaaSCredentialSubject credentialSubject) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(credentialSubject.getOfferedBy().getId().replace("Participant:", ""))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    /**
     * POST request for publishing a Data Delivery offering.
     *
     * @param principal         user auth info
     * @param credentialSubject SaaS self description
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
    public SelfDescriptionsCreateResponse addServiceOfferingDataDelivery(Principal principal,
                                                                         @Valid @RequestBody DataDeliveryCredentialSubject credentialSubject) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(credentialSubject.getOfferedBy().getId().replace("Participant:", ""))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    /**
     * POST request for publishing a Cooperation Contract offering.
     *
     * @param principal         user auth info
     * @param credentialSubject SaaS self description
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingCooperation")
    public SelfDescriptionsCreateResponse addServiceOfferingCooperation(Principal principal,
                                                                        @Valid @RequestBody CooperationCredentialSubject credentialSubject) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(credentialSubject.getOfferedBy().getId().replace("Participant:", ""))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    /**
     * POST request for given an offering id, attempt to copy all fields to a new offering with a new id.
     *
     * @param principal         user auth info
     * @param serviceofferingId id of the offering to regenerate
     * @return creation response of catalog
     * @throws Exception communication or mapping exception
     */
    @PostMapping("/serviceoffering/regenerate/{soId}")
    public SelfDescriptionsCreateResponse regenerateServiceOfferingById(Principal principal,
                                                                        @PathVariable(value = "soId") String serviceofferingId)
            throws Exception {
        return gxfsCatalogRestService.regenerateOffering(serviceofferingId, getRepresentedOrgaIds(principal));
    }

    /**
     * PATCH request for transitioning an offering with the given id to a given offering state.
     *
     * @param principal user auth info
     * @param serviceofferingId id of the offering to transition
     * @param status target offering state
     * @throws Exception exception during transitioning
     */
    @PatchMapping("/serviceoffering/status/{soId}/{status}")
    public void patchStatusServiceOffering(Principal principal,
                                           @PathVariable(value = "soId") String serviceofferingId,
                                           @PathVariable(value = "status") ServiceOfferingState status) {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, status, getRepresentedOrgaIds(principal));
    }

    /**
     * GET request for retrieving all available MERLOT shapes for the catalog.
     *
     * @return Map of shape types to shape files
     * @throws Exception exception during shape fetching
     */
    @GetMapping("/shapes/getAvailableShapesCategorized")
    public Map<String, List<String>> getAvailableShapes() throws Exception {
        return gxfsWizardRestService.getServiceOfferingShapes();
    }

    /**
     * GET request for retrieving a specific MERLOT shapes for the catalog.
     *
     * @return catalog shape
     * @throws Exception exception during shape fetching
     */
    @GetMapping("/shapes/getJSON")
    public String getShapeJson(Principal principal,
                               @RequestParam String name,
                               HttpServletResponse response) throws Exception {
        return gxfsWizardRestService.getShape(name);
    }

}
