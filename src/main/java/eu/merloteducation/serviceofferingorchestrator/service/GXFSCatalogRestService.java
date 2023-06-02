package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsCreateResponse;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingBasicModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class GXFSCatalogRestService {
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

    private void logoutGXFScatalog(String refreshToken) throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, null);
        restTemplate.postForObject(keycloakLogoutUri, request, String.class);
    }

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
                    case DELETED, ARCHIVED -> extension.delete(); // TODO also set status to revoked in catalog
                }
                serviceOfferingExtensionRepository.save(extension);
            } catch (IllegalStateException e) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid state transition requested.");
            }
        }

    }

    public ServiceOfferingDetailedModel getServiceOfferingById(String id) throws Exception {
        // basic input sanitization
        id = Jsoup.clean(id, Safelist.basic());


        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);

        if (extension == null) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }

        String response = restCallAuthenticated(
                gxfscatalogSelfdescriptionsUri + "?withContent=true&ids=" + id, null,
                null, HttpMethod.GET);

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse<ServiceOfferingCredentialSubject> selfDescriptionsResponse = mapper.readValue(response, new TypeReference<>() {});

        // if we do not get exactly one item or the id doesnt start with ServiceOffering, we did not find the correct item
        if (selfDescriptionsResponse.getTotalCount() != 1
                || !selfDescriptionsResponse.getItems().get(0).getMeta().getId().startsWith(OFFERING_START)) {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }

        String sdType = selfDescriptionsResponse.getItems().get(0).getMeta().getContent()
                .getVerifiableCredential().getCredentialSubject().getType();

        if (sdType.equals("merlot:MerlotServiceOfferingSaaS")) {
            SelfDescriptionsResponse<SaaSCredentialSubject> sdResponse = mapper.readValue(response, new TypeReference<>() {});
            SelfDescriptionItem<SaaSCredentialSubject> item = sdResponse.getItems().get(0);

            // map the response to a detailed model
            return new ServiceOfferingDetailedModel<>(
                    item,
                    extension
            );
        } else if (sdType.equals("merlot:MerlotServiceOfferingDataDelivery")) {
            SelfDescriptionsResponse<DataDeliveryCredentialSubject> sdResponse = mapper.readValue(response, new TypeReference<>() {});
            SelfDescriptionItem<DataDeliveryCredentialSubject> item = sdResponse.getItems().get(0);

            // map the response to a detailed model
            return new ServiceOfferingDetailedModel<>(
                    item,
                    extension
            );
        } else {
            throw new ResponseStatusException(NOT_FOUND, OFFERING_NOT_FOUND);
        }
    }

    public Page<ServiceOfferingBasicModel> getAllPublicServiceOfferings(Pageable pageable) throws Exception {

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
        SelfDescriptionsResponse<ServiceOfferingCredentialSubject> selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);

        if (selfDescriptionsResponse.getTotalCount() != extensions.getNumberOfElements()) {
            logger.warn("Inconsistent state detected, there are service offerings in the local database that are not in the catalog.");
        }

        // extract the items from the SelfDescriptionsResponse and map them to ServiceOfferingBasicModel instances
        List<ServiceOfferingBasicModel> models = selfDescriptionsResponse.getItems().stream()
                .map(item -> new ServiceOfferingBasicModel(item, extensionMap.get(item.getMeta().getSdHash())))
                .sorted(Comparator.comparing(offer -> offer.getCreationDate() != null
                                ? (LocalDateTime.parse(offer.getCreationDate(), DateTimeFormatter.ISO_DATE_TIME))
                                : LocalDateTime.MIN,
                        Comparator.reverseOrder()))  // since the catalog does not respect the order of the hashes, we need to reorder again
                .toList();

        return new PageImpl<>(models, pageable, extensions.getTotalElements());
    }

    public Page<ServiceOfferingBasicModel> getOrganizationServiceOfferings(String orgaId, ServiceOfferingState state, Pageable pageable) throws Exception {
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
                gxfscatalogSelfdescriptionsUri + "?withContent=true&hashes=" + extensionHashes, null,
                null, HttpMethod.GET);

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse<ServiceOfferingCredentialSubject> selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);
        if (selfDescriptionsResponse.getTotalCount() != extensions.getNumberOfElements()) {
            logger.warn("Inconsistent state detected, there are service offerings in the local database that are not in the catalog.");
        }

        // extract the items from the SelfDescriptionsResponse and map them to ServiceOfferingBasicModel instances
        List<ServiceOfferingBasicModel> models = selfDescriptionsResponse.getItems().stream()
                .map(item -> new ServiceOfferingBasicModel(item, extensionMap.get(item.getMeta().getSdHash())))
                .sorted(Comparator.comparing(offer -> offer.getCreationDate() != null
                                ? (LocalDateTime.parse(offer.getCreationDate(), DateTimeFormatter.ISO_DATE_TIME))
                                : LocalDateTime.MIN,
                        Comparator.reverseOrder()))  // since the catalog does not respect the order of the hashes, we need to reorder again
                .toList();

        return new PageImpl<>(models, pageable, extensions.getTotalElements());
    }

    @Transactional
    public SelfDescriptionsCreateResponse addServiceOffering(ServiceOfferingCredentialSubject credentialSubject) throws Exception {
        if (!credentialSubject.isMerlotTermsAndConditionsAccepted()) {
            // Merlot TnC not accepted
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot process Self-Description as MERLOT terms and conditions were not accepted.");
        }
        ServiceOfferingExtension extension;
        String previousSdHash = null;
        if (credentialSubject.getId().equals(OFFERING_START + "TBR")) {
            // override creation time time to correspond to the current time and generate an ID
            extension = new ServiceOfferingExtension();
            credentialSubject.setCreationDate(new StringTypeValue(
                    extension.getCreationDate().format( DateTimeFormatter.ISO_INSTANT )));
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
                        extension.getCreationDate().format( DateTimeFormatter.ISO_INSTANT )));
            } else {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description there is none with this id");
            }
        }

        // prepare a json to send to the gxfs catalog, sign it and read the response
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String credentialSubjectJson = mapper.writeValueAsString(credentialSubject);

        String signedVp = presentAndSign(credentialSubjectJson, credentialSubject.getOfferedBy().getId());

        String response = restCallAuthenticated(gxfscatalogSelfdescriptionsUri, signedVp,
                MediaType.APPLICATION_JSON, HttpMethod.POST);

        // delete previous entry if it exists
        if (previousSdHash != null) {
            restCallAuthenticated(gxfscatalogSelfdescriptionsUri + "/" + previousSdHash, null,
                    null, HttpMethod.DELETE);
        }

        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsCreateResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsCreateResponse.class);

        // with a successful response (i.e. no exception was thrown) we are good to save the new or updated self description
        extension.setId(selfDescriptionsResponse.getId());
        extension.setIssuer(selfDescriptionsResponse.getIssuer());
        extension.setCurrentSdHash(selfDescriptionsResponse.getSdHash());
        serviceOfferingExtensionRepository.save(extension);

        return selfDescriptionsResponse;
    }

    private String restCallAuthenticated(String url, String body, MediaType mediaType, HttpMethod method) throws Exception {
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
                        "issuanceDate": "2022-10-19T18:48:09Z",
                        "credentialSubject":\s""" + credentialSubjectJson + """
                    }
                }
                """;

        return gxfsSignerService.signVerifiablePresentation(vp);
    }


}
