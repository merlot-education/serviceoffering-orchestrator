package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingState;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
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
import org.hibernate.ObjectNotFoundException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        Map<String, Object> loginResult = parser.parseMap(response);
        return loginResult;
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
            throw new ResponseStatusException(NOT_FOUND, "No service offering with this id was found.");
        }

        ServiceOfferingExtension extension = serviceOfferingExtensionRepository.findById(id).orElse(null);
        if (extension != null) {
            if (!representedOrgaIds.contains(extension.getIssuer().replace("Participant:", ""))) {
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

        String response = restCallAuthenticated(
                gxfscatalogSelfdescriptionsUri + "?withContent=true&ids=" + id, null,
                null, HttpMethod.GET);

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse<ServiceOfferingCredentialSubject> selfDescriptionsResponse = mapper.readValue(response, new TypeReference<>() {});

        // if we do not get exactly one item or the id doesnt start with ServiceOffering, we did not find the correct item
        if (selfDescriptionsResponse.getTotalCount() != 1
                || !selfDescriptionsResponse.getItems().get(0).getMeta().getId().startsWith("ServiceOffering:")) {
            throw new ResponseStatusException(NOT_FOUND, "No valid service offering with this id was found.");
        }

        String sdType = selfDescriptionsResponse.getItems().get(0).getMeta().getContent()
                .getVerifiableCredential().getCredentialSubject().getType();

        if (sdType.equals("merlot:MerlotServiceOfferingSaaS")) {
            SelfDescriptionsResponse<SaaSCredentialSubject> sdResponse = mapper.readValue(response, new TypeReference<>() {});
            SelfDescriptionItem<SaaSCredentialSubject> item = sdResponse.getItems().get(0);

            // map the response to a detailed model
            return new ServiceOfferingDetailedModel<>(
                    item,
                    serviceOfferingExtensionRepository.findById(item.getMeta().getId()).orElse(null)
            );
        } else if (sdType.equals("merlot:MerlotServiceOfferingDataDelivery")) {
            SelfDescriptionsResponse<DataDeliveryCredentialSubject> sdResponse = mapper.readValue(response, new TypeReference<>() {});
            SelfDescriptionItem<DataDeliveryCredentialSubject> item = sdResponse.getItems().get(0);

            // map the response to a detailed model
            return new ServiceOfferingDetailedModel<>(
                    item,
                    serviceOfferingExtensionRepository.findById(item.getMeta().getId()).orElse(null)
            );
        } else {
            throw new ResponseStatusException(NOT_FOUND, "No valid service offering with this id was found.");
        }
    }

    public List<ServiceOfferingBasicModel> getAllPublicServiceOfferings() throws Exception {

        String response = restCallAuthenticated(
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=ACTIVE", null,
                null, HttpMethod.GET);

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse<ServiceOfferingCredentialSubject> selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);

        // extract the items from the SelfDescriptionsResponse and map them to ServiceOfferingBasicModel instances

        return selfDescriptionsResponse.getItems().stream()
                .filter(item -> item.getMeta().getId().startsWith("ServiceOffering:"))
                .filter(item -> serviceOfferingExtensionRepository.existsById(item.getMeta().getId()))
                .filter(item -> serviceOfferingExtensionRepository.findById(item.getMeta().getId()).orElse(null)
                        .getState() == ServiceOfferingState.RELEASED)
                .map(item -> new ServiceOfferingBasicModel(
                        item,
                        serviceOfferingExtensionRepository.findById(item.getMeta().getId()).orElse(null)
                ))
                .toList();
    }

    public List<ServiceOfferingBasicModel> getOrganizationServiceOfferings(String orgaId) throws Exception {
        String response = restCallAuthenticated(
                gxfscatalogSelfdescriptionsUri + "?withContent=true&statuses=REVOKED,ACTIVE,DEPRECATED", null,
                null, HttpMethod.GET);

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse<ServiceOfferingCredentialSubject> selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);

        // extract the items from the SelfDescriptionsResponse and map them to ServiceOfferingBasicModel instances

        return selfDescriptionsResponse.getItems().stream()
                .filter(item -> item.getMeta().getId().startsWith("ServiceOffering:")
                        && item.getMeta().getIssuer().replace("Participant:", "").equals(orgaId))
                .filter(item -> serviceOfferingExtensionRepository.existsById(item.getMeta().getId()))
                .map(item -> new ServiceOfferingBasicModel(
                        item,
                        serviceOfferingExtensionRepository.findById(item.getMeta().getId()).orElse(null)
                ))
                .toList();
    }

    public SelfDescriptionsCreateResponse addServiceOffering(ServiceOfferingCredentialSubject credentialSubject) throws Exception {

        if (!credentialSubject.isMerlotTermsAndConditionsAccepted()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot process Self-Description as MERLOT terms and conditions were not accepted.");
        }

        if (!credentialSubject.getId().equals("ServiceOffering:TBR") && serviceOfferingExtensionRepository.existsById(credentialSubject.getId())) {
            ServiceOfferingExtension extension = serviceOfferingExtensionRepository
                    .findById(credentialSubject.getId()).orElse(null);
            if (extension != null && extension.getState() != ServiceOfferingState.IN_DRAFT)
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Cannot update Self-Description as it is not in draft");
        } else {
            // override specified time to correspond to the current time and generate an ID
            credentialSubject.setCreationDate(new StringTypeValue(
                    ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT )));
            credentialSubject.setId("ServiceOffering:" + UUID.randomUUID());
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String credentialSubjectJson = mapper.writeValueAsString(credentialSubject);


        String signedVp = presentAndSign(credentialSubjectJson, credentialSubject.getOfferedBy().getId());

        String response = restCallAuthenticated(gxfscatalogSelfdescriptionsUri, signedVp,
                MediaType.APPLICATION_JSON, HttpMethod.POST);

        // create a mapper to map the response to the SelfDescriptionsCreateResponse class
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsCreateResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsCreateResponse.class);

        addServiceOfferingExtension(selfDescriptionsResponse); // TODO load from db if exists

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
        response = StringEscapeUtils.unescapeJson(response)
                .replace("\"{", "{")
                .replace("}\"", "}");

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

    /**
     * Saves a given selfDescriptionsResponse in a ServiceOfferingExtension item in the local database.
     * It will also update an existing field if a service offering of this id was already saved
     * as the catalog also accepts existing ids (and makes the previous entry deprecated in the gxfs catalog)
     * @param selfDescriptionsResponse Object encapsulating the response of adding the service offering to the catalog
     */
    @Transactional
    private void addServiceOfferingExtension(SelfDescriptionsCreateResponse selfDescriptionsResponse) {
        ServiceOfferingExtension extension = new ServiceOfferingExtension(
                selfDescriptionsResponse.getId(),
                selfDescriptionsResponse.getSdHash(),
                selfDescriptionsResponse.getIssuer()
        );
        serviceOfferingExtensionRepository.save(extension);
    }

}
