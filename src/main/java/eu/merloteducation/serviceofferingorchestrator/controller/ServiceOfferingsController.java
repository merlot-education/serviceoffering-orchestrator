package eu.merloteducation.serviceofferingorchestrator.controller;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.service.GxfsWizardApiService;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingBasicDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.service.ServiceOfferingsService;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/")
public class ServiceOfferingsController {

    @Autowired
    private ServiceOfferingsService serviceOfferingsService;

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

        return serviceOfferingsService
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
        return serviceOfferingsService
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
            return serviceOfferingsService.getServiceOfferingById(serviceofferingId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(NOT_FOUND, e.getMessage());
        }
    }

    /**
     * POST request for publishing a Service offering.
     *
     * @param serviceOfferingDto service offering dto
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PostMapping("/serviceoffering")
    //@PreAuthorize("@authorityChecker.representsOrganization(authentication, #serviceOfferingDto.selfDescription.offeredBy.id)")
    public SelfDescriptionMeta addServiceOffering(@Valid @RequestBody ServiceOfferingDto serviceOfferingDto,
                                                     @RequestHeader(name = "Authorization") String authToken) throws Exception {
        return serviceOfferingsService.addServiceOffering(serviceOfferingDto, authToken);
    }

    /**
     * PUT request for updating a Service offering.
     *
     * @param serviceOfferingDto service offering dto
     * @return creation response for this offering
     * @throws Exception exception during offering creation
     */
    @PutMapping("/serviceoffering/{soId}")
    //@PreAuthorize("@authorityChecker.representsOrganization(authentication, #serviceOfferingDto.selfDescription.offeredBy.id)") // TODO add auth again
    public SelfDescriptionMeta updateServiceOffering(@Valid @RequestBody ServiceOfferingDto serviceOfferingDto,
                                                     @PathVariable(value = "soId") String serviceofferingId,
                                                      @RequestHeader(name = "Authorization") String authToken) throws Exception {
        return serviceOfferingsService.updateServiceOffering(serviceOfferingDto, serviceofferingId, authToken);
    }

    /**
     * POST request for given an offering id, attempt to copy all fields to a new offering with a new id.
     *
     * @param serviceofferingId id of the offering to regenerate
     * @return creation response of catalog
     * @throws Exception communication or mapping exception
     */
    @PostMapping("/serviceoffering/regenerate/{soId}")
    @PreAuthorize("@offeringAuthorityChecker.isOfferingIssuer(authentication, #serviceofferingId)")
    public SelfDescriptionMeta regenerateServiceOfferingById(@PathVariable(value = "soId") String serviceofferingId, @RequestHeader(name = "Authorization") String authToken)
            throws Exception {
        return serviceOfferingsService.regenerateOffering(serviceofferingId, authToken);
    }

    /**
     * PATCH request for transitioning an offering with the given id to a given offering state.
     *
     * @param serviceofferingId id of the offering to transition
     * @param status target offering state
     */
    @PatchMapping("/serviceoffering/status/{soId}/{status}")
    @PreAuthorize("@offeringAuthorityChecker.isOfferingIssuer(authentication, #serviceofferingId)")
    public void patchStatusServiceOffering(@PathVariable(value = "soId") String serviceofferingId,
                                           @PathVariable(value = "status") ServiceOfferingState status) {
        serviceOfferingsService.transitionServiceOfferingExtension(serviceofferingId, status);
    }

}
