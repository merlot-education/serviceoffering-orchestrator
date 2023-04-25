package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionsResponse;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingBasicModel;
import eu.merloteducation.serviceofferingorchestrator.models.orchestrator.ServiceOfferingDetailedModel;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class GXFSCatalogRestService {
    @Autowired
    private RestTemplate restTemplate;

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

    public ServiceOfferingDetailedModel getServiceOfferingById(String id) throws Exception {

        // basic input sanitization
        id = Jsoup.clean(id, Safelist.basic());

        // log in as the gxfscatalog user and add the token to the header
        Map<String, Object> gxfscatalogLoginResponse = loginGXFScatalog();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + gxfscatalogLoginResponse.get("access_token"));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);

        // get on the self-description endpoint of the gxfs catalog to get all enrolled participants
        String response = restTemplate.exchange(gxfscatalogSelfdescriptionsUri + "?withContent=true&ids=" + id,
                HttpMethod.GET, request, String.class).getBody();

        // as the catalog returns nested but escaped jsons, we need to manually unescape to properly use it
        response = StringEscapeUtils.unescapeJson(response)
                .replace("\"{", "{")
                .replace("}\"", "}");

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);

        System.out.println(selfDescriptionsResponse.getTotalCount());

        // if we do not get exactly one item or the id doesnt start with ServiceOffering, we did not find the correct item
        if (selfDescriptionsResponse.getTotalCount() != 1
                || !selfDescriptionsResponse.getItems().get(0).getMeta().getId().startsWith("ServiceOffering:")) {
            throw new ResponseStatusException(NOT_FOUND, "No service offering with this id was found.");
        }

        // map the response to a detailed model
        ServiceOfferingDetailedModel model = new ServiceOfferingDetailedModel(selfDescriptionsResponse.getItems().get(0));

        // log out with the gxfscatalog user
        this.logoutGXFScatalog((String) gxfscatalogLoginResponse.get("refresh_token"));
        return model;
    }

    public List<ServiceOfferingBasicModel> getAllPublicServiceOfferings() throws Exception {

        // log in as the gxfscatalog user and add the token to the header
        Map<String, Object> gxfscatalogLoginResponse = loginGXFScatalog();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + gxfscatalogLoginResponse.get("access_token"));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);

        // get on the self-description endpoint of the gxfs catalog to get all enrolled participants
        String response = restTemplate.exchange(gxfscatalogSelfdescriptionsUri + "?withContent=true",
                HttpMethod.GET, request, String.class).getBody();

        // as the catalog returns nested but escaped jsons, we need to manually unescape to properly use it
        response = StringEscapeUtils.unescapeJson(response)
                .replace("\"{", "{")
                .replace("}\"", "}");

        // create a mapper to map the response to the SelfDescriptionResponse class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SelfDescriptionsResponse selfDescriptionsResponse = mapper.readValue(response, SelfDescriptionsResponse.class);

        // extract the items from the SelfDescriptionsResponse and map them to ServiceOfferingBasicModel instances
        List<ServiceOfferingBasicModel> publicServiceOfferings = selfDescriptionsResponse.getItems().stream()
                .filter(item -> item.getMeta().getId().startsWith("ServiceOffering:")
                                && item.getMeta().getStatus().equals("active"))
                .map(ServiceOfferingBasicModel::new).toList();

        // log out with the gxfscatalog user
        this.logoutGXFScatalog((String) gxfscatalogLoginResponse.get("refresh_token"));
        return publicServiceOfferings;
    }

}
