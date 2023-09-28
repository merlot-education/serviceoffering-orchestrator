package eu.merloteducation.serviceofferingorchestrator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingBasicDto;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.CooperationCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.serviceofferingorchestrator.service.GXFSWizardRestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/")
public class ServiceOfferingsController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private GXFSWizardRestService gxfsWizardRestService;

    /**
     * GET request for getting a page of all public service offerings.
     *
     * @param page page number
     * @param size number of items
     * @return page of offerings
     * @throws Exception exception during offering fetching
     */
    @GetMapping("")
    public Page<ServiceOfferingBasicDto> getAllPublicServiceOfferings(@RequestParam(value = "page", defaultValue = "0") int page,
                                                                      @RequestParam(value = "size", defaultValue = "9") @Max(15) int size) throws Exception {

        return gxfsCatalogRestService
                .getAllPublicServiceOfferings(
                        PageRequest.of(page, size, Sort.by("creationDate").descending()));
    }

    /**
     * GET request for getting a page of all offerings of an organization.
     * Optionally also filter by a specific offering state.
     *
     * @param page      page number
     * @param size      number of items
     * @param state     optional offering state filter
     * @param orgaId    organization to fetch the offerings for
     * @return page of organization offerings
     * @throws Exception exception during offering fetching
     */
    @GetMapping("/organization/{orgaId}")
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #orgaId)")
    public Page<ServiceOfferingBasicDto> getOrganizationServiceOfferings(@RequestParam(value = "page", defaultValue = "0") int page,
                                                                           @RequestParam(value = "size", defaultValue = "9") @Max(15) int size,
                                                                           @RequestParam(name = "state", required = false) ServiceOfferingState state,
                                                                           @PathVariable(value = "orgaId") String orgaId) throws Exception {
        return gxfsCatalogRestService
                .getOrganizationServiceOfferings(
                        orgaId, state, PageRequest.of(page, size, Sort.by("creationDate").descending()));
    }

    /**
     * GET request for accessing details to a specific offering.
     *
     * @param serviceofferingId id of the offering to fetch data about
     * @return details to the offering
     * @throws Exception exception during offering fetching
     */
    @GetMapping("/serviceoffering/{soId}")
    @PreAuthorize("@offeringAuthorityChecker.canAccessOffering(authentication, #serviceofferingId)")
    public ServiceOfferingDto getServiceOfferingById(@PathVariable(value = "soId") String serviceofferingId) throws Exception {
        try {
            return gxfsCatalogRestService.getServiceOfferingById(serviceofferingId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(NOT_FOUND, e.getMessage());
        }
    }

    /**
     * POST request for publishing a Software as a Service offering.
     *
     * @param credentialSubject SaaS self description
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingSaaS")
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #credentialSubject.offeredBy.id)")
    public SelfDescriptionsCreateResponse addServiceOfferingSaas(@Valid @RequestBody SaaSCredentialSubject credentialSubject) throws Exception {
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    /**
     * POST request for publishing a Data Delivery offering.
     *
     * @param credentialSubject SaaS self description
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingDataDelivery")
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #credentialSubject.offeredBy.id)")
    public SelfDescriptionsCreateResponse addServiceOfferingDataDelivery(@Valid @RequestBody DataDeliveryCredentialSubject credentialSubject) throws Exception {
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    /**
     * POST request for publishing a Cooperation Contract offering.
     *
     * @param credentialSubject SaaS self description
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PostMapping("/serviceoffering/merlot:MerlotServiceOfferingCooperation")
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #credentialSubject.offeredBy.id)")
    public SelfDescriptionsCreateResponse addServiceOfferingCooperation(@Valid @RequestBody CooperationCredentialSubject credentialSubject) throws Exception {
        return gxfsCatalogRestService.addServiceOffering(credentialSubject);
    }

    /**
     * POST request for given an offering id, attempt to copy all fields to a new offering with a new id.
     *
     * @param serviceofferingId id of the offering to regenerate
     * @return creation response of catalog
     * @throws Exception communication or mapping exception
     */
    @PostMapping("/serviceoffering/regenerate/{soId}")
    @PreAuthorize("@offeringAuthorityChecker.isOfferingIssuedBy(authentication, #serviceofferingId)")
    public SelfDescriptionsCreateResponse regenerateServiceOfferingById(@PathVariable(value = "soId") String serviceofferingId)
            throws Exception {
        return gxfsCatalogRestService.regenerateOffering(serviceofferingId);
    }

    /**
     * PATCH request for transitioning an offering with the given id to a given offering state.
     *
     * @param serviceofferingId id of the offering to transition
     * @param status target offering state
     * @throws Exception exception during transitioning
     */
    @PatchMapping("/serviceoffering/status/{soId}/{status}")
    @PreAuthorize("@offeringAuthorityChecker.isOfferingIssuedBy(authentication, #serviceofferingId)")
    public void patchStatusServiceOffering(@PathVariable(value = "soId") String serviceofferingId,
                                           @PathVariable(value = "status") ServiceOfferingState status) {
        gxfsCatalogRestService.transitionServiceOfferingExtension(serviceofferingId, status);
    }

    /**
     * GET request for retrieving all available MERLOT shapes for the catalog.
     *
     * @return Map of shape types to shape files
     * @throws Exception exception during shape fetching
     */
    @GetMapping("/shapes/getAvailableShapesCategorized")
    public Map<String, List<String>> getAvailableShapes() throws JsonProcessingException {
        return gxfsWizardRestService.getServiceOfferingShapes();
    }

    /**
     * GET request for retrieving a specific MERLOT shapes for the catalog.
     *
     * @return catalog shape
     */
    @GetMapping("/shapes/getJSON")
    public String getShapeJson(@RequestParam String name) {
        return gxfsWizardRestService.getShape(name);
    }

}
