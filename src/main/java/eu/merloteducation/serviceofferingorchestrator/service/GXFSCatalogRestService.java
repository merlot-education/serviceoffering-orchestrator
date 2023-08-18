package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import eu.merloteducation.serviceofferingorchestrator.mappers.ServiceOfferingMapper;
import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsResponse;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
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
    OrganizationOrchestratorClient organizationOrchestratorClient;

    @Autowired
    ServiceOfferingMapper serviceOfferingMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GXFSSignerService gxfsSignerService;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;

    @Value("${keycloak.logout-uri}")
    private String keycloakLogoutUri;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.authorization-grant-type}")
    private String grantType;

    @Value("${keycloak.gxfscatalog-user}")
    private String keycloakGXFScatalogUser;

    @Value("${keycloak.gxfscatalog-pass}")
    private String keycloakGXFScatalogPass;

    @Value("${gxfscatalog.selfdescriptions-uri}")
    private String gxfscatalogSelfdescriptionsUri;

    private final Logger logger = LoggerFactory.getLogger(GXFSCatalogRestService.class);

    private static final String PARTICIPANT_START = "Participant:";
    private static final String OFFERING_START = "ServiceOffering:";
    private static final String OFFERING_NOT_FOUND = "No valid service offering with this id was found.";

    private Map<String, Object> loginGXFScatalog() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", keycloakGXFScatalogUser);
        map.add("password", keycloakGXFScatalogPass);
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", grantType);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, new HttpHeaders());
        String response = restTemplate.postForObject(keycloakTokenUri, request, String.class);

        JsonParser parser = JsonParserFactory.getJsonParser();
        return parser.parseMap(response);
    }

    private void logoutGXFScatalog(String refreshToken) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, null);
        restTemplate.postForObject(keycloakLogoutUri, request, String.class);
    }

    private void deleteOffering(ServiceOfferingExtension extension) {
        extension.delete();
        restCallAuthenticated(gxfscatalogSelfdescriptionsUri + "/" + extension.getCurrentSdHash() + "/revoke",
                null, null, HttpMethod.POST);
    }

    private void purgeOffering(ServiceOfferingExtension extension) {
        if (extension.getState() != ServiceOfferingState.DELETED) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid state transition requested.");
        }
        restCallAuthenticated(gxfscatalogSelfdescriptionsUri + "/" + extension.getCurrentSdHash(), null,
                null, HttpMethod.DELETE);
        serviceOfferingExtensionRepository.delete(extension);
    }

    private SelfDescriptionsResponse getSelfDescriptionByOfferingExtension
            (ServiceOfferingExtension extension) throws Exception {
        String response = restCallAuthenticated(
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&ids=" + extension.getId(),
                null, null, HttpMethod.GET);

        // create a mapper to map the response to the SelfDescriptionResponse class
        return new ObjectMapper().readValue(response, new TypeReference<>() {
        });
    }

    private void handleCatalogError(HttpClientErrorException e)
            throws ResponseStatusException {
        logger.warn("Error in communication with catalog: {}", e.getMessage());

        if (e.getStatusCode() == UNPROCESSABLE_ENTITY) {
            String resultMessageStartTag = "@sh:resultMessage \\\"";
            int resultMessageStart = e.getMessage().indexOf(resultMessageStartTag) + resultMessageStartTag.length();
            int resultMessageEnd = e.getMessage().indexOf("\\\";", resultMessageStart);
            String resultMessage = e.getMessage().substring(
                    resultMessageStart, Math.min(resultMessageEnd, resultMessageStart + 100));
            throw new ResponseStatusException(e.getStatusCode(), resultMessage);
        } else {
            throw new ResponseStatusException(e.getStatusCode(), "Unknown error when communicating with catalog.");
        }
    }

    private String restCallAuthenticated(String url, String body, MediaType mediaType, HttpMethod method) {
        // log in as the gxfscatalog user and add the token to the header
        Map<String, Object> gxfscatalogLoginResponse = loginGXFScatalog();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + gxfscatalogLoginResponse.get("access_token"));
        if (mediaType != null)
            headers.setContentType(mediaType);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        String response =
                restTemplate.exchange(url,
                        method, request, String.class).getBody();

        // as the catalog returns nested but escaped jsons, we need to manually unescape to properly use it

        response = StringEscapeUtils.unescapeJson(response);
        if (response != null)
            response = response.replace("\"{", "{").replace("}\"", "}");

        // log out with the gxfscatalog user
        this.logoutGXFScatalog((String) gxfscatalogLoginResponse.get("refresh_token"));

        return response;
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
     * @param representedOrgaIds ids of the represented organizations
     */
    public void transitionServiceOfferingExtension(String id, ServiceOfferingState targetState, Set<String> representedOrgaIds) {
        if (!serviceOfferingExtensionRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);
        if (extension != null) {
            if (!representedOrgaIds.contains(extension.getIssuer().replace(PARTICIPANT_START, ""))) {
                throw new ResponseStatusException(FORBIDDEN, "Missing permissions to change status of this offering.");
            }

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
     * @param authToken the OAuth2 Token from the user requesting this action
     * @return found offering
     * @throws Exception mapping exception
     */
    public ServiceOfferingDto getServiceOfferingById(String id, String authToken) throws Exception {
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
                authToken != null ? organizationOrchestratorClient
                        .getOrganizationDetails(extension.getIssuer(), Map.of("Authorization", authToken)) : null);
    }

    /**
     * Given the paging parameters, find all offerings that are publicly visible (released).
     *
     * @param pageable  paging parameters
     * @param authToken the OAuth2 Token from the user requesting this action
     * @return page of public offerings
     * @throws Exception mapping exception
     */
    public Page<ServiceOfferingDto> getAllPublicServiceOfferings(Pageable pageable, String authToken) throws Exception {

        Page<ServiceOfferingExtension> extensions = serviceOfferingExtensionRepository
                .findAllByState(ServiceOfferingState.RELEASED, pageable);
        Map<String, ServiceOfferingExtension> extensionMap = extensions.stream()
                .collect(Collectors.toMap(ServiceOfferingExtension::getCurrentSdHash, Function.identity()));
        String extensionHashes = Joiner.on(",")
                .join(extensions.stream().map(ServiceOfferingExtension::getCurrentSdHash)
                        .collect(Collectors.toSet()));

        String response = restCallAuthenticated(
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE&hashes=" + extensionHashes, null,
                null, HttpMethod.GET);


        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);

        if (selfDescriptionsResponse.getTotalCount() != extensions.getNumberOfElements()) {
            logger.warn("Inconsistent state detected, there are service offerings in the local database that are not in the catalog.");
        }

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingDto> models = selfDescriptionsResponse.getItems().stream()
                .map(item -> serviceOfferingMapper.selfDescriptionMetaToServiceOfferingDto(
                        item.getMeta(),
                        extensionMap.get(item.getMeta().getSdHash()),
                        organizationOrchestratorClient
                                .getOrganizationDetails(extensionMap.get(item.getMeta().getSdHash())
                                        .getIssuer(), Map.of("Authorization", authToken))))
                .sorted(Comparator.comparing(offer -> offer.getMetadata().getCreationDate() != null
                                ? (LocalDateTime.parse(offer.getMetadata().getCreationDate(), DateTimeFormatter.ISO_DATE_TIME))
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
     * @param authToken the OAuth2 Token from the user requesting this action
     * @return page of organization offerings
     * @throws Exception mapping exception
     */
    public Page<ServiceOfferingDto> getOrganizationServiceOfferings(String orgaId, ServiceOfferingState state, Pageable pageable, String authToken) throws Exception {
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

        String response = restCallAuthenticated(
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE,REVOKED&hashes=" + extensionHashes, null,
                null, HttpMethod.GET);

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);
        if (selfDescriptionsResponse.getTotalCount() != extensions.getNumberOfElements()) {
            logger.warn("Inconsistent state detected, there are service offerings in the local database that are not in the catalog.");
        }

        // extract the items from the SelfDescriptionsResponse and map them to Dto instances
        List<ServiceOfferingDto> models = selfDescriptionsResponse.getItems().stream()
                .map(item -> serviceOfferingMapper.selfDescriptionMetaToServiceOfferingDto(
                        item.getMeta(),
                        extensionMap.get(item.getMeta().getSdHash()),
                        organizationOrchestratorClient
                                .getOrganizationDetails(extensionMap.get(item.getMeta().getSdHash())
                                        .getIssuer(), Map.of("Authorization", authToken))))
                .sorted(Comparator.comparing(offer -> offer.getMetadata().getCreationDate() != null
                                ? (LocalDateTime.parse(offer.getMetadata().getCreationDate(), DateTimeFormatter.ISO_DATE_TIME))
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
     * @param representedOrgaIds ids of represented organizations for access control
     * @return creation response from catalog
     * @throws Exception communication or mapping exception
     */
    public SelfDescriptionsCreateResponse regenerateOffering(String id, Set<String> representedOrgaIds) throws Exception {
        // basic input sanitization
        id = Jsoup.clean(id, Safelist.basic());

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);

        if (extension == null) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }

        if (!representedOrgaIds.contains(extension.getIssuer().replace(PARTICIPANT_START, ""))) {
            throw new ResponseStatusException(FORBIDDEN, "Not authorized to regenerate this offering");
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

        // prepare a json to send to the gxfs catalog, sign it and read the response
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String credentialSubjectJson = mapper.writeValueAsString(credentialSubject);

        String signedVp = presentAndSign(credentialSubjectJson, credentialSubject.getOfferedBy().getId());

        String response = "";
        try {
            response = restCallAuthenticated(gxfscatalogSelfdescriptionsUri, signedVp,
                    MediaType.APPLICATION_JSON, HttpMethod.POST);
        } catch (HttpClientErrorException e) {
            handleCatalogError(e);
        }

        // delete previous entry if it exists
        if (previousSdHash != null) {
            restCallAuthenticated(gxfscatalogSelfdescriptionsUri + "/" + previousSdHash, null,
                    null, HttpMethod.DELETE);
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
