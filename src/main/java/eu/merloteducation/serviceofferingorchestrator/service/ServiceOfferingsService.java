package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.gxfscataloglibrary.models.client.SelfDescriptionStatus;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxSOTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganisationSignerConfigDto;
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
    private static final String OFFERING_START = "urn:uuid:";
    private static final String OFFERING_NOT_FOUND = "No valid service offering with this id was found.";
    private static final String AUTHORIZATION = "Authorization";

    @Transactional(rollbackOn = {ResponseStatusException.class})
    private void deleteOffering(ServiceOfferingExtension extension) {
        extension.delete();
        serviceOfferingExtensionRepository.save(extension);

        try {
            gxfsCatalogService.revokeSelfDescriptionByHash(extension.getCurrentSdHash());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
    }

    @Transactional(rollbackOn = {ResponseStatusException.class})
    private void purgeOffering(ServiceOfferingExtension extension) {
        if (extension.getState() != ServiceOfferingState.DELETED) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid state transition requested.");
        }
        serviceOfferingExtensionRepository.delete(extension);
        deleteServiceOfferingFromCatalog(extension.getCurrentSdHash());
    }

    private SelfDescriptionMeta getSelfDescriptionByOfferingExtension
            (ServiceOfferingExtension extension) {
        SelfDescriptionMeta sdMeta = null;
        try {
            GXFSCatalogListResponse<SelfDescriptionItem> response = gxfsCatalogService.getSelfDescriptionsByIds(new String[]{extension.getId()},
                    new SelfDescriptionStatus[]{SelfDescriptionStatus.ACTIVE, SelfDescriptionStatus.REVOKED});
            if (response.getTotalCount() != 1
                    || !response.getItems().get(0).getMeta().getId().startsWith(OFFERING_START)) {
                throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
            }
            sdMeta = response.getItems().get(0).getMeta();
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        return sdMeta;
    }

    private List<SelfDescriptionMeta> getSelfDescriptionsByOfferingExtensionList
            (Page<ServiceOfferingExtension> extensions, boolean showRevoked) {
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

        return selfDescriptionsResponse.getItems().stream().map(SelfDescriptionItem::getMeta).toList();
    }

    private void handleCatalogError(WebClientResponseException e) {
        logger.warn("Error in communication with catalog: {}", e.getResponseBodyAsString());
        String messageText;
        try {
            JsonNode errorMessage = objectMapper.readTree(e.getResponseBodyAsString());
            messageText = errorMessage.get("message").asText();
        } catch (JsonProcessingException ignored) {
            messageText = "Unknown error.";
        }
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
     */
    public ServiceOfferingDto getServiceOfferingById(String id) {
        // basic input sanitization
        id = Jsoup.clean(id, Safelist.basic());

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);

        if (extension == null) {
            throw new NoSuchElementException(OFFERING_NOT_FOUND);
        }

        SelfDescriptionMeta sdMeta = getSelfDescriptionByOfferingExtension(extension);
        // if we do not get exactly one item or the id doesn't start with ServiceOffering, we did not find the correct item

        String signerLegalName = null;
        try {
            signerLegalName = getSignerLegalNameFromCatalog(sdMeta.getContent());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        return serviceOfferingMapper.selfDescriptionMetaToServiceOfferingDto(
                sdMeta,
                extension,
                organizationOrchestratorClient.getOrganizationDetails(extension.getIssuer()),
                signerLegalName);
    }

    /**
     * Given the paging parameters, find all offerings that are publicly visible (released).
     *
     * @param pageable  paging parameters
     * @return page of public offerings
     */
    public Page<ServiceOfferingBasicDto> getAllPublicServiceOfferings(Pageable pageable) {
        Page<ServiceOfferingExtension> extensions = serviceOfferingExtensionRepository
                .findAllByState(ServiceOfferingState.RELEASED, pageable);
        Map<String, ServiceOfferingExtension> extensionMap = extensions.stream()
                .collect(Collectors.toMap(ServiceOfferingExtension::getCurrentSdHash, Function.identity()));

        List<SelfDescriptionMeta> items = getSelfDescriptionsByOfferingExtensionList(extensions, false);

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingBasicDto> models = items.stream()
                .map(item -> serviceOfferingMapper.selfDescriptionMetaToServiceOfferingBasicDto(
                        item,
                        extensionMap.get(item.getSdHash()),
                        organizationOrchestratorClient
                                .getOrganizationDetails(extensionMap.get(item.getSdHash())
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
     */
    public Page<ServiceOfferingBasicDto> getOrganizationServiceOfferings(
            String orgaId,
            ServiceOfferingState state, Pageable pageable) {
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

        List<SelfDescriptionMeta> items = getSelfDescriptionsByOfferingExtensionList(extensions, true);

        MerlotParticipantDto providerOrga = organizationOrchestratorClient.getOrganizationDetails(orgaId);

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingBasicDto> models = items.stream()
                .map(item -> serviceOfferingMapper.selfDescriptionMetaToServiceOfferingBasicDto(
                        item,
                        extensionMap.get(item.getSdHash()),
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
     */
    public SelfDescriptionMeta regenerateOffering(String id, String authToken) {
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

        SelfDescriptionMeta sdMeta = getSelfDescriptionByOfferingExtension(extension);

        ServiceOfferingDto offeringDto = new ServiceOfferingDto();
        offeringDto.setSelfDescription(sdMeta.getContent());

        return addServiceOffering(offeringDto, authToken);
    }

    private void patchTermsAndConditions(GxServiceOfferingCredentialSubject credentialSubject,
                                         MerlotParticipantDto participantDto) {

        GxSOTermsAndConditions providerTnC = new GxSOTermsAndConditions(participantDto
                .getSelfDescription().findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class)
                .getTermsAndConditions());

        if (StringUtil.isNullOrEmpty(providerTnC.getUrl())
                || StringUtil.isNullOrEmpty(providerTnC.getHash())) {
            throw new ResponseStatusException(FORBIDDEN, "Cannot create/update self-description without valid provider TnC");
        }

        GxSOTermsAndConditions merlotTnC = new GxSOTermsAndConditions(organizationOrchestratorClient
                .getOrganizationDetails(getMerlotFederationId()).getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class)
                .getTermsAndConditions());

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

    @Transactional(rollbackOn = {ResponseStatusException.class})
    private SelfDescriptionMeta storeServiceOffering(GxServiceOfferingCredentialSubject offeringCs,
                                                     MerlotServiceOfferingCredentialSubject merlotOfferingCs,
                                                     PojoCredentialSubject specificMerlotOfferingCs,
                                                     ServiceOfferingExtension extension,
                                                     String authToken) {
        MerlotParticipantDto participantDto = organizationOrchestratorClient
                .getOrganizationDetails(offeringCs.getProvidedBy().getId(), Map.of(AUTHORIZATION, authToken));

        // request provider details
        patchTermsAndConditions(offeringCs, participantDto);

        OrganisationSignerConfigDto orgaSignerConfig = participantDto.getMetadata().getOrganisationSignerConfigDto();

        SelfDescriptionMeta selfDescriptionsResponse = addServiceOfferingToCatalog(
                List.of(offeringCs, merlotOfferingCs, specificMerlotOfferingCs),
                orgaSignerConfig);

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

        return selfDescriptionsResponse;
    }

    /**
     * Given a self-description, attempt to publish it to the GXFS catalog.
     * If the id is not specified (set to ServiceOffering:TBR), create a new entry,
     * otherwise attempt to update an existing offering with this id.
     *
     * @param serviceOfferingDto self-description of the offering
     * @param authToken authToken to access further backend services
     * @return creation response of the GXFS catalog
     */
    public SelfDescriptionMeta addServiceOffering(ServiceOfferingDto serviceOfferingDto, String authToken) {

        ServiceOfferingExtension extension = new ServiceOfferingExtension();

        // extract credential subjects from VP
        ExtendedVerifiablePresentation vp = serviceOfferingDto.getSelfDescription();
        GxServiceOfferingCredentialSubject offeringCs = vp
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class);
        MerlotServiceOfferingCredentialSubject merlotOfferingCs = vp
                .findFirstCredentialSubjectByType(MerlotServiceOfferingCredentialSubject.class);
        PojoCredentialSubject specificMerlotOfferingCs = handleSpecificMerlotOfferingCs(vp);

        // generate a new ID for the offerings
        String offeringId = OFFERING_START + UUID.randomUUID();
        offeringCs.setId(offeringId);
        merlotOfferingCs.setId(offeringId);
        merlotOfferingCs.setCreationDate(extension.getCreationDate().format(DateTimeFormatter.ISO_INSTANT));
        specificMerlotOfferingCs.setId(offeringId);

        return storeServiceOffering(offeringCs, merlotOfferingCs, specificMerlotOfferingCs, extension, authToken);
    }

    /**
     *
     * Given an offeringDto containing a selfdescription and the id of an existing offering,
     * attempt to update the corresponding catalog entry with the new information.
     *
     * @param serviceOfferingDto dto with self-description of the offering
     * @param offeringId id of the offering to update
     * @param authToken authToken to access further backend services
     * @return creation response of the GXFS catalog
     */
    @Transactional(rollbackOn = {ResponseStatusException.class})
    public SelfDescriptionMeta updateServiceOffering(ServiceOfferingDto serviceOfferingDto,
                                                     String offeringId, String authToken) {

        ExtendedVerifiablePresentation vp = serviceOfferingDto.getSelfDescription();
        GxServiceOfferingCredentialSubject offeringCs = vp
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class);
        MerlotServiceOfferingCredentialSubject merlotOfferingCs = vp
                .findFirstCredentialSubjectByType(MerlotServiceOfferingCredentialSubject.class);
        PojoCredentialSubject specificMerlotOfferingCs = handleSpecificMerlotOfferingCs(vp);

        if (!offeringCs.getId().equals(offeringId)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Given offering id does not match the self-description.");
        }

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository
                .findById(offeringId).orElse(null);

        if (extension == null) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description there is none with this id");
        }

        // handle potential failure points
        String previousSdHash = extension.getCurrentSdHash();

        // must be in draft
        if (extension.getState() != ServiceOfferingState.IN_DRAFT) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description as it is not in draft");
        }

        // issuer may not change
        if (!extension.getIssuer().equals(offeringCs.getProvidedBy().getId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description as it contains invalid fields");
        }

        // override creation date
        merlotOfferingCs.setCreationDate(extension.getCreationDate().format(DateTimeFormatter.ISO_INSTANT));

        SelfDescriptionMeta selfDescriptionsResponse =
                storeServiceOffering(offeringCs, merlotOfferingCs, specificMerlotOfferingCs, extension, authToken);

        // delete previous entry
        try {
            deleteServiceOfferingFromCatalog(previousSdHash);
        } catch (ResponseStatusException ex) {
            //if deleting the previous entry fails, "rollback" the service-offering creation in the catalog
            deleteServiceOfferingFromCatalog(selfDescriptionsResponse.getSdHash());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Service offering could not be updated.");
        }

        return selfDescriptionsResponse;
    }

    private PojoCredentialSubject handleSpecificMerlotOfferingCs(ExtendedVerifiablePresentation vp) {

        // create pojo cs for MERLOT specific offering type from vp
        PojoCredentialSubject specificOfferingCs = serviceOfferingMapper.getSpecificMerlotOfferingCs(vp);

        // if any checks depending on a particular type should be performed, this would be the place

        // if none match this is not a valid credential request
        if (specificOfferingCs == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Given payload does not contain a full MERLOT offering credential");
        }
        return specificOfferingCs;
    }

    private void deleteServiceOfferingFromCatalog(String sdHash) {
        try {
            gxfsCatalogService.deleteSelfDescriptionByHash(sdHash);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
    }

    private boolean isSignerConfigValid(OrganisationSignerConfigDto signerConfig) {
        if (signerConfig == null) {
            return false;
        }

        boolean privateKeyValid = signerConfig.getPrivateKey() != null
            && !signerConfig.getPrivateKey().isBlank();

        boolean verificationMethodValid = signerConfig.getVerificationMethod() != null
            && !signerConfig.getVerificationMethod().isBlank();

        boolean merlotVerificationMethodValid = signerConfig.getMerlotVerificationMethod() != null
            && !signerConfig.getMerlotVerificationMethod().isBlank();

        return privateKeyValid && verificationMethodValid && merlotVerificationMethodValid;
    }

    private SelfDescriptionMeta addServiceOfferingToCatalog(List<PojoCredentialSubject> credentialSubjects,
                                                            OrganisationSignerConfigDto orgaSignerConfig) {
        if (!isSignerConfigValid(orgaSignerConfig)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Service offering cannot be saved: Missing private key and/or verification method.");
        }

        SelfDescriptionMeta response = null;
        try {
            // sign SD using verification method referencing the merlot certificate and the default/merlot private key
            response = gxfsCatalogService.addServiceOffering(credentialSubjects, orgaSignerConfig.getMerlotVerificationMethod());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        } catch (Exception e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unknown error");
        }
        return response;
    }

    private String getMerlotFederationId() {
        return "did:web:" + merlotDomain.replaceFirst(":", "%3A") + ":participant:df15587a-0760-32b5-9c42-bb7be66e8076";
    }

    private String getSignerLegalNameFromCatalog(ExtendedVerifiablePresentation selfDescription) {

        String proofVerificationMethod = selfDescription.getLdProof().getVerificationMethod().toString();

        String signerId = proofVerificationMethod.replaceFirst("#.*", "");

        GXFSCatalogListResponse<GXFSQueryLegalNameItem>
            response = gxfsCatalogService.getParticipantLegalNameByUri(
                    MerlotLegalParticipantCredentialSubject.TYPE_CLASS, signerId);

        // if we do not get exactly one item, we did not find the signer participant and the corresponding legal name
        if (response.getTotalCount() != 1) {
            return null;
        } else {
            return response.getItems().get(0).getLegalName();
        }
    }
}
