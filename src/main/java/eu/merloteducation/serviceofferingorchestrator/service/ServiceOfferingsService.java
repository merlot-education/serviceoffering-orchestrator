package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.gxfscataloglibrary.models.client.SelfDescriptionStatus;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.TermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingBasicDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.mappers.ServiceOfferingMapper;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import io.netty.util.internal.StringUtil;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class ServiceOfferingsService {

    @Autowired
    private OrganizationOrchestratorClient organizationOrchestratorClient;

    @Autowired
    private ServiceOfferingMapper serviceOfferingMapper;

    @Autowired
    private GxfsCatalogService gxfsCatalogService;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${merlot-domain}")
    private String merlotDomain;
    private final Logger logger = LoggerFactory.getLogger(ServiceOfferingsService.class);
    private static final String OFFERING_START = "ServiceOffering:";
    private static final String OFFERING_NOT_FOUND = "No valid service offering with this id was found.";

    @Transactional(rollbackOn = {ResponseStatusException.class})
    private void deleteOffering(ServiceOfferingExtension extension) throws JsonProcessingException {
        extension.delete();
        serviceOfferingExtensionRepository.save(extension);

        try {
            gxfsCatalogService.revokeSelfDescriptionByHash(extension.getCurrentSdHash());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
    }

    @Transactional(rollbackOn = {ResponseStatusException.class})
    private void purgeOffering(ServiceOfferingExtension extension) throws JsonProcessingException {
        if (extension.getState() != ServiceOfferingState.DELETED) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid state transition requested.");
        }
        serviceOfferingExtensionRepository.delete(extension);
        deleteServiceOfferingFromCatalog(extension.getCurrentSdHash());
    }

    private GXFSCatalogListResponse<SelfDescriptionItem> getSelfDescriptionByOfferingExtension
            (ServiceOfferingExtension extension) throws JsonProcessingException {
        GXFSCatalogListResponse<SelfDescriptionItem> response = null;
        try {
            response = gxfsCatalogService.getSelfDescriptionsByIds(new String[]{extension.getId()},
                    new SelfDescriptionStatus[]{SelfDescriptionStatus.ACTIVE, SelfDescriptionStatus.REVOKED});
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        return response;
    }

    private List<SelfDescriptionItem> getSelfDescriptionsByOfferingExtensionList
            (Page<ServiceOfferingExtension> extensions, boolean showRevoked) throws JsonProcessingException {
        String[] extensionHashes = extensions.stream().map(ServiceOfferingExtension::getCurrentSdHash)
                .collect(Collectors.toSet()).toArray(String[]::new);

        if (extensionHashes.length == 0) {
            return Collections.emptyList();
        }

        GXFSCatalogListResponse<SelfDescriptionItem> selfDescriptionsResponse = null;
        try {
            if (showRevoked) {
                selfDescriptionsResponse = gxfsCatalogService.getSelfDescriptionsByHashes(extensionHashes,
                        new SelfDescriptionStatus[]{SelfDescriptionStatus.ACTIVE, SelfDescriptionStatus.REVOKED});
            } else {
                selfDescriptionsResponse = gxfsCatalogService.getSelfDescriptionsByHashes(extensionHashes);
            }
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        if (selfDescriptionsResponse.getTotalCount() != extensions.getNumberOfElements()) {
            logger.warn("Inconsistent state detected, there are service offerings in the local database that are not in the catalog.");
        }

        return selfDescriptionsResponse.getItems();
    }

    private void handleCatalogError(WebClientResponseException e)
            throws ResponseStatusException, JsonProcessingException {
        logger.warn("Error in communication with catalog: {}", e.getResponseBodyAsString());
        JsonNode errorMessage = objectMapper.readTree(e.getResponseBodyAsString());
        String messageText = errorMessage.get("message").asText();
        throw new ResponseStatusException(e.getStatusCode(),
                messageText.substring(0, Math.min(500, messageText.length())));
    }

    /**
     * Given an offering id and a target State, try to transition the offering to the requested state.
     *
     * @param id                 id of the offering
     * @param targetState        requested target state
     */
    public void transitionServiceOfferingExtension(String id, ServiceOfferingState targetState) {
        if (!serviceOfferingExtensionRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);
        if (extension != null) {
            try {
                switch (targetState) {
                    case IN_DRAFT -> extension.inDraft();
                    case RELEASED -> extension.release();
                    case REVOKED -> extension.revoke();
                    case DELETED, ARCHIVED -> deleteOffering(extension);
                    case PURGED -> purgeOffering(extension);
                }
            } catch (ResponseStatusException ex) {
                throw ex;
            } catch (Exception e) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid state transition requested.");
            }
            if (targetState != ServiceOfferingState.PURGED
                && targetState != ServiceOfferingState.DELETED
                && targetState != ServiceOfferingState.ARCHIVED) {
                serviceOfferingExtensionRepository.save(extension);
            }
        }

    }

    /**
     * Attempt to find an offering by the given id.
     *
     * @param id        id of the offering to search for
     * @return found offering
     * @throws Exception mapping exception
     */
    public ServiceOfferingDto getServiceOfferingById(String id) throws JsonProcessingException {
        // basic input sanitization
        id = Jsoup.clean(id, Safelist.basic());

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);

        if (extension == null) {
            throw new NoSuchElementException(OFFERING_NOT_FOUND);
        }

        GXFSCatalogListResponse<SelfDescriptionItem> selfDescriptionsResponse =
                getSelfDescriptionByOfferingExtension(extension);
        // if we do not get exactly one item or the id doesn't start with ServiceOffering, we did not find the correct item
        if (selfDescriptionsResponse.getTotalCount() != 1
                || !selfDescriptionsResponse.getItems().get(0).getMeta().getId().startsWith(OFFERING_START)) {
            throw new NoSuchElementException(OFFERING_NOT_FOUND);
        }

        return serviceOfferingMapper.selfDescriptionMetaToServiceOfferingDto(
                selfDescriptionsResponse.getItems().get(0).getMeta(),
                extension,
                organizationOrchestratorClient.getOrganizationDetails(extension.getIssuer()));
    }

    /**
     * Given the paging parameters, find all offerings that are publicly visible (released).
     *
     * @param pageable  paging parameters
     * @return page of public offerings
     * @throws JsonProcessingException mapping exception
     */
    public Page<ServiceOfferingBasicDto> getAllPublicServiceOfferings(Pageable pageable) throws JsonProcessingException {
        Page<ServiceOfferingExtension> extensions = serviceOfferingExtensionRepository
                .findAllByState(ServiceOfferingState.RELEASED, pageable);
        Map<String, ServiceOfferingExtension> extensionMap = extensions.stream()
                .collect(Collectors.toMap(ServiceOfferingExtension::getCurrentSdHash, Function.identity()));

        List<SelfDescriptionItem> items = getSelfDescriptionsByOfferingExtensionList(extensions, false);

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingBasicDto> models = items.stream()
                .map(item -> serviceOfferingMapper.selfDescriptionMetaToServiceOfferingBasicDto(
                        item.getMeta(),
                        extensionMap.get(item.getMeta().getSdHash()),
                        organizationOrchestratorClient
                                .getOrganizationDetails(extensionMap.get(item.getMeta().getSdHash())
                                        .getIssuer())))
                .sorted(Comparator.comparing(offer -> offer.getCreationDate() != null
                                ? (LocalDateTime.parse(offer.getCreationDate(), DateTimeFormatter.ISO_DATE_TIME))
                                : LocalDateTime.MIN,
                        Comparator.reverseOrder()))  // since the catalog does not respect the order of the hashes, we need to reorder again
                .toList();

        return new PageImpl<>(models, pageable, extensions.getTotalElements()); // TODO check if we need custom impl
    }

    /**
     * Given an organization id and paging parameters, find all offerings that belong to this organization.
     * Optionally, also specify an offering state to filter for a specific state.
     *
     * @param orgaId    id of the organization to fetch the offerings for
     * @param state     optional offering state for filtering
     * @param pageable  paging parameters
     * @return page of organization offerings
     * @throws JsonProcessingException mapping exception
     */
    public Page<ServiceOfferingBasicDto> getOrganizationServiceOfferings(String orgaId, ServiceOfferingState state, Pageable pageable)
            throws JsonProcessingException {
        Page<ServiceOfferingExtension> extensions;
        if (state != null) {
            extensions = serviceOfferingExtensionRepository
                    .findAllByIssuerAndState(orgaId, state, pageable);
        } else {
            extensions = serviceOfferingExtensionRepository
                    .findAllByIssuer(orgaId, pageable);
        }
        Map<String, ServiceOfferingExtension> extensionMap = extensions.stream()
                .collect(Collectors.toMap(ServiceOfferingExtension::getCurrentSdHash, Function.identity()));

        List<SelfDescriptionItem> items = getSelfDescriptionsByOfferingExtensionList(extensions, true);

        MerlotParticipantDto providerOrga = organizationOrchestratorClient.getOrganizationDetails(orgaId);

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingBasicDto> models = items.stream()
                .map(item -> serviceOfferingMapper.selfDescriptionMetaToServiceOfferingBasicDto(
                        item.getMeta(),
                        extensionMap.get(item.getMeta().getSdHash()),
                        providerOrga))
                .sorted(Comparator.comparing(offer -> offer.getCreationDate() != null
                                ? (LocalDateTime.parse(offer.getCreationDate(), DateTimeFormatter.ISO_DATE_TIME))
                                : LocalDateTime.MIN,
                        Comparator.reverseOrder()))  // since the catalog does not respect the order of the hashes, we need to reorder again
                .toList();

        return new PageImpl<>(models, pageable, extensions.getTotalElements()); // TODO check if we need custom impl
    }

    /**
     * Given an id of a service offering, this method attempts to copy all fields to a new offering and give it a new
     * id, making it a separate catalog entry.
     *
     * @param id                 id of the offering to regenerate
     * @return creation response from catalog
     * @throws Exception communication or mapping exception
     */
    public SelfDescriptionMeta regenerateOffering(String id) throws Exception {
        // basic input sanitization
        id = Jsoup.clean(id, Safelist.basic());

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);

        if (extension == null) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }

        if (!(extension.getState() == ServiceOfferingState.RELEASED ||
                extension.getState() == ServiceOfferingState.DELETED ||
                extension.getState() == ServiceOfferingState.ARCHIVED)) {
            throw new ResponseStatusException(PRECONDITION_FAILED, "Invalid state for regenerating this offering");
        }

        GXFSCatalogListResponse<SelfDescriptionItem> selfDescriptionsResponse =
                getSelfDescriptionByOfferingExtension(extension);
        // if we do not get exactly one item or the id doesn't start with ServiceOffering, we did not find the correct item
        if (selfDescriptionsResponse.getTotalCount() != 1
                || !selfDescriptionsResponse.getItems().get(0).getMeta().getId().startsWith(OFFERING_START)) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }
        MerlotServiceOfferingCredentialSubject subject = (MerlotServiceOfferingCredentialSubject) selfDescriptionsResponse.getItems()
                .get(0).getMeta().getContent().getVerifiableCredential().getCredentialSubject();
        subject.setId(OFFERING_START + "TBR");

        return addServiceOffering(subject);
    }

    private void patchTermsAndConditions(MerlotServiceOfferingCredentialSubject credentialSubject) {
        TermsAndConditions providerTnC = ((MerlotOrganizationCredentialSubject) organizationOrchestratorClient
                .getOrganizationDetails(credentialSubject.getOfferedBy().getId())
                .getSelfDescription().getVerifiableCredential().getCredentialSubject()).getTermsAndConditions();

        if (StringUtil.isNullOrEmpty(providerTnC.getContent())
                || StringUtil.isNullOrEmpty(providerTnC.getHash())) {
            throw new ResponseStatusException(FORBIDDEN, "Cannot create/update self-description without valid provider TnC");
        }

        TermsAndConditions merlotTnC = ((MerlotOrganizationCredentialSubject) organizationOrchestratorClient
                .getOrganizationDetails("did:web:" + merlotDomain + "#merlot-federation")
                .getSelfDescription().getVerifiableCredential().getCredentialSubject()).getTermsAndConditions();

        // regardless of if we are updating or creating a new offering, we need to patch the tnc if the frontend does not send them
        if (credentialSubject.getTermsAndConditions() == null) {
            credentialSubject.setTermsAndConditions(new ArrayList<>());
        }
        if (!credentialSubject.getTermsAndConditions().contains(merlotTnC)) {
            credentialSubject.getTermsAndConditions().add(0, merlotTnC);
        }
        if (!credentialSubject.getTermsAndConditions().contains(providerTnC)) {
            credentialSubject.getTermsAndConditions().add(1, providerTnC);
        }
    }

    /**
     * Given a self-description, attempt to publish it to the GXFS catalog.
     * If the id is not specified (set to ServiceOffering:TBR), create a new entry,
     * otherwise attempt to update an existing offering with this id.
     *
     * @param credentialSubject self-description of the offering
     * @return creation response of the GXFS catalog
     * @throws Exception mapping exception
     */
    @Transactional(rollbackOn = {ResponseStatusException.class})
    public SelfDescriptionMeta addServiceOffering(MerlotServiceOfferingCredentialSubject credentialSubject) throws Exception {
        ServiceOfferingExtension extension;
        String previousSdHash = null;
        if (credentialSubject.getId().equals(OFFERING_START + "TBR")) {
            // override creation time to correspond to the current time and generate an ID
            extension = new ServiceOfferingExtension();
            credentialSubject.setCreationDate(extension.getCreationDate().format(DateTimeFormatter.ISO_INSTANT));
            credentialSubject.setId(OFFERING_START + UUID.randomUUID());
        } else {
            extension = serviceOfferingExtensionRepository
                    .findById(credentialSubject.getId()).orElse(null);

            // handle potential failure points
            if (extension != null) {
                previousSdHash = extension.getCurrentSdHash();

                // must be in draft
                if (extension.getState() != ServiceOfferingState.IN_DRAFT) {
                    throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description as it is not in draft");
                }

                // issuer may not change
                if (!extension.getIssuer().equals(credentialSubject.getOfferedBy().getId())) {
                    throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description as it contains invalid fields");
                }

                // override creation date
                credentialSubject.setCreationDate(extension.getCreationDate().format(DateTimeFormatter.ISO_INSTANT));
            } else {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description there is none with this id");
            }
        }

        patchTermsAndConditions(credentialSubject);

        SelfDescriptionMeta selfDescriptionsResponse = null;
        try {
            selfDescriptionsResponse = addServiceOfferingToCatalog(credentialSubject);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        // with a successful response (i.e. no exception was thrown) we are good to save the new or updated self-description
        extension.setId(selfDescriptionsResponse.getId());
        extension.setIssuer(selfDescriptionsResponse.getIssuer());
        extension.setCurrentSdHash(selfDescriptionsResponse.getSdHash());
        try {
            serviceOfferingExtensionRepository.save(extension);
        } catch (Exception e) {
            // if saving fails, "rollback" the service-offering creation in the catalog
            deleteServiceOfferingFromCatalog(selfDescriptionsResponse.getSdHash());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Service offering could not be saved.");
        }

        // delete previous entry if it exists
        if (previousSdHash != null) {
            try {
                deleteServiceOfferingFromCatalog(previousSdHash);
            } catch (ResponseStatusException ex) {
                //if deleting the previous entry fails, "rollback" the service-offering creation in the catalog
                deleteServiceOfferingFromCatalog(selfDescriptionsResponse.getSdHash());
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Service offering could not be updated.");
            }
        }

        return selfDescriptionsResponse;
    }

    private void deleteServiceOfferingFromCatalog(String sdHash) throws JsonProcessingException {
        try {
            gxfsCatalogService.deleteSelfDescriptionByHash(sdHash);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
    }

    private SelfDescriptionMeta addServiceOfferingToCatalog(MerlotServiceOfferingCredentialSubject credentialSubject) throws JsonProcessingException {
        SelfDescriptionMeta response = null;
        try {
            response = gxfsCatalogService.addServiceOffering(credentialSubject);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        } catch (Exception e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unknown error");
        }
        return response;
    }
}
