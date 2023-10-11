package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import eu.merloteducation.serviceofferingorchestrator.mappers.ServiceOfferingMapper;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingBasicDto;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.TermsAndConditions;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsResponse;
import eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator.OrganizationDetails;
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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class GXFSCatalogRestService {

    @Autowired
    private OrganizationOrchestratorClient organizationOrchestratorClient;

    @Autowired
    private ServiceOfferingMapper serviceOfferingMapper;

    @Autowired
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    private GXFSSignerService gxfsSignerService;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @Value("${gxfscatalog.selfdescriptions-uri}")
    private String gxfscatalogSelfdescriptionsUri;
    private final Logger logger = LoggerFactory.getLogger(GXFSCatalogRestService.class);
    private static final String PARTICIPANT_START = "Participant:";
    private static final String OFFERING_START = "ServiceOffering:";
    private static final String OFFERING_NOT_FOUND = "No valid service offering with this id was found.";

    private void deleteOffering(ServiceOfferingExtension extension) {
        extension.delete();
        keycloakAuthService.webCallAuthenticated(
                HttpMethod.POST,
                gxfscatalogSelfdescriptionsUri + "/" + extension.getCurrentSdHash() + "/revoke",
                "",
                null);
    }

    private void purgeOffering(ServiceOfferingExtension extension) {
        if (extension.getState() != ServiceOfferingState.DELETED) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid state transition requested.");
        }
        keycloakAuthService.webCallAuthenticated(
                HttpMethod.DELETE,
                gxfscatalogSelfdescriptionsUri + "/" + extension.getCurrentSdHash(),
                "",
                null);
        serviceOfferingExtensionRepository.delete(extension);
    }

    private SelfDescriptionsResponse getSelfDescriptionByOfferingExtension
            (ServiceOfferingExtension extension) throws Exception {
        String response = keycloakAuthService.webCallAuthenticated(
                HttpMethod.GET,
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&ids=" + extension.getId(),
                "",
                null);

        // create a mapper to map the response to the SelfDescriptionResponse class
        return new ObjectMapper().readValue(response, new TypeReference<>() {
        });
    }

    private void handleCatalogError(WebClientResponseException e)
            throws ResponseStatusException, JsonProcessingException {
        logger.warn("Error in communication with catalog: {}", e.getResponseBodyAsString());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode errorMessage = objectMapper.readTree(e.getResponseBodyAsString());
        throw new ResponseStatusException(e.getStatusCode(), errorMessage.get("message").asText());
    }

    private String presentAndSign(String credentialSubjectJson, String issuer) throws Exception {
        String vp = """
                {
                    "@context": ["https://www.w3.org/2018/credentials/v1"],
                    "@id": "http://example.edu/verifiablePresentation/self-description1",
                    "type": ["VerifiablePresentation"],
                    "verifiableCredential": {
                        "@context": ["https://www.w3.org/2018/credentials/v1"],
                        "@id": "https://www.example.org/ServiceOffering.json",
                        "@type": ["VerifiableCredential"],
                        "issuer": \"""" + issuer + """
                ",
                "issuanceDate": \"""" + OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT) + """
                ",
                "credentialSubject":\s""" + credentialSubjectJson + """
                    }
                }
                """;

        return gxfsSignerService.signVerifiablePresentation(vp);
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
                if (targetState != ServiceOfferingState.PURGED) {
                    serviceOfferingExtensionRepository.save(extension);
                }
            } catch (Exception e) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid state transition requested.");
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
    public ServiceOfferingDto getServiceOfferingById(String id) throws Exception {
        // basic input sanitization
        id = Jsoup.clean(id, Safelist.basic());

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);

        if (extension == null) {
            throw new NoSuchElementException(OFFERING_NOT_FOUND);
        }

        SelfDescriptionsResponse selfDescriptionsResponse = getSelfDescriptionByOfferingExtension(extension);
        // if we do not get exactly one item or the id doesnt start with ServiceOffering, we did not find the correct item
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
     * @throws Exception mapping exception
     */
    public Page<ServiceOfferingBasicDto> getAllPublicServiceOfferings(Pageable pageable) throws Exception {
        Page<ServiceOfferingExtension> extensions = serviceOfferingExtensionRepository
                .findAllByState(ServiceOfferingState.RELEASED, pageable);
        Map<String, ServiceOfferingExtension> extensionMap = extensions.stream()
                .collect(Collectors.toMap(ServiceOfferingExtension::getCurrentSdHash, Function.identity()));
        String extensionHashes = Joiner.on(",")
                .join(extensions.stream().map(ServiceOfferingExtension::getCurrentSdHash)
                        .collect(Collectors.toSet()));

        String response = keycloakAuthService.webCallAuthenticated(
                HttpMethod.GET,
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE&hashes=" + extensionHashes,
                "",
                null);


        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper();
        SelfDescriptionsResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);

        if (selfDescriptionsResponse.getTotalCount() != extensions.getNumberOfElements()) {
            logger.warn("Inconsistent state detected, there are service offerings in the local database that are not in the catalog.");
        }

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingBasicDto> models = selfDescriptionsResponse.getItems().stream()
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

        return new PageImpl<>(models, pageable, extensions.getTotalElements());
    }

    /**
     * Given an organization id and paging parameters, find all offerings that belong to this organization.
     * Optionally, also specify an offering state to filter for a specific state.
     *
     * @param orgaId    id of the organization to fetch the offerings for
     * @param state     optional offering state for filtering
     * @param pageable  paging parameters
     * @return page of organization offerings
     * @throws Exception mapping exception
     */
    public Page<ServiceOfferingBasicDto> getOrganizationServiceOfferings(String orgaId, ServiceOfferingState state, Pageable pageable) throws Exception {
        Page<ServiceOfferingExtension> extensions;
        if (state != null) {
            extensions = serviceOfferingExtensionRepository
                    .findAllByIssuerAndState(PARTICIPANT_START + orgaId, state, pageable);
        } else {
            extensions = serviceOfferingExtensionRepository
                    .findAllByIssuer(PARTICIPANT_START + orgaId, pageable);
        }
        Map<String, ServiceOfferingExtension> extensionMap = extensions.stream()
                .collect(Collectors.toMap(ServiceOfferingExtension::getCurrentSdHash, Function.identity()));
        String extensionHashes = Joiner.on(",")
                .join(extensions.stream().map(ServiceOfferingExtension::getCurrentSdHash)
                        .collect(Collectors.toSet()));

        String response = keycloakAuthService.webCallAuthenticated(
                HttpMethod.GET,
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&hashes=" + extensionHashes,
                "",
                null);

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper();
        SelfDescriptionsResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);
        if (selfDescriptionsResponse.getTotalCount() != extensions.getNumberOfElements()) {
            logger.warn("Inconsistent state detected, there are service offerings in the local database that are not in the catalog.");
        }

        OrganizationDetails providerOrga = organizationOrchestratorClient.getOrganizationDetails(orgaId);

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingBasicDto> models = selfDescriptionsResponse.getItems().stream()
                .map(item -> serviceOfferingMapper.selfDescriptionMetaToServiceOfferingBasicDto(
                        item.getMeta(),
                        extensionMap.get(item.getMeta().getSdHash()),
                        providerOrga))
                .sorted(Comparator.comparing(offer -> offer.getCreationDate() != null
                                ? (LocalDateTime.parse(offer.getCreationDate(), DateTimeFormatter.ISO_DATE_TIME))
                                : LocalDateTime.MIN,
                        Comparator.reverseOrder()))  // since the catalog does not respect the order of the hashes, we need to reorder again
                .toList();

        return new PageImpl<>(models, pageable, extensions.getTotalElements());
    }

    /**
     * Given an id of a service offering, this method attempts to copy all fields to a new offering and give it a new
     * id, making it a separate catalog entry.
     *
     * @param id                 id of the offering to regenerate
     * @return creation response from catalog
     * @throws Exception communication or mapping exception
     */
    public SelfDescriptionsCreateResponse regenerateOffering(String id) throws Exception {
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

        SelfDescriptionsResponse selfDescriptionsResponse =
                getSelfDescriptionByOfferingExtension(extension);
        // if we do not get exactly one item or the id doesn't start with ServiceOffering, we did not find the correct item
        if (selfDescriptionsResponse.getTotalCount() != 1
                || !selfDescriptionsResponse.getItems().get(0).getMeta().getId().startsWith(OFFERING_START)) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }
        ServiceOfferingCredentialSubject subject = selfDescriptionsResponse.getItems().get(0).getMeta()
                .getContent().getVerifiableCredential().getCredentialSubject();
        subject.setId(OFFERING_START + "TBR");

        return addServiceOffering(subject);
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
    @Transactional
    public SelfDescriptionsCreateResponse addServiceOffering(ServiceOfferingCredentialSubject credentialSubject) throws Exception {

        TermsAndConditions providerTnC = organizationOrchestratorClient
                .getOrganizationDetails(credentialSubject.getOfferedBy().getId())
                .getSelfDescription().getVerifiableCredential().getCredentialSubject().getTermsAndConditions();

        if (StringUtil.isNullOrEmpty(providerTnC.getContent().getValue())
                || StringUtil.isNullOrEmpty(providerTnC.getHash().getValue())) {
            throw new ResponseStatusException(FORBIDDEN, "Cannot create/update self-description without valid provider TnC");
        }

        TermsAndConditions merlotTnC = organizationOrchestratorClient
                .getOrganizationDetails("Participant:99")
                .getSelfDescription().getVerifiableCredential().getCredentialSubject().getTermsAndConditions();

        ServiceOfferingExtension extension;
        String previousSdHash = null;
        if (credentialSubject.getId().equals(OFFERING_START + "TBR")) {
            // override creation time to correspond to the current time and generate an ID
            extension = new ServiceOfferingExtension();
            credentialSubject.setCreationDate(new StringTypeValue(
                    extension.getCreationDate().format(DateTimeFormatter.ISO_INSTANT)));
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
                credentialSubject.setCreationDate(new StringTypeValue(
                        extension.getCreationDate().format(DateTimeFormatter.ISO_INSTANT)));
            } else {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description there is none with this id");
            }
        }

        // regardless of if we are updating or creating a new offering, we need to patch the tnc if the frontend does not send them
        if (credentialSubject.getTermsAndConditions() == null) {
            credentialSubject.setTermsAndConditions(new ArrayList<>());
        }
        if (!credentialSubject.getTermsAndConditions().contains(merlotTnC)) {
            credentialSubject.getTermsAndConditions().add(merlotTnC);
        }
        if (!credentialSubject.getTermsAndConditions().contains(providerTnC)) {
            credentialSubject.getTermsAndConditions().add(providerTnC);
        }

        // prepare a json to send to the gxfs catalog, sign it and read the response
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String credentialSubjectJson = mapper.writeValueAsString(credentialSubject);

        String signedVp = presentAndSign(credentialSubjectJson, credentialSubject.getOfferedBy().getId());

        String response = "";
        try {
            response = keycloakAuthService.webCallAuthenticated(
                    HttpMethod.POST,
                    gxfscatalogSelfdescriptionsUri,
                    signedVp,
                    MediaType.APPLICATION_JSON);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        // delete previous entry if it exists
        if (previousSdHash != null) {
            keycloakAuthService.webCallAuthenticated(
                    HttpMethod.DELETE,
                    gxfscatalogSelfdescriptionsUri + "/" + previousSdHash,
                    "",
                    null);
        }

        mapper = new ObjectMapper();
        SelfDescriptionsCreateResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsCreateResponse.class);

        // with a successful response (i.e. no exception was thrown) we are good to save the new or updated self description
        extension.setId(selfDescriptionsResponse.getId());
        extension.setIssuer(selfDescriptionsResponse.getIssuer());
        extension.setCurrentSdHash(selfDescriptionsResponse.getSdHash());
        serviceOfferingExtensionRepository.save(extension);

        return selfDescriptionsResponse;
    }
}
